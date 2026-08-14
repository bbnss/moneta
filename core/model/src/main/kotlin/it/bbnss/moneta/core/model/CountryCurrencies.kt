package it.bbnss.moneta.core.model

import java.util.Locale
import java.util.Currency as JavaCurrency

/**
 * Da codice paese ISO 3166 a valuta ufficiale.
 *
 * La corrispondenza arriva da ICU e non da una tabella scritta a mano: quando
 * un paese cambia moneta — succede più spesso di quanto sembri — l'informazione
 * si aggiorna con il sistema invece che con una nostra release.
 */
object CountryCurrencies {

    /**
     * Valuta ufficiale di [countryCode], oppure `null` se il paese non ne ha
     * una (i territori antartici) o se il codice non è riconosciuto.
     */
    fun currencyOf(countryCode: String?): Currency? {
        val region = countryCode?.trim()?.uppercase(Locale.ROOT) ?: return null
        if (region.length != 2 || !region.all { it.isLetter() }) return null

        val locale = runCatching { Locale.Builder().setRegion(region).build() }.getOrNull()
            ?: return null

        val iso = runCatching { JavaCurrency.getInstance(locale) }.getOrNull() ?: return null
        return Currency.parse(iso.currencyCode)
    }

    /** Nome del paese nella lingua dell'utente, per il testo del suggerimento. */
    fun nameOf(countryCode: String?, locale: Locale = Locale.getDefault()): String? {
        val region = countryCode?.trim()?.uppercase(Locale.ROOT) ?: return null
        if (region.length != 2) return null
        return runCatching {
            Locale.Builder().setRegion(region).build().getDisplayCountry(locale)
        }.getOrNull()?.takeIf { it.isNotBlank() && it != region }
    }
}
