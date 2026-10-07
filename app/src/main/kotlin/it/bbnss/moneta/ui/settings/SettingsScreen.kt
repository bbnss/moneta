package it.bbnss.moneta.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import it.bbnss.moneta.BuildConfig
import it.bbnss.moneta.R
import it.bbnss.moneta.core.data.RefreshWorker
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.ThemeMode
import it.bbnss.moneta.core.providers.UpdateCadence

private const val SOURCE_URL = "https://github.com/bbnss/moneta"

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onProviderSelected: (ProviderId) -> Unit,
    onFailoverChanged: (Boolean) -> Unit,
    onCustomEndpointChanged: (String) -> Unit,
    onVerifyEndpoint: (String) -> Unit,
    onIntervalChanged: (Int) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onOfflineModeChanged: (Boolean) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SectionTitle(stringResource(R.string.settings_section_source))

        state.providers.forEach { provider ->
            ProviderRow(
                option = provider,
                selected = provider.id == state.selectedProvider,
                onSelect = { onProviderSelected(provider.id) },
            )
        }

        SwitchRow(
            title = stringResource(R.string.settings_failover),
            subtitle = stringResource(R.string.settings_failover_explain),
            checked = state.allowFailover,
            onCheckedChange = onFailoverChanged,
        )

        CustomEndpointField(
            value = state.customEndpoint,
            onValueChange = onCustomEndpointChanged,
            onVerify = onVerifyEndpoint,
            result = state.endpointResult,
            verifying = state.verifying,
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionTitle(stringResource(R.string.settings_section_updates))

        IntervalPicker(selected = state.syncIntervalHours, onSelect = onIntervalChanged)

        SwitchRow(
            title = stringResource(R.string.settings_wifi_only),
            subtitle = stringResource(R.string.settings_wifi_only_explain),
            checked = state.wifiOnly,
            onCheckedChange = onWifiOnlyChanged,
        )

        SwitchRow(
            title = stringResource(R.string.settings_offline),
            subtitle = stringResource(R.string.settings_offline_explain),
            checked = state.offlineMode,
            onCheckedChange = onOfflineModeChanged,
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionTitle(stringResource(R.string.settings_section_appearance))

        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )

        ThemeMode.entries.forEach { mode ->
            ChoiceRow(
                label = themeLabel(mode),
                selected = mode == state.themeMode,
                onSelect = { onThemeChanged(mode) },
            )
        }

        SwitchRow(
            title = stringResource(R.string.settings_dynamic_color),
            subtitle = null,
            checked = state.dynamicColor,
            onCheckedChange = onDynamicColorChanged,
        )

        // La lingua si cambia nelle impostazioni di sistema (per-app language):
        // duplicare qui l'elenco significherebbe tenerlo allineato a mano e
        // discostarsi da dove l'utente si aspetta di trovarlo.
        LinkRow(
            title = stringResource(R.string.settings_language),
            subtitle = stringResource(R.string.settings_language_explain),
            onClick = {
                val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Settings.ACTION_APP_LOCALE_SETTINGS
                } else {
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                }
                runCatching {
                    context.startActivity(
                        Intent(action).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        },
                    )
                }
            },
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionTitle(stringResource(R.string.settings_section_about))

        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Text(
            text = stringResource(R.string.settings_licence),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinkRow(
            title = stringResource(R.string.settings_source_code),
            subtitle = SOURCE_URL,
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, SOURCE_URL.toUri()))
                }
            },
        )
        Text(
            text = stringResource(R.string.settings_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ProviderRow(
    option: ProviderOption,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val cadence = stringResource(
        when (option.capabilities.cadence) {
            UpdateCadence.DAILY_BUSINESS -> R.string.settings_cadence_daily
            UpdateCadence.INTRADAY -> R.string.settings_cadence_intraday
            UpdateCadence.MONTHLY -> R.string.settings_cadence_monthly
        },
    )
    val currencies = pluralStringResource(
        R.plurals.settings_provider_currencies,
        option.capabilities.currencyCount,
        option.capabilities.currencyCount,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 8.dp)) {
            Text(option.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (option.id == ProviderId.CUSTOM) option.infoUrl else "$currencies · $cadence",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CustomEndpointField(value: String, onValueChange: (String) -> Unit, onVerify: (String) -> Unit,
                                result: EndpointResult?, verifying: Boolean) {
    var text by remember(value) { mutableStateOf(value) }
    val valid = text.isBlank() || it.bbnss.moneta.core.model.CustomEndpoint.normalize(text) != null
    Column(Modifier.padding(vertical = 8.dp)) {
        OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true,
            isError = !valid, label = { Text(stringResource(R.string.settings_custom_endpoint)) },
            placeholder = { Text("https://example.org/frankfurter") }, modifier = Modifier.fillMaxWidth())
        Text(stringResource(if (valid) R.string.settings_custom_endpoint_explain else R.string.settings_custom_endpoint_invalid),
            style = MaterialTheme.typography.bodySmall)
        Row {
            TextButton(onClick = { onValueChange(text) }, enabled = valid && !verifying) { Text(stringResource(R.string.markup_save)) }
            TextButton(onClick = { onVerify(text) }, enabled = valid && text.isNotBlank() && !verifying) {
                Text(stringResource(if (verifying) R.string.endpoint_verifying else R.string.endpoint_verify))
            }
        }
        result?.let {
            Text(if (it.coverage != null) stringResource(R.string.endpoint_coverage, it.coverage) else
                stringResource(R.string.endpoint_error, when (it.message) {
                    "OFFLINE" -> stringResource(R.string.refresh_offline)
                    "WIFI" -> stringResource(R.string.refresh_wifi)
                    "DISCONNECTED" -> stringResource(R.string.refresh_failed)
                    else -> it.message.orEmpty()
                }), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun IntervalPicker(selected: Int, onSelect: (Int) -> Unit) {
    val options = listOf(6, 12, 24, 48, 168, RefreshWorker.MANUAL)

    // Senza questa riga le voci si leggono come un elenco senza domanda:
    // "settimana" da solo non dice nulla.
    Text(
        text = stringResource(R.string.settings_interval),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )

    options.forEach { hours ->
        ChoiceRow(
            label = when (hours) {
                RefreshWorker.MANUAL -> stringResource(R.string.settings_interval_manual)
                168 -> stringResource(R.string.settings_interval_weekly)
                else -> pluralStringResource(R.plurals.settings_interval_hours, hours, hours)
            },
            selected = hours == selected,
            onSelect = { onSelect(hours) },
        )
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
        ThemeMode.OLED -> R.string.settings_theme_oled
    },
)
