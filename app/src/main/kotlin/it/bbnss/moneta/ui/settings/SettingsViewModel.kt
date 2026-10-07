package it.bbnss.moneta.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.bbnss.moneta.core.data.SettingsStore
import it.bbnss.moneta.core.data.RateRepository
import it.bbnss.moneta.core.model.CustomEndpoint
import it.bbnss.moneta.core.providers.ProviderResult
import kotlinx.coroutines.flow.MutableStateFlow
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.ThemeMode
import it.bbnss.moneta.core.providers.ProviderCapabilities
import it.bbnss.moneta.core.providers.RateProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Una fonte come la vede la schermata impostazioni. */
data class ProviderOption(
    val id: ProviderId,
    val displayName: String,
    val infoUrl: String,
    val capabilities: ProviderCapabilities,
)

data class SettingsUiState(
    val providers: List<ProviderOption> = emptyList(),
    val selectedProvider: ProviderId = ProviderId.FRANKFURTER,
    val allowFailover: Boolean = true,
    val customEndpoint: String = "",
    val syncIntervalHours: Int = 12,
    val wifiOnly: Boolean = false,
    val offlineMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val endpointResult: EndpointResult? = null,
    val verifying: Boolean = false,
)

data class EndpointResult(val coverage: Int? = null, val message: String? = null)

class SettingsViewModel(
    private val settings: SettingsStore,
    private val repository: RateRepository,
    providers: List<RateProvider>,
    private val onSyncSettingsChanged: (intervalHours: Int, wifiOnly: Boolean) -> Unit,
) : ViewModel() {

    private val endpointResult = MutableStateFlow<EndpointResult?>(null)
    private val verifying = MutableStateFlow(false)
    private val options = providers.map {
        ProviderOption(it.id, it.displayName, it.infoUrl, it.capabilities)
    }

    private val source = combine(
        settings.preferredProvider,
        settings.allowFailover,
        settings.customEndpoint,
    ) { provider, failover, endpoint -> Triple(provider, failover, endpoint.orEmpty()) }

    private val updates = combine(
        settings.syncIntervalHours,
        settings.wifiOnly,
        settings.offlineMode,
    ) { interval, wifi, offline -> Triple(interval, wifi, offline) }

    private val appearance = combine(
        settings.themeMode,
        settings.dynamicColor,
    ) { theme, dynamic -> theme to dynamic }

    val state: StateFlow<SettingsUiState> = combine(
        source,
        updates,
        appearance,
        combine(endpointResult, verifying) { result, busy -> result to busy },
    ) { (provider, failover, endpoint), (interval, wifi, offline), (theme, dynamic), (verification, busy) ->
        SettingsUiState(
            providers = options + if (endpoint.isNotBlank()) listOf(ProviderOption(ProviderId.CUSTOM, ProviderId.CUSTOM.displayName,
                endpoint, it.bbnss.moneta.core.providers.ProviderCapabilities(0, it.bbnss.moneta.core.providers.UpdateCadence.DAILY_BUSINESS, historical = true, timeSeries = true))) else emptyList(),
            endpointResult = verification,
            verifying = busy,
            selectedProvider = provider,
            allowFailover = failover,
            customEndpoint = endpoint,
            syncIntervalHours = interval,
            wifiOnly = wifi,
            offlineMode = offline,
            themeMode = theme,
            dynamicColor = dynamic,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(providers = options),
    )

    fun onProviderSelected(id: ProviderId) {
        viewModelScope.launch { settings.setPreferredProvider(id) }
    }

    fun onFailoverChanged(enabled: Boolean) {
        viewModelScope.launch { settings.setAllowFailover(enabled) }
    }

    fun onCustomEndpointChanged(url: String) {
        viewModelScope.launch {
            if (url.isNotBlank() && CustomEndpoint.normalize(url) == null) return@launch
            repository.saveEndpoint(url)
            endpointResult.value = null
        }
    }

    fun onVerifyEndpoint(url: String) {
        if (verifying.value) return
        viewModelScope.launch {
            verifying.value = true
            endpointResult.value = when (val result = repository.verifyEndpoint(url)) {
                is ProviderResult.Success -> EndpointResult(coverage = result.value.currencies.size)
                is ProviderResult.Failure -> EndpointResult(message = result.message ?: result.reason.name)
            }
            verifying.value = false
        }
    }

    /**
     * Cambiare intervallo o vincolo di rete deve riprogrammare il lavoro in
     * background: senza, l'impostazione resterebbe scritta ma senza effetto
     * fino al riavvio dell'app.
     */
    fun onIntervalChanged(hours: Int) {
        viewModelScope.launch {
            settings.setSyncIntervalHours(hours)
            onSyncSettingsChanged(hours, state.value.wifiOnly)
        }
    }

    fun onWifiOnlyChanged(enabled: Boolean) {
        viewModelScope.launch {
            settings.setWifiOnly(enabled)
            onSyncSettingsChanged(state.value.syncIntervalHours, enabled)
        }
    }

    fun onOfflineModeChanged(enabled: Boolean) {
        viewModelScope.launch { settings.setOfflineMode(enabled) }
    }

    fun onThemeChanged(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun onDynamicColorChanged(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColor(enabled) }
    }

    class Factory(
        private val settings: SettingsStore,
        private val repository: RateRepository,
        private val providers: List<RateProvider>,
        private val onSyncSettingsChanged: (Int, Boolean) -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settings, repository, providers, onSyncSettingsChanged) as T
    }
}
