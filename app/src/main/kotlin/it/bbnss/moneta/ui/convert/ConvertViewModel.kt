package it.bbnss.moneta.ui.convert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.bbnss.moneta.core.data.LocalCurrencyDetector
import it.bbnss.moneta.core.data.RateRepository
import it.bbnss.moneta.core.data.RatesState
import it.bbnss.moneta.core.data.RefreshResult
import it.bbnss.moneta.core.data.SettingsStore
import it.bbnss.moneta.core.model.AmountFormat
import it.bbnss.moneta.core.model.CountryCurrencies
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.Freshness
import it.bbnss.moneta.core.model.MonetaryMath
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.calc.Expression
import it.bbnss.moneta.core.ui.components.KeypadKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

enum class Field { FROM, TO }

/** Errori mostrati sotto il risultato, tutti recuperabili. */
sealed interface ConvertError {
    data object DivisionByZero : ConvertError
    data class UnsupportedPair(val provider: ProviderId) : ConvertError
}

/** Esito dell'ultimo aggiornamento, da mostrare come messaggio temporaneo. */
sealed interface RefreshMessage {
    data class Updated(val provider: ProviderId) : RefreshMessage
    data object Failed : RefreshMessage
    data object Offline : RefreshMessage
}

/** Proposta di impostare la valuta del paese in cui sembra trovarsi l'utente. */
data class LocalSuggestion(
    val countryCode: String,
    val countryName: String,
    val currency: Currency,
)

data class ConvertUiState(
    val from: Currency = Currency.EUR,
    val to: Currency = Currency.USD,
    val activeField: Field = Field.FROM,
    /** Testo grezzo digitato, che appartiene sempre al campo attivo. */
    val input: String = "",
    val fromText: String = "",
    val toText: String = "",
    /** Risultato al netto della commissione, se ne è stata impostata una. */
    val withFeeText: String? = null,
    val markupPercent: BigDecimal = BigDecimal.ZERO,
    val rate: BigDecimal? = null,
    val freshness: Freshness? = null,
    val age: Duration? = null,
    val updatedAt: Instant? = null,
    val rateDate: LocalDate? = null,
    val provider: ProviderId? = null,
    val preferredProvider: ProviderId = ProviderId.FRANKFURTER,
    val substituted: Boolean = false,
    val error: ConvertError? = null,
    val refreshing: Boolean = false,
    val message: RefreshMessage? = null,
    val availableCurrencies: List<Currency> = emptyList(),
    val favourites: Set<Currency> = emptySet(),
    val suggestion: LocalSuggestion? = null,
)

