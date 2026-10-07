package it.bbnss.moneta.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import it.bbnss.moneta.core.data.db.*
import it.bbnss.moneta.core.model.*
import it.bbnss.moneta.core.providers.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.*

private class MemoryPreferences(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    private val lock = Mutex()
    override val data: Flow<Preferences> = state
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = lock.withLock {
        transform(state.value).also { state.value = it }
    }
}
private class MemoryDao : RateDao {
    val rows = MutableStateFlow<List<SnapshotWithRates>>(emptyList())
    val history = mutableListOf<SeriesPointEntity>()
    var writes = 0
    override fun observeMostRecent() = rows.map { it.maxByOrNull { row -> row.snapshot.fetchedAt } }
    override fun observeAll() = rows.map { it.sortedByDescending { row -> row.snapshot.fetchedAt } }
    override suspend fun get(providerId: Int) = rows.value.firstOrNull { it.snapshot.providerId == providerId }
    override suspend fun snapshotCount() = rows.value.size
    override suspend fun lastFetchedAt() = rows.value.maxOfOrNull { it.snapshot.fetchedAt }
    override suspend fun deleteSnapshot(providerId: Int) { rows.value = rows.value.filter { it.snapshot.providerId != providerId } }
    override suspend fun deleteSeries(providerId: Int) { history.removeAll { it.providerId == providerId } }
    override suspend fun upsertSnapshot(snapshot: SnapshotEntity) {}
    override suspend fun deleteRates(providerId: Int) {}
    override suspend fun insertRates(rates: List<RateEntity>) {}
    override suspend fun replaceSnapshot(snapshot: SnapshotEntity, rates: List<RateEntity>) {
        writes++
        rows.value = rows.value.filter { it.snapshot.providerId != snapshot.providerId } + SnapshotWithRates(snapshot, rates)
    }
    override suspend fun seriesPoints(providerId: Int, base: String, quote: String, from: String, to: String) =
        history.filter { it.providerId == providerId && it.base == base && it.quote == quote && it.date in from..to }.sortedBy { it.date }
    override suspend fun insertSeriesPoints(points: List<SeriesPointEntity>) { history += points }
}
private class MutableClock(var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant() = now
}

class Repository020Test {
    private val now = Instant.parse("2026-10-07T12:00:00Z")
    private fun row(id: ProviderId = ProviderId.FRANKFURTER, fetched: Instant = now.minusSeconds(86400), date: LocalDate = LocalDate.of(2026, 10, 7)) =
        RateSnapshot(id, Currency.EUR, mapOf(Currency.USD to BigDecimal("1.2"), Currency("VND") to BigDecimal("30000")), date, fetched)

