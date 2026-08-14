package it.bbnss.moneta.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Tasto premuto. Il testo vero lo compone la schermata: qui si emettono eventi. */
sealed interface KeypadKey {
    /** Cifra, separatore decimale o operatore. */
    data class Symbol(val value: Char) : KeypadKey

    data object Backspace : KeypadKey

    data object Clear : KeypadKey

    /** Riduce l'espressione al suo risultato. */
    data object Equals : KeypadKey
}

/**
 * Tastierino del convertitore.
 *
 * Esiste al posto della tastiera di sistema per un motivo pratico: si usa
 * spesso con una mano sola, al mercato o alla cassa, spesso con il sole in
 * faccia. I tasti sono grandi e sempre nella stessa posizione, mentre la
 * tastiera numerica di sistema cambia disposizione da dispositivo a dispositivo
 * e apre suggerimenti che qui non servono.
 *
 * Le parentesi ci sono perché il campo importo accetta espressioni: dividere il
 * conto o aggiungere la mancia si fa qui, senza uscire dall'app.
 */
@Composable
fun MonetaKeypad(
    onKey: (KeypadKey) -> Unit,
    modifier: Modifier = Modifier,
    decimalSeparator: Char = ',',
    clearLabel: String = "C",
    backspaceDescription: String = "Backspace",
    equalsDescription: String = "=",
) {
    val rows = listOf(
        listOf(
            Key.Action(clearLabel, KeypadKey.Clear, KeyKind.MODIFIER),
            Key.Action("(", KeypadKey.Symbol('('), KeyKind.MODIFIER),
            Key.Action(")", KeypadKey.Symbol(')'), KeyKind.MODIFIER),
            Key.Action("÷", KeypadKey.Symbol('÷'), KeyKind.OPERATOR),
        ),
        listOf(
            Key.Digit('7'), Key.Digit('8'), Key.Digit('9'),
            Key.Action("×", KeypadKey.Symbol('×'), KeyKind.OPERATOR),
        ),
        listOf(
            Key.Digit('4'), Key.Digit('5'), Key.Digit('6'),
            Key.Action("−", KeypadKey.Symbol('-'), KeyKind.OPERATOR),
        ),
        listOf(
            Key.Digit('1'), Key.Digit('2'), Key.Digit('3'),
            Key.Action("+", KeypadKey.Symbol('+'), KeyKind.OPERATOR),
        ),
        listOf(
            Key.Action(decimalSeparator.toString(), KeypadKey.Symbol(decimalSeparator), KeyKind.DIGIT),
            Key.Digit('0'),
            Key.Icon(KeypadKey.Backspace, backspaceDescription),
            Key.Action("=", KeypadKey.Equals, KeyKind.PRIMARY, equalsDescription),
        ),
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    KeyButton(
                        key = key,
                        onKey = onKey,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private enum class KeyKind { DIGIT, OPERATOR, MODIFIER, PRIMARY }

private sealed interface Key {
    val kind: KeyKind
    val event: KeypadKey

    data class Digit(val value: Char) : Key {
        override val kind = KeyKind.DIGIT
        override val event = KeypadKey.Symbol(value)
    }

    data class Action(
        val label: String,
        override val event: KeypadKey,
        override val kind: KeyKind,
        val description: String? = null,
    ) : Key

    data class Icon(override val event: KeypadKey, val description: String) : Key {
        override val kind = KeyKind.MODIFIER
    }
}

@Composable
private fun KeyButton(
    key: Key,
    onKey: (KeypadKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container = when (key.kind) {
        KeyKind.DIGIT -> colors.surfaceVariant
        KeyKind.OPERATOR -> colors.secondaryContainer
        KeyKind.MODIFIER -> colors.surfaceVariant.copy(alpha = 0.6f)
        KeyKind.PRIMARY -> colors.primary
    }
    val content = when (key.kind) {
        KeyKind.DIGIT, KeyKind.MODIFIER -> colors.onSurfaceVariant
        KeyKind.OPERATOR -> colors.onSecondaryContainer
        KeyKind.PRIMARY -> colors.onPrimary
    }

    val description = when (key) {
        is Key.Icon -> key.description
        is Key.Action -> key.description ?: key.label
        is Key.Digit -> key.value.toString()
    }

    Surface(
        onClick = { onKey(key.event) },
        modifier = modifier
            // 56dp è il minimo perché resti centrabile senza guardare; con lo
            // schermo al sole e una mano sola, tasti più piccoli si sbagliano.
            .height(56.dp)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(16.dp),
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (key) {
                is Key.Icon -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = null,
                    tint = LocalContentColor.current,
                )

                is Key.Digit -> KeyLabel(key.value.toString())
                is Key.Action -> KeyLabel(key.label)
            }
        }
    }
}

@Composable
private fun KeyLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = Color.Unspecified,
    )
}
