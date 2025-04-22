package com.selauraclient.launcher

import Logo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selauraclient.launcher.ui.theme.SelauraLauncherTheme
import com.selauraclient.launcher.ui.theme.SettingsManager
import com.selauraclient.launcher.ui.theme.poppins

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SelauraLauncherTheme {
                LauncherScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val logs = remember { mutableStateOf("") }
    val settingsManager = SettingsManager(context, scope)
    val theme by settingsManager.getStringAsFlow("theme").collectAsState(initial = "system")
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(header(), actions = {
                ThemeSwitch(settingsManager, theme)
            })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Surface(Modifier.padding(innerPadding).fillMaxSize()) {
            Box {
                Column(Modifier.align(Alignment.BottomCenter)) {
                    Box(contentAlignment = Alignment.Center) {
                        HorizontalDivider()
                        AnimatedButton ({ context.startLauncher(snackbarHostState, scope, logs) }, Modifier.width(150.dp), shape = MaterialTheme.shapes.small) {
                            Text("Launch")
                        }
                    }
                    LogsDisplay(logs)
                }
            }
        }
    }
}

@Composable
private fun LogsDisplay(logs: MutableState<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(20.dp)
            .background(Color(0f, 0f, 0f, .2f), MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        Text("Logs:", fontSize = 15.sp, color = Color.Gray)
        LazyColumn {
            item {
                Text(logs.value, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ThemeSwitch(settingsManager: SettingsManager, theme: String) {
    val nextTheme = when (theme) {
        "dark" -> "light"
        else -> "dark"
    }

    val icon = when (theme) {
        "dark" -> Icons.Default.DarkMode
        "light" -> Icons.Default.LightMode
        else -> Icons.Default.AutoMode
    }

    IconButton(
        { settingsManager.setString("theme", nextTheme) },
        { settingsManager.setString("theme", "system") },
        Modifier.onGloballyPositioned { coordinates ->
            val position = coordinates.localToWindow(Offset.Zero)
            val size = coordinates.size
            Globals.themeSwitchOffset.value = position + Offset(size.width / 2f, size.height / 2f)
        }
    ) {
        Icon(icon, contentDescription = "Toggle theme")
    }
}

private fun header() = @Composable {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            Logo,
            "Logo",
            Modifier.padding(5.dp).size(32.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )
        Text("selaura", fontSize = 25.sp, fontWeight = FontWeight.Thin, fontFamily = poppins)
    }
}