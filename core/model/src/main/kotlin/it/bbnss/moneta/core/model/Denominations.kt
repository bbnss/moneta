package it.bbnss.moneta.core.model

import java.math.BigDecimal

/**
 * Tagli di banconota realmente in circolazione, per valuta.
 *
 * Servono alla tabella da viaggio: davanti a un banco del mercato non si
 * converte "100", si converte *la banconota che si ha in mano*. Un elenco
 * generico 1-2-5-10 sarebbe inutile in Vietnam, dove il taglio più piccolo di
 * uso comune è mille volte più grande, e fuorviante in Kuwait, dove è mille
 * volte più piccolo.
 *
 * L'elenco copre le valute dei paesi più visitati. Per tutte le altre si
 * ripiega su una serie 1-2-5 calibrata sull'ordine di grandezza del tasso, che
 * resta ragionevole anche senza conoscere le banconote.
 */
object Denominations {

    /**
     * Serie di tagli per le valute note. Dove una valuta ha anche monete di uso
     * corrente rilevante (l'euro, la sterlina) si parte dal taglio più piccolo
     * che vale la pena convertire.
     */
    private val KNOWN: Map<String, List<Int>> = mapOf(
        // Europa
        "EUR" to listOf(5, 10, 20, 50, 100, 200),
        "GBP" to listOf(5, 10, 20, 50),
        "CHF" to listOf(10, 20, 50, 100, 200),
        "NOK" to listOf(50, 100, 200, 500, 1000),
        "SEK" to listOf(20, 50, 100, 200, 500, 1000),
        "DKK" to listOf(50, 100, 200, 500, 1000),
        "ISK" to listOf(500, 1000, 2000, 5000, 10000),
        "PLN" to listOf(10, 20, 50, 100, 200, 500),
        "CZK" to listOf(100, 200, 500, 1000, 2000, 5000),
        "HUF" to listOf(500, 1000, 2000, 5000, 10000, 20000),
        "RON" to listOf(1, 5, 10, 50, 100, 200, 500),
        "BGN" to listOf(5, 10, 20, 50, 100),
        "RSD" to listOf(50, 100, 200, 500, 1000, 2000, 5000),
        "UAH" to listOf(20, 50, 100, 200, 500, 1000),
        "RUB" to listOf(10, 50, 100, 200, 500, 1000, 2000, 5000),
        "TRY" to listOf(5, 10, 20, 50, 100, 200),
        "GEL" to listOf(5, 10, 20, 50, 100),
        "AMD" to listOf(1000, 2000, 5000, 10000, 20000, 50000),
        "MDL" to listOf(10, 20, 50, 100, 200, 500, 1000),

        // Americhe
        "USD" to listOf(1, 5, 10, 20, 50, 100),
        "CAD" to listOf(5, 10, 20, 50, 100),
        "MXN" to listOf(20, 50, 100, 200, 500, 1000),
        "BRL" to listOf(2, 5, 10, 20, 50, 100, 200),
        "ARS" to listOf(100, 200, 500, 1000, 2000, 10000, 20000),
        "CLP" to listOf(1000, 2000, 5000, 10000, 20000),
        "COP" to listOf(2000, 5000, 10000, 20000, 50000, 100000),
        "PEN" to listOf(10, 20, 50, 100, 200),
        "UYU" to listOf(20, 50, 100, 200, 500, 1000, 2000),
        "BOB" to listOf(10, 20, 50, 100, 200),
        "CRC" to listOf(1000, 2000, 5000, 10000, 20000, 50000),
        "DOP" to listOf(50, 100, 200, 500, 1000, 2000),
        "CUP" to listOf(5, 10, 20, 50, 100, 200, 500, 1000),
        "GTQ" to listOf(1, 5, 10, 20, 50, 100, 200),

        // Asia
        "JPY" to listOf(1000, 2000, 5000, 10000),
        "CNY" to listOf(1, 5, 10, 20, 50, 100),
        "KRW" to listOf(1000, 5000, 10000, 50000),
        "TWD" to listOf(100, 200, 500, 1000, 2000),
        "HKD" to listOf(20, 50, 100, 500, 1000),
        "SGD" to listOf(2, 5, 10, 50, 100),
        "MYR" to listOf(1, 5, 10, 20, 50, 100),
        "THB" to listOf(20, 50, 100, 500, 1000),
        "VND" to listOf(1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000),
        "KHR" to listOf(500, 1000, 2000, 5000, 10000, 20000, 50000, 100000),
        "LAK" to listOf(1000, 2000, 5000, 10000, 20000, 50000, 100000),
        "MMK" to listOf(50, 100, 200, 500, 1000, 5000, 10000),
        "IDR" to listOf(1000, 2000, 5000, 10000, 20000, 50000, 100000),
        "PHP" to listOf(20, 50, 100, 200, 500, 1000),
        "INR" to listOf(10, 20, 50, 100, 200, 500),
        "PKR" to listOf(10, 20, 50, 100, 500, 1000, 5000),
        "BDT" to listOf(5, 10, 20, 50, 100, 200, 500, 1000),
        "LKR" to listOf(20, 50, 100, 500, 1000, 5000),
        "NPR" to listOf(5, 10, 20, 50, 100, 500, 1000),
        "MNT" to listOf(100, 500, 1000, 5000, 10000, 20000),
        "KZT" to listOf(200, 500, 1000, 2000, 5000, 10000, 20000),
        "UZS" to listOf(1000, 2000, 5000, 10000, 20000, 50000, 100000),

        // Medio Oriente e Africa
        "AED" to listOf(5, 10, 20, 50, 100, 200, 500, 1000),
        "SAR" to listOf(5, 10, 50, 100, 500),
        "QAR" to listOf(1, 5, 10, 50, 100, 500),
        "ILS" to listOf(20, 50, 100, 200),
        "JOD" to listOf(1, 5, 10, 20, 50),
        "LBP" to listOf(1000, 5000, 10000, 20000, 50000, 100000),
        "IRR" to listOf(10000, 20000, 50000, 100000, 500000, 1000000),
        "EGP" to listOf(5, 10, 20, 50, 100, 200),
        "MAD" to listOf(20, 50, 100, 200),
        "TND" to listOf(5, 10, 20, 50),
        "ZAR" to listOf(10, 20, 50, 100, 200),
        "KES" to listOf(50, 100, 200, 500, 1000),
        "TZS" to listOf(500, 1000, 2000, 5000, 10000),
        "UGX" to listOf(1000, 2000, 5000, 10000, 20000, 50000),
        "NGN" to listOf(5, 10, 20, 50, 100, 200, 500, 1000),
        "GHS" to listOf(1, 2, 5, 10, 20, 50, 100, 200),
        "ETB" to listOf(10, 50, 100, 200),

        // Oceania
        "AUD" to listOf(5, 10, 20, 50, 100),
        "NZD" to listOf(5, 10, 20, 50, 100),
        "FJD" to listOf(5, 10, 20, 50, 100),
    )

