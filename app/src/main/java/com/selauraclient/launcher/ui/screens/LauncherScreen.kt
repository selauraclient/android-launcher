package com.selauraclient.launcher.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.selauraclient.launcher.DownloadService.Companion.cancel
import com.selauraclient.launcher.DownloadService.Companion.pause
import com.selauraclient.launcher.DownloadService.Companion.resume
import com.selauraclient.launcher.DownloadState
import com.selauraclient.launcher.global.Data.authData
import com.selauraclient.launcher.global.Data.launcher
import com.selauraclient.launcher.global.Data.launcherLoading
import com.selauraclient.launcher.global.Data.logs
import com.selauraclient.launcher.global.Data.selectedVersionInfo
import com.selauraclient.launcher.global.Data.showMoreDialog
import com.selauraclient.launcher.global.Data.snackbarHostState
import com.selauraclient.launcher.global.Data.startLaunch
import com.selauraclient.launcher.global.Data.themeSwitchOffset
import com.selauraclient.launcher.global.Data.versionsList
import com.selauraclient.launcher.global.Downloader.downloadInfo
import com.selauraclient.launcher.ui.core.AnimatedButton
import com.selauraclient.launcher.ui.core.Container
import com.selauraclient.launcher.ui.core.IconButton
import com.selauraclient.launcher.ui.core.header
import com.selauraclient.launcher.utils.SettingsManager
import com.selauraclient.launcher.utils.addLine
import com.selauraclient.launcher.utils.checkForUpdate
import com.selauraclient.launcher.utils.formatBytes
import com.selauraclient.launcher.utils.formatETA
import com.selauraclient.launcher.utils.formatSpeed
import com.selauraclient.launcher.utils.getLauncher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val showVersionChooser = remember { mutableStateOf(false) }
    val orientation = LocalConfiguration.current.orientation
    Scaffold(Modifier.fillMaxSize(), { TopAppBar(header(), actions = { UpdateClient(); ThemeSwitch(); AccountIcon()}) }, snackbarHost = { SnackbarHost(snackbarHostState.value) }) { innerPadding ->
        Surface(Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            MainContent(orientation) {
                SelectedVersionInfo {
                    showVersionChooser.value = true
                }
                DownloadInfo(context)
                ShaderLoaderManager(context)
            }
        }
    }

    AnimatedVisibility(showVersionChooser.value, enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut()) {
        VersionChooser(onClose = {showVersionChooser.value = false}) { version, status ->
            selectedVersionInfo.value = version to status
            showVersionChooser.value = false
            context.getLauncher(version, status)
        }
    }
}

val shaderLoaders = mapOf<String, String>(
     "" to "None",
     "draco" to "Draco",
     "materialbinloader" to "MaterialBinLoader",
     "mtbinloader2" to "MaterialBinLoader2",
)

