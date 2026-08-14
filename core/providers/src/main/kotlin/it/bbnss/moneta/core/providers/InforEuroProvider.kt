package it.bbnss.moneta.core.providers

import it.bbnss.moneta.core.model.Currency
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
 * InforEuro — tassi ufficiali della Commissione Europea, 152 valute.
 *
 * A differenza di tutte le altre fonti pubblica **una volta al mese** e il
 * valore resta fisso per tutto il mese. Non è una fonte da viaggio: serve a chi
 * deve rendicontare una spesa con il cambio ufficiale UE, dove usare il tasso
 * del giorno sarebbe sbagliato. Per questo la sua cadenza è dichiarata come
 * [UpdateCadence.MONTHLY] e l'indicatore di freschezza non deve allarmare.
 */
class InforEuroProvider(
    private val httpClient: HttpClient,
    private val clock: Clock = Clock.systemUTC(),
) : RateProvider {

    override val id: ProviderId = ProviderId.INFOR_EURO

    override val infoUrl: String =
        "https://commission.europa.eu/funding-tenders/procedures-guidelines-tenders/information-contractors-and-beneficiaries/exchange-rate-inforeuro_en"

    override val capabilities = ProviderCapabilities(
        currencyCount = 152,
        cadence = UpdateCadence.MONTHLY,
    )

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchLatest(): ProviderResult<RateSnapshot> =
        when (val response = httpClient.fetchText(ENDPOINT)) {
            is ProviderResult.Failure -> response
            is ProviderResult.Success -> parse(response.value)
        }

    internal fun parse(body: String): ProviderResult<RateSnapshot> = parseResponse(displayName) {
        val rows = json.parseToJsonElement(body) as? JsonArray ?: return@parseResponse null

        val rates = mutableMapOf<Currency, BigDecimal>()
        for (element in rows) {
            val row = element as? JsonObject ?: continue
            val code = (row["isoA3Code"] as? JsonPrimitive)?.content
            val currency = Currency.parse(code) ?: continue
            val value = row["value"]?.asBigDecimalOrNull() ?: continue
            if (value.signum() <= 0) continue
            // Il valore è già "quante unità per 1 euro": nessuna inversione.
            rates[currency] = value
        }

        if (rates.isEmpty()) return@parseResponse null

        // La risposta non porta una data. Il tasso vale per il mese in corso,
        // quindi lo datiamo al primo giorno del mese.
        val today = LocalDate.now(clock)

        RateSnapshot(
            provider = id,
            pivot = Currency.EUR,
            rates = rates,
            rateDate = today.withDayOfMonth(1),
            fetchedAt = clock.instant(),
        )
    }

    private companion object {
        const val ENDPOINT = "https://ec.europa.eu/budg/inforeuro/api/public/monthly-rates?lang=EN"
    }
}
