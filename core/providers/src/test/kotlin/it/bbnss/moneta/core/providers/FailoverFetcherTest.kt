package it.bbnss.moneta.core.providers

import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.RateSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Provider finto: risponde come gli si dice e conta le chiamate ricevute. */
private class FakeProvider(
    override val id: ProviderId,
    private val result: ProviderResult<RateSnapshot>,
) : RateProvider {

    var calls = 0
        private set

    override val displayName = id.name
    override val infoUrl = "https://example.org"
    override val capabilities = ProviderCapabilities(1, UpdateCadence.DAILY_BUSINESS)

    override suspend fun fetchLatest(): ProviderResult<RateSnapshot> {
        calls++
        return result
    }
}

private fun snapshotFrom(provider: ProviderId) = ProviderResult.Success(
    RateSnapshot(
        provider = provider,
        pivot = Currency.EUR,
        rates = mapOf(Currency.USD to BigDecimal("1.15")),
        rateDate = LocalDate.of(2026, 8, 9),
        fetchedAt = Instant.parse("2026-08-09T16:00:00Z"),
    ),
)

private fun failing(reason: FailureReason) =
    ProviderResult.Failure(reason, "fonte non disponibile")

class FailoverFetcherTest {

    @Test
    fun `VND EUR prosegue oltre una risposta BCE incompleta`() = runTest {
        val ecb = FakeProvider(ProviderId.ECB, snapshotFrom(ProviderId.ECB))
        val complete = FakeProvider(ProviderId.FAWAZAHMED0, ProviderResult.Success(
            snapshotFrom(ProviderId.FAWAZAHMED0).value.copy(rates = mapOf(Currency("VND") to BigDecimal("30000")))))
        val registry = ProviderRegistry(listOf(ecb, complete))
        val outcome = FailoverFetcher(registry).fetchLatest(ProviderId.ECB, required = setOf(Currency("VND"), Currency.EUR)) as FetchOutcome.Fetched
        assertEquals(ProviderId.FAWAZAHMED0, outcome.snapshot.provider)
        assertEquals(FailureReason.UNSUPPORTED, outcome.attempts.single().reason)
        val disabled = FailoverFetcher(registry).fetchLatest(ProviderId.ECB, false, setOf(Currency("VND"), Currency.EUR))
        assertTrue(disabled is FetchOutcome.AllFailed)
        assertEquals(1, complete.calls)
    }

    @Test
    fun `endpoint personale configurato partecipa al fallback`() = runTest {
        val custom = FakeProvider(ProviderId.CUSTOM, snapshotFrom(ProviderId.CUSTOM))
        val outcome = FailoverFetcher(ProviderRegistry(listOf(custom))).fetchLatest(ProviderId.ECB) as FetchOutcome.Fetched
        assertEquals(ProviderId.CUSTOM, outcome.snapshot.provider)
    }

    @Test
    fun `policy bloccata impedisce ogni chiamata`() = runTest {
        val provider = FakeProvider(ProviderId.ECB, snapshotFrom(ProviderId.ECB))
        val outcome = FailoverFetcher(ProviderRegistry(listOf(provider))).fetchLatest(ProviderId.ECB, beforeFetch = { false })
        assertTrue(outcome is FetchOutcome.AllFailed)
        assertEquals(0, provider.calls)
    }

    @Test
    fun `usa la fonte preferita quando risponde`() = runTest {
        val preferred = FakeProvider(ProviderId.FRANKFURTER, snapshotFrom(ProviderId.FRANKFURTER))
        val backup = FakeProvider(ProviderId.ECB, snapshotFrom(ProviderId.ECB))
        val fetcher = FailoverFetcher(ProviderRegistry(listOf(preferred, backup)))

        val outcome = fetcher.fetchLatest(ProviderId.FRANKFURTER) as FetchOutcome.Fetched

        assertEquals(ProviderId.FRANKFURTER, outcome.snapshot.provider)
        assertFalse(outcome.substituted)
        assertTrue(outcome.attempts.isEmpty())
        assertEquals(0, backup.calls)
    }

    /**
     * Il caso per cui esiste tutta questa impalcatura: una fonte muore e l'app
     * continua ad aggiornarsi invece di restare ferma, come è successo ai
     * progetti concorrenti quando due delle loro API hanno chiuso.
     */
    @Test
    fun `passa alla fonte successiva quando la preferita cade`() = runTest {
        val broken = FakeProvider(ProviderId.FRANKFURTER, failing(FailureReason.NETWORK))
        val backup = FakeProvider(ProviderId.FAWAZAHMED0, snapshotFrom(ProviderId.FAWAZAHMED0))
        val fetcher = FailoverFetcher(ProviderRegistry(listOf(broken, backup)))

        val outcome = fetcher.fetchLatest(ProviderId.FRANKFURTER) as FetchOutcome.Fetched

        assertEquals(ProviderId.FAWAZAHMED0, outcome.snapshot.provider)
        assertEquals(1, outcome.attempts.size)
        assertEquals(FailureReason.NETWORK, outcome.attempts.first().reason)
    }

