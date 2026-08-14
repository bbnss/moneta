package it.bbnss.moneta.core.providers

import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.CurrencyKind
import it.bbnss.moneta.core.model.CurrencyMetadata
import it.bbnss.moneta.core.model.ProviderId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class FawazahmedProviderTest {

    @Test
    fun `legge i tassi e conserva la precisione`() = runTest {
        val provider = FawazahmedProvider(
            httpClient = clientReturning(fixture("fawazahmed0_eur.json")),
            clock = FIXED_CLOCK,
        )

        val snapshot = (provider.fetchLatest() as ProviderResult.Success).value
        assertEquals(ProviderId.FAWAZAHMED0, snapshot.provider)
        assertEquals(Currency.EUR, snapshot.pivot)
        assertEquals(LocalDate.of(2026, 8, 9), snapshot.rateDate)
        assertEquals(BigDecimal("30366.38449315"), snapshot.rates[Currency("VND")])
    }

    @Test
    fun `normalizza in maiuscolo i codici minuscoli dell API`() = runTest {
        val provider = FawazahmedProvider(
            httpClient = clientReturning(fixture("fawazahmed0_eur.json")),
            clock = FIXED_CLOCK,
        )

        val snapshot = (provider.fetchLatest() as ProviderResult.Success).value
        assertTrue(snapshot.supports(Currency.USD))
        assertTrue(snapshot.rates.keys.none { it.code.any(Char::isLowerCase) })
    }

    /** È l'unica fonte in elenco che copre crypto e metalli. */
    @Test
    fun `include crypto e metalli`() = runTest {
        val provider = FawazahmedProvider(
            httpClient = clientReturning(fixture("fawazahmed0_eur.json")),
            clock = FIXED_CLOCK,
        )

        val snapshot = (provider.fetchLatest() as ProviderResult.Success).value
        assertTrue(snapshot.supports(Currency("BTC")))
        assertTrue(snapshot.supports(Currency("XAU")))
        assertTrue(snapshot.supports(Currency("1INCH")))
        assertEquals(CurrencyKind.METAL, CurrencyMetadata.of(Currency("XAU")).kind)
    }
}

class ExchangeRateApiProviderTest {

    @Test
    fun `legge i tassi e la data dichiarata dalla fonte`() = runTest {
        val provider = ExchangeRateApiProvider(
            httpClient = clientReturning(fixture("erapi_eur.json")),
            clock = FIXED_CLOCK,
        )

        val snapshot = (provider.fetchLatest() as ProviderResult.Success).value
        assertEquals(ProviderId.EXCHANGERATE_API, snapshot.provider)
        // time_last_update_unix = 1786320151 → 10 agosto 2026 UTC
        assertEquals(LocalDate.of(2026, 8, 10), snapshot.rateDate)
        assertEquals(BigDecimal("1.156035"), snapshot.rates[Currency.USD])
    }

    /**
     * Questa API risponde 200 anche quando fallisce, mettendo l'errore nel
     * corpo: senza questo controllo l'app mostrerebbe un elenco di tassi vuoto
     * spacciandolo per un aggiornamento riuscito.
     */
    @Test
    fun `riconosce un errore applicativo servito con HTTP 200`() = runTest {
        val body = """{"result":"error","error-type":"unsupported-code"}"""
        val provider = ExchangeRateApiProvider(
            httpClient = clientReturning(body),
            clock = FIXED_CLOCK,
        )

        val failure = provider.fetchLatest() as ProviderResult.Failure
        assertEquals(FailureReason.HTTP_ERROR, failure.reason)
        assertTrue(failure.message!!.contains("unsupported-code"))
    }
}

class EcbProviderTest {

    private val provider = EcbProvider(
        httpClient = clientReturning(""),
        clock = FIXED_CLOCK,
    )

    @Test
    fun `legge il feed XML della BCE`() {
        val snapshot = (provider.parse(fixture("ecb_daily.xml")) as ProviderResult.Success).value

        assertEquals(ProviderId.ECB, snapshot.provider)
        assertEquals(Currency.EUR, snapshot.pivot)
        assertEquals(LocalDate.of(2026, 8, 7), snapshot.rateDate)
        assertEquals(4, snapshot.rates.size)
        assertEquals(BigDecimal("1.1535"), snapshot.rates[Currency.USD])
        assertEquals(BigDecimal("0.85765"), snapshot.rates[Currency.GBP])
    }

    @Test
    fun `un XML senza tassi non produce uno snapshot`() {
        val empty = "<?xml version=\"1.0\"?><Envelope><Cube><Cube time='2026-08-07'/></Cube></Envelope>"
        assertTrue(provider.parse(empty) is ProviderResult.Failure)
    }

    @Test
    fun `un XML malformato diventa MALFORMED_RESPONSE`() {
        val failure = provider.parse("<non chiuso") as ProviderResult.Failure
        assertEquals(FailureReason.MALFORMED_RESPONSE, failure.reason)
    }
}
