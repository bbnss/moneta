package it.bbnss.moneta.core.model

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.*
import java.util.Locale

class Release020Test {
    private val today = LocalDate.of(2026, 10, 7)
    private fun snapshot(dates: Map<Currency, LocalDate>) = RateSnapshot(ProviderId.FRANKFURTER, Currency.EUR,
        mapOf(Currency.USD to BigDecimal("1.2"), Currency("ALL") to BigDecimal("93.11")), today, Instant.now(), dates)

    @Test fun `cross rate dates exclude pivot and require every quote date`() {
        val row = snapshot(mapOf(Currency.USD to today, Currency("ALL") to today.minusDays(2)))
        assertEquals(today.minusDays(2), row.dateFor(Currency("ALL"), Currency.USD))
        assertEquals(today, row.dateFor(Currency.EUR, Currency.USD))
        assertEquals(today, row.dateFor(Currency.USD, Currency.EUR))
        assertNull(row.dateFor(Currency.EUR, Currency.EUR))
        assertNull(snapshot(mapOf(Currency.USD to today)).dateFor(Currency("ALL"), Currency.USD))
    }
    @Test fun `freshly downloaded old rates remain stale and boundaries are inclusive`() {
        assertEquals(Freshness.FRESH, FreshnessRules.of(today, today))
        for (days in 1L..3L) assertEquals(Freshness.RECENT, FreshnessRules.of(today.minusDays(days), today))
        for (days in 4L..7L) assertEquals(Freshness.AGING, FreshnessRules.of(today.minusDays(days), today))
        assertEquals(Freshness.STALE, FreshnessRules.of(today.minusDays(8), today))
        assertNull(FreshnessRules.of(null, today))
        val old = snapshot(mapOf(Currency.USD to today.minusDays(20)))
        assertEquals(Freshness.STALE, FreshnessRules.of(old.dateFor(Currency.EUR, Currency.USD), today))
    }
    @Test fun `all network entry points respect offline and wifi but manual mode permits explicit requests`() {
        for (kind in RequestKind.entries) {
            assertEquals(NetworkBlock.OFFLINE, UpdatePolicy.block(kind, true, false, 12, true, true, null))
            assertEquals(NetworkBlock.WIFI, UpdatePolicy.block(kind, false, true, 12, true, false, null))
            assertEquals(NetworkBlock.DISCONNECTED, UpdatePolicy.block(kind, false, false, 12, false, false, null))
            assertEquals(if (kind == RequestKind.AUTOMATIC) NetworkBlock.MANUAL_ONLY else null,
                UpdatePolicy.block(kind, false, false, -1, true, true, null))
        }
        assertEquals(NetworkBlock.NOT_DUE, UpdatePolicy.block(RequestKind.AUTOMATIC, false, false, 24, true, true, Duration.ofHours(23)))
        assertNull(UpdatePolicy.block(RequestKind.AUTOMATIC, false, false, 24, true, true, Duration.ofHours(24)))
    }
    @Test fun `cash and card use the effective rate in both directions`() {
        assertEquals(BigDecimal("114.000"), Fees.convert(BigDecimal("100"), BigDecimal("1.2"), BigDecimal("5"), FeeMode.CASH))
        assertEquals(BigDecimal("126.000"), Fees.convert(BigDecimal("100"), BigDecimal("1.2"), BigDecimal("5"), FeeMode.CARD))
        for (mode in FeeMode.entries) {
            val amount = BigDecimal("12345.6789")
            val result = Fees.convert(amount, BigDecimal("30366.38449315"), BigDecimal("2.5"), mode)
            assertTrue(Fees.convert(result, BigDecimal("30366.38449315"), BigDecimal("2.5"), mode, true).subtract(amount).abs() < BigDecimal("0.00000001"))
        }
    }

    @Test fun `fee comparison keeps the same base amount when editing either field`() {
        val rate = BigDecimal("1.2")
        for ((mode, expectedQuote) in listOf(FeeMode.CASH to "114", FeeMode.CARD to "126")) {
            for (inverse in listOf(false, true)) {
                val entered = BigDecimal(if (inverse) expectedQuote else "100")
                val totals = Fees.amounts(entered, rate, BigDecimal("5"), mode, inverse)
                assertEquals(0, BigDecimal("100").compareTo(totals.base))
                assertEquals(0, BigDecimal(expectedQuote).compareTo(totals.quote))
                assertEquals(0, BigDecimal("120").compareTo(totals.quoteWithoutFee))
            }
        }
    }
    @Test fun `moving favourites skips hidden base and retains it in stored order`() {
        val original = listOf(Currency("CNY"), Currency.EUR, Currency.GBP, Currency.USD)
        val moved = FavouriteOrder.move(original, Currency("CNY"), Currency.EUR, 1)
        assertEquals(listOf(Currency.GBP, Currency("CNY"), Currency.USD), moved.filter { it != Currency.EUR })
        assertEquals(original.toSet(), moved.toSet())
        assertEquals(moved, FavouriteOrder.move(moved, Currency.GBP, Currency.EUR, -1))
    }

    @Test fun `cash counting preserves decimal denominations exactly`() {
        assertEquals(BigDecimal("201.50"), CashCounter.total(mapOf(BigDecimal("100") to 2, BigDecimal("0.50") to 3)))
        assertEquals(BigDecimal.ZERO, CashCounter.total(emptyMap()))
    }
    @Test fun `paste parses active language strictly`() {
        assertEquals(BigDecimal("1234.50"), AmountFormat.parse("1.234,50", Locale.ITALIAN))
        assertEquals(BigDecimal("1234.50"), AmountFormat.parse("1,234.50", Locale.US))
        assertEquals(BigDecimal("-1234.50"), AmountFormat.parse("-1.234,50", Locale.ITALIAN))
        for (text in listOf("2+3", "12.34,50", "1,2,3", "12 EUR", "NaN", "")) assertNull(AmountFormat.parse(text, Locale.ITALIAN))
    }
    @Test fun `custom endpoint accepts deployment paths and rejects incomplete or unsafe URLs`() {
        assertEquals("https://example.org/api/frankfurter", CustomEndpoint.normalize(" https://example.org/api/frankfurter/ "))
        for (url in listOf("http://example.org", "https://", "https://user:secret@example.org", "https://example.org?q=1", "https://example.org/#x", "https://example.org:99999")) assertNull(CustomEndpoint.normalize(url))
    }
}
