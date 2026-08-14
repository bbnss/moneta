package it.bbnss.moneta.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import it.bbnss.moneta.R
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.CurrencyMetadata
import java.util.Locale

/** Voce già pronta per la ricerca: il testo su cui filtrare è calcolato una volta sola. */
private data class CurrencyRow(
    val currency: Currency,
    val name: String,
    val countries: String,
    val flag: String?,
    val haystack: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerSheet(
    currencies: List<Currency>,
    favourites: Set<Currency>,
    onPick: (Currency) -> Unit,
    onToggleFavourite: (Currency) -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    var query by remember { mutableStateOf("") }

    val rows = remember(currencies, locale) { buildRows(currencies, locale) }

    val filtered = remember(rows, query) {
        if (query.isBlank()) rows
        else {
            val needle = query.trim().lowercase(Locale.ROOT)
            rows.filter { it.haystack.contains(needle) }
        }
    }

    val favouriteRows = filtered.filter { it.currency in favourites }
    val otherRows = filtered.filterNot { it.currency in favourites }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                text = stringResource(R.string.convert_pick_currency),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text(stringResource(R.string.convert_search_currency)) },
            )

            if (filtered.isEmpty()) {
                Text(
                    text = stringResource(R.string.convert_no_currency_found, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
                return@Column
            }

            LazyColumn(Modifier.fillMaxWidth()) {
                if (favouriteRows.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.convert_favourites)) }
                    items(favouriteRows, key = { "fav-${it.currency.code}" }) { row ->
                        CurrencyRowItem(row, isFavourite = true, onPick, onToggleFavourite)
                    }
                    item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                }

                if (otherRows.isNotEmpty()) {
                    if (favouriteRows.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.convert_all_currencies)) }
                    }
                    items(otherRows, key = { it.currency.code }) { row ->
                        CurrencyRowItem(row, isFavourite = false, onPick, onToggleFavourite)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun CurrencyRowItem(
    row: CurrencyRow,
    isFavourite: Boolean,
    onPick: (Currency) -> Unit,
    onToggleFavourite: (Currency) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick(row.currency) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // La bandiera dà colore a un elenco altrimenti fatto di sole sigle:
        // si riconosce la valuta con la coda dell'occhio, senza leggere.
        Text(
            text = row.flag ?: "\u2009",
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            text = row.currency.code,
            style = MaterialTheme.typography.titleMedium,
            // Larghezza fissa e carattere monospaziato: i codici restano
            // incolonnati e si scorre la lista leggendo solo quelli.
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(56.dp),
        )

        Column(Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge)
            if (row.countries.isNotEmpty()) {
                Text(
                    text = row.countries,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        IconButton(onClick = { onToggleFavourite(row.currency) }) {
            Icon(
                imageVector = if (isFavourite) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = stringResource(
                    if (isFavourite) R.string.convert_remove_favourite
                    else R.string.convert_add_favourite,
                ),
                tint = if (isFavourite) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun buildRows(currencies: List<Currency>, locale: Locale): List<CurrencyRow> =
    currencies.map { currency ->
        val info = CurrencyMetadata.of(currency, locale)
        val countries = CurrencyMetadata.countriesOf(currency, locale)
        CurrencyRow(
            currency = currency,
            name = info.displayName,
            countries = countries.joinToString(", "),
            flag = CurrencyMetadata.flagOf(currency),
            // Si cerca per codice, per nome della valuta e per paese: chi
            // viaggia sa di essere in Vietnam, non che il dong si chiami VND.
            haystack = buildString {
                append(currency.code.lowercase(Locale.ROOT))
                append(' ')
                append(info.displayName.lowercase(locale))
                append(' ')
                countries.forEach { append(it.lowercase(locale)).append(' ') }
            },
        )
    }.sortedBy { it.currency.code }
