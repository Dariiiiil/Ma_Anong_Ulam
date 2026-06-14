package com.example.maanongulam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Build

private val DarkColorScheme = darkColorScheme(
    primary = DarkGreenPrimary,
    secondary = DarkGreenSecondary,
    tertiary = DarkGreenTertiary,
    background = Color(0xFF1B1C18),
    surface = Color(0xFF1B1C18),
    onPrimary = Color(0xFF00390A),
    onSecondary = Color(0xFF00390A),
    onTertiary = Color(0xFF00390A)
)

private val LightColorScheme = lightColorScheme(
    primary = NatureGreenPrimary,
    secondary = NatureGreenSecondary,
    tertiary = NatureGreenTertiary,
    background = LightGreenBackground,
    surface = LightGreenSurface,
    primaryContainer = LightGreenHeader,
    secondaryContainer = UlamOfTheDayMatch,
    onPrimaryContainer = LightHeaderTitle,
    onSecondaryContainer = Color(0xFF1B1C18),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1B1C18),
    onSurface = Color(0xFF1B1C18)
)

@Composable
fun MaAnongUlamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamicColor to false to force our Nature Green theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
