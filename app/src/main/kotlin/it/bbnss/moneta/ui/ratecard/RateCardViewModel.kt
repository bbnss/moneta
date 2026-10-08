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

data class RateCardRow(val denomination: BigDecimal, val localText: String, val homeText: String,
                       val count: Int = 0, val documented: Boolean = true)
data class RateCardUiState(
    val local: Currency = Currency.EUR,
    val home: Currency = Currency.USD,
    val rows: List<RateCardRow> = emptyList(),
    val counterRows: List<RateCardRow> = emptyList(),
    val catalog: Denominations.Catalog? = null,
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
        val cash = CashRows.build(Denominations.of(local), Denominations.illustrativeAmounts(local), counts)
        fun row(note: BigDecimal, count: Int = 0, documented: Boolean = true) =
            RateCardRow(note, AmountFormat.format(note, local),
                rate?.let { AmountFormat.format(note.multiply(it, MonetaryMath.CONTEXT), home) }.orEmpty(), count, documented)
        RateCardUiState(local = local, home = home, rows = cash.table.map { row(it) },
            counterRows = cash.counter.map { row(it.value, it.count, it.documented) }, catalog = Denominations.catalog(local),
            estimated = !Denominations.areKnown(local), rate = rate, provider = snapshot?.provider,
            rateDate = snapshot?.dateFor(local, home), freshness = rates.freshness, age = rates.age,
            updatedAt = snapshot?.fetchedAt, supported = rate != null,
            availableCurrencies = (allRates.snapshots.flatMap { it.currencies } + local + home).distinct().sortedBy { it.code },
            favourites = favourites.toSet(), totalLocal = AmountFormat.format(cash.total, local),
            totalHome = rate?.let { AmountFormat.format(cash.total.multiply(it, MonetaryMath.CONTEXT), home) }.orEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RateCardUiState())

    fun onCurrencySelected(localField: Boolean, currency: Currency) { viewModelScope.launch {
        val (local, home) = settings.cashPair.first()
        settings.setCashPair(if (localField) currency else local, if (localField) home else currency)
    } }
    fun onSwap() { viewModelScope.launch { val (local, home) = settings.cashPair.first(); settings.setCashPair(home, local) } }
    fun onCount(note: BigDecimal, delta: Int) { viewModelScope.launch {
        val local = state.value.local
        val documented = Denominations.of(local).any { it.compareTo(note) == 0 }
        if (delta > 0 && !documented) return@launch
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
