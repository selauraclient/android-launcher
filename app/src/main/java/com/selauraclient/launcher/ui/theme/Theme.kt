package com.selauraclient.launcher.ui.theme

import android.content.Context
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
import androidx.core.view.drawToBitmap
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.selauraclient.launcher.Globals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    outline = Color(0x4D000000)
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
    outline = Color(0xFFF2F1E3)
)


@Composable
fun SelauraLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = SettingsManager(context, rememberCoroutineScope())

    val themePref = settingsManager.getStringAsFlow("theme").collectAsState("").value
    val targetDarkTheme = when (themePref) {
        "dark" -> true
        "light" -> false
        else -> darkTheme
    }

    val colorScheme = if (targetDarkTheme) DarkColorScheme else LightColorScheme
    val newColorScheme = remember { mutableStateOf<ColorScheme?>(null) }
    CircularRevealThemeContainer(colorScheme, newColorScheme) {
        MaterialTheme(
            colorScheme = newColorScheme.value ?: colorScheme,
            typography = Typography,
            content = content
        )
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
            .fillMaxSize().then(
                if (previousScreen != null) Modifier.background(
                    previousScreen!!
                ) else Modifier.background(colorScheme.onBackground)
            )
            .clip(CircularRevealShape(revealRadius.value, rippleFromCenter))
            .background(colorScheme.background)
    ) {
        content()
    }
}

class CircularRevealShape(private val radius: Float, private val fromCenter: Boolean = false) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = Path().apply {
                val center = Offset(size.width / 2f, size.height / 2f)
                val origin = if (fromCenter) center else Globals.themeSwitchOffset.value
                addOval(Rect(origin - Offset(radius, radius), origin + Offset(radius, radius)))
            }
        )
    }
}


private val Context.settings by preferencesDataStore(name = "settings")
@Suppress("unused")
class SettingsManager(private val context: Context, private val scope: CoroutineScope) {

    fun getIntAsFlow(key: String): Flow<Int> = context.settings.data.map { it[intPreferencesKey(key)] ?: 0 }

    fun setInt(key: String, value: Int) {
        scope.launch { context.settings.edit { it[intPreferencesKey(key)] = value } }
    }

    fun getStringAsFlow(key: String): Flow<String> = context.settings.data.map { it[stringPreferencesKey(key)] ?: "" }

    suspend fun getString(key: String): String = getStringAsFlow(key).first()

    fun setString(key: String, value: String) {
        scope.launch { context.settings.edit { it[stringPreferencesKey(key)] = value } }
    }

    fun getBooleanAsFlow(key: String): Flow<Boolean> = context.settings.data.map { it[booleanPreferencesKey(key)] ?: false }

    fun setBoolean(key: String, value: Boolean) {
        scope.launch { context.settings.edit { it[booleanPreferencesKey(key)] = value } }
    }
}