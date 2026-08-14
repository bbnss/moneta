package it.bbnss.moneta.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CurrencyTest {

    @Test
    fun `normalizza in maiuscolo e taglia gli spazi`() {
        assertEquals(Currency.USD, Currency.parse(" usd "))
        assertEquals(Currency("BTC"), Currency.parse("btc"))
    }

    @Test
    fun `rifiuta codici non utilizzabili`() {
        assertNull(Currency.parse(null))
        assertNull(Currency.parse(""))
        assertNull(Currency.parse("E"))
        assertNull(Currency.parse("EU R"))
        assertNull(Currency.parse("EUR-"))
    }

    @Test
    fun `accetta i codici crypto che iniziano per cifra`() {
        assertEquals(Currency("1INCH"), Currency.parse("1inch"))
    }

    @Test
    fun `i nomi delle valute arrivano tradotti da ICU`() {
        val italian = CurrencyMetadata.of(Currency.USD, Locale.ITALIAN)
        val french = CurrencyMetadata.of(Currency.USD, Locale.FRENCH)

        // Non verifichiamo la stringa esatta, che dipende dalla versione di ICU:
        // verifichiamo che sia localizzata e non il codice grezzo.
        assertTrue(italian.displayName.isNotBlank())
        assertTrue(italian.displayName != Currency.USD.code)
        assertTrue(french.displayName != italian.displayName)
    }

    @Test
    fun `i decimali seguono le regole ISO 4217`() {
        assertEquals(2, CurrencyMetadata.of(Currency.EUR).minorUnits)
        assertEquals(0, CurrencyMetadata.of(Currency.JPY).minorUnits)
        assertEquals(3, CurrencyMetadata.of(Currency("KWD")).minorUnits)
    }

    @Test
    fun `metalli e crypto sono classificati a parte`() {
        assertEquals(CurrencyKind.METAL, CurrencyMetadata.of(Currency("XAU")).kind)
        assertEquals(CurrencyKind.CRYPTO, CurrencyMetadata.of(Currency("BTC")).kind)
        assertEquals(CurrencyKind.FIAT, CurrencyMetadata.of(Currency.EUR).kind)
    }

    @Test
    fun `le crypto hanno abbastanza decimali per essere leggibili`() {
        assertEquals(8, CurrencyMetadata.of(Currency("BTC")).minorUnits)
    }
}