@Composable
private fun ShaderLoaderManager(context: Context) {
    val preferences = context.getSharedPreferences("Selaura", Context.MODE_PRIVATE)
    var shaderLoader by remember { mutableStateOf(preferences.getString("shader_loader", "") ?: "") }
    LaunchedEffect(shaderLoader) {
        preferences.edit { putString("shader_loader", shaderLoader) }
        System.setProperty("shader_loader", shaderLoader)
    }
    Container(enabled = false) {
        var showOptions by remember { mutableStateOf(false) }
        Row(Modifier.padding(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Selected shader loader", fontSize = 18.sp)
                Text(shaderLoaders[shaderLoader] ?: "Unknown")
            }
            IconButton({ showOptions = !showOptions }, modifier = Modifier.align(Alignment.CenterVertically)) {
                Icon(if (showOptions) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, if (showOptions) "Show Options" else "Hide Options" )
            }
        }
        AnimatedVisibility(showOptions, enter = expandVertically(), exit = shrinkVertically()) {
            Column(Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                shaderLoaders.keys.filterNot { it == shaderLoader }.forEach {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                showOptions = false; shaderLoader = it
                            }
                            .padding(12.dp)
                    ) {
                        Text(shaderLoaders[it]!!)
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun ColumnScope.DownloadInfo(context: Context) {
    val info = downloadInfo.value
    AnimatedVisibility(
        info != null && (info.state in listOf(DownloadState.PAUSED, DownloadState.DOWNLOADING)),
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        requireNotNull(info)
        Container(enabled = false) {
            Box {
                val animatedProgress = animateFloatAsState(info.progress.toFloat())
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = animatedProgress.value / 100f
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                )
                Column(Modifier
                    .fillMaxWidth()
                    .padding(12.dp)) {
                    Text("Downloading ${info.currentBatchName}", fontSize = 18.sp)
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("ETA: ${formatETA(info.progress, info.speedKbps, info.totalBytes)}")
                        Text("${String.format("%.2f", info.progress)}%")
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Speed: ${formatSpeed(info.speedKbps)}")
                        Text("${formatBytes(info.downloadedBytes)}/${formatBytes(info.totalBytes)}")
                    }
                    val buttons = remember { listOf<Triple<String, () -> Unit, () -> Boolean>>(
                        Triple("Pause", { context.pause() }, { info.state == DownloadState.DOWNLOADING }),
                        Triple("Resume", { context.resume() }, { info.state == DownloadState.PAUSED }),
                        Triple("Cancel", { context.cancel() }, { info.state in listOf(DownloadState.DOWNLOADING, DownloadState.PAUSED) })
                    ) }

                    Row(Modifier
                        .align(Alignment.End)
                        .padding(top = 3.dp),Arrangement.spacedBy(8.dp)) {
                        buttons.forEach { (label, action, enabled) ->
                            AnimatedButton(action, Modifier.requiredSize(80.dp, 35.dp), enabled(), contentPadding = PaddingValues(0.dp)) {
                                Text(label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(info.state) {
            if (info.state in listOf(
                    DownloadState.FAILED,
                    DownloadState.COMPLETED,
                    DownloadState.CANCELLED
                )
            ) {
                when (info.state) {
                    DownloadState.FAILED -> logs.addLine("Download Failed: ${info.error}")
                    DownloadState.COMPLETED -> {
                        val version =
                            versionsList.values.first { it.versionName == info.currentBatchName }.versionCode
                        val downloaded = context.filesDir.resolve("downloads/$version.tmp")
                        val final = context.filesDir.resolve("downloads/$version")
                        downloaded.renameTo(final)
                        logs.addLine("Download Completed!")
                    }

                    DownloadState.CANCELLED -> logs.addLine("Download Cancelled")
                    else -> {}
                }
                downloadInfo.value?.currentBatchName = ""
            }
        }
    }
}

@Composable
private fun MainContent(orientation: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize(), Arrangement.SpaceBetween) {
        Row(Modifier.weight(1f, false)) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f, false)
                    .padding(20.dp),
                Arrangement.spacedBy(10.dp),
                content = content
            )
            if (orientation == ORIENTATION_LANDSCAPE) LogsDisplay(Modifier
                .fillMaxHeight()
                .fillMaxWidth(.3f), true)
        }
        LaunchingArea(orientation)
    }
}

@Composable
fun SelectedVersionInfo(onClick: () -> Unit = {}) {
    Container(enabled = false) {
        Row(Modifier.padding(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Selected Version", fontSize = 18.sp)
                if (selectedVersionInfo.value != null) {
                    val (selectedVersion, status) = selectedVersionInfo.value!!
                    FlowRow {
                        Text("Version: ${selectedVersion.versionName} - ${selectedVersion.versionCode} ")
                        Text("Type: ${selectedVersion.versionType.displayName}")
                    }
                    Text("Status: $status")
                } else {
                    Text("No version selected\n")
                }
            }
            IconButton(onClick, modifier = Modifier.align(Alignment.CenterVertically)) {
                Icon(Icons.Default.SyncAlt, "Change Version")
            }
        }
    }
}

@Composable
private fun LaunchingArea(orientation: Int) {
    Column {
        val isLoading = launcherLoading.value
        val nonSelected = launcher.value == null
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            AnimatedButton(
                { startLaunch.value = true },
                Modifier.width(150.dp),
                !isLoading && !nonSelected,
                MaterialTheme.shapes.small
            ) {
                Text(if (isLoading) "Loading..." else "Launch")
            }
            HorizontalDivider(Modifier.weight(1f))
        }
        if (orientation == ORIENTATION_PORTRAIT) LogsDisplay(
            Modifier
                .fillMaxHeight(.3f)
                .fillMaxWidth()
        ) else Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun LogsDisplay(modifier: Modifier, landscape: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context, scope) }
    val showLogs by settingsManager.getBooleanAsFlow("show_logs").collectAsState(false)
    val verticalScrollState = rememberScrollState()
    Column(Modifier.defaultMinSize(minHeight = 40.dp)) {
        AnimatedVisibility(
            showLogs,
            enter = if (landscape) expandHorizontally() else expandVertically(),
            exit = if (landscape) shrinkHorizontally() else shrinkVertically()
        ) {
            Column(modifier) {
                LaunchedEffect(logs.value) {
                    verticalScrollState.scrollTo(verticalScrollState.maxValue)
                }
                Container(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize(),
                    enabled = false
                ) {
                    Text("Logs:", Modifier.padding(top = 12.dp, start = 12.dp), Color.Gray, 15.sp)
                    Column(
                        Modifier
                            .padding(bottom = 12.dp)
                            .padding(horizontal = 12.dp)
                            .horizontalScroll(rememberScrollState())
                            .verticalScroll(verticalScrollState)
                    ) {
                        Text(logs.value, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountIcon() {
    IconButton({ showMoreDialog.value = true }) {
        AsyncImage(
            ImageRequest.Builder(LocalContext.current)
                .data(authData.value?.userProfile?.artwork?.url ?: Icon(Icons.Default.AccountCircle, "Account Profile"))
                .crossfade(true)
                .allowHardware(false)
                .build(), "Account Profile",
            Modifier
                .requiredSize(24.dp)
                .clip(CircleShape),
        )
    }
}

@Composable
private fun ThemeSwitch() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsManager(context, scope) }
    val theme by settings.getStringAsFlow("theme").collectAsState("system")
    val next = if (theme == "dark") "light" else "dark"
    val icon = when (theme) {
        "dark" -> Icons.Default.DarkMode
        "light" -> Icons.Default.LightMode
        else -> Icons.Default.AutoMode
    }
    var lastChange by remember { mutableLongStateOf(0) }

    fun change(theme: String) {
        if (System.currentTimeMillis() - lastChange >= 500) {
            settings.setString("theme", theme)
            lastChange = System.currentTimeMillis()
        }
    }

    IconButton(
        onClick = { change(next) },
        modifier = Modifier.onGloballyPositioned {
            val pos = it.localToWindow(Offset.Zero)
            val size = it.size
            themeSwitchOffset.value = pos + Offset(size.width / 2f, size.height / 2f)
        },
        onLongClick = { change("system") }
    ) {
        Icon(icon, contentDescription = "Toggle theme")
    }
}

@Composable
private fun UpdateClient() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    IconButton({ checkForUpdate(context, { scope.launch { snackbarHostState.value.showSnackbar(it)} }) }) {
        Icon(Icons.Default.Update, "Update Client")
    }
}