package it.bbnss.moneta.core.providers

import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.MonetaryMath
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.RateSnapshot
import it.bbnss.moneta.core.providers.internal.attributeOrNull
import it.bbnss.moneta.core.providers.internal.childText
import it.bbnss.moneta.core.providers.internal.elements
import it.bbnss.moneta.core.providers.internal.fetchText
import it.bbnss.moneta.core.providers.internal.parseResponse
import it.bbnss.moneta.core.providers.internal.parseXml
import io.ktor.client.HttpClient
import java.math.BigDecimal
import java.nio.charset.Charset
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Banca Centrale della Federazione Russa.
 *
 * Sta in elenco per una ragione pratica: è la fonte che resta raggiungibile
 * dalla Russia quando le altre non lo sono. Non è però una buona
 * predefinita fuori dal paese — il progetto concorrente ha una issue aperta
 * proprio perché finiva selezionata a sproposito.
 *
 * Tre particolarità, tutte già viste all'opera sui dati reali:
 * 1. il documento è in **windows-1251**, non UTF-8;
 * 2. i decimali usano la **virgola** (`57,7548`);
 * 3. `Value` si riferisce a `Nominal` unità (100 per lo yen), mentre
 *    `VunitRate` è già normalizzato a una unità: si usa quest'ultimo quando
 *    c'è, con `Value / Nominal` come ripiego.
 *
 * Come Bank of Canada, la banca quota la valuta locale per unità estera, cioè
 * l'inverso della convenzione dello snapshot.
 */
class BankRossiiProvider(
    private val httpClient: HttpClient,
    private val clock: Clock = Clock.systemUTC(),
) : RateProvider {

    override val id: ProviderId = ProviderId.BANK_ROSSII

    override val infoUrl: String = "https://www.cbr.ru/development/SXML/"

    override val capabilities = ProviderCapabilities(
        currencyCount = 44,
        cadence = UpdateCadence.DAILY_BUSINESS,
    )

    override suspend fun fetchLatest(): ProviderResult<RateSnapshot> =
        when (val response = httpClient.fetchText(ENDPOINT, WINDOWS_1251)) {
            is ProviderResult.Failure -> response
            is ProviderResult.Success -> parse(response.value)
        }

    internal fun parse(body: String): ProviderResult<RateSnapshot> = parseResponse(displayName) {
        val document = parseXml(body)

        val rateDate = document.documentElement?.attributeOrNull("Date")
            ?.let { runCatching { LocalDate.parse(it, DATE_FORMAT) }.getOrNull() }
            ?: return@parseResponse null

        val rates = mutableMapOf<Currency, BigDecimal>()

        for (valute in document.elements("Valute")) {
            val currency = Currency.parse(valute.childText("CharCode")) ?: continue

            val perUnit = valute.childText("VunitRate")?.toRussianDecimal()
                ?: run {
                    val value = valute.childText("Value")?.toRussianDecimal() ?: return@run null
                    val nominal = valute.childText("Nominal")?.toRussianDecimal()
                        ?: BigDecimal.ONE
                    if (nominal.signum() <= 0) null
                    else value.divide(nominal, MonetaryMath.CONTEXT)
                }
                ?: continue

            if (perUnit.signum() <= 0) continue

            // Da "rubli per 1 unità estera" a "unità estere per 1 rublo".
            rates[currency] = BigDecimal.ONE.divide(perUnit, MonetaryMath.CONTEXT)
        }

        if (rates.isEmpty()) return@parseResponse null

        RateSnapshot(
            provider = id,
            pivot = Currency.RUB,
            rates = rates,
            rateDate = rateDate,
            fetchedAt = clock.instant(),
        )
    }

    private fun String.toRussianDecimal(): BigDecimal? =
        runCatching { BigDecimal(trim().replace(',', '.').replace(" ", "")) }.getOrNull()

    private companion object {
        const val ENDPOINT = "https://www.cbr.ru/scripts/XML_daily.asp"
        val WINDOWS_1251: Charset = Charset.forName("windows-1251")
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
