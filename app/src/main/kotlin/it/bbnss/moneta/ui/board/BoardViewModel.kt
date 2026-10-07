package it.bbnss.moneta.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.bbnss.moneta.core.data.RateRepository
import it.bbnss.moneta.core.data.SettingsStore
import it.bbnss.moneta.core.model.AmountFormat
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.CurrencyMetadata
import it.bbnss.moneta.core.model.Freshness
import it.bbnss.moneta.core.model.calc.Expression
import it.bbnss.moneta.core.ui.components.KeypadKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/** Una valuta preferita con l'importo già convertito e formattato. */
data class BoardRow(
    val currency: Currency,
    val amount: BigDecimal?,
    val formatted: String,
    val name: String,
    val flag: String?,
)

data class BoardUiState(
    val base: Currency = Currency.EUR,
    val input: String = "",
    val inputText: String = "",
    val rows: List<BoardRow> = emptyList(),
    val hasFavourites: Boolean = false,
    val freshness: Freshness? = null,
    val age: Duration? = null,
    val updatedAt: Instant? = null,
    val rateDate: java.time.LocalDate? = null,
    /** Valute coperte dalla fonte, per il selettore di aggiunta. */
    val availableCurrencies: List<Currency> = emptyList(),
    val favourites: Set<Currency> = emptySet(),
)

/**
 * Un importo, tutte le valute preferite convertite insieme.
 *
 * È la richiesta più votata sul progetto concorrente principale, in due issue
 * distinte, e risponde a come si ragiona davvero in viaggio: non "quanto fa in
 * euro" ma "quanto fa in tutte le valute che mi riguardano adesso".
 */
class BoardViewModel(
    private val repository: RateRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val input = MutableStateFlow("1")
    private var initialInput = true
    private var restored = false

    init { viewModelScope.launch {
        val saved = settings.boardCalculation.first()
        input.value = saved.input
        initialInput = saved.initial
        restored = true
    } }

    fun onPaste(text: String, locale: java.util.Locale): Boolean {
        val amount = AmountFormat.parse(text, locale) ?: return false
        input.value = amount.toPlainString()
        initialInput = false
        persistCalculation()
        return true
    }

    private fun persistCalculation() {
        val typed = input.value
        val initial = initialInput
        viewModelScope.launch { settings.setBoardCalculation(typed, initial) }
    }

    fun onMove(currency: Currency, direction: Int) {
        viewModelScope.launch {
            val favourites = settings.favourites.first()
            settings.setFavourites(it.bbnss.moneta.core.model.FavouriteOrder.move(
                favourites, currency, settings.baseCurrency.first(), direction))
        }
    }

    fun requestedCurrencies(): Set<Currency> = state.value.favourites + state.value.base

    val state: StateFlow<BoardUiState> = combine(
        repository.state,
        settings.baseCurrency,
        settings.favourites,
        input,
    ) { rates, base, favourites, typed ->
        val snapshot = rates.snapshotFor(favourites + base)
        val amount = (Expression.evaluate(typed) as? Expression.Result.Value)?.amount

        // La valuta dell'importo non si ripete nell'elenco: è già in cima.
        val targets = favourites.filter { it != base }

        val rows = targets.map { currency ->
            val pairSnapshot = rates.snapshotFor(listOf(base, currency))
            val converted = if (amount != null && pairSnapshot != null) {
                pairSnapshot.convert(amount, base, currency)
            } else {
                null
            }
            BoardRow(
                currency = currency,
                amount = converted,
                formatted = converted?.let { AmountFormat.format(it, currency) }.orEmpty(),
                name = CurrencyMetadata.of(currency).displayName,
                flag = CurrencyMetadata.flagOf(currency),
            )
        }

        BoardUiState(
            base = base,
            input = typed,
            inputText = when {
                typed.isEmpty() -> ""
                Expression.isPlainNumber(typed) -> AmountFormat.groupTypedNumber(typed)
                else -> typed
            },
            rows = rows,
            hasFavourites = favourites.isNotEmpty(),
            freshness = it.bbnss.moneta.core.model.FreshnessRules.of(snapshot?.let { row ->
                targets.map { row.dateFor(base, it) }.takeIf { dates -> dates.all { it != null } }?.filterNotNull()?.minOrNull()
            }, rates.today),
            age = it.bbnss.moneta.core.model.FreshnessRules.rateAge(snapshot?.let { row ->
                targets.map { row.dateFor(base, it) }.takeIf { dates -> dates.all { it != null } }?.filterNotNull()?.minOrNull()
            }, rates.today),
            rateDate = snapshot?.let { row -> targets.map { row.dateFor(base, it) }
                .takeIf { dates -> dates.all { it != null } }?.filterNotNull()?.minOrNull() },
            updatedAt = snapshot?.fetchedAt,
            availableCurrencies = rates.snapshots.flatMap { it.currencies }.distinct().sortedBy { it.code },
            favourites = favourites.toSet(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BoardUiState(),
    )

    fun onKey(key: KeypadKey) {
        if (!restored) return
        if (initialInput && key is KeypadKey.Symbol) input.value = ""
        initialInput = false
        when (key) {
            is KeypadKey.Symbol -> input.value += key.value
            KeypadKey.Backspace -> input.value = input.value.dropLast(1)
            KeypadKey.Clear -> input.value = ""
            KeypadKey.Equals -> {
                val result = Expression.evaluate(input.value)
                if (result is Expression.Result.Value) {
                    input.value = result.amount.stripTrailingZeros().toPlainString()
                }
            }
        }
        persistCalculation()
    }

    /**
     * Promuove una riga a valuta dell'importo.
     *
     * Serve al gesto più frequente in viaggio: si guarda quanto valgono 100
     * euro in dong, poi si vuole sapere l'inverso senza ridigitare nulla.
     */
    /** Aggiunge o toglie una valuta dall'elenco, direttamente da questa schermata. */
    fun onToggleFavourite(currency: Currency) {
        viewModelScope.launch {
            val current = settings.favourites.first()
            settings.setFavourites(
                if (currency in current) current.filter { it != currency } else current + currency,
            )
        }
    }

    fun onSetAsBase(currency: Currency) {
        viewModelScope.launch {
            val previousBase = settings.baseCurrency.first()
            if (previousBase == currency) return@launch

            settings.setPair(currency, settings.quoteCurrency.first())

            // La valuta che lascia il posto entra fra le preferite: altrimenti
            // sparirebbe dall'elenco proprio mentre la si stava guardando.
            val favourites = settings.favourites.first()
            if (previousBase !in favourites) {
                settings.setFavourites(favourites + previousBase)
            }
        }
    }

    class Factory(
        private val repository: RateRepository,
        private val settings: SettingsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BoardViewModel(repository, settings) as T
    }
}
