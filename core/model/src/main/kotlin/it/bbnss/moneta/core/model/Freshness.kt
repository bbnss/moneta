package it.bbnss.moneta.core.model

import java.time.Duration
import java.time.Instant

/**
 * Quanto sono vecchi i dati che l'utente sta guardando.
 *
 * È la funzione più importante dell'app dopo la conversione stessa: un tasso
 * senza età dichiarata è un numero di cui non ci si può fidare, e mostrarlo in
 * silenzio è il difetto comune a quasi tutti i convertitori.
 */
enum class Freshness {
    /** Aggiornato nelle ultime 24 ore. */
    FRESH,

    /** Fino a tre giorni: normale dopo un fine settimana o un festivo. */
    RECENT,

    /** Fino a una settimana: utilizzabile, ma va segnalato. */
    AGING,

    /** Oltre una settimana: da avvisare esplicitamente. */
    STALE,
}

object FreshnessRules {

    private val FRESH_LIMIT: Duration = Duration.ofHours(24)
    private val RECENT_LIMIT: Duration = Duration.ofDays(3)
    private val AGING_LIMIT: Duration = Duration.ofDays(7)

    /**
     * La freschezza si misura sull'ultimo scaricamento riuscito, non sulla data
     * dei tassi: le banche centrali non pubblicano nei fine settimana e nei
     * festivi, quindi un tasso di venerdì consultato di domenica è corretto e
     * non va segnalato come vecchio. La data dei tassi resta comunque visibile
     * nel dettaglio.
     */
    fun of(fetchedAt: Instant, now: Instant = Instant.now()): Freshness {
        val age = Duration.between(fetchedAt, now)
        return when {
            age < FRESH_LIMIT -> Freshness.FRESH
            age < RECENT_LIMIT -> Freshness.RECENT
            age < AGING_LIMIT -> Freshness.AGING
            else -> Freshness.STALE
        }
    }

    fun ageOf(fetchedAt: Instant, now: Instant = Instant.now()): Duration =
        Duration.between(fetchedAt, now)
}
