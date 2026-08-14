package it.bbnss.moneta.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CountryCurrenciesTest {

    @Test
    fun `riconosce le valute dei paesi piu visitati`() {
        assertEquals(Currency("VND"), CountryCurrencies.currencyOf("VN"))
        assertEquals(Currency("THB"), CountryCurrencies.currencyOf("TH"))
        assertEquals(Currency.USD, CountryCurrencies.currencyOf("US"))
        assertEquals(Currency.JPY, CountryCurrencies.currencyOf("JP"))
        assertEquals(Currency.GBP, CountryCurrencies.currencyOf("GB"))
        assertEquals(Currency("MAD"), CountryCurrencies.currencyOf("MA"))
    }

    /** I paesi dell'area euro condividono la stessa moneta. */
    @Test
    fun `l area euro converge sull euro`() {
        for (country in listOf("IT", "DE", "FR", "ES", "PT", "IE", "HR")) {
            assertEquals("Paese $country", Currency.EUR, CountryCurrencies.currencyOf(country))
        }
    }

    @Test
    fun `accetta codici in minuscolo e con spazi`() {
        assertEquals(Currency("VND"), CountryCurrencies.currencyOf(" vn "))
    }

    @Test
    fun `rifiuta input non utilizzabili`() {
        assertNull(CountryCurrencies.currencyOf(null))
        assertNull(CountryCurrencies.currencyOf(""))
        assertNull(CountryCurrencies.currencyOf("V"))
        assertNull(CountryCurrencies.currencyOf("VNM"))
        assertNull(CountryCurrencies.currencyOf("12"))
    }

    @Test
    fun `i nomi dei paesi sono tradotti`() {
        val italian = CountryCurrencies.nameOf("VN", Locale.ITALIAN)
        val french = CountryCurrencies.nameOf("VN", Locale.FRENCH)

        assertTrue(!italian.isNullOrBlank())
        assertTrue(italian != french)
    }

    @Test
    fun `un paese senza valuta non produce un suggerimento`() {
        // L'Antartide non ha una moneta ufficiale.
        assertNull(CountryCurrencies.currencyOf("AQ"))
    }
}
