package it.bbnss.moneta.ui.convert

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.bbnss.moneta.R
import it.bbnss.moneta.core.model.AmountFormat
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.CurrencyMetadata
import it.bbnss.moneta.core.ui.components.KeypadKey
import it.bbnss.moneta.core.ui.components.MonetaKeypad
import it.bbnss.moneta.ui.components.CurrencyPickerSheet
import it.bbnss.moneta.ui.components.FreshnessBadge
import java.text.DecimalFormatSymbols

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
    onMarkupChanged: (java.math.BigDecimal) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerFor by remember { mutableStateOf<Field?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    val openHistoryLabel = stringResource(R.string.history_open)
    var showMarkup by remember { mutableStateOf(false) }

    val messageText = state.message?.let { messageText(it) }
    LaunchedEffect(state.message) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onMessageShown()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.suggestion?.let { suggestion ->
            LocalCurrencyBanner(
                suggestion = suggestion,
                onAccept = onAcceptSuggestion,
                onDismiss = onDismissSuggestion,
            )
        }

        AmountRow(
            currency = state.from,
            text = state.fromText,
            active = state.activeField == Field.FROM,
            onFieldClick = { onFieldSelected(Field.FROM) },
            onCurrencyClick = { pickerFor = Field.FROM },
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            val rotation by animateFloatAsState(
                targetValue = if (state.activeField == Field.FROM) 0f else 180f,
                label = "swap",
            )
            FilledTonalIconButton(onClick = onSwap) {
                Icon(
                    Icons.Default.SwapVert,
                    contentDescription = stringResource(R.string.convert_swap),
                    modifier = Modifier.rotate(rotation),
                )
            }
        }

        AmountRow(
            currency = state.to,
            text = state.toText,
            active = state.activeField == Field.TO,
            onFieldClick = { onFieldSelected(Field.TO) },
            onCurrencyClick = { pickerFor = Field.TO },
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // La riga con la commissione sta sopra il tasso e sotto il
            // risultato: è la cifra che conta davvero per chi sta per pagare.
            state.withFeeText?.let { withFee ->
                Text(
                    text = stringResource(
                        R.string.markup_with_fee,
                        withFee,
                        "${state.markupPercent.stripTrailingZeros().toPlainString()}%",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            if (state.rate != null) {
                // Il tasso è il punto naturale da cui chiedere "e prima?", ma
                // un testo che si può toccare non si distingue da uno che non
                // si può: l'icona rende visibile che lì sotto c'è un grafico.
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenHistory)
                        .semantics(mergeDescendants = true) {
                            contentDescription = openHistoryLabel
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.convert_rate_line,
                            state.from.code,
                            AmountFormat.formatRate(state.rate),
                            state.to.code,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            FreshnessBadge(
                freshness = state.freshness,
                age = state.age,
                updatedAt = state.updatedAt,
                onClick = { showDetails = true },
            )

            // La commissione va impostata dove si guarda il risultato, non
            // sepolta nelle impostazioni: è una scelta che cambia da viaggio a
            // viaggio, e spesso da carta a carta.
            AssistChip(
                onClick = { showMarkup = true },
                label = {
                    Text(
                        if (state.markupPercent.signum() > 0) {
                            stringResource(
                                R.string.markup_chip_set,
                                "${state.markupPercent.stripTrailingZeros().toPlainString()}%",
                            )
                        } else {
                            stringResource(R.string.markup_chip_none)
                        },
                    )
                },
            )

            state.error?.let { error ->
                Text(
                    text = errorText(error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Box(Modifier.weight(1f))

        MonetaKeypad(
            onKey = onKey,
            modifier = Modifier.padding(bottom = 12.dp),
            // Il tasto decimale mostra il simbolo della lingua dell'utente:
            // virgola in italiano, punto in inglese. Il parser accetta comunque
            // entrambi, ma la tastiera deve dire la cosa giusta.
            decimalSeparator = DecimalFormatSymbols
                .getInstance(LocalConfiguration.current.locales[0])
                .decimalSeparator,
            clearLabel = stringResource(R.string.keypad_clear),
            backspaceDescription = stringResource(R.string.keypad_backspace),
            equalsDescription = stringResource(R.string.keypad_equals),
        )
    }

    pickerFor?.let { field ->
        CurrencyPickerSheet(
            currencies = state.availableCurrencies,
            favourites = state.favourites,
            onPick = { currency ->
                onCurrencySelected(field, currency)
                pickerFor = null
            },
            onToggleFavourite = onToggleFavourite,
            onDismiss = { pickerFor = null },
        )
    }

    if (showDetails) {
        RateDetailsDialog(state = state, onDismiss = { showDetails = false })
    }

    if (showMarkup) {
        MarkupDialog(
            current = state.markupPercent,
            onConfirm = {
                onMarkupChanged(it)
                showMarkup = false
            },
            onDismiss = { showMarkup = false },
        )
    }
}

/**
 * Proposta di passare alla valuta del posto in cui sembri trovarti.
 *
 * È un invito, mai un cambio automatico: l'app non deve mai spostare le valute
 * sotto le dita di chi la sta usando. Se lo si rifiuta, per quel paese non
 * torna più.
 */
@Composable
private fun LocalCurrencyBanner(
    suggestion: LocalSuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    R.string.suggestion_local_currency,
                    suggestion.countryName,
                    suggestion.currency.code,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.suggestion_dismiss))
                }
                TextButton(onClick = onAccept) {
                    Text(stringResource(R.string.suggestion_accept, suggestion.currency.code))
                }
            }
        }
    }
}

/**
 * Una delle due valute con il suo importo.
 *
 * Il campo attivo è evidenziato dal bordo: il tastierino scrive lì, e senza un
 * segnale visibile non si capirebbe quale dei due si sta modificando.
 */
@Composable
private fun AmountRow(
    currency: Currency,
    text: String,
    active: Boolean,
    onFieldClick: () -> Unit,
    onCurrencyClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onFieldClick)
            .then(
                if (active) {
                    Modifier.border(2.dp, colors.primary, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (active) colors.surfaceVariant else colors.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                onClick = onCurrencyClick,
                shape = RoundedCornerShape(12.dp),
                color = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // La bandiera si riconosce prima del codice: è il primo
                    // appiglio quando si cambia valuta di continuo in viaggio.
                    CurrencyMetadata.flagOf(currency)?.let { flag ->
                        Text(flag, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = currency.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            Text(
                text = text.ifEmpty { "0" },
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.End,
                maxLines = 1,
                color = if (text.isEmpty()) colors.onSurfaceVariant else colors.onSurface,
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
            )
        }
    }
}

@Composable
private fun messageText(message: RefreshMessage): String = when (message) {
    is RefreshMessage.Updated ->
        stringResource(R.string.refresh_updated, message.provider.displayName)

    RefreshMessage.Failed -> stringResource(R.string.refresh_failed)
    RefreshMessage.Offline -> stringResource(R.string.refresh_offline)
}

@Composable
private fun errorText(error: ConvertError): String = when (error) {
    ConvertError.DivisionByZero -> stringResource(R.string.convert_division_by_zero)
    is ConvertError.UnsupportedPair ->
        stringResource(R.string.convert_unsupported_pair, error.provider.displayName)
}
