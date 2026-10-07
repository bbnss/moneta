package it.bbnss.moneta.ui.convert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.bbnss.moneta.R
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Dettaglio di provenienza del tasso, aperto dal badge di freschezza.
 *
 * Serve a rispondere a "da dove viene questo numero e quanto è vecchio" senza
 * costringere l'utente a fidarsi. Qui compare anche la data dichiarata dalla
 * fonte, che è cosa diversa dall'ultimo aggiornamento riuscito: la Banca
 * Centrale Europea non pubblica nel fine settimana, quindi di domenica il dato
 * è di venerdì ed è perfettamente normale.
 */
@Composable
fun RateDetailsDialog(state: ConvertUiState, onDismiss: () -> Unit,
    onAcceptSuggestion: () -> Unit = {}, onDismissSuggestion: () -> Unit = {}) {
    val locale = LocalConfiguration.current.locales[0]
    val dateTime = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale)
    val dateOnly = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
        title = { Text(stringResource(R.string.freshness_details)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.suggestion?.let { suggestion ->
                    Text(stringResource(R.string.suggestion_local_currency, suggestion.countryName, suggestion.currency.code))
                    TextButton(onClick = { onAcceptSuggestion(); onDismiss() }) { Text(stringResource(R.string.suggestion_accept, suggestion.currency.code)) }
                    TextButton(onClick = { onDismissSuggestion(); onDismiss() }) { Text(stringResource(R.string.suggestion_dismiss)) }
                }
                state.provider?.let { provider ->
                    Text(
                        text = stringResource(R.string.freshness_source, provider.displayName),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                state.updatedAt?.let { instant ->
                    Text(
                        text = stringResource(R.string.freshness_verified, dateTime.format(instant.atZone(ZoneId.systemDefault()))),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                state.rateDate?.let { date ->
                    Text(
                        text = stringResource(
                            R.string.freshness_rate_date,
                            dateOnly.format(date),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.quotationDates.forEach { (currency, date) ->
                    Text("${currency.code}: ${date?.let(dateOnly::format) ?: stringResource(R.string.freshness_unknown)}",
                        style = MaterialTheme.typography.bodyMedium)
                }

                if (state.substituted && state.provider != null) {
                    Text(
                        modifier = Modifier,
                        text = stringResource(
                            R.string.freshness_substituted,
                            state.preferredProvider.displayName,
                            state.provider.displayName,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
}
