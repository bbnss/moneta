package it.bbnss.moneta.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.bbnss.moneta.core.data.RateRepository
import it.bbnss.moneta.core.data.SeriesResult
import it.bbnss.moneta.core.data.SettingsStore
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.HistoryRange
import it.bbnss.moneta.core.model.MonetaryMath
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.RatePoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class HistoryUiState(
    val base: Currency = Currency.EUR,
    val quote: Currency = Currency.USD,
    val range: HistoryRange = HistoryRange.SIX_MONTHS,
    val points: List<RatePoint> = emptyList(),
    val loading: Boolean = true,
    /**
     * Chi ha pubblicato questi numeri.
     *
     * Un grafico senza fonte è un'affermazione senza autore: fonti diverse
     * danno curve diverse per la stessa coppia, e la fonte che ha risposto qui
     * può non essere quella scelta nelle impostazioni — la catena di ricaduta
     * può aver preso un'altra strada.
     */
    val provider: ProviderId? = null,
    /** Perché il grafico è vuoto, quando lo è. */
    val emptyReason: SeriesResult? = null,
) {
    val firstDate: LocalDate? get() = points.firstOrNull()?.date
    val lastDate: LocalDate? get() = points.lastOrNull()?.date

    val low: BigDecimal? get() = points.minOfOrNull { it.rate }
    val high: BigDecimal? get() = points.maxOfOrNull { it.rate }
    val latest: BigDecimal? get() = points.lastOrNull()?.rate

    /** Variazione percentuale fra il primo e l'ultimo punto del periodo. */
    val changePercent: BigDecimal?
        get() {
            val first = points.firstOrNull()?.rate ?: return null
            val last = points.lastOrNull()?.rate ?: return null
            if (first.signum() == 0) return null
            return last.subtract(first)
                .divide(first, MonetaryMath.CONTEXT)
                .multiply(BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP)
        }
}

class HistoryViewModel(
    private val repository: RateRepository,
    settings: SettingsStore,
) : ViewModel() {

    private val range = MutableStateFlow(HistoryRange.SIX_MONTHS)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HistoryUiState> = combine(
        settings.baseCurrency,
        settings.quoteCurrency,
        range,
    ) { base, quote, selected -> Triple(base, quote, selected) }
        .flatMapLatest { (base, quote, selected) ->
            flow {
                // Si dichiara subito il caricamento con la coppia giusta, così
                // il titolo non resta indietro rispetto alla selezione.
                emit(
                    HistoryUiState(
                        base = base,
                        quote = quote,
                        range = selected,
                        loading = true,
                    ),
                )
                val result = repository.series(base, quote, selected)
                val series = (result as? SeriesResult.Available)?.series
                emit(
                    HistoryUiState(
                        base = base,
                        quote = quote,
                        range = selected,
                        points = series?.points.orEmpty(),
                        loading = false,
                        provider = series?.provider,
                        emptyReason = result.takeIf { it !is SeriesResult.Available },
                    ),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )

    fun onRangeSelected(selected: HistoryRange) {
        range.value = selected
    }

    class Factory(
        private val repository: RateRepository,
        private val settings: SettingsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistoryViewModel(repository, settings) as T
    }
}
