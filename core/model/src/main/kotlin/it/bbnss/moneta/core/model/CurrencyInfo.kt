package it.bbnss.moneta.core.model

import java.util.Locale
import java.util.Currency as JavaCurrency

enum class CurrencyKind { FIAT, METAL, CRYPTO }

/**
 * Metadati di presentazione di una valuta.
 *
 * I nomi tradotti arrivano da ICU tramite [JavaCurrency.getDisplayName]: circa
 * 180 codici ISO sono già localizzati in ogni lingua supportata da Android, a
 * costo zero e senza stringhe da mantenere. I concorrenti li tengono hardcoded
 * in inglese in `strings.xml`, il che significa centinaia di righe da tradurre
 * a mano per ogni lingua.
 */
data class CurrencyInfo(
    val currency: Currency,
    val displayName: String,
    val symbol: String?,
    val minorUnits: Int,
    val kind: CurrencyKind,
)

object CurrencyMetadata {

    /** Codici ISO 4217 riservati ai metalli preziosi, quotati per oncia troy. */
    private val METALS = setOf("XAU", "XAG", "XPT", "XPD")

    /**
     * Valute senza cifre decimali o con un numero di decimali che ICU non
     * conosce. ICU copre già i casi ISO (JPY 0, KWD 3), quindi qui restano solo
     * le eccezioni non-ISO.
     */
    private val NON_ISO_MINOR_UNITS = mapOf(
        "BTC" to 8,
        "ETH" to 8,
        "XAU" to 4,
        "XAG" to 4,
        "XPT" to 4,
        "XPD" to 4,
    )

    /**
     * Paesi che usano una valuta, con i nomi nella lingua dell'utente.
     *
     * Serve a cercare "Vietnam" e trovare il dong, che è come ragiona chi
     * viaggia: si conosce il paese in cui si è atterrati, non il codice ISO
     * della sua moneta.
     *
     * La tabella si ricava da ICU invertendo le località disponibili, così non
     * c'è nessun elenco paese-valuta da mantenere a mano e i nomi arrivano già
     * tradotti in ogni lingua supportata da Android.
     */
    fun countriesOf(currency: Currency, locale: Locale = Locale.getDefault()): List<String> =
        countryIndex(locale)[currency].orEmpty()

    /**
     * Bandiera del paese della valuta, come emoji.
     *
     * Serve a riconoscere una valuta di colpo, senza leggere: in un elenco di
     * duecento codici a tre lettere il colore è l'unica cosa che si distingue
     * con la coda dell'occhio.
     *
     * È generata dal codice paese invece che da immagini incluse nell'app:
     * nessun asset da mantenere, nessuna licenza da verificare, nessun peso
     * aggiunto all'APK, e funziona anche per le valute che aggiungeremo domani.
     *
     * `null` per crypto, metalli e valute condivise da più paesi senza una
     * bandiera sensata (il franco CFA, il dollaro dei Caraibi Orientali): meglio
     * nessuna bandiera che quella sbagliata.
     */
    fun flagOf(currency: Currency): String? {
        // L'euro non è di un paese: gli spetta la bandiera dell'Unione.
        if (currency == Currency.EUR) return flagEmoji("EU")

        val codes = countryCodeIndex[currency].orEmpty()
        if (codes.isEmpty()) return null

        // I codici ISO 4217 sono quasi sempre "paese + iniziale della valuta":
        // USD→US, VND→VN, THB→TH. Quando il prefisso è davvero uno dei paesi
        // che la usano, è quello giusto.
        val prefix = currency.code.take(2)
        val region = when {
            prefix in codes -> prefix
            codes.size == 1 -> codes.first()
            else -> return null
        }
        return flagEmoji(region)
    }

    /**
     * Le bandiere emoji si compongono da due "regional indicator": lettere in
     * un blocco Unicode separato che i font rendono come bandiera.
     */
    private fun flagEmoji(region: String): String =
        region.uppercase(Locale.ROOT)
            .map { String(Character.toChars(REGIONAL_INDICATOR_A + (it.code - 'A'.code))) }
            .joinToString("")

    private const val REGIONAL_INDICATOR_A = 0x1F1E6

    /** Valuta → codici dei paesi che la usano. Non dipende dalla lingua. */
    private val countryCodeIndex: Map<Currency, List<String>> by lazy {
        buildMap<Currency, MutableList<String>> {
            for (available in Locale.getAvailableLocales()) {
                val region = available.country.takeIf { it.length == 2 } ?: continue
                val iso = runCatching { JavaCurrency.getInstance(available) }.getOrNull() ?: continue
                val currency = Currency.parse(iso.currencyCode) ?: continue
                val codes = getOrPut(currency) { mutableListOf() }
                if (region !in codes) codes.add(region)
            }
        }.mapValues { it.value.sorted() }
    }

    private val countryCache = HashMap<String, Map<Currency, List<String>>>()

    @Synchronized
    private fun countryIndex(locale: Locale): Map<Currency, List<String>> =
        countryCache.getOrPut(locale.toLanguageTag()) {
            buildMap<Currency, MutableList<String>> {
                for (available in Locale.getAvailableLocales()) {
                    if (available.country.isEmpty()) continue
                    val iso = runCatching { JavaCurrency.getInstance(available) }.getOrNull()
                        ?: continue
                    val currency = Currency.parse(iso.currencyCode) ?: continue
                    val name = available.getDisplayCountry(locale).takeIf { it.isNotBlank() }
                        ?: continue
                    val names = getOrPut(currency) { mutableListOf() }
                    if (name !in names) names.add(name)
                }
            }.mapValues { it.value.sorted() }
        }

    fun of(currency: Currency, locale: Locale = Locale.getDefault()): CurrencyInfo {
        val iso = runCatching { JavaCurrency.getInstance(currency.code) }.getOrNull()

        val kind = when {
            currency.code in METALS -> CurrencyKind.METAL
            iso != null -> CurrencyKind.FIAT
            else -> CurrencyKind.CRYPTO
        }

        // getDefaultFractionDigits() vale -1 per i codici "senza valuta" come XAU.
        val isoMinorUnits = iso?.defaultFractionDigits?.takeIf { it >= 0 }

        return CurrencyInfo(
            currency = currency,
            displayName = iso?.getDisplayName(locale) ?: currency.code,
            symbol = iso?.getSymbol(locale)?.takeIf { it != currency.code },
            minorUnits = NON_ISO_MINOR_UNITS[currency.code] ?: isoMinorUnits ?: 2,
            kind = kind,
        )
    }
}
