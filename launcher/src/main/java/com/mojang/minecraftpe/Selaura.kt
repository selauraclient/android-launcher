package com.mojang.minecraftpe

import android.annotation.SuppressLint
import android.os.Bundle
import java.lang.reflect.Method

class Launcher : MainActivity() {
    @SuppressLint("MissingSuperCall")
    class Selaura : MainActivity() {
        @SuppressLint("DiscouragedPrivateApi")
        override fun onCreate(bundle: Bundle?) {
            try {
                val addAssetPath: Method = assets.javaClass.getDeclaredMethod("addAssetPath", String::class.java)
                intent.getStringArrayListExtra("APKS")?.forEach { addAssetPath.invoke(assets, it) }
                super.onCreate(bundle)
            } catch (e: Exception) {
                throw RuntimeException(e)
                e.printStackTrace()
            }
        }

        init {
            System.loadLibrary("Selaura")
            System.getProperty("shader_loader", "")?.let {
                if (it.isNotEmpty()) System.loadLibrary(it)
            }
        }
    }
}
