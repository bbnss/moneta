package it.bbnss.moneta.core.providers

import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.RateSnapshot

/** Tentativo andato male, conservato per poterlo mostrare nel dettaglio. */
data class FailedAttempt(
    val provider: ProviderId,
    val reason: FailureReason,
    val message: String?,
)

sealed interface FetchOutcome {

    data class Fetched(
        val snapshot: RateSnapshot,
        val preferred: ProviderId,
        val attempts: List<FailedAttempt>,
    ) : FetchOutcome {
        /** I dati arrivano da una fonte diversa da quella scelta dall'utente. */
        val substituted: Boolean get() = snapshot.provider != preferred
    }

    data class AllFailed(val attempts: List<FailedAttempt>) : FetchOutcome
}

/**
 * Prova le fonti in ordine finché una risponde.
 *
 * È la risposta diretta al problema che ha già colpito i progetti concorrenti:
 * hanno perso due fonti per chiusura del servizio e l'app ha semplicemente
 * smesso di aggiornarsi. Qui la morte di una fonte è un evento previsto, non
 * un guasto.
 *
 * Due regole importanti:
 * - la fonte che ha effettivamente risposto viene sempre riportata al chiamante,
 *   perché cambiare fonte cambia i numeri e l'utente deve poterlo vedere;
 * - i tentativi falliti vengono conservati, così le impostazioni possono
 *   mostrare *perché* la fonte preferita non è stata usata invece di limitarsi
 *   a un generico errore.
 */
class FailoverFetcher(private val registry: ProviderRegistry) {

    suspend fun fetchLatest(
        preferred: ProviderId,
        allowFailover: Boolean = true,
        required: Set<it.bbnss.moneta.core.model.Currency> = emptySet(),
        beforeFetch: suspend () -> Boolean = { true },
    ): FetchOutcome {
        val chain = when {
            allowFailover -> registry.failoverChain(preferred)
            else -> listOfNotNull(registry[preferred])
        }

        if (chain.isEmpty()) {
            return FetchOutcome.AllFailed(
                listOf(
                    FailedAttempt(
                        provider = preferred,
                        reason = FailureReason.UNSUPPORTED,
                        message = "Fonte non disponibile in questa versione dell'app",
                    ),
                ),
            )
        }

        val attempts = mutableListOf<FailedAttempt>()

        for (provider in chain) {
            if (!beforeFetch()) break
            when (val result = provider.fetchLatest()) {
                is ProviderResult.Success -> {
                    if (!required.all(result.value::supports)) {
                        attempts += FailedAttempt(provider.id, FailureReason.UNSUPPORTED, "Missing requested currencies")
                        continue
                    }
                    return FetchOutcome.Fetched(
                    snapshot = result.value,
                    preferred = preferred,
                    attempts = attempts.toList(),
                )
                }

                is ProviderResult.Failure -> {
                    attempts += FailedAttempt(provider.id, result.reason, result.message)
                    if (!result.canFailover) break
                }
            }
        }

        return FetchOutcome.AllFailed(attempts.toList())
    }
}
