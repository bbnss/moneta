package it.bbnss.moneta.core.providers

import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.RateSnapshot
import it.bbnss.moneta.core.providers.internal.attributeOrNull
import it.bbnss.moneta.core.providers.internal.elements
import it.bbnss.moneta.core.providers.internal.fetchText
import it.bbnss.moneta.core.providers.internal.parseResponse
import it.bbnss.moneta.core.providers.internal.parseXml
import io.ktor.client.HttpClient
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

/**
 * Feed XML giornaliero della Banca Centrale Europea.
 *
 * Copre solo 29 valute, quindi non è mai la scelta migliore per copertura. Sta
 * in elenco perché è l'unica fonte che non è un'API: è un file statico
 * pubblicato dalla banca centrale stessa, senza intermediari, senza chiavi e
 * senza aziende che possono chiudere. Quando tutto il resto smette di
 * funzionare — ed è successo davvero ai progetti concorrenti, che hanno perso
 * due fonti per chiusura del servizio — questo continua a rispondere.
 */
class EcbProvider(
    private val httpClient: HttpClient,
    private val clock: Clock = Clock.systemUTC(),
) : RateProvider {

    override val id: ProviderId = ProviderId.ECB

    override val infoUrl: String =
        "https://www.ecb.europa.eu/stats/policy_and_exchange_rates/euro_reference_exchange_rates/html/index.en.html"

    override val capabilities = ProviderCapabilities(
        currencyCount = 30,
        cadence = UpdateCadence.DAILY_BUSINESS,
    )

    override suspend fun fetchLatest(): ProviderResult<RateSnapshot> =
        when (val response = httpClient.fetchText(DAILY_URL)) {
            is ProviderResult.Failure -> response
            is ProviderResult.Success -> parse(response.value)
        }

    internal fun parse(body: String): ProviderResult<RateSnapshot> = parseResponse(displayName) {
        val document = parseXml(body)

        var rateDate: LocalDate? = null
        val rates = mutableMapOf<Currency, BigDecimal>()

        // Il feed annida tre livelli di <Cube>: il contenitore, quello che porta
        // la data e uno per valuta. Si scorrono tutti in piano, prendendo da
        // ciascuno gli attributi che ha.
        for (element in document.elements("Cube")) {
            element.attributeOrNull("time")?.let { time ->
                rateDate = runCatching { LocalDate.parse(time) }.getOrNull()
            }

            val code = element.attributeOrNull("currency") ?: continue
            val rate = element.attributeOrNull("rate") ?: continue
            val currency = Currency.parse(code) ?: continue
            val value = runCatching { BigDecimal(rate) }.getOrNull() ?: continue
            rates[currency] = value
        }

        val date = rateDate
        if (rates.isEmpty() || date == null) null
        else RateSnapshot(
            provider = id,
            pivot = Currency.EUR,
            rates = rates,
            rateDate = date,
            fetchedAt = clock.instant(),
        )
    }

    private companion object {
        const val DAILY_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
    }
}
