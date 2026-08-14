package it.bbnss.moneta.core.data

import android.content.Context
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import java.util.Locale

/** Da dove viene il codice paese. Determina quanto ci si può fidare. */
enum class CountrySource {
    /** La rete mobile a cui il telefono è agganciato: dove sei adesso. */
    MOBILE_NETWORK,

    /** Il paese dell'operatore della SIM: da dove vieni. */
    SIM,

    /** Il fuso orario del dispositivo. */
    TIME_ZONE,

    /** Le impostazioni di lingua e area del sistema. */
    SYSTEM_LOCALE,
}

data class DetectedCountry(val code: String, val source: CountrySource)

/**
 * Capisce in che paese si trova l'utente, e da quale proviene.
 *
 * **Nessun permesso, nessun GPS, nessuna rete.** Tutti i segnali usati qui sono
 * già a disposizione di qualunque app: il paese della rete mobile, quello della
 * SIM, il fuso orario e la lingua di sistema. Funziona anche in aereo e non
 * lascia tracce.
 *
 * La distinzione fra i due metodi è il punto interessante: la **rete** dice
 * dove sei — è il paese della cella a cui sei agganciato — mentre la **SIM**
 * dice da dove vieni, perché resta quella di casa anche dall'altra parte del
 * mondo. Sono esattamente le due valute che servono a un convertitore da
 * viaggio, e nessun progetto concorrente le usa.
 */
class LocalCurrencyDetector(context: Context) {

    private val appContext = context.applicationContext

    private val telephony: TelephonyManager?
        get() = runCatching { appContext.getSystemService<TelephonyManager>() }.getOrNull()

    /** Il paese in cui ci si trova adesso. */
    fun detectLocal(): DetectedCountry? =
        networkCountry()
            ?: timeZoneCountry()
            ?: localeCountry()

    /**
     * Il paese di provenienza, per proporre la valuta "di casa".
     *
     * Qui la SIM viene prima del fuso orario: chi è in viaggio ha la SIM del
     * proprio paese ma il fuso di quello in cui si trova.
     */
    fun detectHome(): DetectedCountry? =
        simCountry()
            ?: localeCountry()

    private fun networkCountry(): DetectedCountry? =
        telephony?.networkCountryIso
            ?.normalize()
            ?.let { DetectedCountry(it, CountrySource.MOBILE_NETWORK) }

    private fun simCountry(): DetectedCountry? =
        telephony?.simCountryIso
            ?.normalize()
            ?.let { DetectedCountry(it, CountrySource.SIM) }

    private fun timeZoneCountry(): DetectedCountry? =
        runCatching {
            // ICU sa risalire dal fuso orario al paese; java.util.TimeZone no.
            android.icu.util.TimeZone.getRegion(java.util.TimeZone.getDefault().id)
        }.getOrNull()
            ?.normalize()
            ?.let { DetectedCountry(it, CountrySource.TIME_ZONE) }

    private fun localeCountry(): DetectedCountry? =
        Locale.getDefault().country
            .normalize()
            ?.let { DetectedCountry(it, CountrySource.SYSTEM_LOCALE) }

    /**
     * Le API di telefonia restituiscono stringhe vuote quando il dato non c'è, e
     * ICU risponde `ZZ` quando il fuso non è attribuibile a un paese.
     */
    private fun String?.normalize(): String? =
        this?.trim()?.uppercase(Locale.ROOT)
            ?.takeIf { it.length == 2 && it.all(Char::isLetter) && it != "ZZ" }
}
