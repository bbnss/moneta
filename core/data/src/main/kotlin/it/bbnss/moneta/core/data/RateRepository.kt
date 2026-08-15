package it.bbnss.moneta.core.data

import it.bbnss.moneta.core.data.db.RateDao
import it.bbnss.moneta.core.data.db.toDomain
import it.bbnss.moneta.core.data.db.toEntities
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.Freshness
import it.bbnss.moneta.core.model.FreshnessRules
import it.bbnss.moneta.core.model.HistoryRange
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.RateSeries
import it.bbnss.moneta.core.model.RateSnapshot
import it.bbnss.moneta.core.providers.FailedAttempt
import it.bbnss.moneta.core.providers.FailureReason
import it.bbnss.moneta.core.providers.FailoverFetcher
import it.bbnss.moneta.core.providers.FetchOutcome
import it.bbnss.moneta.core.providers.ProviderRegistry
import it.bbnss.moneta.core.providers.ProviderResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import java.time.Duration

/**
 * Cosa deve mostrare la schermata, in ogni momento.
 *
 * [snapshot] è `null` soltanto nell'istante che precede il caricamento del
 * seed. Da lì in poi c'è sempre qualcosa da mostrare: è questo che rende l'app
 * utilizzabile senza rete.
 */
data class RatesState(
    val snapshot: RateSnapshot?,
    val freshness: Freshness?,
    val age: Duration?,
    val preferredProvider: ProviderId,
) {
    /**
     * I dati mostrati vengono da una fonte diversa da quella scelta. Non è un
     * errore, ma va detto: fonti diverse danno numeri diversi.
     */
    val substituted: Boolean
        get() = snapshot != null && snapshot.provider != preferredProvider
}

/**
 * Esito della richiesta di una serie storica.
 *
 * I casi sono distinti perché richiedono risposte diverse dall'utente: una
 * fonte che non pubblica storici non è un guasto e non serve riprovare, mentre
 * un errore di rete sì. Un generico "nessuno storico" non direbbe quale dei due
 * è, lasciando l'utente senza nulla da fare.
 */
sealed interface SeriesResult {

    data class Available(val series: RateSeries) : SeriesResult

    /** Nessuna delle fonti disponibili pubblica serie storiche. */
    data object Unsupported : SeriesResult

    /** La fonte c'è ma non ha risposto: vale la pena riprovare. */
    data class Unavailable(val reason: FailureReason) : SeriesResult

    /** Modalità offline attiva e nulla in cache per questo intervallo. */
    data object Offline : SeriesResult
}

sealed interface RefreshResult {

    data class Updated(
        val provider: ProviderId,
        val substituted: Boolean,
        val attempts: List<FailedAttempt>,
    ) : RefreshResult

    data class Failed(val attempts: List<FailedAttempt>) : RefreshResult

    /** L'utente ha disattivato ogni traffico: non è un errore. */
    data object SkippedOffline : RefreshResult
}

