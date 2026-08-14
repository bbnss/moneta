package it.bbnss.moneta.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import it.bbnss.moneta.core.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF00629D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFE5FF),
    onPrimaryContainer = Color(0xFF001D33),
    secondary = Color(0xFF51606F),
    tertiary = Color(0xFF00629D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BCBFF),
    onPrimary = Color(0xFF003354),
    primaryContainer = Color(0xFF004A77),
    onPrimaryContainer = Color(0xFFCFE5FF),
    secondary = Color(0xFFB9C8DA),
    tertiary = Color(0xFF9BCBFF),
)

@Composable
fun MonetaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.OLED -> true
    }

    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    var scheme = when {
        dynamicColor && supportsDynamic && dark -> dynamicDarkColorScheme(context)
        dynamicColor && supportsDynamic -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }

    if (themeMode == ThemeMode.OLED) {
        scheme = scheme.copy(background = Color.Black, surface = Color.Black)
    }

    MaterialTheme(colorScheme = scheme, content = content)
}
