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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Endpoint aperto di [ExchangeRate-API](https://www.exchangerate-api.com/docs/free):
 * 166 valute, nessuna chiave, infrastruttura completamente indipendente dalle
 * altre fonti in elenco. Serve proprio a questo — se Frankfurter e la CDN sono
 * entrambe irraggiungibili, è improbabile che lo sia anche questa.
 *
 * Espone `time_last_update_unix`, quindi qui la data dei tassi è quella
 * dichiarata dalla fonte e non va dedotta.
 */
class ExchangeRateApiProvider(
    private val httpClient: HttpClient,
    private val clock: Clock = Clock.systemUTC(),
) : RateProvider {

    override val id: ProviderId = ProviderId.EXCHANGERATE_API

    override val infoUrl: String = "https://www.exchangerate-api.com/docs/free"

    override val capabilities = ProviderCapabilities(
        currencyCount = 166,
        cadence = UpdateCadence.DAILY_BUSINESS,
    )

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchLatest(): ProviderResult<RateSnapshot> {
        val url = "https://open.er-api.com/v6/latest/${Currency.EUR.code}"
        return when (val response = httpClient.fetchText(url)) {
            is ProviderResult.Failure -> response
            is ProviderResult.Success -> parse(response.value)
        }
    }

    private fun parse(body: String): ProviderResult<RateSnapshot> {
        val root = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull()
            ?: return ProviderResult.Failure(
                FailureReason.MALFORMED_RESPONSE,
                "$displayName: risposta non interpretabile",
            )

        // L'API risponde 200 anche sugli errori applicativi, segnalandoli nel corpo.
        val result = (root["result"] as? JsonPrimitive)?.content
        if (result != null && result != "success") {
            val detail = (root["error-type"] as? JsonPrimitive)?.content
            return ProviderResult.Failure(
                FailureReason.HTTP_ERROR,
                "$displayName ha risposto '$result'${detail?.let { ": $it" }.orEmpty()}",
            )
        }

        return parseResponse(displayName) {
            val table = root["rates"] as? JsonObject ?: return@parseResponse null

            val rateDate = (root["time_last_update_unix"] as? JsonPrimitive)
                ?.content?.toLongOrNull()
                ?.let { Instant.ofEpochSecond(it).atZone(ZoneOffset.UTC).toLocalDate() }
                ?: LocalDate.now(clock)

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
                rateDate = rateDate,
                fetchedAt = clock.instant(),
            )
        }
    }
}
