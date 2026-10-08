package it.bbnss.moneta.ui.history

import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ChartRateFormatTest {
    private fun format(value: String, locale: Locale = Locale.US) = ChartRateFormat.format(BigDecimal(value), locale)

    @Test fun largeRatesUseThousandsAndMillionsWithTwoDecimals() {
        assertEquals("123.46k", format("123456.789"))
        assertEquals("1.23m", format("1234567.89"))
        assertEquals("29.5m", format("29500000"))
    }
    @Test fun labelsFollowTheAppLocale() {
        assertEquals("123,46k", format("123456.789", Locale.ITALY))
        assertEquals("1,12", format("1.12345", Locale.ITALY))
    }
    @Test fun roundedBoundariesDoNotShowOneThousandThousands() {
        assertEquals("1k", format("999.999"))
        assertEquals("1m", format("999999"))
        assertEquals("-1m", format("-999999"))
    }
    @Test fun tinyReciprocalRatesStayNonzero() {
        assertEquals("3.33E-5", format("0.0000333333"))
        assertEquals("3,33E-5", format("0.0000333333", Locale.ITALY))
        assertEquals("0", format("0"))
    }
}
