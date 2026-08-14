package it.bbnss.moneta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.bbnss.moneta.R
import it.bbnss.moneta.core.model.Freshness
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Età dei dati mostrati, sempre visibile sotto il risultato.
 *
 * È l'elemento che distingue quest'app dalle altre. Un tasso senza età
 * dichiarata è un numero di cui non ci si può fidare, e quasi tutti i
 * convertitori ne mostrano uno vecchio senza dirlo: chi è offline da una
 * settimana vede gli stessi numeri di chi si è appena sincronizzato.
 *
 * Il colore non è mai l'unico segnale — il testo dice sempre l'età per esteso,
 * perché il colore da solo non arriva a chi non lo distingue.
 */
@Composable
fun FreshnessBadge(
    freshness: Freshness?,
    age: Duration?,
    updatedAt: Instant?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()

    val accent = when (freshness) {
        Freshness.FRESH -> if (dark) Color(0xFF81C784) else Color(0xFF2E7D32)
        Freshness.RECENT -> colors.onSurfaceVariant
        Freshness.AGING -> if (dark) Color(0xFFFFB74D) else Color(0xFFB26A00)
        Freshness.STALE -> colors.error
        null -> colors.onSurfaceVariant
    }

    val days = age?.toDays()?.toInt() ?: 0
    val zone = ZoneId.systemDefault()

    val label = when {
        freshness == null || updatedAt == null -> stringResource(R.string.freshness_unknown)

        // "Fresco" significa meno di 24 ore, che non è la stessa cosa di "oggi":
        // alle 10 del mattino un dato delle 21 di ieri rientra nelle 24 ore ma
        // resta di ieri. Su un'app che esiste per dire l'età vera del dato,
        // chiamarlo "oggi" sarebbe la prima piccola bugia.
        freshness == Freshness.FRESH -> {
            val updatedOn = updatedAt.atZone(zone).toLocalDate()
            val time = shortTimeFormatter().format(updatedAt.atZone(zone))
            if (updatedOn == LocalDate.now(zone)) {
                stringResource(R.string.freshness_today, time)
            } else {
                stringResource(R.string.freshness_yesterday, time)
            }
        }

        freshness == Freshness.STALE ->
            pluralStringResource(R.plurals.freshness_stale, days, days)

        else -> pluralStringResource(R.plurals.freshness_days_ago, days, days)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).background(accent, CircleShape))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (freshness == Freshness.STALE) colors.error else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun shortTimeFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(LocalConfiguration.current.locales[0])
