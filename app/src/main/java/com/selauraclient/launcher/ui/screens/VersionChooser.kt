package com.selauraclient.launcher.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.core.content.edit
import com.selauraclient.launcher.DownloadRequest
import com.selauraclient.launcher.DownloadService.Companion.startDownloads
import com.selauraclient.launcher.DownloadState
import com.selauraclient.launcher.global.Data
import com.selauraclient.launcher.global.Data.dialogState
import com.selauraclient.launcher.global.Data.downloadedVersions
import com.selauraclient.launcher.global.Data.installedMC
import com.selauraclient.launcher.global.Data.logs
import com.selauraclient.launcher.global.Data.searchFilters
import com.selauraclient.launcher.global.Data.showMessage
import com.selauraclient.launcher.global.Data.snackbarHostState
import com.selauraclient.launcher.global.Data.versionsList
import com.selauraclient.launcher.global.DialogState
import com.selauraclient.launcher.global.Downloader.downloadInfo
import com.selauraclient.launcher.global.MINECRAFT
import com.selauraclient.launcher.global.Version
import com.selauraclient.launcher.global.VersionType.BETA
import com.selauraclient.launcher.global.VersionType.RELEASE
import com.selauraclient.launcher.ui.core.AnimatedButton
import com.selauraclient.launcher.ui.core.CategoryTitle
import com.selauraclient.launcher.ui.core.Container
import com.selauraclient.launcher.ui.core.IconButton
import com.selauraclient.launcher.ui.core.Title
import com.selauraclient.launcher.ui.core.header
import com.selauraclient.launcher.utils.addLine
import com.selauraclient.launcher.utils.extractTo
import com.selauraclient.launcher.utils.getApks
import com.selauraclient.launcher.utils.getFileFromUri
import com.selauraclient.launcher.utils.getFileName
import com.selauraclient.launcher.utils.popyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.solrudev.ackpine.splits.Apk
import ru.solrudev.ackpine.splits.SplitPackage.Companion.toSplitPackage
import ru.solrudev.ackpine.splits.ZippedApkSplits
import ru.solrudev.ackpine.splits.get

val tabs = listOf("Installed", "Download")
val archiveExtensions = listOf("apkm", "apks", "xapk", "zip")

