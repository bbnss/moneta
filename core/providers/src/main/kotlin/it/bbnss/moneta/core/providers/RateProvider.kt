package it.bbnss.moneta.core.providers

import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.RateSeries
import it.bbnss.moneta.core.model.RateSnapshot
import java.time.LocalDate

/** Ogni quanto la fonte pubblica nuovi tassi. */
enum class UpdateCadence {
    /** Una volta per giorno lavorativo, tipicamente nel pomeriggio. */
    DAILY_BUSINESS,

    /** Più volte al giorno. */
    INTRADAY,

    /** Una volta al mese (tassi ufficiali di rendicontazione). */
    MONTHLY,
}

/**
 * Descrizione di una fonte, per aiutare l'utente a sceglierla.
 *
 * Questi campi servono a **descrivere**, non a decidere: il flusso del codice
 * non deve mai dipendere da uno di questi booleani. Chi sa fare una cosa lo
 * dimostra facendola, e chi non sa risponde [FailureReason.UNSUPPORTED].
 *
 * Non è una preferenza di stile: R8 può ridurre a costante un booleano con
 * valore predefinito quando non vede tutti i punti in cui viene assegnato, e la
 * funzione che lo interrogava si è ritrovata a non selezionare più nessuna
 * fonte — solo nelle build di release, dove è più difficile accorgersene.
 */
data class ProviderCapabilities(
    /** Numero indicativo di valute coperte, per orientare la scelta dell'utente. */
    val currencyCount: Int,
    val cadence: UpdateCadence,
    /** Sa restituire i tassi di una data passata. */
    val historical: Boolean = false,
    /** Sa restituire una serie storica per il grafico. */
    val timeSeries: Boolean = false,
    val includesCrypto: Boolean = false,
    val includesMetals: Boolean = false,
)

/**
 * Contratto di una fonte dei tassi.
 *
 * Le implementazioni non conoscono né la cache né la UI: prendono dalla rete,
 * normalizzano e restituiscono. Tutto ciò che riguarda persistenza, failover e
 * anzianità del dato vive in `:core:data`, così questi parser restano
 * testabili sulla JVM contro risposte registrate.
 */
interface RateProvider {

    val id: ProviderId

    /** Nome mostrato all'utente. Deriva dall'id: un nome proprio, non tradotto. */
    val displayName: String get() = id.displayName

    /** Pagina informativa della fonte, linkata dalle impostazioni. */
    val infoUrl: String

    val capabilities: ProviderCapabilities

    suspend fun fetchLatest(): ProviderResult<RateSnapshot>

    suspend fun fetchAt(date: LocalDate): ProviderResult<RateSnapshot> =
        ProviderResult.Failure(FailureReason.UNSUPPORTED, "$displayName non offre tassi storici")

    suspend fun fetchSeries(
        base: Currency,
        quote: Currency,
        from: LocalDate,
        to: LocalDate,
    ): ProviderResult<RateSeries> =
        ProviderResult.Failure(FailureReason.UNSUPPORTED, "$displayName non offre serie storiche")
}
