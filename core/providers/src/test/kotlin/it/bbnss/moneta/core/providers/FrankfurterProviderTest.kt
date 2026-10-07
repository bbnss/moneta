package it.bbnss.moneta.core.providers

import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.ProviderId
import io.ktor.http.HttpStatusCode
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate

class FrankfurterProviderTest {

    private fun provider(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        FrankfurterProvider(
            httpClient = clientReturning(body, status),
            clock = FIXED_CLOCK,
        )

    @Test
    fun `ALL USD conserva entrambe le date e usa la meno recente`() = runTest {
        val snapshot = provider(fixture("frankfurter_latest.json")).fetchLatest().valueOrNull()!!
        assertEquals(LocalDate.of(2026, 8, 7), snapshot.rateDates[Currency("ALL")])
        assertEquals(LocalDate.of(2026, 8, 9), snapshot.rateDates[Currency.USD])
        assertEquals(LocalDate.of(2026, 8, 7), snapshot.dateFor(Currency("ALL"), Currency.USD))
    }

    @Test
    fun `endpoint personale conserva il sottopercorso`() = runTest {
        var requested = ""
        val client = io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { request ->
            requested = request.url.toString()
            respond(fixture("frankfurter_latest.json"), HttpStatusCode.OK)
        })
        FrankfurterProvider(client, "https://example.org/travel/rates", ProviderId.CUSTOM).fetchLatest()
        assertEquals("https://example.org/travel/rates/v2/rates?base=EUR", requested)
        client.close()
    }

    @Test
    fun `legge i tassi correnti`() = runTest {
        val result = provider(fixture("frankfurter_latest.json")).fetchLatest()

        val snapshot = (result as ProviderResult.Success).value
        assertEquals(ProviderId.FRANKFURTER, snapshot.provider)
        assertEquals(Currency.EUR, snapshot.pivot)
        assertEquals(4, snapshot.rates.size)
        assertEquals(BigDecimal("1.1535"), snapshot.rates[Currency.USD])
    }

    /**
     * Frankfurter aggrega 84 banche centrali e non pubblicano tutte lo stesso
     * giorno: una valuta poco liquida può restare indietro. La data dello
     * snapshot è la più recente fra quelle presenti.
     */
    @Test
    fun `usa la data piu recente quando le righe hanno date diverse`() = runTest {
        val result = provider(fixture("frankfurter_latest.json")).fetchLatest()

        val snapshot = (result as ProviderResult.Success).value
        assertEquals(LocalDate.of(2026, 8, 9), snapshot.rateDate)
    }

    /**
     * Il tasso viene costruito dal testo del numero JSON, non da un `Double`:
     * passando da `Double` questa cifra perderebbe le ultime posizioni.
     */
    @Test
    fun `conserva tutte le cifre pubblicate dalla fonte`() = runTest {
        val result = provider(fixture("frankfurter_latest.json")).fetchLatest()

        val snapshot = (result as ProviderResult.Success).value
        assertEquals(BigDecimal("30366.38449315"), snapshot.rates[Currency("VND")])
    }

    @Test
    fun `legge la serie storica e la ordina per data`() = runTest {
        val result = provider(fixture("frankfurter_series.json"))
            .fetchSeries(Currency.EUR, Currency.JPY, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3))

        val series = (result as ProviderResult.Success).value
        assertEquals(3, series.points.size)
        assertEquals(
            listOf(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 7, 3),
            ),
            series.points.map { it.date },
        )
    }

    @Test
    fun `una risposta HTTP di errore diventa HTTP_ERROR`() = runTest {
        val result = provider("nope", HttpStatusCode.InternalServerError).fetchLatest()

        val failure = result as ProviderResult.Failure
        assertEquals(FailureReason.HTTP_ERROR, failure.reason)
    }

    /**
     * Il caso che conta di più nel lungo periodo: se la fonte cambia formato,
     * l'errore deve dire *questo*, così il job di liveness apre una issue
     * invece di far comparire all'utente un generico "errore di rete".
     */
    @Test
    fun `un corpo inatteso diventa MALFORMED_RESPONSE`() = runTest {
        val result = provider("""{"unexpected":true}""").fetchLatest()

        val failure = result as ProviderResult.Failure
        assertEquals(FailureReason.MALFORMED_RESPONSE, failure.reason)
    }

    @Test
    fun `un array vuoto non produce uno snapshot vuoto`() = runTest {
        val result = provider("[]").fetchLatest()

        assertTrue(result is ProviderResult.Failure)
    }

    @Test
    fun `un guasto di rete diventa NETWORK`() = runTest {
        val provider = FrankfurterProvider(
            httpClient = clientFailing(IOException("rete assente")),
            clock = FIXED_CLOCK,
        )

        val failure = provider.fetchLatest() as ProviderResult.Failure
        assertEquals(FailureReason.NETWORK, failure.reason)
    }

    @Test
    fun `l endpoint self-hosted usa lo stesso parser`() = runTest {
        val custom = FrankfurterProvider(
            httpClient = clientReturning(fixture("frankfurter_latest.json")),
            baseUrl = "https://tassi.example.org",
            id = ProviderId.CUSTOM,
            clock = FIXED_CLOCK,
        )

        val snapshot = (custom.fetchLatest() as ProviderResult.Success).value
        assertEquals(ProviderId.CUSTOM, snapshot.provider)
    }
}
