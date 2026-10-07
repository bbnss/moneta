package it.bbnss.moneta.core.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class Freshness { FRESH, RECENT, AGING, STALE }

object FreshnessRules {
    fun of(rateDate: LocalDate?, today: LocalDate = LocalDate.now()): Freshness? {
        if (rateDate == null) return null
        val days = ChronoUnit.DAYS.between(rateDate, today).coerceAtLeast(0)
        return when {
            days == 0L -> Freshness.FRESH
            days <= 3 -> Freshness.RECENT
            days <= 7 -> Freshness.AGING
            else -> Freshness.STALE
        }
    }
    fun rateAge(rateDate: LocalDate?, today: LocalDate = LocalDate.now()): Duration? =
        rateDate?.let { Duration.ofDays(ChronoUnit.DAYS.between(it, today).coerceAtLeast(0)) }
    fun ageOf(fetchedAt: Instant, now: Instant = Instant.now()): Duration =
        Duration.between(fetchedAt, now).coerceAtLeast(Duration.ZERO)
}
