package io.github.bbadis.opuntiapad.ui.theme

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
import io.github.bbadis.opuntiapad.core.settings.ThemeMode

// Brand palette inspired by the prickly pear (Opuntia): cactus green, fruit pink, desert sand.
private val CactusGreen = Color(0xFF2D6A4F)
private val CactusGreenLight = Color(0xFF7FD39B)
private val PricklyPink = Color(0xFFC2185B)
private val PricklyPinkLight = Color(0xFFFF8FB1)

private val LightColors = lightColorScheme(
    primary = CactusGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F0CB),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF8A5A2B),
    secondaryContainer = Color(0xFFFFE8C2),
    onSecondaryContainer = Color(0xFF2E1500),
    tertiary = PricklyPink,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E2),
    onTertiaryContainer = Color(0xFF3E001D),
)

private val DarkColors = darkColorScheme(
    primary = CactusGreenLight,
    onPrimary = Color(0xFF00391E),
    primaryContainer = Color(0xFF14522F),
    onPrimaryContainer = Color(0xFFB7F0CB),
    secondary = Color(0xFFF4C48E),
    secondaryContainer = Color(0xFF5B3A14),
    onSecondaryContainer = Color(0xFFFFE8C2),
    tertiary = PricklyPinkLight,
    onTertiary = Color(0xFF5E1133),
    tertiaryContainer = Color(0xFF8E0E3E),
    onTertiaryContainer = Color(0xFFFFD9E2),
)

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun OpuntiaPadTheme(dark: Boolean, dynamicColor: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
