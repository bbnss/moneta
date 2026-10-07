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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.ui.components.CurrencyPickerSheet
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
fun RateCardScreen(state: RateCardUiState,
    onCurrencySelected: (Boolean, Currency) -> Unit,
    onSwap: () -> Unit,
    onCount: (java.math.BigDecimal, Int) -> Unit,
    onReset: () -> Unit,
    onToggleFavourite: (Currency) -> Unit,
    modifier: Modifier = Modifier) {
    var counting by rememberSaveable { mutableStateOf(false) }
    var picker by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { picker = false }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cash_home, state.home.code))
            }
            IconButton(onClick = onSwap) { Icon(Icons.Default.SwapHoriz, stringResource(R.string.convert_swap)) }
            TextButton(onClick = { picker = true }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cash_local, state.local.code))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !counting, onClick = { counting = false }, label = { Text(stringResource(R.string.cash_table)) })
            FilterChip(selected = counting, onClick = { counting = true }, label = { Text(stringResource(R.string.cash_count)) })
        }
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
            rateDate = state.rateDate,
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
                    if (counting) {
                        IconButton(onClick = { onCount(row.denomination, -1) }, enabled = row.count > 0) {
                            Icon(Icons.Default.Remove, stringResource(R.string.cash_remove, row.localText))
                        }
                        Text(row.count.toString(), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { onCount(row.denomination, 1) }) {
                            Icon(Icons.Default.Add, stringResource(R.string.cash_add, row.localText))
                        }
                    } else Text(
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

        if (counting) {
            Text(stringResource(R.string.cash_total, state.totalLocal, state.local.code, state.totalHome.ifEmpty { "—" }, state.home.code),
                style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cash_reset)) }
        }

        val shareTitle = stringResource(R.string.ratecard_share_title)
        val shareLabel = stringResource(R.string.ratecard_share)

        if (!counting) OutlinedButton(
            onClick = { shareTable(context, state, shareTitle) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Text(shareLabel, Modifier.padding(start = 8.dp))
        }
    }
    picker?.let { localField ->
        CurrencyPickerSheet(state.availableCurrencies, state.favourites,
            onPick = { onCurrencySelected(localField, it); picker = null },
            onToggleFavourite = onToggleFavourite, onDismiss = { picker = null })
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
