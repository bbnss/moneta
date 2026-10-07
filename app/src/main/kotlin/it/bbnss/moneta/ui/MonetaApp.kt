package it.bbnss.moneta.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import it.bbnss.moneta.R
import it.bbnss.moneta.core.data.DataContainer
import it.bbnss.moneta.core.data.RefreshWorker
import it.bbnss.moneta.ui.board.BoardScreen
import it.bbnss.moneta.ui.board.BoardViewModel
import it.bbnss.moneta.ui.convert.ConvertScreen
import it.bbnss.moneta.ui.convert.ConvertViewModel
import it.bbnss.moneta.ui.history.HistoryScreen
import it.bbnss.moneta.ui.history.HistoryViewModel
import it.bbnss.moneta.ui.ratecard.RateCardScreen
import it.bbnss.moneta.ui.settings.SettingsScreen
import it.bbnss.moneta.ui.settings.SettingsViewModel
import it.bbnss.moneta.ui.ratecard.RateCardViewModel

/**
 * Lo storico non è una sezione della barra inferiore: si apre toccando il tasso
 * e riguarda la coppia che si sta guardando in quel momento.
 */
private const val HISTORY_ROUTE = "history"

/** Anche le impostazioni sono un dettaglio, non una sezione della barra. */
private const val SETTINGS_ROUTE = "settings"

private enum class Destination(
    val route: String,
    @StringRes val label: Int,
    @StringRes val title: Int,
    val icon: ImageVector,
) {
    CONVERT("convert", R.string.nav_convert, R.string.convert_title, Icons.Default.SwapHoriz),
    BOARD("board", R.string.nav_board, R.string.board_title, Icons.AutoMirrored.Filled.List),
    RATE_CARD("ratecard", R.string.nav_ratecard, R.string.nav_ratecard, Icons.Default.Payments),
}

