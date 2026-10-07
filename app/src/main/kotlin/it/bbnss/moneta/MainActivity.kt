package it.bbnss.moneta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import it.bbnss.moneta.core.model.RequestKind
import it.bbnss.moneta.core.model.ThemeMode
import it.bbnss.moneta.core.ui.theme.MonetaTheme
import it.bbnss.moneta.ui.MonetaApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as MonetaApplication).container

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.settings.ensureCashPair()
                container.rateRepository.ensureSeeded()
                container.rateRepository.updateTime()
                launch { container.rateRepository.refresh(kind = RequestKind.AUTOMATIC) }
                while (true) {
                    delay(60_000)
                    container.rateRepository.updateTime()
                }
            }
        }

        setContent {
            // Il tema viene dalle preferenze, non dal solo sistema: chi vuole il
            // nero pieno su AMOLED lo sceglie a prescindere da come è impostato
            // il telefono.
            val themeMode by container.settings.themeMode
                .collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val dynamicColor by container.settings.dynamicColor
                .collectAsStateWithLifecycle(true)

            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK, ThemeMode.OLED -> true
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            MonetaTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                MonetaApp(container = container)
            }
        }
    }
}
