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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
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
 * Quanto tastierino serve.
 *
 * Non tutte le schermate chiedono la stessa cosa: nel convertitore si fanno
 * conti veri — dividere il conto, aggiungere la mancia — mentre nell'elenco
 * delle valute si scrive solo una cifra e si guarda l'elenco. Dare a entrambe
 * la stessa tastiera significa rubare mezzo schermo a chi non ne ha bisogno.
 */
enum class KeypadLayout { CALCULATOR, NUMERIC }

/**
 * Altezza dei tasti adatta allo schermo su cui si sta girando.
 *
 * Su un telefono corto — o con il testo di sistema ingrandito, che è lo stesso
 * problema visto da un'altra angolazione — cinque righe da 56dp più i due campi
 * degli importi non ci stanno. Meglio tasti leggermente più bassi che una riga
 * di tasti irraggiungibile.
 */
@Composable
fun rememberKeyHeight(): Dp =
    if (LocalConfiguration.current.screenHeightDp < 700) 48.dp else 56.dp

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
    layout: KeypadLayout = KeypadLayout.CALCULATOR,
    keyHeight: Dp = 56.dp,
    decimalSeparator: Char = ',',
    clearLabel: String = "C",
    backspaceDescription: String = "Backspace",
    equalsDescription: String = "=",
) {
    val decimal = Key.Action(
        decimalSeparator.toString(),
        KeypadKey.Symbol(decimalSeparator),
        KeyKind.DIGIT,
    )
    val backspace = Key.Icon(KeypadKey.Backspace, backspaceDescription)
    val clear = Key.Action(clearLabel, KeypadKey.Clear, KeyKind.MODIFIER)

    val rows = when (layout) {
        KeypadLayout.CALCULATOR -> listOf(
            listOf(
                clear,
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
                decimal,
                Key.Digit('0'),
                backspace,
                Key.Action("=", KeypadKey.Equals, KeyKind.PRIMARY, equalsDescription),
            ),
        )

        // Tre colonne come su un tastierino telefonico, senza operatori: si
        // digita un importo e basta. Una riga in meno e tasti più bassi
        // liberano un terzo dello schermo per l'elenco delle valute.
        KeypadLayout.NUMERIC -> listOf(
            listOf(Key.Digit('7'), Key.Digit('8'), Key.Digit('9'), backspace),
            listOf(Key.Digit('4'), Key.Digit('5'), Key.Digit('6'), Key.Digit('0')),
            listOf(Key.Digit('1'), Key.Digit('2'), Key.Digit('3'), decimal),
        )
    }

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
                        height = keyHeight,
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
    height: Dp,
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
            // 56dp è la misura di riferimento: sotto, con lo schermo al sole e
            // una mano sola, i tasti si sbagliano. Su schermi bassi si scende a
            // 48dp, che resta sopra il minimo tattile raccomandato — meglio un
            // tasto un po' più basso che una riga di tasti fuori dallo schermo.
            .height(height)
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