val transitionSpec: AnimatedContentTransitionScope<Int>.() -> ContentTransform = {
    val direction = if (targetState > initialState) 1 else -1
    (slideInHorizontally(
        initialOffsetX = { it * direction },
        animationSpec = tween(500)
    ) + fadeIn(animationSpec = tween(500))).togetherWith(
        slideOutHorizontally(
            targetOffsetX = { -it * direction },
            animationSpec = tween(500)
        ) + fadeOut(animationSpec = tween(500))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionChooser(selectVersionsOnly: Boolean = false, onClose: () -> Unit, onChoose: (Version, String) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult showMessage("File not selected")
        scope.launch(Dispatchers.IO) {
            val progress = Animatable(-1f)
            val action = mutableStateOf("")
            val setProgress: (Float) -> Unit = { scope.launch { progress.animateTo(it) } }
            val setAction: (String) -> Unit = {
                if (it.startsWith("Import Failed:")) {
                    dialogState.value.cancellable = true
                    dialogState.value.onCancel = {
                        dialogState.value.hide()
                    }
                    setProgress(0f)
                }
                action.value = it
            }
            dialogState.value = DialogState(show = true, cancellable = false, okText = "") {
                Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp), Alignment.CenterHorizontally) {
                    Text("Importing", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    if (progress.value >= 0f) LinearProgressIndicator(progress::value) else LinearProgressIndicator()
                    Text(action.value, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
            val extension = context.getFileName(uri)?.substringAfterLast(".") ?: return@launch setAction("Import Failed: Null file name")
            val (packageName, versionCode) = when (extension) {
                in archiveExtensions -> getInfoFromBundle(context, uri)
                "apk" -> Apk.fromUri(uri, context)?.let { it.packageName to it.versionCode } ?: return@launch setAction("Import Failed: Invalid APK")
                else -> return@launch setAction("Import Failed: Unsupported file type")
            }

            when {
                packageName != MINECRAFT -> return@launch setAction("Import Failed: Not Minecraft")
                versionCode < 972100000 -> return@launch setAction("Import Failed: Unsupported version")
                downloadedVersions.contains(versionCode) -> return@launch setAction("Import Failed: Version already imported")
            }

            try {
                val tmpDir = context.filesDir.resolve("downloads/${versionCode}tmp").apply { mkdirs() }
                val downloadDir = context.filesDir.resolve("downloads/$versionCode")
                setAction("Retrieving File")
                val file = context.getFileFromUri(uri) ?: return@launch setAction("Import Failed: Could not retrieve file")

                if (extension in archiveExtensions) {
                    file.extractTo(tmpDir) {
                        setAction("Extracting ${file.name}\n${(it * 100).toInt()}%")
                        setProgress(it)
                    }
                } else {
                    file.popyTo(tmpDir.resolve(file.name)) {
                        setAction("Copying ${file.name}\n${(it * 100).toInt()}%")
                        setProgress(it)
                    }
                }
                file.delete()
                setAction(if (tmpDir.renameTo(downloadDir)) "Finished" else "Import Failed: Could not rename folder")
            } catch (e: Exception) {
                setAction("Import Failed: ${e.localizedMessage ?: "Unknown error"}")
            }
            dialogState.value.cancellable = true
        }
    }
    BackHandler(true, onClose)
    Scaffold(
        topBar = {
            TopAppBar(header(), actions = {
                IconButton({
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    launcher.launch(intent)
                }) {
                    Icon(Icons.Default.Outbox, "Import")
                }
                IconButton(onClose) {
                    Icon(Icons.Default.Close, "Close")
                }
            })
        },
        snackbarHost = { SnackbarHost(snackbarHostState.value) }
    ) { paddings ->
        Surface(Modifier.padding(paddings)) {
            val downloads = downloadedVersions
            val versionsList = versionsList
            var state by remember { mutableIntStateOf(0) }
            val searchValue = remember { mutableStateOf("") }
            Column {
                Column(Modifier.padding(8.dp)) {
                    SearchBar(searchValue)
                    if (!selectVersionsOnly) {
                        PrimaryTabRow(state, Modifier, Color.Transparent) {
                            tabs.forEachIndexed { index, title ->
                                Container(
                                    { state = index }, Modifier
                                        .padding(horizontal = 8.dp)
                                        .padding(top = 4.dp, bottom = 8.dp)
                                        .height(40.dp)
                                ) {
                                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                                        Text(title)
                                    }
                                }
                            }
                        }
                    }
                }

                val showed = remember { mutableStateListOf<Version>() }
                val downloadsVersions = versionsList.filterWith {
                    !downloads.contains(it.versionCode) &&
                            (searchFilters.contains("Beta") || it.versionType != BETA) &&
                            it.toString().contains(searchValue.value)
                }

                LaunchedEffect(state) {
                    showed.clear()
                    if (state == 1) {
                        val filtered = versionsList.filterWith { !downloadsVersions.contains(it) }
                        showed.addAll(filtered)
                        downloadsVersions.fastForEach {
                            showed.add(it)
                            delay(20)
                        }
                    }
                }

                AnimatedContent(state, Modifier.weight(1f), transitionSpec) {targetState ->
                    LazyColumn(Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()) {
                        if (targetState == 0) {
                            val downloadedVersions = versionsList.filterWith { downloads.contains(it.versionCode) && it.toString().contains(searchValue.value) }
                            item {
                                val mcInfo = installedMC.value
                                if (mcInfo != null) {
                                    Title("Installed Version")
                                    mcInfo.Card { onChoose(mcInfo, "Installed") }
                                }
                                if (downloadedVersions.isNotEmpty()) Title("Downloaded Versions")
                            }
                            items(downloadedVersions) { it.Card { onChoose(it, "Downloaded") } }
                        } else {
                            items(downloadsVersions) { v ->
                                AnimatedVisibility(
                                    visible = showed.contains(v),
                                    enter = slideInHorizontally(
                                        initialOffsetX = { it / 2 },
                                        animationSpec = tween(500)
                                    ) + fadeIn(animationSpec = tween(200)),
                                    exit = slideOutHorizontally(
                                        targetOffsetX = { -it / 2 },
                                        animationSpec = tween(500)
                                    ) + fadeOut(animationSpec = tween(500))
                                ) {
                                    v.Card(false, {
                                        AnimatedButton({
                                            scope.launch {
                                                try {
                                                    val outputDir = context.filesDir.resolve("downloads/${v.versionCode}.tmp/")
                                                    outputDir.mkdirs()
                                                    val request = getApks(v.versionCode.toInt()).map { DownloadRequest(it.url, it.name, it.size, outputDir.absolutePath) }
                                                    context.startDownloads(request, v.versionName)
                                                    onClose()
                                                } catch (e: Exception) {
                                                    if (e.stackTraceToString().contains("App not purchased")) {
                                                        dialogState.value = DialogState(
                                                            true, false, "Logout", "",{
                                                                val accountData = context.getSharedPreferences("account_data", Context.MODE_PRIVATE)
                                                                accountData.edit(true) {
                                                                    remove("token")
                                                                    remove("email")
                                                                }
                                                                Data.authData.value = null
                                                                dialogState.value.hide()
                                                            }
                                                        ) {
                                                            Text("Not Purchased", style = MaterialTheme.typography.titleLarge)
                                                            Text("This account doesn't own minecraft, did you buy minecraft with this account?")
                                                        }
                                                    } else {
                                                        logs.addLine(e.stackTraceToString())
                                                    }
                                                }
                                            }
                                        }, Modifier.graphicsLayer {
                                            scaleX = 0.7f
                                            scaleY = 0.7f
                                            transformOrigin = TransformOrigin(.8f, .5f)
                                        }, downloadInfo.value?.state !in listOf(DownloadState.DOWNLOADING, DownloadState.PAUSED)) { Text("Download") }
                                    }) { onChoose(v, "Not Downloaded") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(searchValue: MutableState<String>) {
    Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
        Container(modifier = Modifier
            .weight(1f)
            .height(40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Search,
                    "Search",
                    Modifier
                        .fillMaxHeight()
                        .padding(13.dp, 10.dp, 6.dp, 10.dp)
                        .aspectRatio(1f)
                )
                BasicTextField(
                    searchValue.value,
                    { searchValue.value = it },
                    Modifier.fillMaxHeight(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface)
                ) { innerTextField ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { innerTextField() }
                }
            }
        }
        var expanded by remember { mutableStateOf(false) }
        IconButton({ expanded = true }) {
            Icon(Icons.Default.FilterList, "Filter")
            DropdownMenu(expanded, { expanded = false }) {
                CategoryTitle("Show")
                FilterOption("Beta", searchFilters)
                FilterOption("Unsupported", searchFilters)
            }
        }
    }
}

inline fun SnapshotStateMap<Long, Version>.filterWith(condition: (Version) -> Boolean): List<Version> = this.values.sortedByDescending { it.versionCode }.fastFilter(condition)

@Composable
private fun Version.Card(enabled: Boolean = true, extraContent: @Composable () -> Unit = {}, onClick: () -> Unit) {
    Container(onClick, Modifier
        .padding(vertical = 3.dp)
        .height(40.dp)
        .fillMaxWidth(), enabled) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Text("${if (versionType == RELEASE) "" else "Beta "}${versionName}", Modifier.padding(8.dp))
            Spacer(Modifier.weight(1f))
            extraContent()
        }
    }
}

@Composable
private fun FilterOption(option: String, filters: MutableList<String>) {
    var checked by remember { mutableStateOf(filters.contains(option)) }
    val toggle = {
        checked = !checked
        if (checked) filters.add(option) else filters.remove(option)
    }
    Row(Modifier
        .fillMaxWidth()
        .clickable { toggle() }
        .padding(start = 16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(option)
        Checkbox(checked, { toggle() })
    }
}

fun getInfoFromBundle(context: Context, bundleUri: Uri): Pair<String, Long> = runBlocking {
    val splitsSequence = ZippedApkSplits.getApksForUri(bundleUri, context).toSplitPackage()
    splitsSequence.get().base.first().apk.run { packageName to versionCode }
}