package com.selauraclient.launcher

import android.util.Log

val isDebug = BuildConfig.DEBUG
private const val TAG = "DownloadService"
fun logI(message: String, throwable: Throwable? = null) {
    if (isDebug) Log.i(TAG, message, throwable)
}

fun logD(message: String) {
    if (isDebug) Log.d(TAG, message)
}

fun logW(message: String) {
    if (isDebug) Log.w(TAG, message)
}

fun logE(message: String, throwable: Throwable? = null) {
    if (isDebug) Log.e(TAG, message, throwable)
}