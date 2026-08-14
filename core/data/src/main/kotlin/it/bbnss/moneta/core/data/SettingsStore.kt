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
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

private val Context.preferencesStore: DataStore<Preferences> by preferencesDataStore("settings")

class SettingsStore(context: Context) {

    private val store = context.applicationContext.preferencesStore

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
        val stored = prefs[KEY_FAVOURITES]
            ?: return@map DEFAULT_FAVOURITES
        stored.mapNotNull { Currency.parse(it) }.sortedBy { it.code }
    }

    suspend fun setFavourites(currencies: Collection<Currency>) {
        store.edit {
            it[KEY_FAVOURITES] = currencies.map { currency -> currency.code }.toSet()
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

    suspend fun snapshotOfSettings(): Settings = Settings(
        preferredProvider = preferredProvider.first(),
        allowFailover = allowFailover.first(),
        customEndpoint = customEndpoint.first(),
        offlineMode = offlineMode.first(),
    )

    data class Settings(
        val preferredProvider: ProviderId,
        val allowFailover: Boolean,
        val customEndpoint: String?,
        val offlineMode: Boolean,
    )

    private companion object {
        val DEFAULT_FAVOURITES = listOf(
            Currency.EUR,
            Currency.USD,
            Currency("CNY"),
            Currency.GBP,
        ).sortedBy { it.code }

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
