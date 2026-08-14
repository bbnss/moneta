package it.bbnss.moneta.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/**
 * Formattazione degli importi secondo la lingua dell'utente.
 *
 * I separatori delle migliaia e dei decimali cambiano da paese a paese, e in
 * arabo e persiano cambiano anche le cifre. Delegare tutto a ICU è l'unico modo
 * perché "trentamila dong" si legga giusto ovunque; scriverlo a mano
 * significherebbe sbagliarlo in metà delle lingue in cui vogliamo pubblicare.
 */
object AmountFormat {

    /**
     * Importo con il numero di decimali corretto per la valuta.
     *
     * Le cifre seguono le regole ISO 4217 (yen zero, dinaro kuwaitiano tre), ma
     * quando il risultato sarebbe uno zero secco — un dong convertito in euro —
     * si aggiungono cifre finché il numero non dice qualcosa. Mostrare "0,00"
     * a chi sta chiedendo quanto vale una banconota è peggio che inutile.
     */
    fun format(
        value: BigDecimal,
        currency: Currency,
        locale: Locale = Locale.getDefault(),
    ): String {
        val minorUnits = CurrencyMetadata.of(currency, locale).minorUnits
        return formatPlain(value, minorUnits, locale)
    }

    fun formatPlain(
        value: BigDecimal,
        minorUnits: Int,
        locale: Locale = Locale.getDefault(),
    ): String {
        val scale = MonetaryMath.adaptiveScale(value, minorUnits)
        val format = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = minOf(minorUnits, scale)
            maximumFractionDigits = scale
            roundingMode = RoundingMode.HALF_UP
        }
        return format.format(value)
    }

    /**
     * Il tasso mostrato sotto il risultato ("1 EUR = 30.197 VND").
     *
     * Qui non contano i decimali della valuta ma le cifre significative: un
     * tasso di 0,000033 e uno di 30197 devono risultare entrambi leggibili.
     */
    fun formatRate(rate: BigDecimal, locale: Locale = Locale.getDefault()): String {
        val scale = when {
            rate >= BigDecimal(1000) -> 2
            rate >= BigDecimal.ONE -> 4
            else -> MonetaryMath.adaptiveScale(rate, 4, maxExtraDigits = 4)
        }
        val format = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = scale
            roundingMode = RoundingMode.HALF_UP
        }
        return format.format(rate)
    }

    /**
     * Raggruppa le migliaia in ciò che l'utente sta digitando.
     *
     * `45000` letto di corsa può essere 4.500 o 450.000; `45.000` no. Il testo
     * grezzo resta quello digitato: qui si tocca solo la resa a schermo, e solo
     * se non è in corso un calcolo.
     */
    fun groupTypedNumber(raw: String, locale: Locale = Locale.getDefault()): String {
        if (raw.isEmpty()) return raw

        val symbols = DecimalFormatSymbols.getInstance(locale)
        val separatorIndex = raw.indexOfFirst { it == '.' || it == ',' }

        val integerPart = if (separatorIndex >= 0) raw.substring(0, separatorIndex) else raw
        val rest = if (separatorIndex >= 0) raw.substring(separatorIndex + 1) else null

        if (integerPart.isEmpty() || !integerPart.all { it.isDigit() }) return raw

        val grouped = DecimalFormat("#,##0", symbols)
            .format(BigDecimal(integerPart))

        // Il separatore decimale resta visibile appena digitato, anche senza
        // cifre dopo: altrimenti premere la virgola non darebbe alcun riscontro.
        return if (rest == null) grouped else grouped + symbols.decimalSeparator + rest
    }
}
