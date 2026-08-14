package it.bbnss.moneta.core.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Insieme dei tassi restituito da una fonte in un dato momento.
 *
 * Lo snapshot conserva il **pivot nativo del provider** (EUR per Frankfurter e
 * BCE, CAD per Bank of Canada, NOK per Norges Bank) invece di ribasare tutto su
 * una valuta comune. Ribasare significherebbe una divisione in più su ogni
 * singolo tasso al momento del salvataggio, quindi un arrotondamento in più
 * ereditato da tutte le conversioni successive. Il cross-rate si calcola una
 * volta sola, al momento della conversione richiesta.
 *
 * @param rates quanti [Currency] valgono una unità di [pivot].
 * @param rateDate data dichiarata dal provider per questi tassi.
 * @param fetchedAt quando li abbiamo effettivamente scaricati.
 */
data class RateSnapshot(
    val provider: ProviderId,
    val pivot: Currency,
    val rates: Map<Currency, BigDecimal>,
    val rateDate: LocalDate,
    val fetchedAt: Instant,
) {

    val currencies: Set<Currency> get() = rates.keys + pivot

    fun supports(currency: Currency): Boolean =
        currency == pivot || rates.containsKey(currency)

    /** Quanti [currency] vale una unità di [pivot]. */
    fun rateOf(currency: Currency): BigDecimal? =
        if (currency == pivot) BigDecimal.ONE else rates[currency]

    /**
     * Quanti [to] vale una unità di [from]. `null` se una delle due valute non
     * è coperta da questa fonte.
     */
    fun crossRate(from: Currency, to: Currency): BigDecimal? {
        if (from == to) return BigDecimal.ONE
        val fromRate = rateOf(from) ?: return null
        val toRate = rateOf(to) ?: return null
        if (fromRate.signum() == 0) return null
        return toRate.divide(fromRate, MonetaryMath.CONTEXT)
    }

    /** Converte [amount] da [from] a [to]. `null` se la coppia non è coperta. */
    fun convert(amount: BigDecimal, from: Currency, to: Currency): BigDecimal? {
        val rate = crossRate(from, to) ?: return null
        return amount.multiply(rate, MonetaryMath.CONTEXT)
    }
}

/** Un punto della serie storica di una coppia. */
data class RatePoint(val date: LocalDate, val rate: BigDecimal)

/** Andamento di una coppia nel tempo, per il grafico. */
data class RateSeries(
    val provider: ProviderId,
    val base: Currency,
    val quote: Currency,
    val points: List<RatePoint>,
)
