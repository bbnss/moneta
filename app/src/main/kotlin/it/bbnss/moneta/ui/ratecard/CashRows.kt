package it.bbnss.moneta.ui.ratecard

import it.bbnss.moneta.core.model.CashCounter
import java.math.BigDecimal

/** Preserve old counts while preventing undocumented/example amounts from becoming new notes. */
internal data class CashRows(val table: List<BigDecimal>, val counter: List<CashNote>, val total: BigDecimal) {
    data class CashNote(val value: BigDecimal, val count: Int, val documented: Boolean)

    companion object {
        fun build(documented: List<BigDecimal>, examples: List<BigDecimal>, saved: Map<BigDecimal, Int>): CashRows {
            val counts = saved.entries.filter { it.key.signum() > 0 && it.value > 0 }
                .groupBy { it.key.stripTrailingZeros() }
                .mapValues { (_, entries) -> entries.sumOf { it.value.toLong() }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt() }
            val verified = documented.map { it.stripTrailingZeros() }.toSet()
            val counter = (verified + counts.keys).sorted().map { CashNote(it, counts[it] ?: 0, it in verified) }
            return CashRows(documented.ifEmpty { examples }, counter, CashCounter.total(counts))
        }
    }
}
