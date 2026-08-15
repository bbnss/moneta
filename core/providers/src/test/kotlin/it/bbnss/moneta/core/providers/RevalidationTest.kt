package it.bbnss.moneta.core.providers

import it.bbnss.moneta.core.model.Currency
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Ogni richiesta di tassi deve rivalidare con la fonte.
 *
 * Senza, una copia ancora "fresca" nella cache HTTP risponde 200 senza che la
 * rete venga toccata, e l'app finisce per datare a oggi dei tassi di ieri —
 * il che accadeva perfino con il telefono in modalità aereo. È un bug di
 * onestà, non di prestazioni: qui si blocca alla radice.
 */
class RevalidationTest {

    private val requestedCacheControl = mutableListOf<String?>()

    private fun client(body: String): HttpClient = HttpClient(
        MockEngine { request ->
            requestedCacheControl += request.headers[HttpHeaders.CacheControl]
            respond(
                body,
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    )

    @Test
    fun `i tassi correnti vengono sempre rivalidati con la fonte`() = runTest {
        FrankfurterProvider(
            httpClient = client(fixture("frankfurter_latest.json")),
            clock = FIXED_CLOCK,
        ).fetchLatest()

        assertEquals(listOf<String?>("max-age=0"), requestedCacheControl)
    }

    @Test
    fun `anche le serie storiche vengono rivalidate`() = runTest {
        FrankfurterProvider(
            httpClient = client(fixture("frankfurter_series.json")),
            clock = FIXED_CLOCK,
        ).fetchSeries(
            base = Currency.EUR,
            quote = Currency.USD,
            from = LocalDate.of(2026, 8, 1),
            to = LocalDate.of(2026, 8, 9),
        )

        assertEquals(listOf<String?>("max-age=0"), requestedCacheControl)
    }
}
