package com.selauraclient.launcher

import Logo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selauraclient.launcher.ui.theme.SelauraLauncherTheme

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
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen() {

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar({
                Row(verticalAlignment = Alignment.CenterVertically){
                    Image(Logo, "yeah", Modifier.padding(10.dp, 15.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
                    Text("selaura", fontFamily = FontFamily(
                        Font(googleFont = GoogleFont("Poppins"), fontProvider = provider)
                    ), fontSize = 25.sp)
                }
            })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Surface(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                OutlinedButton({ context.startLauncher(snackbarHostState, scope) }, shape = MaterialTheme.shapes.small) {
                    Text("Launch")
                }
            }
        }
    }
}