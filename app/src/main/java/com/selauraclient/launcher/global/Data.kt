package com.selauraclient.launcher.global

import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager.GET_META_DATA
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.gplayapi.helpers.AuthHelper.Token.AAS
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.selauraclient.launcher.DownloadInfo
import com.selauraclient.launcher.DownloadService
import com.selauraclient.launcher.logD
import com.selauraclient.launcher.utils.Launcher
import com.selauraclient.launcher.utils.getNativeDeviceProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException

data class DialogState(
    val show: Boolean = false,
    var cancellable: Boolean = true,
    val okText: String = "OK",
    val cancelText: String = "Cancel",
    val onOk: () -> Unit = {},
    var onCancel: () -> Unit = {},
    val onDismiss: () -> Unit = {},
    val modifier: Modifier = Modifier.padding(20.dp),
    val content: @Composable (DialogState) -> Unit = {}
) {
    fun hideIfCancellable() { if (cancellable) hide() }
    fun hide() { update(copy(show = false)) }

    private fun update(newState: DialogState) {
        Data.dialogState.value = newState
    }
}

enum class VersionType(val displayName: String) {
    RELEASE("Release"), BETA("Beta"), UNKNOWN("Unknown")
}

data class Version(
    val versionCode: Long,
    val versionName: String,
    val versionType: VersionType
)

object Data {
    val themeSwitchOffset = mutableStateOf(Offset.Zero)
    val dialogState = mutableStateOf(DialogState())
    val okHttpClient by lazy { OkHttpClient.Builder().build() }
    val versionsList = mutableStateMapOf<Long, Version>()
    val installedMC = mutableStateOf<Version?>(null)
    val downloadedVersions = mutableStateListOf<Long>()
    val searchFilters = mutableStateListOf<String>()
    val startLaunch = mutableStateOf(false)
    val launcher = mutableStateOf<Launcher?>(null)
    val launcherLoading = mutableStateOf(false)
    val snackbarHostState = mutableStateOf(SnackbarHostState())
    val logs = mutableStateOf("")
    val selectedVersionInfo = mutableStateOf<Pair<Version, String>?>(null)
    val showLoginScreen = mutableStateOf(false)
    val authData = mutableStateOf<AuthData?>(null)
    val authDataLoading = mutableStateOf(false)
    val isNetworkConnected = mutableStateOf(false)
    val showMoreDialog = mutableStateOf(false)

    fun updateInstalledMCInfo(context: Context) {
        try { context.packageManager.getPackageInfo(MINECRAFT, GET_META_DATA) } catch (_: Exception) {null}?.let {
            installedMC.value = Version(it.longVersionCode, it.versionName!!, versionsList[it.longVersionCode]?.versionType ?: VersionType.UNKNOWN)
        }
    }
    fun updateVersionsList(context: Context, onFinish: () -> Unit = {}) {
        val gson = Gson()
        val localVersionsList = File(context.filesDir, "local_versions_list.json")
        if (localVersionsList.exists()) {
            versionsList.putAll(
                gson.fromJson(
                    localVersionsList.readText(),
                    object : TypeToken<Map<Long, Version>>() {}.type
                )
            )
        }
        Thread {
            okHttpClient.newCall(Request.Builder().url(VERSIONS_LINK).build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!localVersionsList.exists()) {
                        dialogState.value = DialogState(
                            show = true,
                            okText = "Understood",
                            cancellable = false,
                            onOk = { dialogState.value.hide() },
                            content = { Text("Failed to update versions list and load local versions list, the app wont be able to switch to versions you haven't downloaded or know which version is beta or release") }
                        )
                    } else {
                        onFinish()
                        logD("from local versions list")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    versionsList.clear()
                    response.body?.string()?.split("],")?.forEach {
                        val data = it.replace("[", "").replace("]", "").split(",")
                        val versionCode = data[0].toLong()
                        if (versionCode > 972100000) {
                            val versionName = data[1].replace("\"", "")
                            val versionType = if (data[2] == "1") VersionType.BETA else VersionType.RELEASE
                            versionsList[versionCode] = Version(versionCode, versionName, versionType)
                        }
                    }
                    localVersionsList.writeText(gson.toJson(versionsList))
                    onFinish()
                    logD("from internet")
                }
            })
        }.start()
    }
    fun updateAuthData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                authDataLoading.value = true
                val accountData = context.getSharedPreferences("account_data", Context.MODE_PRIVATE)
                val token = accountData.getString("token", null)
                val email = accountData.getString("email", null)

                authData.value = if (token != null && email != null) {
                    AuthHelper.build(
                        email,
                        token,
                        AAS,
                        properties = getNativeDeviceProperties(context)
                    )
                } else null
                authDataLoading.value = false
            } catch (_ : Exception) { }
        }
    }
    fun showMessage(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            snackbarHostState.value.showSnackbar(message)
        }
    }
}

object Downloader {
    val downloadInfo = mutableStateOf<DownloadInfo?>(null)
    var isBound by mutableStateOf(false)
    var downloadService: DownloadService? = null
    lateinit var connection: ServiceConnection

    fun Context.bindToService() {
        if (!isBound) {
            Intent(this, DownloadService::class.java).also {
                bindService(it, connection, BIND_AUTO_CREATE)
            }
        }
    }

    fun Context.unbindFromService() {
        if (isBound) {
            unbindService(connection)
            isBound = false
            downloadService = null
        }
    }
}