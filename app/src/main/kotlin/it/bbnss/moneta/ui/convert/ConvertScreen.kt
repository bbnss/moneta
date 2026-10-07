package it.bbnss.moneta.ui.convert

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import it.bbnss.moneta.R
import it.bbnss.moneta.core.model.*
import it.bbnss.moneta.core.model.calc.Expression
import it.bbnss.moneta.core.ui.components.*
import it.bbnss.moneta.ui.components.*
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun ConvertScreen(
    state: ConvertUiState,
    snackbarHostState: SnackbarHostState,
    onKey: (KeypadKey) -> Unit,
    onFieldSelected: (Field) -> Unit,
    onCurrencySelected: (Field, Currency) -> Unit,
    onToggleFavourite: (Currency) -> Unit,
    onSwap: () -> Unit,
    onMessageShown: () -> Unit,
    onAcceptSuggestion: () -> Unit,
    onDismissSuggestion: () -> Unit,
    onOpenHistory: () -> Unit,
    onMarkupChanged: (BigDecimal) -> Unit,
    onFeeModeChanged: (FeeMode) -> Unit,
    onPaste: (Field, String, Locale) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerFor by remember { mutableStateOf<Field?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    var showMarkup by remember { mutableStateOf(false) }
    var calculator by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val locale = LocalConfiguration.current.locales[0]
    LaunchedEffect(state.input) {
        if (state.input.isNotEmpty() && !Expression.isPlainNumber(state.input)) calculator = true
    }
    val message = state.message?.let { messageText(it) }
    LaunchedEffect(state.message) {
        if (message != null) { snackbarHostState.showSnackbar(message); onMessageShown() }
    }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AmountRow(state.from, state.fromText, state.activeField == Field.FROM,
                onFieldClick = { onFieldSelected(Field.FROM) }, onCurrencyClick = { pickerFor = Field.FROM },
                onCopy = { clipboard.setText(AnnotatedString(state.fromText.ifEmpty { "0" })) },
                onPaste = { onPaste(Field.FROM, clipboard.getText()?.text.orEmpty(), locale) },
                onClear = { onFieldSelected(Field.FROM); onKey(KeypadKey.Clear) })
            AmountRow(state.to, state.toText, state.activeField == Field.TO,
                onFieldClick = { onFieldSelected(Field.TO) }, onCurrencyClick = { pickerFor = Field.TO },
                onCopy = { clipboard.setText(AnnotatedString(state.toText.ifEmpty { "0" })) },
                onPaste = { onPaste(Field.TO, clipboard.getText()?.text.orEmpty(), locale) },
                onClear = { onFieldSelected(Field.TO); onKey(KeypadKey.Clear) })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showMarkup = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (state.feeMode == FeeMode.CASH) R.string.fee_receive else R.string.fee_cost) +
                        " · " + stringResource(R.string.markup_label) + " ${state.markupPercent.stripTrailingZeros().toPlainString()}%")
                }
                IconButton(onClick = onSwap) { Icon(Icons.Default.SwapVert, stringResource(R.string.convert_swap)) }
            }
            state.rate?.let { rate ->
                Text(stringResource(R.string.reference_rate, state.from.code, AmountFormat.formatRate(rate), state.to.code),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FreshnessBadge(state.freshness, state.age, state.updatedAt, rateDate = state.rateDate,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp), onClick = { showDetails = true })
                IconButton(onClick = onOpenHistory) { Icon(Icons.Default.ShowChart, stringResource(R.string.history_open)) }
            }
            state.error?.let { Text(errorText(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
        TextButton(onClick = { calculator = !calculator }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (calculator) R.string.keypad_numeric else R.string.keypad_calculator))
        }
        MonetaKeypad(onKey, Modifier.padding(bottom = 4.dp),
            layout = if (calculator) KeypadLayout.CALCULATOR else KeypadLayout.NUMERIC,
            keyHeight = 48.dp, decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator,
            clearLabel = stringResource(R.string.keypad_clear), backspaceDescription = stringResource(R.string.keypad_backspace),
            equalsDescription = stringResource(R.string.keypad_equals))
    }
    pickerFor?.let { field -> CurrencyPickerSheet(state.availableCurrencies, state.favourites,
        onPick = { onCurrencySelected(field, it); pickerFor = null },
        onToggleFavourite = onToggleFavourite, onDismiss = { pickerFor = null }) }
    if (showDetails) RateDetailsDialog(state, onDismiss = { showDetails = false },
        onAcceptSuggestion = onAcceptSuggestion, onDismissSuggestion = onDismissSuggestion)
    if (showMarkup) MarkupDialog(state.markupPercent, state.feeMode, onFeeModeChanged,
        onConfirm = { onMarkupChanged(it); showMarkup = false }, onDismiss = { showMarkup = false })
}

@Composable
private fun AmountRow(currency: Currency, text: String, active: Boolean, onFieldClick: () -> Unit,
                      onCurrencyClick: () -> Unit, onCopy: () -> Unit, onPaste: () -> Unit, onClear: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth().clickable(onClick = onFieldClick)
        .then(if (active) Modifier.border(2.dp, colors.primary, RoundedCornerShape(16.dp)) else Modifier),
        shape = RoundedCornerShape(16.dp), color = if (active) colors.surfaceVariant else colors.surface) {
        Row(Modifier.heightIn(min = 60.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCurrencyClick) {
                Text("${CurrencyMetadata.flagOf(currency).orEmpty()} ${currency.code}", style = MaterialTheme.typography.titleMedium)
            }
            AmountText(text, Modifier.weight(1f))
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.amount_actions)) }
                DropdownMenu(menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.amount_copy)) }, onClick = { onCopy(); menu = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.amount_paste)) }, onClick = { onPaste(); menu = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.board_clear_amount)) }, onClick = { onClear(); menu = false })
                }
            }
        }
    }
}

@Composable
private fun messageText(message: RefreshMessage): String = when (message) {
    is RefreshMessage.Updated -> stringResource(R.string.refresh_updated, message.provider.displayName)
    RefreshMessage.Failed -> stringResource(R.string.refresh_failed)
    RefreshMessage.Offline -> stringResource(R.string.refresh_offline)
    RefreshMessage.Wifi -> stringResource(R.string.refresh_wifi)
    RefreshMessage.InvalidPaste -> stringResource(R.string.amount_invalid)
}
@Composable
private fun errorText(error: ConvertError): String = when (error) {
    ConvertError.DivisionByZero -> stringResource(R.string.convert_division_by_zero)
    is ConvertError.UnsupportedPair -> stringResource(R.string.convert_unsupported_pair, error.provider.displayName)
}
