package it.bbnss.moneta.core.providers

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * I parser dei provider vengono verificati contro risposte **registrate**, mai
 * contro la rete: un test che chiama l'API vera fallisce quando si è offline,
 * quando la fonte è lenta e ogni volta che i tassi cambiano. La verifica che
 * gli endpoint reali siano ancora vivi è un lavoro separato, affidato al job
 * settimanale di liveness in CI.
 */
internal fun fixture(name: String): String =
    requireNotNull(object {}.javaClass.getResource("/fixtures/$name")) {
        "Fixture mancante: $name"
    }.readText()

internal val FIXED_CLOCK: Clock =
    Clock.fixed(Instant.parse("2026-08-09T18:00:00Z"), ZoneOffset.UTC)

internal fun clientReturning(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpClient = HttpClient(
    MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) },
)

/** Client che fallisce sempre, per verificare la classificazione degli errori. */
internal fun clientFailing(error: Throwable): HttpClient =
    HttpClient(MockEngine { throw error })
