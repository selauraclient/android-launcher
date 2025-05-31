package com.selauraclient.launcher.utils

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.MutableState
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

fun Activity.hideSystemBars() {
    val window = this.window
    val decorView = window.decorView
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val controller = WindowInsetsControllerCompat(window, decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }
}

fun Context.getFileName(uri: Uri): String? = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
    if (cursor.moveToFirst()) {
        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let {
            cursor.getString(it)
        }
    } else null
}

fun Context.getFileFromUri(uri: Uri): File? {
    val inputStream = contentResolver.openInputStream(uri) ?: return null
    val fileName = getFileName(uri)
    val tempFile = File(cacheDir, fileName ?: throw IOException("Cannot get file name"))
    inputStream.use { input ->
        tempFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return tempFile
}

suspend fun File.popyTo(
    targetFile: File,
    onProgress: (Float) -> Unit
) = withContext(Dispatchers.IO) {
    val totalBytes = length().takeIf { it > 0 } ?: return@withContext
    var bytesCopied = 0L
    var lastReportedProgress = -1f
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    inputStream().use { input ->
        targetFile.outputStream().use { output ->
            var bytes = input.read(buffer)
            while (bytes >= 0) {
                output.write(buffer, 0, bytes)
                bytesCopied += bytes
                val progress = bytesCopied.toFloat() / totalBytes
                if ((progress - lastReportedProgress) >= 0.001f) {
                    onProgress(progress)
                    lastReportedProgress = progress
                }
                bytes = input.read(buffer)
            }
        }
    }
}

suspend fun File.extractTo(
    destinationDir: File,
    onProgress: (Float) -> Unit
) = withContext(Dispatchers.IO) {
    ZipFile(this@extractTo).use { zip ->
        val entries = zip.entries().asSequence().toList()
        val totalBytes = entries.sumOf { file -> file.size.takeIf { it > 0 } ?: 0L }
            .takeIf { it > 0 } ?: return@withContext
        var bytesCopied = 0L
        var lastProgress = 0f
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

        for (entry in entries) {
            val targetFile = File(destinationDir, entry.name)

            if (entry.isDirectory) {
                targetFile.mkdirs()
                continue
            } else {
                targetFile.parentFile?.mkdirs()
            }

            zip.getInputStream(entry).use { input ->
                targetFile.outputStream().use { output ->
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        val progress = bytesCopied.toFloat() / totalBytes
                        if (progress - lastProgress >= 0.001f) {
                            onProgress(progress)
                            lastProgress = progress
                        }
                        bytes = input.read(buffer)
                    }
                }
            }
        }
        if (lastProgress < 1f) onProgress(1f)
    }
}

fun ZipFile.contains(entry: String): Boolean = use { entries().toList().firstOrNull { it.name.startsWith(entry) } != null }
inline fun ZipFile.runOperation(noinline condition: (ZipEntry) -> Boolean, operation: (ZipEntry, ZipFile) -> Unit) = with(this) {
    entries().asSequence().filter(condition).forEach { operation(it, this) }
}

fun MutableState<String>.addLine(string: String) { this.value += string + "\n" }

fun Context.browse(url: String) {
    val customTabsIntent = CustomTabsIntent.Builder().build()
    customTabsIntent.launchUrl(this, url.toUri())
}