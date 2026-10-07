package it.bbnss.moneta.ui.convert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.bbnss.moneta.R
import java.math.BigDecimal

/**
 * Maggiorazione applicata da carta o cambiavalute.
 *
 * Il tasso di riferimento non è quello che si ottiene allo sportello: la
 * differenza è la commissione, e dichiararla trasforma un numero teorico in
 * quello che ci si troverà davvero in mano. I valori proposti coprono i casi
 * comuni — carte senza commissione, carte bancarie tradizionali, cambiavalute
 * in aeroporto — così nella maggior parte dei casi non c'è nulla da digitare.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkupDialog(
    current: BigDecimal,
    mode: it.bbnss.moneta.core.model.FeeMode,
    onModeChanged: (it.bbnss.moneta.core.model.FeeMode) -> Unit,
    onConfirm: (BigDecimal) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = remember {
        listOf("0", "0.5", "1", "1.5", "2", "2.5", "3", "5", "8", "12").map { BigDecimal(it) }
    }
    var selected by remember { mutableStateOf(current) }
    var selectedMode by remember { mutableStateOf(mode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.markup_label)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                it.bbnss.moneta.core.model.FeeMode.entries.forEach { option ->
                    FilterChip(selected = selectedMode == option, onClick = { selectedMode = option },
                        label = { Text(stringResource(if (option == it.bbnss.moneta.core.model.FeeMode.CASH) R.string.fee_cash else R.string.fee_card)) })
                }
                Text(
                    text = stringResource(R.string.markup_explain),
                    style = MaterialTheme.typography.bodyMedium,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        FilterChip(
                            selected = option.compareTo(selected) == 0,
                            onClick = { selected = option },
                            label = {
                                Text(
                                    if (option.signum() == 0) {
                                        stringResource(R.string.markup_none)
                                    } else {
                                        "${option.stripTrailingZeros().toPlainString()}%"
                                    },
                                )
                            },
                            modifier = Modifier,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onModeChanged(selectedMode); onConfirm(selected) }) {
                Text(stringResource(R.string.markup_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.markup_cancel))
            }
        },
    )
}
