package com.selauraclient.launcher.utils

import android.content.Context
import android.os.Build
import android.os.FileObserver
import android.os.FileObserver.CREATE
import android.os.FileObserver.DELETE
import android.os.FileObserver.MOVED_FROM
import android.os.FileObserver.MOVED_TO
import com.selauraclient.launcher.global.Data

class DownloadsFolderWatcher(context: Context) {
    private var fileObserver: FileObserver? = null
    private val downloads = context.filesDir.resolve("downloads")
    init {
        if (!downloads.exists()) {
            downloads.mkdirs()
        }
        val onEvent = { _: Int, path: String? ->
            if (path != null) {
                downloads.listFiles { file -> file.isDirectory && file.name.toLongOrNull() != null }?.map { it.name.toLong() }?.toLongArray()?.let {
                    Data.downloadedVersions.clear()
                    Data.downloadedVersions.addAll(it.toList())
                }
            }
        }
        onEvent(0, "")

        val mask = CREATE or DELETE or MOVED_FROM or MOVED_TO
        fileObserver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(downloads, mask) {
                override fun onEvent(event: Int, path: String?) {
                    onEvent(event, path)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            object : FileObserver(downloads.absolutePath, mask) {
                override fun onEvent(event: Int, path: String?) {
                    onEvent(event, path)
                }
            }
        }.apply { startWatching() }
    }

    fun stopWatching() {
        fileObserver?.stopWatching()
        fileObserver = null
    }
}