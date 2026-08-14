package it.bbnss.moneta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.bbnss.moneta.core.model.ThemeMode
import it.bbnss.moneta.core.ui.theme.MonetaTheme
import it.bbnss.moneta.ui.MonetaApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as MonetaApplication).container

        setContent {
            // Il tema viene dalle preferenze, non dal solo sistema: chi vuole il
            // nero pieno su AMOLED lo sceglie a prescindere da come è impostato
            // il telefono.
            val themeMode by container.settings.themeMode
                .collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val dynamicColor by container.settings.dynamicColor
                .collectAsStateWithLifecycle(true)

            MonetaTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                MonetaApp(container = container)
            }
        }
    }
}
