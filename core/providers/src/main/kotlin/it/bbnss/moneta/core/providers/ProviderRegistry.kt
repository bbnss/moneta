package it.bbnss.moneta.core.providers

import it.bbnss.moneta.core.model.ProviderId
import io.ktor.client.HttpClient
import java.time.Clock

/**
 * Elenco delle fonti disponibili e ordine in cui provarle.
 *
 * @param customBaseUrl istanza Frankfurter self-hostata configurata dall'utente.
 *   È la risposta al caso in cui tutti gli endpoint pubblici siano bloccati:
 *   chi ha un proprio server continua a usare l'app senza modificarla.
 */
class ProviderRegistry internal constructor(val all: List<RateProvider>) {

    constructor(
        httpClient: HttpClient,
        customBaseUrl: String? = null,
        clock: Clock = Clock.systemUTC(),
    ) : this(
        buildList {
            add(FrankfurterProvider(httpClient, clock = clock))
            add(FawazahmedProvider(httpClient, clock))
            add(ExchangeRateApiProvider(httpClient, clock))
            add(EcbProvider(httpClient, clock))
            add(BankOfCanadaProvider(httpClient, clock))
            add(NorgesBankProvider(httpClient, clock))
            add(InforEuroProvider(httpClient, clock))
            add(BankRossiiProvider(httpClient, clock))
            if (!customBaseUrl.isNullOrBlank()) {
                add(
                    FrankfurterProvider(
                        httpClient = httpClient,
                        baseUrl = customBaseUrl.trimEnd('/'),
                        id = ProviderId.CUSTOM,
                        clock = clock,
                    ),
                )
            }
        },
    )

    private val byId: Map<ProviderId, RateProvider> = all.associateBy { it.id }

    operator fun get(id: ProviderId): RateProvider? = byId[id]

    /**
     * Ordine predefinito del failover automatico: prima copertura e storico,
     * poi le fonti su CDN (raggiungibili dove le API dirette non lo sono),
     * infine le banche centrali, che coprono poche valute ma difficilmente
     * spariscono.
     *
     * InforEuro è deliberatamente **escluso**: pubblica una volta al mese, e
     * subentrare in automatico significherebbe mostrare un tasso di settimane
     * prima subito dopo un aggiornamento riuscito, senza che nulla nell'età del
     * dato lo lasci intuire. Resta selezionabile a mano da chi lo cerca, cioè
     * chi deve rendicontare col cambio ufficiale UE.
     */
    fun failoverChain(preferred: ProviderId): List<RateProvider> {
        val ordered = LinkedHashSet<ProviderId>()
        ordered.add(preferred)
        ordered.addAll(DEFAULT_ORDER)
        return ordered.mapNotNull { byId[it] }
    }

    companion object {
        val DEFAULT_PROVIDER = ProviderId.FRANKFURTER

        private val DEFAULT_ORDER = listOf(
            ProviderId.FRANKFURTER,
            ProviderId.FAWAZAHMED0,
            ProviderId.EXCHANGERATE_API,
            ProviderId.ECB,
            ProviderId.BANK_OF_CANADA,
            ProviderId.NORGES_BANK,
            ProviderId.BANK_ROSSII,
        )
    }
}
