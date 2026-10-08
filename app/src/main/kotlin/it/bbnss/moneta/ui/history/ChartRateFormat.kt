package it.bbnss.moneta.ui.history

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/** Compact chart labels only: the exchange rate used in calculations stays unchanged. */
internal object ChartRateFormat {
    fun format(value: BigDecimal, locale: Locale = Locale.getDefault()): String {
        val rounded = value.abs().setScale(2, RoundingMode.HALF_UP)
        val (divisor, suffix) = when {
            rounded >= BigDecimal("1000000") -> BigDecimal("1000000") to "m"
            rounded >= BigDecimal("1000") -> BigDecimal("1000") to "k"
            else -> BigDecimal.ONE to ""
        }
        // Promote a rounded 1000k to 1m rather than displaying a misleading boundary.
        val scaled = value.divide(divisor, 2, RoundingMode.HALF_UP)
        if (suffix == "k" && scaled.abs() >= BigDecimal("1000")) {
            return format(value.divide(BigDecimal("1000000"), 2, RoundingMode.HALF_UP), locale) + "m"
        }
        // Tiny reciprocal rates must not turn into zero with the two-decimal limit.
        if (value.signum() != 0 && scaled.signum() == 0) {
            return DecimalFormat("0.##E0", DecimalFormatSymbols.getInstance(locale)).format(value)
        }
        return NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed = false
            maximumFractionDigits = 2
            roundingMode = RoundingMode.HALF_UP
        }.format(scaled) + suffix
    }
}
