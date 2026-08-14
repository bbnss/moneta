package it.bbnss.moneta.ui.board

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.bbnss.moneta.R
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.CurrencyMetadata
import it.bbnss.moneta.core.ui.components.KeypadKey
import it.bbnss.moneta.core.ui.components.MonetaKeypad
import it.bbnss.moneta.ui.components.CurrencyPickerSheet
import it.bbnss.moneta.ui.components.FreshnessBadge
import java.text.DecimalFormatSymbols

@Composable
fun BoardScreen(
    state: BoardUiState,
    onKey: (KeypadKey) -> Unit,
    onSetAsBase: (Currency) -> Unit,
    onToggleFavourite: (Currency) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BaseAmountRow(currency = state.base, text = state.inputText)

        if (!state.hasFavourites) {
            EmptyState(Modifier.weight(1f), onAdd = { showPicker = true })
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            ) {
                items(state.rows, key = { it.currency.code }) { row ->
                    BoardRowItem(
                        row = row,
                        setAsBaseLabel = stringResource(
                            R.string.board_set_as_base,
                            row.currency.code,
                        ),
                        onLongPress = { onSetAsBase(row.currency) },
                    )
                    HorizontalDivider()
                }

                // Aggiungere una valuta si fa da qui, dove si guarda l'elenco:
                // andarla a cercare nella schermata di conversione sarebbe un
                // giro inutile.
                item {
                    TextButton(
                        onClick = { showPicker = true },
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(
                            text = stringResource(R.string.board_add_currency),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.board_base_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
        }

        FreshnessBadge(
            freshness = state.freshness,
            age = state.age,
            updatedAt = state.updatedAt,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        MonetaKeypad(
            onKey = onKey,
            modifier = Modifier.padding(bottom = 12.dp),
            decimalSeparator = DecimalFormatSymbols
                .getInstance(LocalConfiguration.current.locales[0])
                .decimalSeparator,
            clearLabel = stringResource(R.string.keypad_clear),
            backspaceDescription = stringResource(R.string.keypad_backspace),
            equalsDescription = stringResource(R.string.keypad_equals),
        )
    }

    if (showPicker) {
        CurrencyPickerSheet(
            currencies = state.availableCurrencies,
            favourites = state.favourites,
            // Toccare una valuta qui significa "voglio vederla nell'elenco",
            // non "convertila adesso": la scelta la aggiunge alle preferite.
            onPick = { currency ->
                if (currency !in state.favourites) onToggleFavourite(currency)
                showPicker = false
            },
            onToggleFavourite = onToggleFavourite,
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun BaseAmountRow(currency: Currency, text: String) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CurrencyMetadata.flagOf(currency)?.let { flag ->
                Text(flag, style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = currency.code,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = text.ifEmpty { "0" },
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoardRowItem(
    row: BoardRow,
    setAsBaseLabel: String,
    onLongPress: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Tenere premuto promuove la valuta a base: è il gesto per
            // invertire la domanda ("e il contrario quanto fa?") senza
            // ridigitare l'importo.
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            // Una pressione prolungata è invisibile a chi usa TalkBack:
            // dichiararla come azione le dà un nome e la rende raggiungibile.
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(setAsBaseLabel) {
                        onLongPress()
                        true
                    },
                )
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = row.flag ?: "\u2009",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = row.currency.code,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(52.dp),
        )
        Text(
            text = row.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = row.formatted.ifEmpty { "—" },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.board_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.board_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(onClick = onAdd, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(
                text = stringResource(R.string.board_add_currency),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
