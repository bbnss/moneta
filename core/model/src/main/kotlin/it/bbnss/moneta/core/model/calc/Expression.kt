package it.bbnss.moneta.core.model.calc

import it.bbnss.moneta.core.model.MonetaryMath
import java.math.BigDecimal

/**
 * Piccolo valutatore di espressioni aritmetiche su [BigDecimal].
 *
 * Serve perché al ristorante non si digita un numero, si fa un conto: `12+8*3`,
 * `84/4`, `36+10%`. Farlo dentro il campo importo evita di uscire dall'app per
 * aprire la calcolatrice e rientrare.
 *
 * È scritto a mano e non con una libreria per due motivi: nessuna dipendenza in
 * più da giustificare a F-Droid, e soprattutto il controllo completo
 * sull'aritmetica — quasi tutte le librerie di espressioni lavorano in `Double`,
 * che è esattamente ciò che questa app non fa mai col denaro.
 */
object Expression {

    private val HUNDRED = BigDecimal(100)

    sealed interface Result {
        data class Value(val amount: BigDecimal) : Result

        /**
         * L'espressione è troncata a metà (`12+`): capita a ogni tasto premuto.
         * Non è un errore da mostrare, è il normale stato di chi sta scrivendo.
         */
        data object Incomplete : Result

        data class Invalid(val reason: Reason) : Result

        enum class Reason { DIVISION_BY_ZERO, MALFORMED }
    }

    fun evaluate(input: String): Result {
        val text = input.trim()
        if (text.isEmpty()) return Result.Incomplete

        return when (val parsed = parse(text)) {
            null -> {
                // Prima di dichiarare un errore si prova a valutare il prefisso
                // valido: mentre si digita, "12+" deve continuare a mostrare 12.
                val truncated = text.trimEnd { it in "+-*/×÷(., " }
                if (truncated.isNotEmpty() && truncated != text) {
                    when (val retry = parse(truncated)) {
                        null -> Result.Invalid(Result.Reason.MALFORMED)
                        else -> evaluateNode(retry)
                    }
                } else {
                    Result.Incomplete
                }
            }

            else -> evaluateNode(parsed)
        }
    }

    /** `true` se il testo è un semplice numero, senza operazioni. */
    fun isPlainNumber(input: String): Boolean =
        input.isNotEmpty() && input.all { it.isDigit() || it == '.' || it == ',' }

    private fun evaluateNode(node: Node): Result = try {
        Result.Value(eval(node))
    } catch (e: ArithmeticException) {
        Result.Invalid(
            if (e.message?.contains("zero", ignoreCase = true) == true) {
                Result.Reason.DIVISION_BY_ZERO
            } else {
                Result.Reason.MALFORMED
            },
        )
    }

    // -- albero sintattico ---------------------------------------------------

    private sealed interface Node
    private data class Num(val value: BigDecimal) : Node
    private data class Neg(val operand: Node) : Node
    private data class Percent(val operand: Node) : Node
    private data class Bin(val op: Char, val left: Node, val right: Node) : Node

    private fun eval(node: Node): BigDecimal = when (node) {
        is Num -> node.value
        is Neg -> eval(node.operand).negate()
        // Una percentuale da sola vale la sua frazione: 10% = 0,1.
        is Percent -> eval(node.operand).divide(HUNDRED, MonetaryMath.CONTEXT)

        is Bin -> {
            val left = eval(node.left)
            if ((node.op == '+' || node.op == '-') && node.right is Percent) {
                // Comportamento da calcolatrice, quello che si dà per scontato
                // quando si aggiunge la mancia: `36+10%` è 39,60, non 36,10.
                val percentage = eval(node.right.operand)
                val delta = left.multiply(percentage, MonetaryMath.CONTEXT)
                    .divide(HUNDRED, MonetaryMath.CONTEXT)
                if (node.op == '+') left.add(delta) else left.subtract(delta)
            } else {
                val right = eval(node.right)
                when (node.op) {
                    '+' -> left.add(right)
                    '-' -> left.subtract(right)
                    '*' -> left.multiply(right, MonetaryMath.CONTEXT)
                    else -> {
                        if (right.signum() == 0) throw ArithmeticException("division by zero")
                        left.divide(right, MonetaryMath.CONTEXT)
                    }
                }
            }
        }
    }

    // -- analisi sintattica --------------------------------------------------

    /**
     * Discesa ricorsiva invece dello shunting-yard: la percentuale ha bisogno di
     * sapere qual è l'operando a sinistra, e con un albero esplicito la regola
     * si legge in una riga.
     */
    private fun parse(text: String): Node? {
        val parser = Parser(text)
        val node = parser.expression() ?: return null
        parser.skipSpaces()
        return if (parser.atEnd) node else null
    }

    private class Parser(private val text: String) {
        private var position = 0

        val atEnd: Boolean get() = position >= text.length

        fun skipSpaces() {
            while (position < text.length && text[position] == ' ') position++
        }

        private fun peek(): Char? {
            skipSpaces()
            return text.getOrNull(position)
        }

        fun expression(): Node? {
            var left = term() ?: return null
            while (true) {
                val op = peek()
                if (op != '+' && op != '-') return left
                position++
                val right = term() ?: return null
                left = Bin(op, left, right)
            }
        }

        private fun term(): Node? {
            var left = factor() ?: return null
            while (true) {
                val op = when (peek()) {
                    '*', '×' -> '*'
                    '/', '÷', ':' -> '/'
                    else -> return left
                }
                position++
                val right = factor() ?: return null
                left = Bin(op, left, right)
            }
        }

        private fun factor(): Node? {
            val sign = peek()
            if (sign == '-') {
                position++
                return factor()?.let { Neg(it) }
            }
            if (sign == '+') {
                position++
                return factor()
            }

            var node = primary() ?: return null
            if (peek() == '%') {
                position++
                node = Percent(node)
            }
            return node
        }

        private fun primary(): Node? {
            when (peek()) {
                '(' -> {
                    position++
                    val inner = expression() ?: return null
                    if (peek() != ')') return null
                    position++
                    return inner
                }

                else -> return number()
            }
        }

        private fun number(): Node? {
            skipSpaces()
            val start = position
            var seenSeparator = false

            while (position < text.length) {
                val char = text[position]
                when {
                    char.isDigit() -> position++
                    // Virgola e punto sono intercambiabili: la tastiera cambia
                    // simbolo a seconda della lingua, il significato no.
                    (char == '.' || char == ',') && !seenSeparator -> {
                        seenSeparator = true
                        position++
                    }

                    else -> break
                }
            }

            if (position == start) return null

            val literal = text.substring(start, position).replace(',', '.')
            // Un separatore finale ("12,") è un numero a metà digitazione.
            if (literal.endsWith('.')) return null
            return runCatching { Num(BigDecimal(literal)) }.getOrNull()
        }
    }
}
