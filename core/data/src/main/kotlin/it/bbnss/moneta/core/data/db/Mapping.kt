package it.bbnss.moneta.core.data.db

import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.RatePoint
import it.bbnss.moneta.core.model.RateSeries
import it.bbnss.moneta.core.model.RateSnapshot
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Conversione fra modello di dominio e righe del database.
 *
 * I tassi passano da e verso `String` con [BigDecimal.toPlainString]: niente
 * notazione esponenziale, così anche i valori minuscoli delle crypto restano
 * leggibili e riparsabili senza perdite.
 */
internal fun RateSnapshot.toEntities(): Pair<SnapshotEntity, List<RateEntity>> {
    val snapshot = SnapshotEntity(
        providerId = provider.stableId,
        pivot = pivot.code,
        rateDate = rateDate.toString(),
        fetchedAt = fetchedAt.toEpochMilli(),
    )
    val rows = rates.map { (currency, value) ->
        RateEntity(
            providerId = provider.stableId,
            currency = currency.code,
            value = value.toPlainString(),
        )
    }
    return snapshot to rows
}

/**
 * `null` se la riga è inutilizzabile (fonte rimossa dall'app, pivot illeggibile
 * o nessun tasso): meglio comportarsi come se il dato non ci fosse che mostrare
 * numeri di provenienza incerta.
 */
internal fun SnapshotWithRates.toDomain(): RateSnapshot? {
    val provider = ProviderId.fromStableId(snapshot.providerId) ?: return null
    val pivot = Currency.parse(snapshot.pivot) ?: return null
    val date = runCatching { LocalDate.parse(snapshot.rateDate) }.getOrNull() ?: return null

    val parsed = rates.mapNotNull { row ->
        val currency = Currency.parse(row.currency) ?: return@mapNotNull null
        val value = runCatching { BigDecimal(row.value) }.getOrNull() ?: return@mapNotNull null
        currency to value
    }.toMap()

    if (parsed.isEmpty()) return null

    return RateSnapshot(
        provider = provider,
        pivot = pivot,
        rates = parsed,
        rateDate = date,
        fetchedAt = Instant.ofEpochMilli(snapshot.fetchedAt),
    )
}

internal fun RateSeries.toEntities(): List<SeriesPointEntity> = points.map { point ->
    SeriesPointEntity(
        providerId = provider.stableId,
        base = base.code,
        quote = quote.code,
        date = point.date.toString(),
        value = point.rate.toPlainString(),
    )
}

internal fun List<SeriesPointEntity>.toDomain(
    provider: ProviderId,
    base: Currency,
    quote: Currency,
): RateSeries = RateSeries(
    provider = provider,
    base = base,
    quote = quote,
    points = mapNotNull { row ->
        val date = runCatching { LocalDate.parse(row.date) }.getOrNull() ?: return@mapNotNull null
        val value = runCatching { BigDecimal(row.value) }.getOrNull() ?: return@mapNotNull null
        RatePoint(date, value)
    },
)
