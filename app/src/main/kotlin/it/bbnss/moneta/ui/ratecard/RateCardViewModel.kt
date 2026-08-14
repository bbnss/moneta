package it.bbnss.moneta.ui.ratecard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.bbnss.moneta.core.data.RateRepository
import it.bbnss.moneta.core.data.SettingsStore
import it.bbnss.moneta.core.model.AmountFormat
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.Denominations
import it.bbnss.moneta.core.model.Freshness
import it.bbnss.moneta.core.model.ProviderId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Una riga della tabella: una banconota e il suo valore in valuta di casa. */
data class RateCardRow(
    val denomination: BigDecimal,
    val localText: String,
    val homeText: String,
)

data class RateCardUiState(
    val local: Currency = Currency.EUR,
    val home: Currency = Currency.USD,
    val rows: List<RateCardRow> = emptyList(),
    /** I tagli sono dedotti, non presi dall'elenco delle banconote reali. */
    val estimated: Boolean = false,
    val rate: BigDecimal? = null,
    val provider: ProviderId? = null,
    val rateDate: LocalDate? = null,
    val freshness: Freshness? = null,
    val age: Duration? = null,
    val updatedAt: Instant? = null,
    val supported: Boolean = true,
)

/**
 * La tabella da viaggio.
 *
 * Al mercato non si tira fuori la calcolatrice: si guarda la banconota che si
 * ha in mano e si vuole sapere quanto vale. Questa schermata risponde a quella
 * domanda in anticipo, per tutti i tagli in circolazione, e funziona
 * interamente offline perché non c'è nulla da calcolare al momento.
 *
 * È la traduzione di una richiesta aperta sul progetto concorrente
 * ("mostra le conversioni per 1x, 5x, 10x, 20x"), ma legata alle banconote vere
 * invece che a multipli arbitrari.
 */
class RateCardViewModel(
    repository: RateRepository,
    settings: SettingsStore,
) : ViewModel() {

    val state: StateFlow<RateCardUiState> = combine(
        repository.state,
        settings.baseCurrency,
        settings.quoteCurrency,
    ) { rates, local, home ->
        val snapshot = rates.snapshot
        val rate = snapshot?.crossRate(local, home)

        // Quante unità locali vale una unità di casa: serve a calibrare i tagli
        // stimati per le valute fuori elenco.
        val inverse = snapshot?.crossRate(home, local)

        val denominations = Denominations.of(local, inverse)

        val rows = if (rate == null) {
            emptyList()
        } else {
            denominations.map { denomination ->
                RateCardRow(
                    denomination = denomination,
                    localText = AmountFormat.format(denomination, local),
                    homeText = AmountFormat.format(
                        denomination.multiply(rate, it.bbnss.moneta.core.model.MonetaryMath.CONTEXT),
                        home,
                    ),
                )
            }
        }

        RateCardUiState(
            local = local,
            home = home,
            rows = rows,
            estimated = !Denominations.areKnown(local),
            rate = rate,
            provider = snapshot?.provider,
            rateDate = snapshot?.rateDate,
            freshness = rates.freshness,
            age = rates.age,
            updatedAt = snapshot?.fetchedAt,
            supported = snapshot == null || rate != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RateCardUiState(),
    )

    class Factory(
        private val repository: RateRepository,
        private val settings: SettingsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RateCardViewModel(repository, settings) as T
    }
}