class RateRepository(
    private val dao: RateDao,
    private val settings: SettingsStore,
    private val seedLoader: SeedLoader,
    private val registryFactory: (String?) -> ProviderRegistry,
    private val clock: Clock = Clock.systemUTC(),
) {

    /**
     * Il database è l'unica sorgente di verità della UI: la rete scrive qui, e
     * la schermata non aspetta mai una risposta di rete per mostrare qualcosa.
     */
    val state: Flow<RatesState> = combine(
        dao.observeMostRecent(),
        settings.preferredProvider,
    ) { row, preferred ->
        val snapshot = row?.toDomain()
        RatesState(
            snapshot = snapshot,
            freshness = snapshot?.let { FreshnessRules.of(it.fetchedAt, clock.instant()) },
            age = snapshot?.let { FreshnessRules.ageOf(it.fetchedAt, clock.instant()) },
            preferredProvider = preferred,
        )
    }

    /**
     * Popola il database con i tassi inclusi nell'APK, se è ancora vuoto.
     *
     * Va invocato all'avvio prima di qualunque tentativo di rete: se l'utente
     * apre l'app per la prima volta in aereo deve comunque poter convertire.
     */
    suspend fun ensureSeeded(): Boolean {
        if (dao.snapshotCount() > 0) return false
        val seed = seedLoader.load() ?: return false
        persist(seed)
        return true
    }

    suspend fun refresh(): RefreshResult {
        val current = settings.snapshotOfSettings()
        if (current.offlineMode) return RefreshResult.SkippedOffline

        val registry = registryFactory(current.customEndpoint)
        val outcome = FailoverFetcher(registry).fetchLatest(
            preferred = current.preferredProvider,
            allowFailover = current.allowFailover,
        )

        return when (outcome) {
            is FetchOutcome.Fetched -> {
                persist(outcome.snapshot)
                RefreshResult.Updated(
                    provider = outcome.snapshot.provider,
                    substituted = outcome.substituted,
                    attempts = outcome.attempts,
                )
            }

            is FetchOutcome.AllFailed -> RefreshResult.Failed(outcome.attempts)
        }
    }

    /**
     * Serie storica di una coppia, per il grafico.
     *
     * Si legge prima dalla cache: chi riapre lo stesso grafico due volte in un
     * giorno non deve riscaricare nulla, e chi è offline vede comunque ciò che
     * aveva già consultato. La rete si interpella solo se la cache non copre
     * l'intervallo richiesto.
     *
     * Non tutte le fonti offrono serie storiche: se quella preferita non lo fa,
     * si usa la prima che ne è capace invece di restituire un grafico vuoto.
     */
    suspend fun series(
        base: Currency,
        quote: Currency,
        range: HistoryRange,
    ): SeriesResult {
        val today = LocalDate.now(clock)
        val from = range.startDate(today)

        val current = settings.snapshotOfSettings()
        val registry = registryFactory(current.customEndpoint)
        val chain = registry.failoverChain(current.preferredProvider)

        // La cache basta solo se copre l'intervallo chiesto da tutte e due le
        // parti.
        //
        // Guardare solo il punto più recente non basta: dopo aver visto sei
        // mesi, chiedere "Max" trovava in cache proprio quei sei mesi, con
        // l'ultimo punto di oggi, e restituiva quelli — un grafico etichettato
        // vent'anni che ne mostrava sei. Dall'altro lato serve tolleranza: le
        // fonti pubblicano nei giorni lavorativi, e nessuna ha uno storico
        // infinito, quindi si accetta uno scarto di qualche giorno.
        var newestCached: RateSeries? = null
        for (provider in chain) {
            val cached = dao.seriesPoints(
                providerId = provider.id.stableId,
                base = base.code,
                quote = quote.code,
                from = from.toString(),
                to = today.toString(),
            ).toDomain(provider.id, base, quote)

            if (cached.points.isEmpty()) continue
            if (newestCached == null) newestCached = cached

            val reachesToday = cached.points.last().date >= today.minusDays(3)
            val reachesBack = cached.points.first().date <= from.plusDays(SERIES_START_TOLERANCE_DAYS)
            if (reachesToday && reachesBack) return SeriesResult.Available(cached)
        }

        if (current.offlineMode) {
            return newestCached?.let { SeriesResult.Available(it) } ?: SeriesResult.Offline
        }

        // Chi sa rispondere lo dice rispondendo: si interroga la catena invece
        // di fidarsi di una capacità dichiarata. Le fonti che non offrono serie
        // storiche restituiscono UNSUPPORTED senza toccare la rete.
        var failure: FailureReason? = null
        for (provider in chain) {
            when (val fetched = provider.fetchSeries(base, quote, from, today)) {
                is ProviderResult.Success -> {
                    dao.insertSeriesPoints(fetched.value.toEntities())
                    return SeriesResult.Available(fetched.value)
                }

                is ProviderResult.Failure ->
                    if (fetched.reason != FailureReason.UNSUPPORTED) failure = fetched.reason
            }
        }

        // Se la rete non risponde resta comunque ciò che avevamo salvato.
        newestCached?.let { return SeriesResult.Available(it) }

        return failure?.let { SeriesResult.Unavailable(it) } ?: SeriesResult.Unsupported
    }

    /** Quanto è vecchio il dato più recente che abbiamo, per decidere se aggiornare. */
    suspend fun ageOfNewestData(): Duration? =
        dao.lastFetchedAt()?.let { Duration.ofMillis(clock.millis() - it) }

    suspend fun currentFavourites() = settings.favourites.first()

    private suspend fun persist(snapshot: RateSnapshot) {
        val (entity, rates) = snapshot.toEntities()
        dao.replaceSnapshot(entity, rates)
    }

    private companion object {
        /**
         * Di quanto può iniziare più tardi la serie in cache rispetto a quella
         * chiesta, restando accettabile.
         *
         * Il primo punto utile cade quasi sempre qualche giorno dopo l'inizio
         * dell'intervallo, fra fine settimana e festività. Una settimana copre
         * anche i ponti più lunghi senza mascherare una cache davvero corta.
         */
        const val SERIES_START_TOLERANCE_DAYS = 7L
    }
}
