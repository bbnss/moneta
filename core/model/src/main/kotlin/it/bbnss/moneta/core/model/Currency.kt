package it.bbnss.moneta.core.model

/**
 * Codice di una valuta, normalizzato in maiuscolo.
 *
 * Non è un enum chiuso di proposito: i provider aggiungono e tolgono valute nel
 * tempo, e alcuni servono anche crypto (`BTC`, `1INCH`) o metalli (`XAU`). Un
 * enum costringerebbe a rilasciare una nuova versione dell'app ogni volta che
 * una fonte amplia la copertura, ed è il motivo per cui l'app va aggiornata per
 * vedere valute nuove nei progetti concorrenti.
 */
@JvmInline
value class Currency(val code: String) : Comparable<Currency> {

    init {
        require(code.length in 2..10) { "Codice valuta di lunghezza non valida: '$code'" }
        require(code.all { it.isLetterOrDigit() }) { "Codice valuta non alfanumerico: '$code'" }
        require(code.none { it.isLowerCase() }) { "Codice valuta non normalizzato: '$code'" }
    }

    override fun compareTo(other: Currency): Int = code.compareTo(other.code)

    override fun toString(): String = code

    companion object {
        /** Normalizza e valida un codice arbitrario; `null` se non è utilizzabile. */
        fun parse(raw: String?): Currency? {
            val normalized = raw?.trim()?.uppercase() ?: return null
            if (normalized.length !in 2..10) return null
            if (!normalized.all { it.isLetterOrDigit() }) return null
            return Currency(normalized)
        }

        val EUR = Currency("EUR")
        val USD = Currency("USD")
        val GBP = Currency("GBP")
        val CHF = Currency("CHF")
        val JPY = Currency("JPY")
        val CAD = Currency("CAD")
        val NOK = Currency("NOK")
        val RUB = Currency("RUB")
    }
}
