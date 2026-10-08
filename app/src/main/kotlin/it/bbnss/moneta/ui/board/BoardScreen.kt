package it.bbnss.moneta.ui.board

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.MoreVert
import it.bbnss.moneta.ui.components.AmountText
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.bbnss.moneta.R
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.CurrencyMetadata
import it.bbnss.moneta.core.ui.components.KeypadKey
import it.bbnss.moneta.core.ui.components.KeypadLayout
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
    onMove: (Currency, Int) -> Unit,
    onPaste: (String, java.util.Locale) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val locale = LocalConfiguration.current.locales[0]
    val clipboardLabels = BoardClipboardLabels(
        stringResource(R.string.board_copy_title), stringResource(R.string.board_copy_rate_date),
        stringResource(R.string.board_copy_verified), stringResource(R.string.board_copy_unknown))
    var invalidPaste by remember { mutableStateOf(false) }
    var reordering by rememberSaveable { mutableStateOf(false) }
    val dragStep = with(LocalDensity.current) { 60.dp.toPx() }
    val moveUp = stringResource(R.string.favourites_up)
    val moveDown = stringResource(R.string.favourites_down)
    var showPicker by remember { mutableStateOf(false) }
    var showBasePicker by rememberSaveable { mutableStateOf(false) }
    var showKeypad by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BaseAmountRow(
            currency = state.base,
            text = state.inputText,
            keypadVisible = showKeypad,
            onToggleKeypad = { showKeypad = !showKeypad },
            onClear = { onKey(KeypadKey.Clear) },
            onCurrencyClick = { showBasePicker = true },
        )

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            TextButton(onClick = { clipboard.setText(AnnotatedString(state.clipboardText(clipboardLabels, locale))) }) { Text(stringResource(R.string.amount_copy)) }
            TextButton(onClick = { invalidPaste = !onPaste(clipboard.getText()?.text.orEmpty(), locale) }) { Text(stringResource(R.string.amount_paste)) }
            TextButton(onClick = { reordering = !reordering }) {
                Text(stringResource(if (reordering) R.string.favourites_done else R.string.favourites_reorder))
            }
        }
        if (invalidPaste) Text(stringResource(R.string.amount_invalid), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))

        if (!state.hasFavourites) {
            EmptyState(Modifier.weight(1f), onAdd = { showPicker = true })
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            ) {
                items(state.rows, key = { it.currency.code }) { row ->
                    Row(Modifier.fillMaxWidth().then(if (reordering) Modifier.pointerInput(row.currency, dragStep) {
                        var distance = 0f
                        detectDragGesturesAfterLongPress(onDragStart = { distance = 0f }, onDrag = { change, drag ->
                            change.consume()
                            distance += drag.y
                            if (kotlin.math.abs(distance) >= dragStep) {
                                onMove(row.currency, if (distance > 0) 1 else -1)
                                distance = 0f
                            }
                        })
                    }.semantics {
                        customActions = listOf(CustomAccessibilityAction(moveUp) { onMove(row.currency, -1); true },
                            CustomAccessibilityAction(moveDown) { onMove(row.currency, 1); true })
                    } else Modifier), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { BoardRowItem(
                        row = row,
                        reordering = reordering,
                        setAsBaseLabel = stringResource(
                            R.string.board_set_as_base,
                            row.currency.code,
                        ),
                        onLongPress = { if (!reordering) onSetAsBase(row.currency) },
                    )
                    }
                    if (reordering) {
                        IconButton(onClick = { onMove(row.currency, -1) }) { Icon(Icons.Default.KeyboardArrowUp, moveUp) }
                        IconButton(onClick = { onMove(row.currency, 1) }) { Icon(Icons.Default.KeyboardArrowDown, moveDown) }
                    }
                    }
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
                        text = stringResource(if (reordering) R.string.board_reorder_hint else R.string.board_base_hint),
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
            rateDate = state.rateDate,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        // Qui non si fanno conti: si scrive un importo e si guarda l'elenco.
        // Il tastierino da calcolatrice rubava metà schermo alle valute, che
        // sono il motivo per cui si apre questa schermata; questo ha tre
        // colonne, una riga in meno e si può chiudere del tutto.
        if (showKeypad) {
            MonetaKeypad(
                onKey = onKey,
                modifier = Modifier.padding(bottom = 12.dp),
                layout = KeypadLayout.NUMERIC,
                keyHeight = 48.dp,
                decimalSeparator = DecimalFormatSymbols
                    .getInstance(LocalConfiguration.current.locales[0])
                    .decimalSeparator,
                clearLabel = stringResource(R.string.keypad_clear),
                backspaceDescription = stringResource(R.string.keypad_backspace),
                equalsDescription = stringResource(R.string.keypad_equals),
            )
        }
    }

    if (showPicker || showBasePicker) {
        CurrencyPickerSheet(
            currencies = state.availableCurrencies,
            favourites = state.favourites,
            onPick = { currency ->
                if (showBasePicker) onSetAsBase(currency)
                else if (currency !in state.favourites) onToggleFavourite(currency)
                showPicker = false
                showBasePicker = false
            },
            onToggleFavourite = onToggleFavourite,
            onDismiss = { showPicker = false; showBasePicker = false },
        )
    }
}

@Composable
private fun BaseAmountRow(
    currency: Currency,
    text: String,
    keypadVisible: Boolean,
    onToggleKeypad: () -> Unit,
    onClear: () -> Unit,
    onCurrencyClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val currencyDescription = stringResource(R.string.board_pick_base, currency.code)
            TextButton(onClick = onCurrencyClick,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                modifier = Modifier.semantics { contentDescription = currencyDescription }) {
                CurrencyMetadata.flagOf(currency)?.let { flag ->
                    Text(flag, style = MaterialTheme.typography.titleLarge)
                }
                Text(currency.code, style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 6.dp))
                Icon(Icons.Default.ExpandMore, null, Modifier.width(16.dp))
            }
            AmountText(text, Modifier.weight(1f))

            // Azzerare è l'operazione più frequente qui: si arriva con un
            // importo vecchio e se ne vuole scrivere uno nuovo.
            if (text.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(R.string.board_clear_amount),
                    )
                }
            }

            IconButton(onClick = onToggleKeypad) {
                Icon(
                    imageVector = if (keypadVisible) {
                        Icons.Default.KeyboardArrowDown
                    } else {
                        Icons.Default.KeyboardArrowUp
                    },
                    contentDescription = stringResource(
                        if (keypadVisible) R.string.board_hide_keypad else R.string.board_show_keypad,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoardRowItem(
    row: BoardRow,
    reordering: Boolean,
    setAsBaseLabel: String,
    onLongPress: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Tenere premuto promuove la valuta a base: è il gesto per
            // invertire la domanda ("e il contrario quanto fa?") senza
            // ridigitare l'importo.
            .then(if (reordering) Modifier else Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress))
            // Una pressione prolungata è invisibile a chi usa TalkBack:
            // dichiararla come azione le dà un nome e la rende raggiungibile.
            .semantics {
                customActions = if (reordering) emptyList() else listOf(
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
