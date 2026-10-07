package it.bbnss.moneta.ui.ratecard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.bbnss.moneta.core.data.RateRepository
import it.bbnss.moneta.core.data.SettingsStore
import it.bbnss.moneta.core.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

data class RateCardRow(val denomination: BigDecimal, val localText: String, val homeText: String, val count: Int = 0)
data class RateCardUiState(
    val local: Currency = Currency.EUR,
    val home: Currency = Currency.USD,
    val rows: List<RateCardRow> = emptyList(),
    val estimated: Boolean = false,
    val rate: BigDecimal? = null,
    val provider: ProviderId? = null,
    val rateDate: LocalDate? = null,
    val freshness: Freshness? = null,
    val age: Duration? = null,
    val updatedAt: Instant? = null,
    val supported: Boolean = true,
    val availableCurrencies: List<Currency> = emptyList(),
    val favourites: Set<Currency> = emptySet(),
    val totalLocal: String = "0",
    val totalHome: String = "",
)

class RateCardViewModel(private val repository: RateRepository, private val settings: SettingsStore) : ViewModel() {
    init { viewModelScope.launch { settings.ensureCashPair() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pairCounts = settings.cashPair.flatMapLatest { pair ->
        settings.cashCounts(pair.first).map { pair to it }
    }
    val state: StateFlow<RateCardUiState> = combine(repository.state, pairCounts, settings.favourites) { allRates, pairCounts, favourites ->
        val (pair, counts) = pairCounts
        val (local, home) = pair
        val rates = allRates.forPair(local, home)
        val snapshot = rates.snapshot
        val rate = snapshot?.crossRate(local, home)
        val denominations = Denominations.of(local, snapshot?.crossRate(home, local))
        // Preserve counted denominations if estimated denominations change with the exchange rate.
        val notes = (denominations + counts.filterValues { it > 0 }.keys).distinct().sorted()
        val rows = notes.map { note ->
            RateCardRow(note, AmountFormat.format(note, local),
                rate?.let { AmountFormat.format(note.multiply(it, MonetaryMath.CONTEXT), home) }.orEmpty(), counts[note] ?: 0)
        }
        val total = CashCounter.total(counts)
        RateCardUiState(local, home, rows, !Denominations.areKnown(local), rate, snapshot?.provider,
            snapshot?.dateFor(local, home), rates.freshness, rates.age, snapshot?.fetchedAt, rate != null,
            allRates.snapshots.flatMap { it.currencies }.distinct().sortedBy { it.code }, favourites.toSet(),
            AmountFormat.format(total, local), rate?.let { AmountFormat.format(total.multiply(it, MonetaryMath.CONTEXT), home) }.orEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RateCardUiState())

    fun onCurrencySelected(localField: Boolean, currency: Currency) { viewModelScope.launch {
        val (local, home) = settings.cashPair.first()
        settings.setCashPair(if (localField) currency else local, if (localField) home else currency)
    } }
    fun onSwap() { viewModelScope.launch { val (local, home) = settings.cashPair.first(); settings.setCashPair(home, local) } }
    fun onCount(note: BigDecimal, delta: Int) { viewModelScope.launch {
        val local = state.value.local
        settings.adjustCashCount(local, note, delta)
    } }
    fun onReset() { viewModelScope.launch { settings.resetCashCounts(state.value.local) } }
    fun onToggleFavourite(currency: Currency) { viewModelScope.launch {
        val current = settings.favourites.first()
        settings.setFavourites(if (currency in current) current - currency else current + currency)
    } }
    fun requestedCurrencies() = setOf(state.value.local, state.value.home)

    class Factory(private val repository: RateRepository, private val settings: SettingsStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RateCardViewModel(repository, settings) as T
    }
}