    /** Il cambio di fonte non deve mai passare inosservato: cambia i numeri. */
    @Test
    fun `segnala che i dati vengono da una fonte diversa`() = runTest {
        val broken = FakeProvider(ProviderId.FRANKFURTER, failing(FailureReason.TIMEOUT))
        val backup = FakeProvider(ProviderId.ECB, snapshotFrom(ProviderId.ECB))
        val fetcher = FailoverFetcher(ProviderRegistry(listOf(broken, backup)))

        val outcome = fetcher.fetchLatest(ProviderId.FRANKFURTER) as FetchOutcome.Fetched

        assertTrue(outcome.substituted)
    }

    @Test
    fun `con failover disattivato prova solo la fonte scelta`() = runTest {
        val broken = FakeProvider(ProviderId.FRANKFURTER, failing(FailureReason.NETWORK))
        val backup = FakeProvider(ProviderId.ECB, snapshotFrom(ProviderId.ECB))
        val fetcher = FailoverFetcher(ProviderRegistry(listOf(broken, backup)))

        val outcome = fetcher.fetchLatest(ProviderId.FRANKFURTER, allowFailover = false)

        assertTrue(outcome is FetchOutcome.AllFailed)
        assertEquals(0, backup.calls)
    }

    /**
     * Se la fonte ha risposto dicendo "non faccio questo", ripiegare su
     * un'altra darebbe all'utente un dato che non ha chiesto.
     */
    @Test
    fun `un rifiuto esplicito interrompe la catena`() = runTest {
        val refusing = FakeProvider(ProviderId.FRANKFURTER, failing(FailureReason.UNSUPPORTED))
        val backup = FakeProvider(ProviderId.ECB, snapshotFrom(ProviderId.ECB))
        val fetcher = FailoverFetcher(ProviderRegistry(listOf(refusing, backup)))

        val outcome = fetcher.fetchLatest(ProviderId.FRANKFURTER)

        assertTrue(outcome is FetchOutcome.AllFailed)
        assertEquals(0, backup.calls)
    }

    @Test
    fun `raccoglie tutti i tentativi falliti`() = runTest {
        val fetcher = FailoverFetcher(
            ProviderRegistry(
                listOf(
                    FakeProvider(ProviderId.FRANKFURTER, failing(FailureReason.NETWORK)),
                    FakeProvider(ProviderId.FAWAZAHMED0, failing(FailureReason.TIMEOUT)),
                    FakeProvider(ProviderId.ECB, failing(FailureReason.MALFORMED_RESPONSE)),
                ),
            ),
        )

        val outcome = fetcher.fetchLatest(ProviderId.FRANKFURTER) as FetchOutcome.AllFailed

        assertEquals(3, outcome.attempts.size)
        assertEquals(
            listOf(FailureReason.NETWORK, FailureReason.TIMEOUT, FailureReason.MALFORMED_RESPONSE),
            outcome.attempts.map { it.reason },
        )
    }

    /** La fonte scelta va provata per prima anche se nell'ordine predefinito viene dopo. */
    @Test
    fun `la fonte preferita apre sempre la catena`() = runTest {
        val frankfurter = FakeProvider(ProviderId.FRANKFURTER, snapshotFrom(ProviderId.FRANKFURTER))
        val rossii = FakeProvider(ProviderId.BANK_ROSSII, snapshotFrom(ProviderId.BANK_ROSSII))
        val fetcher = FailoverFetcher(ProviderRegistry(listOf(frankfurter, rossii)))

        val outcome = fetcher.fetchLatest(ProviderId.BANK_ROSSII) as FetchOutcome.Fetched

        assertEquals(ProviderId.BANK_ROSSII, outcome.snapshot.provider)
        assertEquals(0, frankfurter.calls)
    }

    /**
     * InforEuro pubblica una volta al mese: subentrare in automatico
     * mostrerebbe un tasso di settimane prima subito dopo un aggiornamento
     * riuscito, senza che l'età del dato lo lasci sospettare.
     */
    @Test
    fun `InforEuro resta fuori dal failover automatico`() = runTest {
        val broken = FakeProvider(ProviderId.FRANKFURTER, failing(FailureReason.NETWORK))
        val monthly = FakeProvider(ProviderId.INFOR_EURO, snapshotFrom(ProviderId.INFOR_EURO))
        val fetcher = FailoverFetcher(ProviderRegistry(listOf(broken, monthly)))

        val outcome = fetcher.fetchLatest(ProviderId.FRANKFURTER)

        assertTrue(outcome is FetchOutcome.AllFailed)
        assertEquals(0, monthly.calls)
    }

    /** Ma resta usabile se è l'utente a sceglierlo esplicitamente. */
    @Test
    fun `InforEuro funziona se scelto a mano`() = runTest {
        val monthly = FakeProvider(ProviderId.INFOR_EURO, snapshotFrom(ProviderId.INFOR_EURO))
        val fetcher = FailoverFetcher(ProviderRegistry(listOf(monthly)))

        val outcome = fetcher.fetchLatest(ProviderId.INFOR_EURO) as FetchOutcome.Fetched

        assertEquals(ProviderId.INFOR_EURO, outcome.snapshot.provider)
    }
}
