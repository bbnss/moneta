package it.bbnss.moneta.core.providers

import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.MonetaryMath
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.RateSnapshot
import it.bbnss.moneta.core.providers.internal.asBigDecimalOrNull
import it.bbnss.moneta.core.providers.internal.fetchText
import it.bbnss.moneta.core.providers.internal.parseResponse
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

/**
 * Norges Bank, in formato SDMX-JSON.
 *
 * È il parser più laborioso dell'insieme perché SDMX non mette i valori accanto
 * alle loro etichette: le serie hanno chiavi posizionali come `0:1:0:0`, dove
 * ogni numero è un indice dentro l'elenco dei valori della dimensione
 * corrispondente. Il codice valuta va quindi risolto passando per
 * `structure.dimensions.series`.
 *
 * Il dettaglio che rovina i conti se ignorato è `UNIT_MULT`: molte valute sono
 * quotate **per 100 unità** (146,95 corone per 100 corone danesi, non per una).
 * Anche il moltiplicatore è un indice dentro l'elenco dei valori
 * dell'attributo. Come le altre banche centrali, quota la valuta locale per
 * unità estera, cioè l'inverso della nostra convenzione.
 */
class NorgesBankProvider(
    private val httpClient: HttpClient,
    private val clock: Clock = Clock.systemUTC(),
) : RateProvider {

    override val id: ProviderId = ProviderId.NORGES_BANK

    override val infoUrl: String = "https://app.norges-bank.no/query/index.html#/no/"

    override val capabilities = ProviderCapabilities(
        currencyCount = 41,
        cadence = UpdateCadence.DAILY_BUSINESS,
    )

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchLatest(): ProviderResult<RateSnapshot> =
        when (val response = httpClient.fetchText(ENDPOINT)) {
            is ProviderResult.Failure -> response
            is ProviderResult.Success -> parse(response.value)
        }

    internal fun parse(body: String): ProviderResult<RateSnapshot> = parseResponse(displayName) {
        val data = (json.parseToJsonElement(body) as? JsonObject)?.get("data") as? JsonObject
            ?: return@parseResponse null
        val structure = data["structure"] as? JsonObject ?: return@parseResponse null

        val seriesDimensions = (structure["dimensions"] as? JsonObject)
            ?.get("series") as? JsonArray ?: return@parseResponse null

        // Posizione di BASE_CUR nella chiave della serie, e relativi codici.
        val baseCurPosition = seriesDimensions.indexOfFirst { it.idField() == "BASE_CUR" }
        if (baseCurPosition < 0) return@parseResponse null
        val currencyCodes = (seriesDimensions[baseCurPosition] as JsonObject).valueIds()

        val observationDates = ((structure["dimensions"] as? JsonObject)
            ?.get("observation") as? JsonArray)
            ?.firstOrNull { it.idField() == "TIME_PERIOD" }
            ?.let { (it as JsonObject).valueIds() }
            ?: return@parseResponse null

        // UNIT_MULT è a sua volta un indice dentro l'elenco dei suoi valori.
        val seriesAttributes = (structure["attributes"] as? JsonObject)
            ?.get("series") as? JsonArray
        val unitMultPosition = seriesAttributes?.indexOfFirst { it.idField() == "UNIT_MULT" } ?: -1
        val unitMultValues = unitMultPosition
            .takeIf { it >= 0 }
            ?.let { (seriesAttributes!![it] as JsonObject).valueIds() }
            .orEmpty()

        val series = ((data["dataSets"] as? JsonArray)?.firstOrNull() as? JsonObject)
            ?.get("series") as? JsonObject ?: return@parseResponse null

        val latest = mutableMapOf<Currency, Pair<LocalDate, BigDecimal>>()

        for ((key, element) in series) {
            val entry = element as? JsonObject ?: continue

            val positions = key.split(':')
            val currencyIndex = positions.getOrNull(baseCurPosition)?.toIntOrNull() ?: continue
            val currency = Currency.parse(currencyCodes.getOrNull(currencyIndex)) ?: continue

            val multiplier = resolveMultiplier(entry, unitMultPosition, unitMultValues)

            val observations = entry["observations"] as? JsonObject ?: continue
            for ((timeIndex, observation) in observations) {
                val date = timeIndex.toIntOrNull()
                    ?.let { observationDates.getOrNull(it) }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: continue

                val quoted = (observation as? JsonArray)?.firstOrNull()?.asBigDecimalOrNull()
                    ?: continue
                if (quoted.signum() <= 0) continue

                val previous = latest[currency]
                if (previous == null || date.isAfter(previous.first)) {
                    // Da "corone per <multiplier> unità estere" a
                    // "unità estere per 1 corona".
                    latest[currency] = date to multiplier.divide(quoted, MonetaryMath.CONTEXT)
                }
            }
        }

        if (latest.isEmpty()) return@parseResponse null

        RateSnapshot(
            provider = id,
            pivot = Currency.NOK,
            rates = latest.mapValues { it.value.second },
            rateDate = latest.values.maxOf { it.first },
            fetchedAt = clock.instant(),
        )
    }

    /** `UNIT_MULT` è l'esponente di 10: "2" significa quotazione per 100 unità. */
    private fun resolveMultiplier(
        entry: JsonObject,
        unitMultPosition: Int,
        unitMultValues: List<String>,
    ): BigDecimal {
        if (unitMultPosition < 0) return BigDecimal.ONE
        val attributes = entry["attributes"] as? JsonArray ?: return BigDecimal.ONE
        val index = (attributes.getOrNull(unitMultPosition) as? JsonPrimitive)
            ?.content?.toIntOrNull() ?: return BigDecimal.ONE
        val exponent = unitMultValues.getOrNull(index)?.toIntOrNull() ?: return BigDecimal.ONE
        return BigDecimal.TEN.pow(exponent)
    }

    private fun kotlinx.serialization.json.JsonElement.idField(): String? =
        ((this as? JsonObject)?.get("id") as? JsonPrimitive)?.content

    private fun JsonObject.valueIds(): List<String> =
        (this["values"] as? JsonArray)
            ?.mapNotNull { ((it as? JsonObject)?.get("id") as? JsonPrimitive)?.content }
            .orEmpty()

    private companion object {
        const val ENDPOINT =
            "https://data.norges-bank.no/api/data/EXR/B..NOK.SP?lastNObservations=1&format=sdmx-json"
    }
}