/**
 * Guscio dell'app: barra superiore, navigazione e messaggi.
 *
 * Le tre destinazioni leggono tutte dallo stesso database, quindi un
 * aggiornamento avviato da qui si riflette ovunque senza doverlo propagare a
 * mano: il pulsante di ricarica resta uno solo, nella barra, valido per tutte
 * le schermate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonetaApp(container: DataContainer) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val convertViewModel: ConvertViewModel = viewModel(
        factory = ConvertViewModel.Factory(
            repository = container.rateRepository,
            settings = container.settings,
            detector = container.localCurrencyDetector,
        ),
    )
    val boardViewModel: BoardViewModel = viewModel(
        factory = BoardViewModel.Factory(container.rateRepository, container.settings),
    )
    val rateCardViewModel: RateCardViewModel = viewModel(
        factory = RateCardViewModel.Factory(container.rateRepository, container.settings),
    )

    val convertState by convertViewModel.state.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val onHistory = currentRoute?.hierarchy?.any { it.route == HISTORY_ROUTE } == true
    val onSettings = currentRoute?.hierarchy?.any { it.route == SETTINGS_ROUTE } == true
    val onDetail = onHistory || onSettings
    val current = Destination.entries.firstOrNull { destination ->
        currentRoute?.hierarchy?.any { it.route == destination.route } == true
    } ?: Destination.CONVERT

    val refreshText = when (val message = convertState.message) {
        is it.bbnss.moneta.ui.convert.RefreshMessage.Updated -> stringResource(R.string.refresh_updated, message.provider.displayName)
        it.bbnss.moneta.ui.convert.RefreshMessage.Failed -> stringResource(R.string.refresh_failed)
        it.bbnss.moneta.ui.convert.RefreshMessage.Offline -> stringResource(R.string.refresh_offline)
        it.bbnss.moneta.ui.convert.RefreshMessage.Wifi -> stringResource(R.string.refresh_wifi)
        it.bbnss.moneta.ui.convert.RefreshMessage.InvalidPaste -> stringResource(R.string.amount_invalid)
        null -> null
    }
    LaunchedEffect(convertState.message, current) {
        if (current != Destination.CONVERT && refreshText != null) {
            snackbarHostState.showSnackbar(refreshText)
            convertViewModel.onMessageShown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            onHistory -> stringResource(
                                R.string.history_title,
                                convertState.from.code,
                                convertState.to.code,
                            )

                            onSettings -> stringResource(R.string.settings_title)
                            else -> stringResource(current.title)
                        },
                    )
                },
                navigationIcon = {
                    // Storico e impostazioni sono schermate di dettaglio, non
                    // sezioni: ci si arriva da un punto preciso e si torna lì.
                    if (onDetail) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.history_back),
                            )
                        }
                    }
                },
                actions = {
                    if (!onSettings) {
                        IconButton(onClick = { navController.navigate(SETTINGS_ROUTE) }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_open),
                            )
                        }
                    }

                    // Aggiornare i tassi non ha senso mentre si configurano le
                    // preferenze: il pulsante resta dove serve.
                    if (!onSettings) {
                    IconButton(
                        onClick = {
                            val required = when (current) {
                                Destination.BOARD -> boardViewModel.requestedCurrencies()
                                Destination.RATE_CARD -> rateCardViewModel.requestedCurrencies()
                                else -> setOf(convertState.from, convertState.to)
                            }
                            convertViewModel.onRefresh(required)
                        },
                        enabled = !convertState.refreshing,
                    ) {
                        if (convertState.refreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh),
                            )
                        }
                    }
                    }
                },
            )
        },
        bottomBar = {
            // Nelle schermate di dettaglio la barra sparisce: tenerla visibile
            // lascerebbe evidenziata una sezione in cui non ci si trova più.
            if (onDetail) return@Scaffold

            Surface {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp)) {
                    Destination.entries.forEach { destination ->
                        Column(Modifier.weight(1f).fillMaxHeight().selectable(
                            selected = destination == current, role = Role.Tab,
                            onClick = {
                                if (destination != current) navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center) {
                            Box(Modifier.size(48.dp, 26.dp).background(
                                if (destination == current) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center) {
                                Icon(destination.icon, null, Modifier.size(20.dp))
                            }
                            Text(stringResource(destination.label), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.CONVERT.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.CONVERT.route) {
                ConvertScreen(
                    state = convertState,
                    snackbarHostState = snackbarHostState,
                    onKey = convertViewModel::onKey,
                    onFieldSelected = convertViewModel::onFieldSelected,
                    onCurrencySelected = convertViewModel::onCurrencySelected,
                    onToggleFavourite = convertViewModel::onToggleFavourite,
                    onSwap = convertViewModel::onSwap,
                    onMessageShown = convertViewModel::onMessageShown,
                    onAcceptSuggestion = convertViewModel::onAcceptSuggestion,
                    onDismissSuggestion = convertViewModel::onDismissSuggestion,
                    onOpenHistory = { navController.navigate(HISTORY_ROUTE) },
                    onMarkupChanged = convertViewModel::onMarkupChanged,
                    onFeeModeChanged = convertViewModel::onFeeModeChanged,
                    onPaste = convertViewModel::onPaste,
                )
            }

            composable(SETTINGS_ROUTE) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        settings = container.settings,
                        repository = container.rateRepository,
                        providers = container.providerCatalog,
                        onSyncSettingsChanged = { hours, wifiOnly ->
                            RefreshWorker.schedule(context, hours, wifiOnly)
                        },
                    ),
                )
                val state by settingsViewModel.state.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = state,
                    onProviderSelected = settingsViewModel::onProviderSelected,
                    onFailoverChanged = settingsViewModel::onFailoverChanged,
                    onCustomEndpointChanged = settingsViewModel::onCustomEndpointChanged,
                    onVerifyEndpoint = settingsViewModel::onVerifyEndpoint,
                    onIntervalChanged = settingsViewModel::onIntervalChanged,
                    onWifiOnlyChanged = settingsViewModel::onWifiOnlyChanged,
                    onOfflineModeChanged = settingsViewModel::onOfflineModeChanged,
                    onThemeChanged = settingsViewModel::onThemeChanged,
                    onDynamicColorChanged = settingsViewModel::onDynamicColorChanged,
                )
            }

            composable(HISTORY_ROUTE) {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.Factory(
                        container.rateRepository,
                        container.settings,
                    ),
                )
                val state by historyViewModel.state.collectAsStateWithLifecycle()
                HistoryScreen(
                    state = state,
                    onRangeSelected = historyViewModel::onRangeSelected,
                )
            }

            composable(Destination.BOARD.route) {
                val state by boardViewModel.state.collectAsStateWithLifecycle()
                BoardScreen(
                    state = state,
                    onKey = boardViewModel::onKey,
                    onSetAsBase = boardViewModel::onSetAsBase,
                    onToggleFavourite = boardViewModel::onToggleFavourite,
                    onMove = boardViewModel::onMove,
                    onPaste = boardViewModel::onPaste,
                )
            }

            composable(Destination.RATE_CARD.route) {
                val state by rateCardViewModel.state.collectAsStateWithLifecycle()
                RateCardScreen(state = state, onCurrencySelected = rateCardViewModel::onCurrencySelected,
                    onSwap = rateCardViewModel::onSwap, onCount = rateCardViewModel::onCount,
                    onReset = rateCardViewModel::onReset, onToggleFavourite = rateCardViewModel::onToggleFavourite)
            }
        }
    }
}
