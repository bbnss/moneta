package it.bbnss.moneta.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.bbnss.moneta.core.model.FeeMode
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

private val Context.preferencesStore: DataStore<Preferences> by preferencesDataStore("settings")

class SettingsStore internal constructor(private val store: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.preferencesStore)

    /**
     * Fonte scelta dall'utente. Si persiste [ProviderId.stableId], non
     * l'ordinale: aggiungere o togliere una fonte non deve cambiare quella
     * selezionata da chi ha già l'app installata.
     */
    val preferredProvider: Flow<ProviderId> = store.data.map { prefs ->
        prefs[KEY_PROVIDER]?.let { ProviderId.fromStableId(it) } ?: ProviderId.FRANKFURTER
    }

    suspend fun setPreferredProvider(id: ProviderId) {
        store.edit { it[KEY_PROVIDER] = id.stableId }
    }

    /** Se una fonte non risponde, provare le altre. Attivo per impostazione predefinita. */
    val allowFailover: Flow<Boolean> = store.data.map { it[KEY_FAILOVER] ?: true }

    suspend fun setAllowFailover(enabled: Boolean) {
        store.edit { it[KEY_FAILOVER] = enabled }
    }

    /** Istanza Frankfurter self-hostata, per chi ha gli endpoint pubblici bloccati. */
    val customEndpoint: Flow<String?> = store.data.map { it[KEY_CUSTOM_ENDPOINT] }

    suspend fun setCustomEndpoint(url: String?) {
        store.edit { prefs ->
            if (url.isNullOrBlank()) prefs.remove(KEY_CUSTOM_ENDPOINT)
            else prefs[KEY_CUSTOM_ENDPOINT] = url.trim()
        }
    }

    val syncIntervalHours: Flow<Int> = store.data.map { it[KEY_SYNC_HOURS] ?: 12 }

    suspend fun setSyncIntervalHours(hours: Int) {
        store.edit { it[KEY_SYNC_HOURS] = hours }
    }

    val wifiOnly: Flow<Boolean> = store.data.map { it[KEY_WIFI_ONLY] ?: false }

    suspend fun setWifiOnly(enabled: Boolean) {
        store.edit { it[KEY_WIFI_ONLY] = enabled }
    }

    /** Blocca ogni traffico: l'app resta pienamente utilizzabile sui dati in cache. */
    val offlineMode: Flow<Boolean> = store.data.map { it[KEY_OFFLINE] ?: false }

    suspend fun setOfflineMode(enabled: Boolean) {
        store.edit { it[KEY_OFFLINE] = enabled }
    }

    val baseCurrency: Flow<Currency> = store.data.map {
        Currency.parse(it[KEY_BASE]) ?: Currency.EUR
    }

    val quoteCurrency: Flow<Currency> = store.data.map {
        Currency.parse(it[KEY_QUOTE]) ?: Currency.USD
    }

    suspend fun setPair(base: Currency, quote: Currency) {
        store.edit {
            it[KEY_BASE] = base.code
            it[KEY_QUOTE] = quote.code
        }
    }

    /**
     * Valute preferite.
     *
     * Chi apre l'app per la prima volta non deve trovare una schermata vuota da
     * riempire: si parte dalle quattro valute più confrontate al mondo. La
     * distinzione fra "mai impostate" e "svuotate apposta" conta — chi le
     * toglie tutte non se le deve ritrovare al riavvio.
     */
    val favourites: Flow<List<Currency>> = store.data.map { prefs ->
        prefs[KEY_FAVOURITES_ORDER]?.let { order ->
            return@map order.split(',').mapNotNull(Currency::parse).distinct()
        }
        val stored = prefs[KEY_FAVOURITES]
            ?: return@map DEFAULT_FAVOURITES
        stored.mapNotNull { Currency.parse(it) }.sortedBy { it.code }
    }

    suspend fun setFavourites(currencies: Collection<Currency>) {
        store.edit {
            it[KEY_FAVOURITES] = currencies.map { currency -> currency.code }.toSet()
            it[KEY_FAVOURITES_ORDER] = currencies.distinct().joinToString(",") { currency -> currency.code }
        }
    }

    /**
     * Paese per cui l'utente ha già rifiutato il suggerimento sulla valuta
     * locale. Si ricorda per non riproporlo a ogni apertura: un suggerimento
     * che torna dopo che l'hai scartato smette di essere un aiuto.
     */
    val dismissedCountry: Flow<String?> = store.data.map { it[KEY_DISMISSED_COUNTRY] }

    suspend fun dismissCountrySuggestion(countryCode: String) {
        store.edit { it[KEY_DISMISSED_COUNTRY] = countryCode.uppercase() }
    }

    /**
     * Maggiorazione percentuale applicata al risultato, per avvicinarsi a quanto
     * la carta o il cambiavalute daranno davvero. I tassi di riferimento non
     * sono quelli che si ottengono allo sportello.
     */
    val markupPercent: Flow<BigDecimal> = store.data.map { prefs ->
        prefs[KEY_MARKUP]?.let { runCatching { BigDecimal(it) }.getOrNull() } ?: BigDecimal.ZERO
    }

    suspend fun setMarkupPercent(percent: BigDecimal) {
        store.edit { it[KEY_MARKUP] = percent.toPlainString() }
    }

    /** Tema scelto: sistema, chiaro, scuro oppure nero pieno per gli AMOLED. */
    val themeMode: Flow<ThemeMode> = store.data.map { prefs ->
        prefs[KEY_THEME]?.let { name -> runCatching { ThemeMode.valueOf(name) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[KEY_THEME] = mode.name }
    }

    /**
     * Colori presi dallo sfondo di sistema (Material You). Chi lo disattiva
     * torna alla tavolozza dell'app, che resta leggibile ovunque.
     */
    val dynamicColor: Flow<Boolean> = store.data.map { it[KEY_DYNAMIC_COLOR] ?: true }

    suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    val feeMode: Flow<FeeMode> = store.data.map {
        it[KEY_FEE_MODE]?.let { name -> runCatching { FeeMode.valueOf(name) }.getOrNull() }
            ?: FeeMode.CASH
    }
    suspend fun setFeeMode(mode: FeeMode) {
        store.edit { it[KEY_FEE_MODE] = mode.name }
    }

    data class Calculation(val input: String = "1", val field: String = "FROM", val initial: Boolean = true)
    val calculation: Flow<Calculation> = store.data.map {
        Calculation(it[KEY_INPUT] ?: "1", it[KEY_FIELD] ?: "FROM", it[KEY_INITIAL] ?: true)
    }
    suspend fun setCalculation(input: String, field: String, initial: Boolean) {
        store.edit { it[KEY_INPUT] = input; it[KEY_FIELD] = field; it[KEY_INITIAL] = initial }
    }
    val boardCalculation: Flow<Calculation> = store.data.map {
        Calculation(it[KEY_BOARD_INPUT] ?: "1", initial = it[KEY_BOARD_INITIAL] ?: true)
    }
    suspend fun setBoardCalculation(input: String, initial: Boolean) {
        store.edit { it[KEY_BOARD_INPUT] = input; it[KEY_BOARD_INITIAL] = initial }
    }

    val cashPair: Flow<Pair<Currency, Currency>> = store.data.map {
        val local = Currency.parse(it[KEY_CASH_LOCAL]) ?: Currency.parse(it[KEY_BASE]) ?: Currency.EUR
        val home = Currency.parse(it[KEY_CASH_HOME]) ?: Currency.parse(it[KEY_QUOTE]) ?: Currency.USD
        local to home
    }
    suspend fun ensureCashPair() {
        store.edit {
            if (it[KEY_CASH_LOCAL] == null) it[KEY_CASH_LOCAL] = it[KEY_BASE] ?: "EUR"
            if (it[KEY_CASH_HOME] == null) it[KEY_CASH_HOME] = it[KEY_QUOTE] ?: "USD"
        }
    }
    suspend fun setCashPair(local: Currency, home: Currency) {
        store.edit { it[KEY_CASH_LOCAL] = local.code; it[KEY_CASH_HOME] = home.code }
    }
    fun cashCounts(currency: Currency): Flow<Map<BigDecimal, Int>> = store.data.map { prefs ->
        prefs[stringPreferencesKey("cash_counts_" + currency.code)].orEmpty().split(';').mapNotNull { entry ->
            val parts = entry.split(':')
            val denomination = parts.getOrNull(0)?.toBigDecimalOrNull() ?: return@mapNotNull null
            val count = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
            denomination to count
        }.toMap()
    }
    suspend fun adjustCashCount(currency: Currency, denomination: BigDecimal, delta: Int) {
        store.edit { prefs ->
            val key = stringPreferencesKey("cash_counts_" + currency.code)
            val counts = prefs[key].orEmpty().split(';').mapNotNull { entry ->
                val parts = entry.split(':')
                val note = parts.getOrNull(0)?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return@mapNotNull null
                val count = parts.getOrNull(1)?.toLongOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
                note.stripTrailingZeros().toPlainString() to count.coerceAtMost(Int.MAX_VALUE.toLong())
            }.groupBy({ it.first }, { it.second }).mapValues { (_, values) ->
                values.fold(0L) { total, value -> (total + value).coerceAtMost(Int.MAX_VALUE.toLong()) }
            }.toMutableMap()
            val noteKey = denomination.stripTrailingZeros().toPlainString()
            val previous = counts[noteKey] ?: 0
            counts[noteKey] = (previous + delta).coerceIn(0, Int.MAX_VALUE.toLong())
            prefs[key] = counts.entries.joinToString(";") { it.key + ":" + it.value }
        }
    }
    suspend fun resetCashCounts(currency: Currency) {
        store.edit { it.remove(stringPreferencesKey("cash_counts_" + currency.code)) }
    }

    suspend fun snapshotOfSettings(): Settings {
        val prefs = store.data.first()
        return Settings(
            preferredProvider = prefs[KEY_PROVIDER]?.let(ProviderId::fromStableId) ?: ProviderId.FRANKFURTER,
            allowFailover = prefs[KEY_FAILOVER] ?: true,
            customEndpoint = prefs[KEY_CUSTOM_ENDPOINT],
            offlineMode = prefs[KEY_OFFLINE] ?: false,
            wifiOnly = prefs[KEY_WIFI_ONLY] ?: false,
            intervalHours = prefs[KEY_SYNC_HOURS] ?: 12,
        )
    }

    data class Settings(
        val preferredProvider: ProviderId,
        val allowFailover: Boolean,
        val customEndpoint: String?,
        val offlineMode: Boolean,
        val wifiOnly: Boolean,
        val intervalHours: Int,
    )

    private companion object {
        val DEFAULT_FAVOURITES = listOf(
            Currency.EUR,
            Currency.USD,
            Currency("CNY"),
            Currency.GBP,
        ).sortedBy { it.code }

        val KEY_FAVOURITES_ORDER = stringPreferencesKey("favourite_order")
        val KEY_FEE_MODE = stringPreferencesKey("fee_mode")
        val KEY_INPUT = stringPreferencesKey("convert_input")
        val KEY_FIELD = stringPreferencesKey("convert_field")
        val KEY_INITIAL = booleanPreferencesKey("convert_initial")
        val KEY_BOARD_INPUT = stringPreferencesKey("board_input")
        val KEY_BOARD_INITIAL = booleanPreferencesKey("board_initial")
        val KEY_CASH_LOCAL = stringPreferencesKey("cash_local")
        val KEY_CASH_HOME = stringPreferencesKey("cash_home")
        val KEY_PROVIDER = intPreferencesKey("preferred_provider")
        val KEY_FAILOVER = booleanPreferencesKey("allow_failover")
        val KEY_CUSTOM_ENDPOINT = stringPreferencesKey("custom_endpoint")
        val KEY_SYNC_HOURS = intPreferencesKey("sync_interval_hours")
        val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val KEY_OFFLINE = booleanPreferencesKey("offline_mode")
        val KEY_BASE = stringPreferencesKey("base_currency")
        val KEY_QUOTE = stringPreferencesKey("quote_currency")
        val KEY_FAVOURITES = stringSetPreferencesKey("favourite_currencies")
        val KEY_DISMISSED_COUNTRY = stringPreferencesKey("dismissed_country")
        val KEY_MARKUP = stringPreferencesKey("markup_percent")
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}