class ConvertViewModel(
    private val repository: RateRepository,
    private val settings: SettingsStore,
    private val detector: LocalCurrencyDetector,
) : ViewModel() {

    private val input = MutableStateFlow("")
    private val activeField = MutableStateFlow(Field.FROM)
    private val refreshing = MutableStateFlow(false)
    private val message = MutableStateFlow<RefreshMessage?>(null)
    private val detectedCountry = MutableStateFlow<String?>(null)

    private data class Preferences(
        val base: Currency,
        val quote: Currency,
        val favourites: Set<Currency>,
        val dismissedCountry: String?,
        val markup: BigDecimal,
    )

    private data class Data(
        val rates: RatesState,
        val preferences: Preferences,
        val country: String?,
    )

    private val preferences = combine(
        settings.baseCurrency,
        settings.quoteCurrency,
        settings.favourites,
        settings.dismissedCountry,
        settings.markupPercent,
    ) { base, quote, favourites, dismissed, markup ->
        Preferences(base, quote, favourites.toSet(), dismissed, markup)
    }

    private val data = combine(
        repository.state,
        preferences,
        detectedCountry,
    ) { rates, prefs, country -> Data(rates, prefs, country) }

    val state: StateFlow<ConvertUiState> = combine(
        data,
        input,
        activeField,
        refreshing,
        message,
    ) { current, typed, field, isRefreshing, lastMessage ->
        buildState(current, typed, field, isRefreshing, lastMessage)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConvertUiState(),
    )

    init {
        viewModelScope.launch {
            // I tassi inclusi nell'APK vanno caricati prima di qualunque
            // tentativo di rete: la prima schermata non deve mai essere vuota.
            repository.ensureSeeded()

            // Il rilevamento legge dai servizi di sistema: fuori dal thread
            // principale, e una volta sola per sessione.
            detectedCountry.value = withContext(Dispatchers.IO) {
                detector.detectLocal()?.code
            }

            refreshIfStale()
        }
    }

    fun onKey(key: KeypadKey) {
        when (key) {
            is KeypadKey.Symbol -> append(key.value)
            KeypadKey.Backspace -> input.value = input.value.dropLast(1)
            KeypadKey.Clear -> input.value = ""
            KeypadKey.Equals -> collapseToResult()
        }
    }

    fun onFieldSelected(field: Field) {
        if (activeField.value == field) return
        // Passando all'altro campo il testo riparte: si sta per digitare un
        // importo nuovo, non per continuare il calcolo precedente.
        activeField.value = field
        input.value = ""
    }

    fun onCurrencySelected(field: Field, currency: Currency) {
        viewModelScope.launch {
            val current = state.value
            when (field) {
                // Scegliere da un lato la valuta che sta già dall'altro non deve
                // produrre "1 PHP = 1 PHP", che non dice nulla: le due valute si
                // scambiano, che è quasi sempre ciò che si voleva davvero.
                Field.FROM -> when (currency) {
                    current.to -> settings.setPair(currency, current.from)
                    else -> settings.setPair(currency, current.to)
                }

                Field.TO -> when (currency) {
                    current.from -> settings.setPair(current.to, currency)
                    else -> settings.setPair(current.from, currency)
                }
            }
        }
    }

    fun onSwap() {
        viewModelScope.launch {
            val current = state.value
            // Si scambiano solo le valute: l'importo digitato resta nel campo
            // dov'è, e cambia il significato di quel numero. Spostare anche il
            // campo attivo farebbe migrare la cifra da un riquadro all'altro,
            // che è disorientante proprio nel momento in cui si sta guardando.
            settings.setPair(current.to, current.from)
        }
    }

    /** Accetta il suggerimento: la valuta locale diventa quella di partenza. */
    fun onAcceptSuggestion() {
        val suggestion = state.value.suggestion ?: return
        viewModelScope.launch {
            settings.setPair(suggestion.currency, state.value.to)
            settings.dismissCountrySuggestion(suggestion.countryCode)
        }
    }

    fun onDismissSuggestion() {
        val suggestion = state.value.suggestion ?: return
        viewModelScope.launch { settings.dismissCountrySuggestion(suggestion.countryCode) }
    }

    fun onMarkupChanged(percent: BigDecimal) {
        viewModelScope.launch { settings.setMarkupPercent(percent) }
    }

    fun onToggleFavourite(currency: Currency) {
        viewModelScope.launch {
            val current = state.value.favourites
            settings.setFavourites(
                if (currency in current) current - currency else current + currency,
            )
        }
    }

    fun onRefresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            message.value = when (val result = repository.refresh()) {
                is RefreshResult.Updated -> RefreshMessage.Updated(result.provider)
                is RefreshResult.Failed -> RefreshMessage.Failed
                RefreshResult.SkippedOffline -> RefreshMessage.Offline
            }
            refreshing.value = false
        }
    }

    fun onMessageShown() {
        message.value = null
    }

    /**
     * Aggiorna solo se serve.
     *
     * Aprire il convertitore cinque secondi al mercato non deve costare una
     * richiesta di rete: in roaming il traffico si paga, e i tassi di
     * riferimento cambiano una volta al giorno.
     */
    private suspend fun refreshIfStale() {
        if (settings.offlineMode.first()) return
        val age = repository.ageOfNewestData()
        if (age == null || age > Duration.ofHours(6)) onRefresh()
    }

    private fun append(symbol: Char) {
        val current = input.value
        val isOperator = symbol in "+-×÷*/"

        input.value = when {
            // Due operatori di fila: si sostituisce il precedente invece di
            // rifiutare il tasto, che è ciò che si aspetta chi si è corretto.
            isOperator && current.isNotEmpty() && current.last() in "+-×÷*/" ->
                current.dropLast(1) + symbol

            isOperator && current.isEmpty() && symbol != '-' -> current

            else -> current + symbol
        }
    }

    /** Sostituisce l'espressione con il suo risultato, come il tasto `=`. */
    private fun collapseToResult() {
        val result = Expression.evaluate(input.value)
        if (result is Expression.Result.Value) {
            input.value = result.amount.stripTrailingZeros().toPlainString()
        }
    }

    private fun buildState(
        current: Data,
        typed: String,
        field: Field,
        isRefreshing: Boolean,
        lastMessage: RefreshMessage?,
    ): ConvertUiState {
        val (rates, prefs, country) = current
        val (base, quote, favourites, dismissed, markup) = prefs
        val snapshot = rates.snapshot
        val evaluated = Expression.evaluate(typed)

        val amount = (evaluated as? Expression.Result.Value)?.amount

        val sourceCurrency = if (field == Field.FROM) base else quote
        val targetCurrency = if (field == Field.FROM) quote else base

        val converted = if (amount != null && snapshot != null) {
            snapshot.convert(amount, sourceCurrency, targetCurrency)
        } else {
            null
        }

        val typedText = when {
            typed.isEmpty() -> ""
            Expression.isPlainNumber(typed) -> AmountFormat.groupTypedNumber(typed)
            else -> typed
        }

        val convertedText = converted?.let { AmountFormat.format(it, targetCurrency) }.orEmpty()

        // La commissione riduce quello che si riceve davvero: il tasso di
        // riferimento non è mai quello che dà lo sportello.
        val withFee = if (markup.signum() > 0 && converted != null) {
            val factor = BigDecimal.ONE.subtract(
                markup.divide(BigDecimal(100), MonetaryMath.CONTEXT),
            )
            AmountFormat.format(
                converted.multiply(factor, MonetaryMath.CONTEXT),
                targetCurrency,
            )
        } else {
            null
        }

        val error = when {
            evaluated is Expression.Result.Invalid &&
                evaluated.reason == Expression.Result.Reason.DIVISION_BY_ZERO ->
                ConvertError.DivisionByZero

            // La coppia non è coperta dalla fonte: succede davvero, per esempio
            // chiedendo il dong alla Banca Centrale Europea.
            snapshot != null && amount != null && converted == null ->
                ConvertError.UnsupportedPair(snapshot.provider)

            else -> null
        }

        return ConvertUiState(
            from = base,
            to = quote,
            activeField = field,
            input = typed,
            fromText = if (field == Field.FROM) typedText else convertedText,
            toText = if (field == Field.TO) typedText else convertedText,
            withFeeText = withFee,
            markupPercent = markup,
            rate = snapshot?.crossRate(base, quote),
            freshness = rates.freshness,
            age = rates.age,
            updatedAt = snapshot?.fetchedAt,
            rateDate = snapshot?.rateDate,
            provider = snapshot?.provider,
            preferredProvider = rates.preferredProvider,
            substituted = rates.substituted,
            error = error,
            refreshing = isRefreshing,
            message = lastMessage,
            availableCurrencies = snapshot?.currencies?.sortedBy { it.code }.orEmpty(),
            favourites = favourites,
            suggestion = suggestionFor(country, dismissed, base, quote, rates),
        )
    }

    /**
     * Il suggerimento compare solo quando è davvero utile.
     *
     * Non si propone la valuta che l'utente sta già usando, non si insiste su
     * un paese per cui ha già detto di no, e non si propone una valuta che la
     * fonte attuale non copre — sarebbe un invito a un vicolo cieco.
     */
    private fun suggestionFor(
        countryCode: String?,
        dismissed: String?,
        base: Currency,
        quote: Currency,
        rates: RatesState,
    ): LocalSuggestion? {
        if (countryCode == null || countryCode == dismissed) return null

        val currency = CountryCurrencies.currencyOf(countryCode) ?: return null
        if (currency == base || currency == quote) return null
        if (rates.snapshot?.supports(currency) != true) return null

        val name = CountryCurrencies.nameOf(countryCode) ?: return null

        return LocalSuggestion(countryCode = countryCode, countryName = name, currency = currency)
    }

    class Factory(
        private val repository: RateRepository,
        private val settings: SettingsStore,
        private val detector: LocalCurrencyDetector,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ConvertViewModel(repository, settings, detector) as T
    }
}
