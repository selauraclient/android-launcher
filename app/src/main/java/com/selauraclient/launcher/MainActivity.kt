package com.selauraclient.launcher

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.drawToBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.selauraclient.launcher.global.Data
import com.selauraclient.launcher.global.Data.authData
import com.selauraclient.launcher.global.Data.downloadedVersions
import com.selauraclient.launcher.global.Data.installedMC
import com.selauraclient.launcher.global.Data.isNetworkConnected
import com.selauraclient.launcher.global.Data.selectedVersionInfo
import com.selauraclient.launcher.global.Data.showMessage
import com.selauraclient.launcher.global.Data.updateAuthData
import com.selauraclient.launcher.global.Data.updateInstalledMCInfo
import com.selauraclient.launcher.global.Data.updateVersionsList
import com.selauraclient.launcher.global.Data.versionsList
import com.selauraclient.launcher.global.Downloader.bindToService
import com.selauraclient.launcher.global.Downloader.connection
import com.selauraclient.launcher.global.Downloader.downloadInfo
import com.selauraclient.launcher.global.Downloader.downloadService
import com.selauraclient.launcher.global.Downloader.isBound
import com.selauraclient.launcher.global.Downloader.unbindFromService
import com.selauraclient.launcher.ui.core.Dialog
import com.selauraclient.launcher.ui.core.PermissionHandler
import com.selauraclient.launcher.ui.screens.LauncherScreen
import com.selauraclient.launcher.ui.screens.LoginScreen
import com.selauraclient.launcher.ui.screens.MoreDialog
import com.selauraclient.launcher.ui.theme.CircularRevealShape
import com.selauraclient.launcher.ui.theme.SelauraLauncherTheme
import com.selauraclient.launcher.utils.DownloadsFolderWatcher
import com.selauraclient.launcher.utils.NetworkHelper
import com.selauraclient.launcher.utils.checkForUpdate
import com.selauraclient.launcher.utils.getLauncher
import com.selauraclient.launcher.utils.hideSystemBars
import kotlinx.coroutines.launch
import kotlin.math.hypot

class Initializer(application: Application) : AndroidViewModel(application) {
    init {
        val context = application.applicationContext
        updateVersionsList(context) {
            updateInstalledMCInfo(context)
            val selectedVer = installedMC.value
            if (selectedVer != null) {
                selectedVersionInfo.value = selectedVer to "Installed"
            } else if (downloadedVersions.isNotEmpty()) {
                val version = downloadedVersions.first()
                selectedVersionInfo.value = versionsList.values.first{ it.versionCode == version } to "Downloaded"
            }
            if (selectedVersionInfo.value != null) {
                context.getLauncher(selectedVersionInfo.value!!.first, selectedVersionInfo.value!!.second)
            }
        }
        checkForUpdate(context)
    }
}



class MainActivity : ComponentActivity() {
    private val networkHelper by lazy { NetworkHelper(this) }
    private lateinit var watcher: DownloadsFolderWatcher
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        watcher = DownloadsFolderWatcher(this)
        ViewModelProvider(this)[Initializer::class.java]
        connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val downloadBinder = binder as? DownloadService.DownloadBinder
                downloadService = downloadBinder?.getService()
                isBound = true

                lifecycleScope.launch {
                    downloadService?.overallDownloadInfo?.collect {
                        downloadInfo.value = it
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                downloadService = null
                isBound = false
            }
        }
        networkHelper.observe(this) { isConnected ->
            isNetworkConnected.value = isConnected
            if (isConnected) {
                if (authData.value == null) updateAuthData(this)
            } else {
                showMessage("No internet connection")
            }
        }
        enableEdgeToEdge()
        setContent {
            SelauraLauncherTheme {
                LauncherScreen()
                StoragePermissionHandler()
                MoreDialog()
                LoginScreen()
                Dialog(Data.dialogState.value)
                LaunchTransition(this) {
                    startActivity(Data.launcher.value!!.getIntent())
                    finish()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindToService()
    }

    override fun onStop() {
        super.onStop()
        unbindFromService()
    }

    override fun onDestroy() {
        super.onDestroy()
        watcher.stopWatching()
        println("onDestory")
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LaunchTransition(activity: Activity, onFinish: () -> Unit) {
    if (Data.startLaunch.value) {
        val view = LocalView.current
        var screen by remember { mutableStateOf<Brush?>(null) }
        val radius = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            activity.hideSystemBars()
            screen = ShaderBrush(ImageShader(view.drawToBitmap().asImageBitmap()))
            val screenWidthPx = activity.resources.displayMetrics.widthPixels.toFloat()
            val screenHeightPx = activity.resources.displayMetrics.heightPixels.toFloat()
            radius.animateTo(hypot(screenWidthPx, screenHeightPx), animationSpec = tween(600))
            onFinish()
        }
        if (screen != null) {
            Scaffold {
                Box(Modifier
                    .fillMaxSize()
                    .background(screen!!)
                    .clip(CircularRevealShape(radius.value, true))
                    .background(Color.Black))
            }
        }
    }
}

@Composable
fun StoragePermissionHandler() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val context = LocalContext.current

    PermissionHandler(
        checkPermission = { Environment.isExternalStorageManager() },
        requestPermission = {
            it.launch(Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                "package:${context.packageName}".toUri()
            ))
        },
        dialogContent = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Storage Permission Required", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Please grant storage access for full functionality.")
            }
        }
    )
}