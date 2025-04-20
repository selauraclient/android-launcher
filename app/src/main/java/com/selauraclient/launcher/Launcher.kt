package com.selauraclient.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.GET_META_DATA
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Objects
import java.util.concurrent.Executors
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

const val minecraft = "com.mojang.minecraftpe"
const val launcher = "launcher.dex"

fun Context.startLauncher(snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    val executor = Executors.newSingleThreadExecutor()
    executor.submit {
        val cacheDexDir = File(codeCacheDir, "dex").apply { deleteRecursively() }
        val mcInfo = packageManager.getApplicationInfo(minecraft, GET_META_DATA)
        val pathList = Objects.requireNonNull(classLoader.javaClass.superclass).getDeclaredField("pathList").apply { isAccessible = true }[classLoader]
        val addDexPath = pathList.javaClass.getDeclaredMethod("addDexPath", String::class.java, File::class.java)
        val addNativePath = pathList.javaClass.getDeclaredMethod("addNativePath", MutableCollection::class.java)

        val libDir = File(mcInfo.nativeLibraryDir)
        val libDirList = mutableListOf(libDir.absolutePath)

        if (libDir.listFiles().isNullOrEmpty() || "arm64" !in libDir.absolutePath) {
            var extracted: String? = null
            ZipInputStream(FileInputStream(mcInfo.splitSourceDirs?.firstOrNull { "arm64_v8a" in it } ?: mcInfo.sourceDir)).use { zis ->
                generateSequence { zis.nextEntry }.forEach { entry ->
                    if (!entry.isDirectory && entry.name.startsWith("lib/arm64-v8a/")) {
                        val outFile = File(codeCacheDir, entry.name.removePrefix("lib/arm64-v8a/"))
                        extracted = copyFile(zis, outFile).name
                    }
                    zis.closeEntry()
                }
            }
            if (extracted.isNullOrEmpty()) {
                scope.launch {
                    snackbarHostState.showSnackbar("You are using the wrong architecture")
                }
                return@submit
            }
        }
        addNativePath.invoke(pathList, libDirList)

        copyFile(assets.open(launcher), File(cacheDexDir, launcher)).apply {
            if (setReadOnly()) {
                addDexPath.invoke(pathList, absolutePath, null)
            }
        }

        ZipFile(mcInfo.sourceDir).use { zipFile ->
            (2 downTo 0).forEach { i ->
                zipFile.getEntry("classes${if (i == 0) "" else i}.dex")?.let { dexEntry ->
                    copyFile(zipFile.getInputStream(dexEntry), File(cacheDexDir, dexEntry.name)).apply {
                        if (setReadOnly()) {
                            addDexPath.invoke(pathList, absolutePath, null)
                        }
                    }
                }
            }
        }
        val apks = arrayListOf<String>(mcInfo.sourceDir)
        mcInfo.splitSourceDirs?.asList()?.let { apks.addAll(it) }
        startActivity(Intent(this, classLoader.loadClass("$minecraft.Launcher")).apply {
            putStringArrayListExtra("APKS", apks)
        })
        (this as? Activity)?.finish()
    }
}

private fun copyFile(from: InputStream, to: File): File {
    to.parentFile?.takeIf { !it.exists() }?.also { require(it.mkdirs()) { "Failed to create directories: ${it.absolutePath}" } }
    from.use { Files.copy(it, to.toPath(), StandardCopyOption.REPLACE_EXISTING) }
    return to
}