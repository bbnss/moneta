package it.bbnss.moneta.ui.ratecard

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.bbnss.moneta.R
import it.bbnss.moneta.core.model.AmountFormat
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.CurrencyMetadata
import it.bbnss.moneta.ui.components.AmountText
import it.bbnss.moneta.ui.components.CurrencyPickerSheet
import it.bbnss.moneta.ui.components.FreshnessBadge
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun RateCardScreen(state: RateCardUiState,
    onCurrencySelected: (Boolean, Currency) -> Unit,
    onSwap: () -> Unit,
    onCount: (BigDecimal, Int) -> Unit,
    onReset: () -> Unit,
    onToggleFavourite: (Currency) -> Unit,
    modifier: Modifier = Modifier) {
    var counting by rememberSaveable { mutableStateOf(false) }
    var picker by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var showCatalog by rememberSaveable { mutableStateOf(false) }
    val canCount = state.catalog != null || state.counterRows.any { it.count > 0 }
    val showingCount = counting && canCount
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    val shareTitle = stringResource(if (state.estimated) R.string.cash_indicative else R.string.ratecard_share_title)
    val unknown = stringResource(R.string.board_copy_unknown)
    val shareDate = state.rateDate?.format(dateFormat) ?: unknown

    LaunchedEffect(state.local, canCount) { if (!canCount) counting = false }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            CashCurrencyButton(stringResource(R.string.cash_banknotes), state.local,
                onClick = { picker = true }, modifier = Modifier.weight(1f))
            IconButton(onClick = onSwap) { Icon(Icons.Default.SwapHoriz, stringResource(R.string.convert_swap)) }
            CashCurrencyButton(stringResource(R.string.cash_value_in), state.home,
                onClick = { picker = false }, modifier = Modifier.weight(1f))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !showingCount, onClick = { counting = false }, label = { Text(stringResource(R.string.cash_table)) })
            FilterChip(selected = showingCount, onClick = { counting = true }, enabled = canCount,
                label = { Text(stringResource(R.string.cash_count)) })
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item {
                if (!showingCount) Text(stringResource(when {
                    state.estimated -> R.string.cash_indicative_hint
                    else -> R.string.cash_table_hint
                },
                    state.local.code, state.home.code), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.rate?.let {
                    Text(stringResource(R.string.convert_rate_line, state.local.code, AmountFormat.formatRate(it), state.home.code),
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
                FreshnessBadge(state.freshness, state.age, state.updatedAt, rateDate = state.rateDate,
                    modifier = Modifier.padding(vertical = 4.dp))
                if (!state.supported) Text(stringResource(R.string.ratecard_unsupported, state.provider?.displayName.orEmpty()),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp))
                if (state.estimated) Text(stringResource(R.string.cash_catalog_missing, state.local.code),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp))
                else if (!showingCount) TextButton(onClick = { showCatalog = true }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Icon(Icons.Default.Info, null, Modifier.size(16.dp))
                    Text(stringResource(R.string.cash_catalog_details), Modifier.padding(start = 6.dp))
                }
                if (state.counterRows.any { !it.documented && it.count > 0 }) {
                    Text(stringResource(R.string.cash_saved_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(stringResource(if (state.estimated && !showingCount) R.string.cash_amount_header else R.string.cash_note_header,
                        state.local.code), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Text(if (showingCount) stringResource(R.string.cash_quantity) else stringResource(R.string.cash_value_header, state.home.code),
                        style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
                HorizontalDivider()
            }
            items(if (showingCount) state.counterRows else state.rows, key = { it.denomination.toPlainString() }) { row ->
                Column {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(row.localText, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()))
                        if (showingCount) {
                            IconButton(onClick = { onCount(row.denomination, -1) }, enabled = row.count > 0) {
                                Icon(Icons.Default.Remove, stringResource(R.string.cash_remove, "${row.localText} ${state.local.code}"))
                            }
                            val quantityDescription = stringResource(R.string.cash_quantity_description,
                                row.localText, state.local.code, row.count)
                            Text(row.count.toString(), style = MaterialTheme.typography.titleMedium, maxLines = 1,
                                textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 24.dp, max = 64.dp)
                                    .horizontalScroll(rememberScrollState()).semantics { contentDescription = quantityDescription })
                            IconButton(onClick = { onCount(row.denomination, 1) }, enabled = row.documented) {
                                Icon(Icons.Default.Add, stringResource(R.string.cash_add, "${row.localText} ${state.local.code}"))
                            }
                        } else Text(row.homeText.ifEmpty { "—" }, style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End, maxLines = 1,
                            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()))
                    }
                    if (showingCount && !row.documented) Text(stringResource(R.string.cash_saved_note),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                HorizontalDivider()
            }
            if (!showingCount) item {
                OutlinedButton(onClick = { shareTable(context, state, shareTitle, shareDate) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Icon(Icons.Default.Share, null)
                    Text(stringResource(R.string.ratecard_share), Modifier.padding(start = 8.dp))
                }
            }
        }

        if (showingCount) Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.cash_count_total), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onReset, enabled = state.counterRows.any { it.count > 0 }) {
                        Icon(Icons.Default.RestartAlt, stringResource(R.string.cash_reset))
                    }
                }
                CashTotal(state.local, state.totalLocal)
                CashTotal(state.home, state.totalHome.ifEmpty { "—" })
                if (state.counterRows.any { !it.documented && it.count > 0 }) {
                    Text(stringResource(R.string.cash_saved_total), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
    picker?.let { localField ->
        CurrencyPickerSheet(state.availableCurrencies, state.favourites,
            onPick = { onCurrencySelected(localField, it); picker = null },
            onToggleFavourite = onToggleFavourite, onDismiss = { picker = null })
    }
    if (showCatalog) state.catalog?.let { catalog ->
        val uriHandler = LocalUriHandler.current
        AlertDialog(onDismissRequest = { showCatalog = false },
            title = { Text(stringResource(R.string.cash_catalog_details)) },
            text = { Column {
                Text(stringResource(R.string.cash_catalog_checked, catalog.checkedOn.format(dateFormat)))
                Text(catalog.issuer, modifier = Modifier.padding(top = 8.dp))
                Text(stringResource(R.string.cash_catalog_scope), style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp))
            } },
            confirmButton = { TextButton(onClick = { uriHandler.openUri(catalog.source) }) { Text(stringResource(R.string.cash_catalog_source)) } },
            dismissButton = { TextButton(onClick = { showCatalog = false }) { Text(stringResource(android.R.string.ok)) } })
    }
}

@Composable
private fun CashCurrencyButton(label: String, currency: Currency, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 64.dp), shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("${CurrencyMetadata.flagOf(currency).orEmpty()} ${currency.code}", style = MaterialTheme.typography.titleMedium)
        }
        Icon(Icons.Default.ExpandMore, null, Modifier.size(16.dp))
    }
}

@Composable
private fun CashTotal(currency: Currency, amount: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(currency.code, style = MaterialTheme.typography.titleMedium)
        AmountText(amount, Modifier.weight(1f).padding(start = 12.dp))
    }
}

private fun shareTable(context: Context, state: RateCardUiState, title: String, date: String) {
    val body = buildString {
        appendLine(title)
        state.rate?.let { appendLine("1 ${state.local.code} = ${AmountFormat.formatRate(it)} ${state.home.code}") }
        appendLine("${state.provider?.displayName.orEmpty()} · $date")
        appendLine()
        state.rows.forEach { appendLine("${it.localText} ${state.local.code} = ${it.homeText.ifEmpty { "—" }} ${state.home.code}") }
        append("\nMoneta\nhttps://github.com/bbnss/moneta")
    }
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, title); putExtra(Intent.EXTRA_TEXT, body)
    }, title))
}
