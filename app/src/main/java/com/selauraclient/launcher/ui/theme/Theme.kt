package com.selauraclient.launcher.ui.theme

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.drawToBitmap
import com.selauraclient.launcher.global.Data
import com.selauraclient.launcher.utils.SettingsManager
import kotlin.math.hypot

private val LightColorScheme = lightColorScheme(
    background = Color(0xFFF2F1E3),
    surface = Color(0xFFF2F1E3),
    onBackground = Color(0xFF222222),
    onSurface = Color(0xFF222222),
    primary = Color(0xFF222222),
    onPrimary = Color(0xFFF2F1E3),
    secondary = Color(0x0D000000),
    tertiary = Color(0x1A000000),
    outline = Color(0XFFEAE9E2),
    surfaceContainer = Color(0xFFF8F7EF)
    )

private val DarkColorScheme = darkColorScheme(
    background = Color(0xFF222222),
    surface = Color(0xFF222222),
    onBackground = Color(0xFFF2F1E3),
    onSurface = Color(0xFFF2F1E3),
    primary = Color(0xFFF2F1E3),
    onPrimary = Color(0xFF222222),
    secondary = Color(0x1AFFFFFF),
    tertiary = Color(0x1AFFFFFF),
    outline = Color(0xFF444444),
    surfaceContainer = Color(0xFF181818)
)

@Composable
fun SelauraLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context, scope) }
    val targetDarkTheme = when (settingsManager.getStringAsFlow("theme").collectAsState("").value) {
        "dark" -> true
        "light" -> false
        else -> darkTheme
    }
    val activity = LocalActivity.current
    LaunchedEffect(targetDarkTheme) {
        activity?.run {
            WindowInsetsControllerCompat(window, window.decorView).run {
                isAppearanceLightStatusBars = !targetDarkTheme
                isAppearanceLightNavigationBars = !targetDarkTheme
            }
        }
    }

    val colorScheme = if (targetDarkTheme) DarkColorScheme else LightColorScheme
    val newColorScheme = remember { mutableStateOf<ColorScheme?>(null) }
    CircularRevealThemeContainer(colorScheme, newColorScheme) {
        MaterialTheme(newColorScheme.value ?: colorScheme, typography = typography()) { content() }
    }
}

@Composable
fun CircularRevealThemeContainer(
    colorScheme: ColorScheme,
    newColorScheme: MutableState<ColorScheme?>,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val rootView = LocalView.current
    val screenWidthPx = context.resources.displayMetrics.widthPixels.toFloat()
    val screenHeightPx = context.resources.displayMetrics.heightPixels.toFloat()
    val maxRadius = remember { hypot(screenWidthPx, screenHeightPx) }

    val revealRadius = remember { Animatable(0f) }
    var previousScreen by remember { mutableStateOf<Brush?>(null) }

    var rippleFromCenter by remember { mutableStateOf(false) }
    LaunchedEffect(colorScheme) {
        rippleFromCenter = newColorScheme.value == null
        previousScreen = ShaderBrush(ImageShader(rootView.drawToBitmap().asImageBitmap()))
        revealRadius.snapTo(0f)
        newColorScheme.value = colorScheme
        revealRadius.animateTo(maxRadius, animationSpec = tween(600))
    }

    Box(
        Modifier
            .fillMaxSize()
            .then(previousScreen?.let { Modifier.background(it) } ?: Modifier.background(colorScheme.onBackground))
            .clip(CircularRevealShape(revealRadius.value, rippleFromCenter))
            .background(colorScheme.background)
    ) { content() }
}

class CircularRevealShape(private val radius: Float, private val fromCenter: Boolean = false) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Generic(Path().apply {
                val origin = if (fromCenter) Offset(size.width / 2f, size.height / 2f) else Data.themeSwitchOffset.value
                addOval(Rect(origin - Offset(radius, radius), origin + Offset(radius, radius)))
            }
        )
    }
}