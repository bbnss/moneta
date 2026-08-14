package it.bbnss.moneta.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DenominationsTest {

    @Test
    fun `usa i tagli reali quando li conosce`() {
        assertTrue(Denominations.areKnown(Currency.EUR))
        assertEquals(
            listOf(5, 10, 20, 50, 100, 200).map { BigDecimal(it) },
            Denominations.of(Currency.EUR),
        )
    }

    /**
     * Il caso che dà senso alla tabella: in Vietnam la banconota più piccola di
     * uso corrente è da mille. Una serie 1-2-5 sarebbe inutilizzabile.
     */
    @Test
    fun `le valute ad alta denominazione partono dal loro taglio reale`() {
        val dong = Denominations.of(Currency("VND"))
        assertEquals(BigDecimal(1000), dong.first())
        assertEquals(BigDecimal(500000), dong.last())
    }

    @Test
    fun `lo yen non ha tagli sotto il migliaio`() {
        assertEquals(BigDecimal(1000), Denominations.of(Currency.JPY).first())
    }

    @Test
    fun `una valuta sconosciuta ricade su una serie stimata`() {
        val unknown = Currency("ZZZ")
        assertFalse(Denominations.areKnown(unknown))
        assertTrue(Denominations.of(unknown).isNotEmpty())
    }

    /**
     * Per le valute fuori elenco l'ordine di grandezza si deduce dal tasso: con
     * 25.000 unità per euro la tabella deve parlare di migliaia, non di unità.
     */
    @Test
    fun `la serie stimata segue l ordine di grandezza del tasso`() {
        val small = Denominations.of(Currency("ZZZ"), referenceRate = BigDecimal("1.2"))
        val large = Denominations.of(Currency("ZZZ"), referenceRate = BigDecimal("25000"))

        assertEquals(BigDecimal.ONE, small.first())
        assertTrue("Attesi migliaia, ottenuto ${large.first()}", large.first() >= BigDecimal(1000))
    }

    @Test
    fun `i tagli sono sempre in ordine crescente`() {
        for (code in listOf("EUR", "USD", "VND", "JPY", "IRR", "THB", "IDR")) {
            val values = Denominations.of(Currency(code))
            assertEquals(
                "Tagli non ordinati per $code",
                values.sorted(),
                values,
            )
        }
    }

    @Test
    fun `copre le valute dei paesi piu visitati`() {
        val mustHave = listOf(
            "EUR", "USD", "GBP", "JPY", "THB", "VND", "IDR", "TRY", "MXN",
            "INR", "CNY", "AED", "EGP", "MAD", "ZAR", "BRL", "AUD",
        )
        for (code in mustHave) {
            assertTrue("Tagli mancanti per $code", Denominations.areKnown(Currency(code)))
        }
    }
}