    @Test fun `cache selects complete pair and never mixes sources`() = runTest {
        val complete = row()
        val partial = row(ProviderId.ECB, now).copy(rates = mapOf(Currency.USD to BigDecimal("1.3")))
        val rates = RatesState(partial, null, null, ProviderId.ECB, listOf(partial, complete))
        assertEquals(ProviderId.FRANKFURTER, rates.forPair(Currency("VND"), Currency.EUR).snapshot!!.provider)
        assertEquals(BigDecimal("0.00003333333333333333"), rates.forPair(Currency("VND"), Currency.EUR).snapshot!!.crossRate(Currency("VND"), Currency.EUR))
        assertNull(rates.forPair(Currency("CHF"), Currency.EUR).snapshot)
    }
    @Test fun `tick advances freshness without database mutations`() = runTest {
        val dao = MemoryDao()
        val (entity, rates) = row().toEntities(); dao.replaceSnapshot(entity, rates)
        val clock = MutableClock(now)
        val client = HttpClient(MockEngine { error("no network during tick") })
        val repository = RateRepository(dao, SettingsStore(MemoryPreferences()), { null }, { ProviderRegistry(client) }, clock)
        assertEquals(Freshness.FRESH, repository.state.first().forPair(Currency.EUR, Currency.USD).freshness)
        clock.now = now.plusSeconds(86400 * 8); repository.updateTime()
        assertEquals(Freshness.STALE, repository.state.first().forPair(Currency.EUR, Currency.USD).freshness)
        assertEquals(1, dao.writes)
        client.close()
    }
    @Test fun `every blocked request keeps verified time and cache unchanged`() = runTest {
        for (offline in listOf(true, false)) {
            val dao = MemoryDao()
            val original = row(); val (entity, rates) = original.toEntities(); dao.replaceSnapshot(entity, rates)
            val settings = SettingsStore(MemoryPreferences())
            settings.setOfflineMode(offline); settings.setWifiOnly(!offline)
            var requests = 0
            val client = HttpClient(MockEngine { requests++; error("policy must block") })
            val repository = RateRepository(dao, settings, { null }, { ProviderRegistry(client, it) }, MutableClock(now), { NetworkStatus(true, false) })
            repository.refresh(kind = RequestKind.AUTOMATIC)
            repository.refresh(kind = RequestKind.MANUAL)
            repository.series(Currency.EUR, Currency.USD, HistoryRange.SIX_MONTHS)
            repository.verifyEndpoint("https://example.org/api")
            assertEquals(0, requests)
            assertEquals(original.fetchedAt.toEpochMilli(), dao.lastFetchedAt())
            assertEquals(1, dao.writes)
            client.close()
        }
    }
    @Test fun `manual mode blocks startup and worker but permits explicit update`() = runTest {
        val dao = MemoryDao(); val settings = SettingsStore(MemoryPreferences())
        settings.setSyncIntervalHours(-1)
        var requests = 0
        val client = HttpClient(MockEngine { requests++; respond("""[{"date":"2026-10-01","base":"EUR","quote":"USD","rate":1.23456789012345}]""", HttpStatusCode.OK) })
        val repository = RateRepository(dao, settings, { null }, { ProviderRegistry(client, it, MutableClock(now)) }, MutableClock(now))
        assertTrue(repository.refresh(kind = RequestKind.AUTOMATIC) is RefreshResult.Blocked)
        assertEquals(0, requests)
        assertTrue(repository.refresh() is RefreshResult.Updated)
        assertEquals(1, requests)
        assertEquals(Freshness.AGING, repository.state.first().forPair(Currency.EUR, Currency.USD).freshness)
        client.close()
    }
    @Test fun `incomplete response keeps usable cache when failover is disabled`() = runTest {
        val dao = MemoryDao(); val original = row()
        val (entity, rates) = original.toEntities(); dao.replaceSnapshot(entity, rates)
        val settings = SettingsStore(MemoryPreferences()); settings.setAllowFailover(false)
        var requests = 0
        val client = HttpClient(MockEngine { requests++; respond("""[{"date":"2026-10-07","quote":"USD","rate":1.3}]""", HttpStatusCode.OK) })
        val repository = RateRepository(dao, settings, { null }, { ProviderRegistry(client, it, MutableClock(now)) }, MutableClock(now))
        assertTrue(repository.refresh(setOf(Currency("VND"), Currency.EUR)) is RefreshResult.Failed)
        assertEquals(1, requests); assertEquals(1, dao.writes)
        assertEquals(original, repository.state.first().snapshotFor(listOf(Currency("VND"), Currency.EUR)))
        client.close()
    }
    @Test fun `endpoint verification leaves selection and cache intact and changing URL invalidates custom data`() = runTest {
        val dao = MemoryDao(); val settings = SettingsStore(MemoryPreferences())
        settings.setCustomEndpoint("https://old.example/api"); settings.setPreferredProvider(ProviderId.CUSTOM)
        val custom = row(ProviderId.CUSTOM).copy(endpoint = "https://old.example/api")
        val (entity, rates) = custom.toEntities(); dao.replaceSnapshot(entity, rates)
        dao.history += SeriesPointEntity(99, "EUR", "USD", "2026-10-01", "1.2")
        var requests = 0
        val client = HttpClient(MockEngine { requests++; respond("""[{"date":"2026-10-07","quote":"USD","rate":1.3}]""", HttpStatusCode.OK) })
        val repository = RateRepository(dao, settings, { null }, { ProviderRegistry(client, it) }, MutableClock(now))
        assertTrue(repository.verifyEndpoint("https://new.example/sub") is ProviderResult.Success)
        assertEquals(1, requests); assertEquals(ProviderId.CUSTOM, settings.preferredProvider.first()); assertEquals(1, dao.snapshotCount())
        repository.saveEndpoint("https://new.example/sub")
        assertEquals("https://new.example/sub", settings.customEndpoint.first()); assertEquals(0, dao.snapshotCount()); assertTrue(dao.history.isEmpty())
        client.close()
    }
    @Test fun `mapping preserves precision and missing migrated quote dates`() {
        val original = row().copy(rateDates = mapOf(Currency.USD to LocalDate.of(2026, 10, 5)))
        val (entity, rates) = original.toEntities()
        assertEquals(original, SnapshotWithRates(entity, rates).toDomain())
        val migrated = SnapshotWithRates(entity, rates.map { it.copy(rateDate = null) }).toDomain()!!
        assertNull(migrated.dateFor(Currency.EUR, Currency.USD))
    }
    @Test fun `preferences migrate alphabetical favourites cash mode and restore explicit clears`() = runTest {
        val old = mutablePreferencesOf(stringSetPreferencesKey("favourite_currencies") to setOf("USD", "EUR", "CNY"), stringPreferencesKey("markup_percent") to "2.5")
        val memory = MemoryPreferences(old)
        val settings = SettingsStore(memory)
        assertEquals(listOf(Currency("CNY"), Currency.EUR, Currency.USD), settings.favourites.first())
        assertEquals(FeeMode.CASH, settings.feeMode.first()); assertEquals(BigDecimal("2.5"), settings.markupPercent.first())
        assertEquals("1", settings.calculation.first().input)
        settings.setFavourites(listOf(Currency.USD, Currency.EUR, Currency("CNY")))
        settings.setCalculation("(12+3)/5", "TO", false); settings.setBoardCalculation("", false)
        val reopened = SettingsStore(memory)
        assertEquals(listOf(Currency.USD, Currency.EUR, Currency("CNY")), reopened.favourites.first())
        assertEquals(SettingsStore.Calculation("(12+3)/5", "TO", false), reopened.calculation.first())
        assertEquals("", reopened.boardCalculation.first().input); assertFalse(reopened.boardCalculation.first().initial)
    }
    @Test fun `cash pair is independent and counts stay attached to local currency`() = runTest {
        val settings = SettingsStore(MemoryPreferences()); settings.setPair(Currency("VND"), Currency.EUR); settings.ensureCashPair()
        settings.adjustCashCount(Currency("VND"), BigDecimal("100000"), 2)
        settings.setPair(Currency.USD, Currency.GBP)
        assertEquals(Currency("VND") to Currency.EUR, settings.cashPair.first())
        settings.setCashPair(Currency("VND"), Currency.USD)
        assertEquals(2, settings.cashCounts(Currency("VND")).first()[BigDecimal("100000")])
        assertTrue(settings.cashCounts(Currency.USD).first().isEmpty())
        settings.resetCashCounts(Currency("VND")); assertTrue(settings.cashCounts(Currency("VND")).first().isEmpty())
    }
}
