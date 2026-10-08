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
            listOf(5, 10, 20, 50, 100, 200, 500).map { BigDecimal(it) },
            Denominations.of(Currency.EUR),
        )
    }

    /**
     * Vietnam includes lower cotton denominations alongside polymer banknotes.
     * Generic 1-2-5 examples would be unsuitable as banknote denominations.
     */
    @Test
    fun `le valute ad alta denominazione partono dal loro taglio reale`() {
        val dong = Denominations.of(Currency("VND"))
        assertEquals(BigDecimal(200), dong.first())
        assertTrue(BigDecimal(1000) in dong)
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
        assertTrue(Denominations.of(unknown).isEmpty())
        assertTrue(Denominations.illustrativeAmounts(unknown).isNotEmpty())
    }

    @Test fun `illustrative amounts are independent of rates and cannot be counted`() {
        assertTrue(Denominations.of(Currency("ZZZ")).isEmpty())
        assertEquals(BigDecimal.ONE, Denominations.illustrativeAmounts(Currency("ZZZ")).first())
        assertTrue(Denominations.illustrativeAmounts(Currency("UGX")).first() >= BigDecimal(1000))
    }

    @Test fun `corrects omitted and withdrawn notes`() {
        assertTrue(BigDecimal("2") in Denominations.of(Currency.USD))
        assertTrue(BigDecimal("1000") in Denominations.of(Currency("CHF")))
        assertFalse(BigDecimal("1000") in Denominations.of(Currency("DKK")))
    }

    @Test fun `fractional Egyptian notes remain decimals`() {
        assertEquals(BigDecimal("0.25"), Denominations.of(Currency("EGP")).first())
        assertTrue(BigDecimal("0.5") in Denominations.of(Currency("EGP")))
    }

    @Test fun `every counted catalog has dated primary-source metadata`() {
        for ((_, catalog) in Denominations.catalogs) {
            assertTrue(catalog.source.startsWith("https://"))
            assertTrue(catalog.issuer.isNotBlank())
            assertEquals(java.time.LocalDate.of(2026, 10, 8), catalog.checkedOn)
            assertTrue(catalog.values.all { it.signum() > 0 })
            assertEquals(catalog.values.sorted(), catalog.values)
            assertEquals(catalog.values.size, catalog.values.distinct().size)
        }
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
