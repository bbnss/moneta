package it.bbnss.moneta.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class AmountFormatTest {

    private val italian = Locale.ITALY
    private val us = Locale.US

    @Test
    fun `usa i separatori della lingua`() {
        assertEquals("1.234,56", AmountFormat.format(BigDecimal("1234.56"), Currency.EUR, italian))
        assertEquals("1,234.56", AmountFormat.format(BigDecimal("1234.56"), Currency.EUR, us))
    }

    @Test
    fun `segue i decimali previsti dalla valuta`() {
        assertEquals("1.235", AmountFormat.format(BigDecimal("1234.56"), Currency.JPY, italian))
        assertEquals("1,234.560", AmountFormat.format(BigDecimal("1234.56"), Currency("KWD"), us))
    }

    /**
     * Il caso concreto: un dong vale circa 0,000033 euro. Con i due decimali
     * canonici l'utente leggerebbe "0,00", cioè nulla di utile.
     */
    @Test
    fun `mostra cifre in piu quando l importo e minuscolo`() {
        val formatted = AmountFormat.format(BigDecimal("0.0000331"), Currency.EUR, us)
        assertEquals("0.000033", formatted)
    }

    @Test
    fun `non aggiunge cifre quando non servono`() {
        assertEquals("12.35", AmountFormat.format(BigDecimal("12.3456"), Currency.EUR, us))
    }

    @Test
    fun `i tassi grandi restano leggibili`() {
        assertEquals("30,197", AmountFormat.formatRate(BigDecimal("30197.0"), us))
    }

    @Test
    fun `i tassi minuscoli conservano le cifre significative`() {
        val formatted = AmountFormat.formatRate(BigDecimal("0.0000331"), us)
        assertTrue("Tasso illeggibile: $formatted", formatted.trimEnd('0').length > 4)
    }

    @Test
    fun `raggruppa le migliaia mentre si digita`() {
        assertEquals("45.000", AmountFormat.groupTypedNumber("45000", italian))
        assertEquals("45,000", AmountFormat.groupTypedNumber("45000", us))
    }

    /** Premere la virgola deve dare un riscontro anche prima di digitare i decimali. */
    @Test
    fun `mantiene il separatore decimale appena digitato`() {
        assertEquals("45.000,", AmountFormat.groupTypedNumber("45000,", italian))
        assertEquals("45.000,5", AmountFormat.groupTypedNumber("45000,5", italian))
    }

    @Test
    fun `lascia intatto un testo che non e un numero`() {
        assertEquals("12+8", AmountFormat.groupTypedNumber("12+8", italian))
        assertEquals("", AmountFormat.groupTypedNumber("", italian))
    }
}
