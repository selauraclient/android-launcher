package com.selauraclient.launcher.ui.theme

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

private val LightColorScheme = lightColorScheme(
    background = Color(0xFFF2F1E3), // --selaura-light
    surface = Color(0xFFF2F1E3),
    onBackground = Color(0xFF222222), // --selaura-dark
    onSurface = Color(0xFF222222),
    primary = Color(0xFF222222), // --selaura-soft (rgba(0,0,0,0.05))
    secondary = Color(0x0D000000), // --selaura-faint
    tertiary = Color(0x1A000000), // --selaura-scrollbar-bg (rgba(0,0,0,0.1))
    outline = Color(0x4D000000) // --selaura-scrollbar-thumb (rgba(0,0,0,0.3))
)

private val DarkColorScheme = darkColorScheme(
    background = Color(0xFF222222), // --selaura-light (overridden in dark mode)
    surface = Color(0xFF222222),
    onBackground = Color(0xFFF2F1E3), // --selaura-dark (overridden)
    onSurface = Color(0xFFF2F1E3),
    primary = Color(0xFFF2F1E3), // --selaura-soft (rgba(255,255,255,0.05))
    secondary = Color(0x1AFFFFFF), // --selaura-faint
    tertiary = Color(0x1AFFFFFF), // --selaura-scrollbar-bg
    outline = Color(0xFFF2F1E3) // --selaura-scrollbar-thumb (rgba(255,255,255,0.4))
)


@Composable
fun SelauraLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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