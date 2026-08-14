package it.bbnss.moneta.core.providers

import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.RateSnapshot
import it.bbnss.moneta.core.providers.internal.asBigDecimalOrNull
import it.bbnss.moneta.core.providers.internal.fetchText
import it.bbnss.moneta.core.providers.internal.parseResponse
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

/**
 * [fawazahmed0/exchange-api](https://github.com/fawazahmed0/exchange-api) —
 * 338 codici, crypto e metalli inclusi, licenza CC0.
 *
 * Il motivo per cui è il primo fallback non è la copertura ma **dove vive**:
 * i dati sono file statici serviti da CDN (jsDelivr, con un mirror su
 * Cloudflare Pages) invece che da un endpoint applicativo. Dove un'API diretta
 * è bloccata o irraggiungibile, una CDN spesso continua a rispondere. Se il
 * primo host fallisce si prova il secondo prima di dichiarare l'errore: è un
 * fallback interno al provider, distinto da quello fra provider diversi.
 */
class FawazahmedProvider(
    private val httpClient: HttpClient,
    private val clock: Clock = Clock.systemUTC(),
) : RateProvider {

    override val id: ProviderId = ProviderId.FAWAZAHMED0

    override val infoUrl: String = "https://github.com/fawazahmed0/exchange-api"

    override val capabilities = ProviderCapabilities(
        currencyCount = 338,
        cadence = UpdateCadence.DAILY_BUSINESS,
        historical = true,
        timeSeries = false,
        includesCrypto = true,
        includesMetals = true,
    )

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchLatest(): ProviderResult<RateSnapshot> = fetchSnapshot("latest")

    override suspend fun fetchAt(date: LocalDate): ProviderResult<RateSnapshot> =
        fetchSnapshot(date.toString())

    private suspend fun fetchSnapshot(version: String): ProviderResult<RateSnapshot> {
        val paths = mirrorsFor(version)

        var lastFailure: ProviderResult.Failure? = null
        for (url in paths) {
            when (val response = httpClient.fetchText(url)) {
                is ProviderResult.Success -> return parse(response.value)
                is ProviderResult.Failure -> lastFailure = response
            }
        }
        return lastFailure ?: ProviderResult.Failure(
            FailureReason.NETWORK,
            "$displayName: nessun mirror raggiungibile",
        )
    }

    private fun mirrorsFor(version: String): List<String> = listOf(
        "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@$version/v1/currencies/$PIVOT_LOWER.min.json",
        "https://$version.currency-api.pages.dev/v1/currencies/$PIVOT_LOWER.min.json",
    )

    private fun parse(body: String): ProviderResult<RateSnapshot> = parseResponse(displayName) {
        val root = json.parseToJsonElement(body) as? JsonObject ?: return@parseResponse null

        val date = (root["date"] as? JsonPrimitive)?.content
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return@parseResponse null

        val table = root[PIVOT_LOWER] as? JsonObject ?: return@parseResponse null

        val rates = mutableMapOf<Currency, BigDecimal>()
        for ((code, element) in table) {
            val currency = Currency.parse(code) ?: continue
            val rate = element.asBigDecimalOrNull() ?: continue
            rates[currency] = rate
        }

        if (rates.isEmpty()) null
        else RateSnapshot(
            provider = id,
            pivot = Currency.EUR,
            rates = rates,
            rateDate = date,
            fetchedAt = clock.instant(),
        )
    }

    private companion object {
        /** Questa API usa codici minuscoli nei percorsi e nelle chiavi. */
        const val PIVOT_LOWER = "eur"
    }
}
