package com.selauraclient.launcher.utils

import android.annotation.SuppressLint

@SuppressLint("DefaultLocale")
fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024)
    else -> "%.2f GB".format(bytes / 1024.0 / 1024 / 1024)
}

@SuppressLint("DefaultLocale")
fun formatETA(progress: Float, speedKbps: Double, totalBytes: Long): String {
    if (speedKbps <= 0 || progress > 100) return "Unknown"
    val seconds = ((totalBytes * (100 - progress) / 100.0) / (speedKbps * 1024)).toLong()
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%dh %02dm %02ds".format(h, m, s)
    else if (m > 0) "%dm %02ds".format(m, s)
    else "%ds".format(s)
}

@SuppressLint("DefaultLocale")
fun formatSpeed(speedKbps: Double): String = if (speedKbps >= 1024) "%.2f MB/s".format(speedKbps / 1024) else "%.1f KB/s".format(speedKbps)
