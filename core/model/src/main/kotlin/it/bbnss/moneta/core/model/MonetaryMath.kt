package it.bbnss.moneta.core.model

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Regole aritmetiche condivise da tutta l'app.
 *
 * Il denaro non viene mai rappresentato con `Float` o `Double`: sono
 * approssimazioni binarie e su valute ad alta denominazione (VND, IDR, IRR) gli
 * errori diventano visibili all'utente. È un difetto reale e tuttora aperto nel
 * principale progetto concorrente, che salva i tassi come `Float`.
 */
object MonetaryMath {

    /**
     * Precisione dei passaggi intermedi. 16 cifre significative sono
     * abbondanti per qualsiasi coppia reale e mantengono l'errore di
     * arrotondamento molto sotto l'ultima cifra mostrata.
     */
    val CONTEXT: MathContext = MathContext(16, RoundingMode.HALF_UP)

    /**
     * Arrotonda per la visualizzazione. È l'**unico** punto in cui si arrotonda:
     * arrotondare a metà calcolo propaga l'errore.
     */
    fun forDisplay(value: BigDecimal, minorUnits: Int): BigDecimal =
        value.setScale(minorUnits, RoundingMode.HALF_UP)

    /**
     * Numero di decimali con cui mostrare un importo molto piccolo.
     *
     * Convertendo 1 VND in EUR il risultato è 0,00003…: con i 2 decimali
     * canonici dell'euro l'utente leggerebbe "0,00". In questi casi si mostrano
     * cifre in più finché il numero non dice qualcosa.
     */
    fun adaptiveScale(value: BigDecimal, minorUnits: Int, maxExtraDigits: Int = 6): Int {
        if (value.signum() == 0) return minorUnits
        val rounded = value.abs().setScale(minorUnits, RoundingMode.HALF_UP)
        if (rounded.signum() != 0) return minorUnits
        // Quante cifre servono per far comparire la prima cifra significativa.
        val leadingZeros = value.abs().scale() - value.abs().precision()
        return minOf(leadingZeros + 2, minorUnits + maxExtraDigits)
    }
}
