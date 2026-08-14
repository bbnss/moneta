package it.bbnss.moneta.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class RateSnapshotTest {

    private fun snapshot(
        pivot: Currency,
        rates: Map<Currency, String>,
    ) = RateSnapshot(
        provider = ProviderId.FRANKFURTER,
        pivot = pivot,
        rates = rates.mapValues { BigDecimal(it.value) },
        rateDate = LocalDate.of(2026, 8, 9),
        fetchedAt = Instant.parse("2026-08-09T16:00:00Z"),
    )

    private val eurBased = snapshot(
        pivot = Currency.EUR,
        rates = mapOf(
            Currency.USD to "1.1535",
            Currency.JPY to "182.64",
            Currency("VND") to "30366.38449315",
        ),
    )

    @Test
    fun `il pivot vale sempre uno`() {
        assertEquals(0, BigDecimal.ONE.compareTo(eurBased.rateOf(Currency.EUR)))
    }

    @Test
    fun `converte dal pivot verso una quotata`() {
        val result = eurBased.convert(BigDecimal("100"), Currency.EUR, Currency.USD)
        assertNotNull(result)
        assertEquals(0, BigDecimal("115.35").compareTo(result!!.setScale(2, java.math.RoundingMode.HALF_UP)))
    }

    @Test
    fun `converte fra due valute che non sono il pivot`() {
        // 1 USD = 182.64 / 1.1535 JPY = 158.335…
        val rate = eurBased.crossRate(Currency.USD, Currency.JPY)
        assertNotNull(rate)
        assertEquals(
            0,
            BigDecimal("158.34").compareTo(rate!!.setScale(2, java.math.RoundingMode.HALF_UP)),
        )
    }

    @Test
    fun `una valuta uguale a se stessa vale uno anche se assente`() {
        val unknown = Currency("ZZZ")
        assertEquals(0, BigDecimal.ONE.compareTo(eurBased.crossRate(unknown, unknown)))
    }

    @Test
    fun `restituisce null per una valuta non coperta`() {
        assertNull(eurBased.crossRate(Currency.EUR, Currency("ZZZ")))
        assertNull(eurBased.convert(BigDecimal.ONE, Currency("ZZZ"), Currency.EUR))
    }

    /**
     * Il test che un'implementazione basata su `Float` non supera: le valute ad
     * alta denominazione hanno bisogno di molte più cifre significative di
     * quante `Float` ne offra (circa 7).
     */
    @Test
    fun `andata e ritorno su una valuta ad alta denominazione non perde cifre`() {
        val amount = BigDecimal("999999999")
        val inEuro = eurBased.convert(amount, Currency("VND"), Currency.EUR)!!
        val backToDong = eurBased.convert(inEuro, Currency.EUR, Currency("VND"))!!

        assertEquals(
            0,
            amount.compareTo(backToDong.setScale(0, java.math.RoundingMode.HALF_UP)),
        )
    }

    /**
     * Lo stesso mercato descritto con pivot diversi deve dare lo stesso
     * cross-rate: è ciò che permette di non ribasare gli snapshot al salvataggio
     * e di supportare fonti con pivot nativi differenti (CAD, NOK, RUB).
     */
    @Test
    fun `il cross-rate non dipende dal pivot scelto dalla fonte`() {
        // Stesso mercato, ma espresso con USD come pivot.
        val usdBased = snapshot(
            pivot = Currency.USD,
            rates = mapOf(
                Currency.EUR to BigDecimal.ONE.divide(BigDecimal("1.1535"), MonetaryMath.CONTEXT).toPlainString(),
                Currency.JPY to BigDecimal("182.64").divide(BigDecimal("1.1535"), MonetaryMath.CONTEXT).toPlainString(),
            ),
        )

        val fromEurPivot = eurBased.crossRate(Currency.EUR, Currency.JPY)!!
        val fromUsdPivot = usdBased.crossRate(Currency.EUR, Currency.JPY)!!

        // Tolleranza a 10 decimali: ben oltre qualunque cifra mostrata a schermo.
        val difference = fromEurPivot.subtract(fromUsdPivot).abs()
        assertTrue(
            "Scostamento fra pivot diversi: $difference",
            difference < BigDecimal("0.0000000001"),
        )
    }

    @Test
    fun `supports riconosce il pivot e le quotate`() {
        assertTrue(eurBased.supports(Currency.EUR))
        assertTrue(eurBased.supports(Currency.USD))
        assertTrue(!eurBased.supports(Currency("ZZZ")))
    }
}
