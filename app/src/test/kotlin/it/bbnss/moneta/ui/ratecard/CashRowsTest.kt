package it.bbnss.moneta.ui.ratecard

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class CashRowsTest {
    private fun notes(vararg values: String) = values.map(::BigDecimal)
    @Test fun examplesNeverBecomeCountableBanknotes() {
        val result = CashRows.build(emptyList(), notes("1", "2", "5"), emptyMap())
        assertEquals(notes("1", "2", "5"), result.table)
        assertTrue(result.counter.isEmpty())
    }
    @Test fun withdrawnDenominationIsPreservedWithoutOfferingNewCounts() {
        val result = CashRows.build(notes("50", "100", "200", "500"), emptyList(), mapOf(BigDecimal("1000") to 2))
        val saved = result.counter.single { it.value.compareTo(BigDecimal("1000")) == 0 }
        assertFalse(saved.documented)
        assertEquals(2, saved.count)
        assertEquals(0, BigDecimal("2000").compareTo(result.total))
        assertFalse(result.table.contains(BigDecimal("1000")))
    }
    @Test fun unknownCurrencyRetainsOnlyPreviouslyCountedAmounts() {
        val result = CashRows.build(emptyList(), notes("1", "2", "5"), mapOf(BigDecimal("25") to 3))
        assertEquals(1, result.counter.size)
        assertFalse(result.counter.single().documented)
        assertEquals(BigDecimal("75"), result.total)
    }
    @Test fun fractionalDenominationsUseExactDecimalTotalsAndMergeOldScales() {
        val result = CashRows.build(notes("0.25", "0.5", "1"), emptyList(),
            mapOf(BigDecimal("0.50") to 2, BigDecimal("0.5") to 1, BigDecimal("0.25") to 3))
        assertEquals(3, result.counter.single { it.value.compareTo(BigDecimal("0.5")) == 0 }.count)
        assertTrue(result.counter.all { it.documented })
        assertEquals(0, BigDecimal("2.25").compareTo(result.total))
    }
    @Test fun removedOldCountDoesNotLeaveAnInventedNote() {
        val result = CashRows.build(notes("5"), emptyList(), mapOf(BigDecimal("1000") to 0))
        assertEquals(1, result.counter.size)
        assertTrue(result.counter.single().documented)
        assertEquals(BigDecimal.ZERO, result.total)
    }
}
