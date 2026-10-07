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
import it.bbnss.moneta.core.model.RequestKind
import it.bbnss.moneta.core.model.NetworkBlock
import it.bbnss.moneta.core.model.UpdatePolicy
import it.bbnss.moneta.core.model.CustomEndpoint
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
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
    val snapshots: List<RateSnapshot> = listOfNotNull(snapshot),
    val today: LocalDate = LocalDate.now(),

) {
    /**
     * I dati mostrati vengono da una fonte diversa da quella scelta. Non è un
     * errore, ma va detto: fonti diverse danno numeri diversi.
     */
    fun snapshotFor(required: Collection<Currency>): RateSnapshot? =
        snapshots.firstOrNull { row -> required.all(row::supports) }

    fun forPair(from: Currency, to: Currency): RatesState {
        val selected = snapshotFor(listOf(from, to))
        val date = selected?.dateFor(from, to)
        return copy(snapshot = selected, freshness = FreshnessRules.of(date, today),
            age = FreshnessRules.rateAge(date, today))
    }

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
    data class Blocked(val reason: NetworkBlock) : SeriesResult
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
    data class Blocked(val reason: NetworkBlock) : RefreshResult
}

class RateRepository(
    private val dao: RateDao,
    private val settings: SettingsStore,
    private val seedLoader: () -> RateSnapshot?,
    private val registryFactory: (String?) -> ProviderRegistry,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val networkStatus: () -> NetworkStatus = { NetworkStatus(true, true) },
) {

    /**
     * Il database è l'unica sorgente di verità della UI: la rete scrive qui, e
     * la schermata non aspetta mai una risposta di rete per mostrare qualcosa.
     */
    private val time = MutableStateFlow(clock.instant())
    private val refreshMutex = Mutex()

    fun updateTime() { time.value = clock.instant() }

    val state: Flow<RatesState> = combine(
        dao.observeAll(), settings.preferredProvider, settings.customEndpoint, time,
    ) { rows, preferred, endpoint, now ->
        val snapshots = rows.mapNotNull { it.toDomain() }.filter {
            it.provider != ProviderId.CUSTOM || (endpoint != null && it.endpoint == endpoint)
        }
        val snapshot = snapshots.firstOrNull()
        val today = now.atZone(clock.zone).toLocalDate()
        RatesState(snapshot, null, null, preferred, snapshots, today)
    }

    private suspend fun block(kind: RequestKind, required: Set<Currency>? = null): NetworkBlock? {
        val current = settings.snapshotOfSettings()
        val network = networkStatus()
        val cached = if (required == null) null else state.first().snapshotFor(required)
        val age = if (required == null) ageOfNewestData() else cached?.let { FreshnessRules.ageOf(it.fetchedAt, clock.instant()) }
        return UpdatePolicy.block(kind, current.offlineMode, current.wifiOnly, current.intervalHours,
            network.connected, network.wifi, age)
    }

    suspend fun saveEndpoint(raw: String) = refreshMutex.withLock {
        val normalized = if (raw.isBlank()) null else requireNotNull(CustomEndpoint.normalize(raw))
        if (normalized != settings.customEndpoint.first()) {
            dao.deleteSnapshot(ProviderId.CUSTOM.stableId)
            dao.deleteSeries(ProviderId.CUSTOM.stableId)
            settings.setCustomEndpoint(normalized)
        }
    }

    suspend fun verifyEndpoint(raw: String): ProviderResult<RateSnapshot> {
        val endpoint = CustomEndpoint.normalize(raw) ?: return ProviderResult.Failure(FailureReason.MALFORMED_RESPONSE, "Invalid HTTPS endpoint")
        val blocked = block(RequestKind.VERIFY)
        if (blocked != null) return ProviderResult.Failure(FailureReason.NETWORK, blocked.name)
        return registryFactory(endpoint)[ProviderId.CUSTOM]!!.fetchLatest()
    }

    /**
     * Popola il database con i tassi inclusi nell'APK, se è ancora vuoto.
     *
     * Va invocato all'avvio prima di qualunque tentativo di rete: se l'utente
     * apre l'app per la prima volta in aereo deve comunque poter convertire.
     */
    suspend fun ensureSeeded(): Boolean = refreshMutex.withLock {
        if (dao.snapshotCount() > 0) return@withLock false
        val seed = seedLoader() ?: return@withLock false
        persist(seed)
        true
    }

    suspend fun refresh(required: Set<Currency>? = null, kind: RequestKind = RequestKind.MANUAL): RefreshResult = refreshMutex.withLock {
        val currencies = required ?: setOf(settings.baseCurrency.first(), settings.quoteCurrency.first())
        val blocked = block(kind, currencies)
        if (blocked != null) return@withLock if (blocked == NetworkBlock.OFFLINE) RefreshResult.SkippedOffline else RefreshResult.Blocked(blocked)
        val current = settings.snapshotOfSettings()
        val registry = registryFactory(current.customEndpoint)
        val outcome = FailoverFetcher(registry).fetchLatest(current.preferredProvider, current.allowFailover, currencies, beforeFetch = { block(kind, currencies) == null })
        when (outcome) {
            is FetchOutcome.Fetched -> {
                // Recheck user policy after a suspended fetch before accepting its result.
                val latest = settings.snapshotOfSettings()
                if (latest.offlineMode) return@withLock RefreshResult.SkippedOffline
                persist(outcome.snapshot.copy(endpoint = if (outcome.snapshot.provider == ProviderId.CUSTOM) current.customEndpoint else null))
                updateTime()
                RefreshResult.Updated(outcome.snapshot.provider, outcome.substituted, outcome.attempts)
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
    ): SeriesResult = refreshMutex.withLock {
        val today = LocalDate.now(clock)
        val from = range.startDate(today)

        val current = settings.snapshotOfSettings()
        val registry = registryFactory(current.customEndpoint)
        val chain = if (current.allowFailover) registry.failoverChain(current.preferredProvider) else listOfNotNull(registry[current.preferredProvider])

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
            if (reachesToday && reachesBack) return@withLock SeriesResult.Available(cached)
        }

        val blocked = block(RequestKind.HISTORY)
        if (blocked != null) {
            return@withLock newestCached?.let { SeriesResult.Available(it) }
                ?: if (blocked == NetworkBlock.OFFLINE) SeriesResult.Offline else SeriesResult.Blocked(blocked)
        }

        // Chi sa rispondere lo dice rispondendo: si interroga la catena invece
        // di fidarsi di una capacità dichiarata. Le fonti che non offrono serie
        // storiche restituiscono UNSUPPORTED senza toccare la rete.
        var failure: FailureReason? = null
        for (provider in chain) {
            if (block(RequestKind.HISTORY) != null) break
            when (val fetched = provider.fetchSeries(base, quote, from, today)) {
                is ProviderResult.Success -> {
                    dao.insertSeriesPoints(fetched.value.toEntities())
                    return@withLock SeriesResult.Available(fetched.value)
                }

                is ProviderResult.Failure ->
                    if (fetched.reason != FailureReason.UNSUPPORTED) failure = fetched.reason
            }
        }

        // Se la rete non risponde resta comunque ciò che avevamo salvato.
        newestCached?.let { return@withLock SeriesResult.Available(it) }

        return@withLock failure?.let { SeriesResult.Unavailable(it) } ?: SeriesResult.Unsupported
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
