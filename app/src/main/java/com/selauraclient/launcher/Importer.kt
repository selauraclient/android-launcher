package com.selauraclient.launcher

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.selauraclient.launcher.global.Data.authData
import com.selauraclient.launcher.global.Data.isNetworkConnected
import com.selauraclient.launcher.global.Data.showMessage
import com.selauraclient.launcher.global.Data.startLaunch
import com.selauraclient.launcher.global.Data.updateAuthData
import com.selauraclient.launcher.global.Data.updateInstalledMCInfo
import com.selauraclient.launcher.global.Data.updateVersionsList
import com.selauraclient.launcher.global.MINECRAFT
import com.selauraclient.launcher.ui.screens.VersionChooser
import com.selauraclient.launcher.ui.theme.SelauraLauncherTheme
import com.selauraclient.launcher.utils.DownloadsFolderWatcher
import com.selauraclient.launcher.utils.NetworkHelper
import com.selauraclient.launcher.utils.getLauncher

class Importer : ComponentActivity() {
    private val networkHelper by lazy { NetworkHelper(this) }
    private lateinit var watcher: DownloadsFolderWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        watcher = DownloadsFolderWatcher(this)
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    @SuppressLint("IntentReset")
    private fun handleIntent(intent: Intent) {
        var newIntent = Intent(intent).apply {
            setClassName(this@Importer, "$MINECRAFT.Selaura")
        }

        val launch = {
            startActivity(newIntent)
            finish()
        }

        if (isMcRunning) {
            launch()
        } else {
            updateVersionsList(this)
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
                    LaunchedEffect(Unit) {
                        updateInstalledMCInfo(this@Importer)
                    }
                    VersionChooser(true, { finish() }) { version, status ->
                        this.getLauncher(version, status) {
                            newIntent = it.getIntent()
                            startLaunch.value = true
                        }
                    }
                    StoragePermissionHandler()
                    LaunchTransition(this) {
                        launch()
                    }
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        watcher.stopWatching()
    }
    private inline val isMcRunning: Boolean
        get() = try { Class.forName("$MINECRAFT.Selaura", false, classLoader); true } catch (_: Exception) { false }
}
