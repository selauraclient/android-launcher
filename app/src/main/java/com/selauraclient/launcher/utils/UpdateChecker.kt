package com.selauraclient.launcher.utils

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.selauraclient.launcher.global.Data.okHttpClient
import okhttp3.Request
import java.io.IOException

fun checkForUpdate(context: Context, onFinish: (String) -> Unit = {}) {
    val sharedPreferences = context.getSharedPreferences("update_checker", Context.MODE_PRIVATE)
    val lastCheckedDate = sharedPreferences.getString("last_checked", null)
    val requestBuilder = Request.Builder().url("https://cdn.selauraclient.com/android/libSelaura.so")
    val file = context.dataDir.resolve("Selaura/libSelaura.so")
    val head = requestBuilder.head()
    if (file.exists() && lastCheckedDate != null) {
        head.header("If-Modified-Since", lastCheckedDate)
    }

    Thread {
        try {
            okHttpClient.newCall(head.build()).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val newDate = response.header("Date")
                        sharedPreferences.edit { putString("last_checked", newDate) }
                        val request = requestBuilder.get().build()
                        okHttpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful && response.body != null) {
                                file.writeBytes(response.body!!.bytes())
                            }
                            onFinish("Updated")
                            Log.d("UpdateChecker", "Downloaded new version")
                        }

                        Log.d("UpdateChecker", "Updated at: $newDate")
                    }
                    304 -> {
                        onFinish("No new updates")
                        Log.d("UpdateChecker", "Not modified since $lastCheckedDate")
                    }
                    else -> {
                        onFinish("Failed to update")
                        Log.w("UpdateChecker", "Unexpected response: ${response.code}")
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("UpdateChecker", "Network error: ${e.message}")
        }
    }.start()
}