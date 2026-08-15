package it.bbnss.moneta.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.bbnss.moneta.R
import it.bbnss.moneta.core.data.SeriesResult
import it.bbnss.moneta.core.providers.FailureReason
import it.bbnss.moneta.core.model.AmountFormat
import it.bbnss.moneta.core.model.HistoryRange
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onRangeSelected: (HistoryRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Sei filtri non entrano in una riga su uno schermo stretto: una `Row`
        // schiacciava l'ultimo fino a scriverne l'etichetta una lettera per
        // riga. Andando a capo restano tutti leggibili e raggiungibili, senza
        // scorrimenti orizzontali nascosti.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HistoryRange.entries.forEach { range ->
                FilterChip(
                    selected = range == state.range,
                    onClick = { onRangeSelected(range) },
                    label = { Text(labelOf(range), maxLines = 1) },
                )
            }
        }

        when {
            state.loading -> Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.history_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            state.points.size < 2 -> Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Il motivo per cui il grafico è vuoto cambia cosa può fare
                // l'utente: riprovare, connettersi, o rassegnarsi.
                Text(
                    text = emptyMessage(state.emptyReason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            else -> {
                RateChart(allPoints = state.points, modifier = Modifier.fillMaxWidth())

                state.changePercent?.let { change ->
                    Text(
                        text = stringResource(
                            R.string.history_change,
                            formatSignedPercent(change),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        // Verde e rosso non bastano da soli: il segno resta nel
                        // testo, per chi non distingue i due colori.
                        color = when {
                            change.signum() > 0 -> MaterialTheme.colorScheme.primary
                            change.signum() < 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Statistic(stringResource(R.string.history_low), state.low)
                    Statistic(stringResource(R.string.history_latest), state.latest)
                    Statistic(stringResource(R.string.history_high), state.high)
                }

                // Da dove arrivano questi numeri, e fin dove arrivano davvero.
                // Il periodo mostrato non coincide sempre con quello chiesto:
                // una fonte può non avere storico così indietro, e vederlo
                // scritto evita di leggere "5 anni" su una curva di due.
                SourceLine(state)
            }
        }
    }
}

@Composable
private fun SourceLine(state: HistoryUiState) {
    val provider = state.provider ?: return
    val first = state.firstDate ?: return
    val last = state.lastDate ?: return

    val formatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }

    Text(
        text = stringResource(
            R.string.history_source,
            provider.displayName,
            formatter.format(first),
            formatter.format(last),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Statistic(label: String, value: BigDecimal?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value?.let { AmountFormat.formatRate(it) } ?: "—",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun emptyMessage(reason: SeriesResult?): String = when (reason) {
    is SeriesResult.Unavailable -> when (reason.reason) {
        FailureReason.MALFORMED_RESPONSE -> stringResource(R.string.history_error_format)
        else -> stringResource(R.string.history_error_network)
    }

    SeriesResult.Offline -> stringResource(R.string.history_error_offline)
    else -> stringResource(R.string.history_empty)
}

private fun formatSignedPercent(change: BigDecimal): String {
    val sign = if (change.signum() > 0) "+" else ""
    return "$sign${change.toPlainString()}%"
}

@Composable
private fun labelOf(range: HistoryRange): String = stringResource(
    when (range) {
        HistoryRange.ONE_MONTH -> R.string.history_range_1m
        HistoryRange.THREE_MONTHS -> R.string.history_range_3m
        HistoryRange.SIX_MONTHS -> R.string.history_range_6m
        HistoryRange.ONE_YEAR -> R.string.history_range_1y
        HistoryRange.FIVE_YEARS -> R.string.history_range_5y
        HistoryRange.MAX -> R.string.history_range_max
    },
)
