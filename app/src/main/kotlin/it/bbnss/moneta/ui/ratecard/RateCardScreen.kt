package it.bbnss.moneta.ui.ratecard

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.bbnss.moneta.R
import it.bbnss.moneta.core.model.AmountFormat
import it.bbnss.moneta.ui.components.FreshnessBadge

@Composable
fun RateCardScreen(state: RateCardUiState, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.ratecard_title, state.local.code, state.home.code),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.ratecard_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.rate != null) {
            Text(
                text = stringResource(
                    R.string.convert_rate_line,
                    state.local.code,
                    AmountFormat.formatRate(state.rate),
                    state.home.code,
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        FreshnessBadge(
            freshness = state.freshness,
            age = state.age,
            updatedAt = state.updatedAt,
            modifier = Modifier.padding(vertical = 4.dp),
        )

        if (!state.supported) {
            Text(
                text = stringResource(
                    R.string.ratecard_unsupported,
                    state.provider?.displayName.orEmpty(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 24.dp),
            )
            return@Column
        }

        if (state.estimated) {
            Text(
                text = stringResource(R.string.ratecard_estimated, state.local.code),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        HorizontalDivider(Modifier.padding(top = 8.dp))

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(state.rows, key = { it.denomination.toPlainString() }) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = row.localText,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = row.homeText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalDivider()
            }
        }

        val shareTitle = stringResource(R.string.ratecard_share_title)
        val shareLabel = stringResource(R.string.ratecard_share)

        OutlinedButton(
            onClick = { shareTable(context, state, shareTitle) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Text(shareLabel, Modifier.padding(start = 8.dp))
        }
    }
}

/**
 * Condivide la tabella come testo semplice.
 *
 * Testo e non immagine di proposito: si incolla in un messaggio a chi viaggia
 * con te, resta leggibile ovunque e non pesa nulla su una connessione lenta.
 */
private fun shareTable(context: Context, state: RateCardUiState, title: String) {
    val body = buildString {
        appendLine(title)
        state.rate?.let {
            appendLine("1 ${state.local.code} = ${AmountFormat.formatRate(it)} ${state.home.code}")
        }
        state.provider?.let { appendLine("${it.displayName} · ${state.rateDate}") }
        appendLine()
        val width = state.rows.maxOfOrNull { it.localText.length } ?: 0
        state.rows.forEach { row ->
            appendLine("${row.localText.padStart(width)}  =  ${row.homeText}")
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, title))
}
