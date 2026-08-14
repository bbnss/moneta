package it.bbnss.moneta.core.model.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

private fun value(input: String): BigDecimal {
    val result = Expression.evaluate(input)
    assertTrue("Atteso un valore per '$input', ottenuto $result", result is Expression.Result.Value)
    return (result as Expression.Result.Value).amount
}

private fun assertValue(expected: String, input: String) {
    assertEquals("Espressione '$input'", 0, BigDecimal(expected).compareTo(value(input)))
}

class ExpressionTest {

    @Test
    fun `un numero e gia un espressione`() {
        assertValue("45000", "45000")
        assertValue("12.5", "12.5")
    }

    @Test
    fun `accetta virgola e punto come separatore decimale`() {
        assertValue("12.5", "12,5")
        assertValue("12.5", "12.5")
    }

    @Test
    fun `rispetta la precedenza degli operatori`() {
        assertValue("36", "12+8*3")
        assertValue("60", "(12+8)*3")
    }

    @Test
    fun `accetta i simboli di moltiplicazione e divisione della tastiera`() {
        assertValue("36", "12×3")
        assertValue("4", "12÷3")
    }

    @Test
    fun `divide il conto`() {
        assertValue("21", "84/4")
    }

    /**
     * Comportamento da calcolatrice: `36+10%` è il conto con la mancia, cioè
     * 39,60. Interpretare la percentuale come "più 0,1" darebbe 36,10 e
     * renderebbe la funzione inutile proprio nel caso per cui esiste.
     */
    @Test
    fun `la percentuale dopo una somma e relativa al totale`() {
        assertValue("39.6", "36+10%")
        assertValue("32.4", "36-10%")
    }

    @Test
    fun `una percentuale da sola vale la sua frazione`() {
        assertValue("0.1", "10%")
        assertValue("3.6", "36*10%")
    }

    @Test
    fun `gestisce il segno meno iniziale`() {
        assertValue("-5", "-5")
        assertValue("5", "10+-5")
    }

    /**
     * Mentre si digita l'espressione è quasi sempre incompleta: non deve mai
     * comparire un errore, deve restare visibile il risultato del prefisso.
     */
    @Test
    fun `un operatore in coda non e un errore`() {
        assertValue("12", "12+")
        assertValue("12", "12*")
        assertValue("45", "45,")
    }

    @Test
    fun `l input vuoto e incompleto, non sbagliato`() {
        assertEquals(Expression.Result.Incomplete, Expression.evaluate(""))
        assertEquals(Expression.Result.Incomplete, Expression.evaluate("   "))
    }

    @Test
    fun `segnala la divisione per zero`() {
        val result = Expression.evaluate("12/0")
        assertEquals(
            Expression.Result.Invalid(Expression.Result.Reason.DIVISION_BY_ZERO),
            result,
        )
    }

    @Test
    fun `rifiuta le parentesi non chiuse`() {
        assertTrue(Expression.evaluate("(12+3") !is Expression.Result.Value)
    }

    /**
     * La divisione non deve arrotondare a metà strada: un terzo di 100 va
     * mantenuto con tutte le cifre utili, non troncato a due decimali.
     */
    @Test
    fun `la divisione conserva le cifre significative`() {
        val result = value("100/3")
        assertTrue("Attese molte cifre, ottenuto $result", result.precision() >= 15)
    }

    /** Le valute ad alta denominazione non devono perdere unità. */
    @Test
    fun `regge importi molto grandi senza perdere cifre`() {
        assertValue("999999999", "999999999")
        assertValue("1999999998", "999999999*2")
    }

    @Test
    fun `riconosce un numero semplice da un calcolo`() {
        assertTrue(Expression.isPlainNumber("45000"))
        assertTrue(Expression.isPlainNumber("45,5"))
        assertTrue(!Expression.isPlainNumber("45+5"))
    }
}