    /** `true` se i tagli sono quelli reali della valuta e non una stima. */
    fun areKnown(currency: Currency): Boolean = currency.code in KNOWN

    /**
     * Tagli da mostrare per [currency].
     *
     * Se la valuta non è in elenco si costruisce una serie 1-2-5 centrata sul
     * suo ordine di grandezza, dedotto da [referenceRate] — quante unità di
     * questa valuta vale una unità di quella di casa. Serve a non proporre
     * "1, 2, 5" dove la banconota più piccola è da diecimila.
     */
    fun of(currency: Currency, referenceRate: BigDecimal? = null): List<BigDecimal> {
        KNOWN[currency.code]?.let { known ->
            return known.map { BigDecimal(it) }
        }

        // Si parte dalla potenza di dieci più vicina al valore di una unità
        // della valuta di casa, così la tabella copre gli importi che si
        // maneggiano davvero.
        val magnitude = referenceRate
            ?.takeIf { it.signum() > 0 }
            ?.let { rate -> maxOf(0, rate.precision() - rate.scale() - 1) }
            ?: 1

        val base = BigDecimal.TEN.pow(maxOf(0, magnitude - 1))
        return listOf(1, 2, 5, 10, 20, 50, 100, 200).map { base.multiply(BigDecimal(it)) }
    }
}
