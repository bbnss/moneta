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
import it.bbnss.moneta.core.model.FeeMode
import it.bbnss.moneta.core.model.Fees
import it.bbnss.moneta.core.model.Freshness
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
    data object Wifi : RefreshMessage
    data object InvalidPaste : RefreshMessage
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
    val feeMode: FeeMode = FeeMode.CASH,
    val quotationDates: Map<Currency, LocalDate?> = emptyMap(),
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

    private val input = MutableStateFlow("1")
    private var initialInput = true
    private var restored = false
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
        val feeMode: FeeMode,
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
        combine(settings.markupPercent, settings.feeMode) { percent, mode -> percent to mode },
    ) { base, quote, favourites, dismissed, markup ->
        Preferences(base, quote, favourites.toSet(), dismissed, markup.first, markup.second)
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
            val saved = settings.calculation.first()
            input.value = saved.input
            activeField.value = runCatching { Field.valueOf(saved.field) }.getOrDefault(Field.FROM)
            initialInput = saved.initial
            restored = true
            repository.ensureSeeded()

            // Il rilevamento legge dai servizi di sistema: fuori dal thread
            // principale, e una volta sola per sessione.
            detectedCountry.value = withContext(Dispatchers.IO) {
                detector.detectLocal()?.code
            }


        }
    }

    private fun persistCalculation() {
        val typed = input.value
        val field = activeField.value.name
        val initial = initialInput
        viewModelScope.launch { settings.setCalculation(typed, field, initial) }
    }

    fun onPaste(field: Field, text: String, locale: java.util.Locale) {
        val amount = AmountFormat.parse(text, locale)
        if (amount == null) { message.value = RefreshMessage.InvalidPaste; return }
        activeField.value = field
        input.value = amount.toPlainString()
        initialInput = false
        persistCalculation()
    }

    fun onFeeModeChanged(mode: FeeMode) {
        viewModelScope.launch { settings.setFeeMode(mode) }
    }

    fun onKey(key: KeypadKey) {
        if (!restored) return
        if (initialInput && key is KeypadKey.Symbol && (key.value.isDigit() || key.value in ".,")) input.value = ""
        initialInput = false
        when (key) {
            is KeypadKey.Symbol -> append(key.value)
            KeypadKey.Backspace -> input.value = input.value.dropLast(1)
            KeypadKey.Clear -> input.value = ""
            KeypadKey.Equals -> collapseToResult()
        }
        persistCalculation()
    }

    fun onFieldSelected(field: Field) {
        if (activeField.value == field) return
        val current = state.value
        val amount = (Expression.evaluate(input.value) as? Expression.Result.Value)?.amount
        val converted = if (amount != null && current.rate != null) {
            Fees.convert(amount, current.rate, current.markupPercent, current.feeMode, inverse = activeField.value == Field.TO)
        } else null
        activeField.value = field
        input.value = converted?.stripTrailingZeros()?.toPlainString().orEmpty()
        initialInput = false
        persistCalculation()
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

    fun onRefresh(required: Set<Currency>? = null) {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            message.value = when (val result = repository.refresh(required ?: setOf(state.value.from, state.value.to))) {
                is RefreshResult.Updated -> RefreshMessage.Updated(result.provider)
                is RefreshResult.Failed -> RefreshMessage.Failed
                RefreshResult.SkippedOffline -> RefreshMessage.Offline
                is RefreshResult.Blocked -> if (result.reason == it.bbnss.moneta.core.model.NetworkBlock.WIFI) RefreshMessage.Wifi else RefreshMessage.Failed
            }
            refreshing.value = false
        }
    }

    fun onMessageShown() {
        message.value = null
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
        val (allRates, prefs, country) = current
        val (base, quote, favourites, dismissed, markup, mode) = prefs
        val rates = allRates.forPair(base, quote)
        val snapshot = rates.snapshot
        val evaluated = Expression.evaluate(typed)

        val amount = (evaluated as? Expression.Result.Value)?.amount

        val sourceCurrency = if (field == Field.FROM) base else quote
        val targetCurrency = if (field == Field.FROM) quote else base

        val converted = if (amount != null && snapshot != null) {
            snapshot.crossRate(base, quote)?.let { rate ->
                Fees.convert(amount, rate, markup, mode, inverse = field == Field.TO)
            }
        } else {
            null
        }

        val typedText = when {
            typed.isEmpty() -> ""
            Expression.isPlainNumber(typed) -> AmountFormat.groupTypedNumber(typed)
            else -> typed
        }

        val convertedText = converted?.let { AmountFormat.format(it, targetCurrency) }.orEmpty()

        val withFee: String? = null

        val error = when {
            evaluated is Expression.Result.Invalid &&
                evaluated.reason == Expression.Result.Reason.DIVISION_BY_ZERO ->
                ConvertError.DivisionByZero

            // La coppia non è coperta dalla fonte: succede davvero, per esempio
            // chiedendo il dong alla Banca Centrale Europea.
            allRates.snapshots.isNotEmpty() && amount != null && converted == null ->
                ConvertError.UnsupportedPair(rates.preferredProvider)

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
            feeMode = mode,
            quotationDates = snapshot?.datesFor(base, quote).orEmpty(),
            rate = snapshot?.crossRate(base, quote),
            freshness = rates.freshness,
            age = rates.age,
            updatedAt = snapshot?.fetchedAt,
            rateDate = snapshot?.dateFor(base, quote),
            provider = snapshot?.provider,
            preferredProvider = rates.preferredProvider,
            substituted = rates.substituted,
            error = error,
            refreshing = isRefreshing,
            message = lastMessage,
            availableCurrencies = allRates.snapshots.flatMap { it.currencies }.distinct().sortedBy { it.code },
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
