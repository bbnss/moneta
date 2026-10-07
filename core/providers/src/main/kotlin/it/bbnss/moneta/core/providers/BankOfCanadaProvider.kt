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
 * Bank of Canada, servizio Valet — fonte ufficiale nordamericana.
 *
 * Il pivot è il dollaro canadese e le serie si chiamano `FXAUDCAD`, cioè
 * "dollari canadesi per 1 dollaro australiano": la direzione è **opposta** alla
 * convenzione dello snapshot, quindi ogni tasso va invertito.
 *
 * L'altra trappola è che `recent=1` restituisce l'ultima osservazione *per
 * serie*, non l'ultima giornata: le valute dismesse (il dong vietnamita è
 * fermo al 2019) arrivano in righe con date diverse. Si tiene il valore più
 * recente per ciascuna valuta, e come data dello snapshot la più recente in
 * assoluto.
 */
class BankOfCanadaProvider(
    private val httpClient: HttpClient,
    private val clock: Clock = Clock.systemUTC(),
) : RateProvider {

    override val id: ProviderId = ProviderId.BANK_OF_CANADA

    override val infoUrl: String = "https://www.bankofcanada.ca/valet/docs"

    override val capabilities = ProviderCapabilities(
        currencyCount = 27,
        cadence = UpdateCadence.DAILY_BUSINESS,
    )

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchLatest(): ProviderResult<RateSnapshot> =
        when (val response = httpClient.fetchText(ENDPOINT)) {
            is ProviderResult.Failure -> response
            is ProviderResult.Success -> parse(response.value)
        }

    internal fun parse(body: String): ProviderResult<RateSnapshot> = parseResponse(displayName) {
        val root = json.parseToJsonElement(body) as? JsonObject ?: return@parseResponse null
        val observations = root["observations"] as? JsonArray ?: return@parseResponse null

        // Per ogni valuta teniamo la coppia (data, tasso) più recente.
        val latest = mutableMapOf<Currency, Pair<LocalDate, BigDecimal>>()

        for (element in observations) {
            val row = element as? JsonObject ?: continue
            val date = (row["d"] as? JsonPrimitive)?.content
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: continue

            for ((key, series) in row) {
                val currency = currencyOfSeries(key) ?: continue
                val quoted = (series as? JsonObject)?.get("v")?.asBigDecimalOrNull() ?: continue
                if (quoted.signum() <= 0) continue

                val previous = latest[currency]
                if (previous == null || date.isAfter(previous.first)) {
                    latest[currency] = date to quoted
                }
            }
        }

        if (latest.isEmpty()) return@parseResponse null

        val rates = latest.mapValues { (_, entry) ->
            // Da "CAD per 1 unità estera" a "unità estere per 1 CAD".
            BigDecimal.ONE.divide(entry.second, MonetaryMath.CONTEXT)
        }
        val rateDate = latest.values.maxOf { it.first }

        RateSnapshot(
            provider = id,
            pivot = Currency.CAD,
            rates = rates,
            rateDate = rateDate,
            fetchedAt = clock.instant(),
            rateDates = latest.mapValues { it.value.first },
        )
    }

    /** `FXAUDCAD` → `AUD`. Ignora qualunque altra chiave, compresa `d`. */
    private fun currencyOfSeries(key: String): Currency? {
        if (key.length != 8 || !key.startsWith("FX") || !key.endsWith("CAD")) return null
        return Currency.parse(key.substring(2, 5))
    }

    private companion object {
        const val ENDPOINT =
            "https://www.bankofcanada.ca/valet/observations/group/FX_RATES_DAILY/json?recent=1"
    }
}
