package it.bbnss.moneta.core.model

import java.math.BigDecimal

enum class FeeMode { CASH, CARD }

data class FeeAmounts(
    val base: BigDecimal,
    val quote: BigDecimal,
    val quoteWithoutFee: BigDecimal,
)

object Fees {
    /** Both totals describe the same base amount, including when the quote is edited. */
    fun amounts(amount: BigDecimal, rate: BigDecimal, percent: BigDecimal, mode: FeeMode,
                inverse: Boolean = false): FeeAmounts {
        val base = if (inverse) convert(amount, rate, percent, mode, inverse = true) else amount
        val quote = if (inverse) amount else convert(amount, rate, percent, mode)
        return FeeAmounts(base, quote, base.multiply(rate, MonetaryMath.CONTEXT))
    }

    fun factor(percent: BigDecimal, mode: FeeMode): BigDecimal {
        require(percent >= BigDecimal.ZERO && percent < BigDecimal("100"))
        val fraction = percent.divide(BigDecimal("100"), MonetaryMath.CONTEXT)
        return if (mode == FeeMode.CASH) BigDecimal.ONE.subtract(fraction) else BigDecimal.ONE.add(fraction)
    }
    fun convert(amount: BigDecimal, rate: BigDecimal, percent: BigDecimal, mode: FeeMode,
                inverse: Boolean = false): BigDecimal {
        val effective = rate.multiply(factor(percent, mode), MonetaryMath.CONTEXT)
        return if (inverse) amount.divide(effective, MonetaryMath.CONTEXT)
            else amount.multiply(effective, MonetaryMath.CONTEXT)
    }
}

object CashCounter {
    fun total(counts: Map<BigDecimal, Int>): BigDecimal = counts.entries.fold(BigDecimal.ZERO) { sum, (note, count) ->
        require(count >= 0)
        sum.add(note.multiply(BigDecimal(count)))
    }
}
