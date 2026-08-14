package it.bbnss.moneta.core.providers

import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.RatePoint
import it.bbnss.moneta.core.model.RateSeries
import it.bbnss.moneta.core.model.RateSnapshot
import it.bbnss.moneta.core.providers.internal.asBigDecimalOrNull
import it.bbnss.moneta.core.providers.internal.fetchText
import it.bbnss.moneta.core.providers.internal.parseResponse
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

/**
 * [Frankfurter](https://frankfurter.dev) v2 — la fonte predefinita.
 *
 * Aggrega 84 banche centrali in 201 valute con storico fino al 1948, senza
 * chiave e senza quote. È un salto netto rispetto alla v1 usata dai progetti
 * concorrenti, ferma a una trentina di valute della sola BCE.
 *
 * I nomi dei parametri sono quelli e solo quelli: `quotes`, `from`, `to`,
 * `date`, `providers`. Le varianti al singolare o in stile `start_date`
 * fanno rispondere 422.
 *
 * La stessa classe serve anche le istanze self-hostate: basta cambiare
 * [baseUrl] e [id], ed è il modo in cui l'app resta utilizzabile dove gli
 * endpoint pubblici sono bloccati.
 */
class FrankfurterProvider(
    private val httpClient: HttpClient,
    private val baseUrl: String = DEFAULT_BASE_URL,
    override val id: ProviderId = ProviderId.FRANKFURTER,
    private val clock: Clock = Clock.systemUTC(),
) : RateProvider {

    override val infoUrl: String = "https://frankfurter.dev"

    override val capabilities = ProviderCapabilities(
        currencyCount = 201,
        cadence = UpdateCadence.DAILY_BUSINESS,
        historical = true,
        timeSeries = true,
    )

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchLatest(): ProviderResult<RateSnapshot> =
        fetchSnapshot("$baseUrl/v2/rates?base=$PIVOT")

    override suspend fun fetchAt(date: LocalDate): ProviderResult<RateSnapshot> =
        fetchSnapshot("$baseUrl/v2/rates?base=$PIVOT&date=$date")

    override suspend fun fetchSeries(
        base: Currency,
        quote: Currency,
        from: LocalDate,
        to: LocalDate,
    ): ProviderResult<RateSeries> {
        val url = "$baseUrl/v2/rates?base=${base.code}&quotes=${quote.code}&from=$from&to=$to"
        return when (val response = httpClient.fetchText(url)) {
            is ProviderResult.Failure -> response
            is ProviderResult.Success -> parseResponse(displayName) {
                val points = json.parseToJsonElement(response.value)
                    .let { it as? JsonArray ?: return@parseResponse null }
                    .mapNotNull { element ->
                        val row = element as? JsonObject ?: return@mapNotNull null
                        val date = row["date"]?.asStringOrNull()?.toLocalDateOrNull()
                        val rate = row["rate"]?.asBigDecimalOrNull()
                        if (date == null || rate == null) null else RatePoint(date, rate)
                    }
                    .sortedBy { it.date }

                if (points.isEmpty()) null
                else RateSeries(provider = id, base = base, quote = quote, points = points)
            }
        }
    }

    private suspend fun fetchSnapshot(url: String): ProviderResult<RateSnapshot> =
        when (val response = httpClient.fetchText(url)) {
            is ProviderResult.Failure -> response
            is ProviderResult.Success -> parseResponse(displayName) {
                val rows = json.parseToJsonElement(response.value) as? JsonArray
                    ?: return@parseResponse null

                val rates = mutableMapOf<Currency, BigDecimal>()
                var latestDate: LocalDate? = null

                for (element in rows) {
                    val row = element as? JsonObject ?: continue
                    val quote = Currency.parse(row["quote"]?.asStringOrNull()) ?: continue
                    val rate = row["rate"]?.asBigDecimalOrNull() ?: continue
                    rates[quote] = rate

                    // Ogni riga porta la propria data: le fonti non pubblicano
                    // tutte lo stesso giorno, quindi una valuta illiquida può
                    // essere ferma a due giorni prima. Come data dello snapshot
                    // teniamo la più recente e la mostriamo come "tassi al ...".
                    val date = row["date"]?.asStringOrNull()?.toLocalDateOrNull()
                    if (date != null && (latestDate == null || date.isAfter(latestDate))) {
                        latestDate = date
                    }
                }

                if (rates.isEmpty() || latestDate == null) return@parseResponse null

                RateSnapshot(
                    provider = id,
                    pivot = PIVOT,
                    rates = rates,
                    rateDate = latestDate,
                    fetchedAt = clock.instant(),
                )
            }
        }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.frankfurter.dev"

        /**
         * L'euro è il pivot naturale di Frankfurter ed è una valuta "forte":
         * i tassi restano numeri di grandezza ragionevole e il cross-rate
         * perde meno cifre significative rispetto a un pivot ad alta
         * denominazione.
         */
        private val PIVOT = Currency.EUR
    }
}

private fun kotlinx.serialization.json.JsonElement.asStringOrNull(): String? =
    (this as? kotlinx.serialization.json.JsonPrimitive)?.content

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this) }.getOrNull()
