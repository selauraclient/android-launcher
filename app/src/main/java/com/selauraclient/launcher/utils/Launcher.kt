package com.selauraclient.launcher.utils

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.content.pm.PackageManager.GET_META_DATA
import androidx.compose.ui.util.fastForEach
import com.selauraclient.launcher.global.Data.launcher
import com.selauraclient.launcher.global.Data.launcherLoading
import com.selauraclient.launcher.global.Data.logs
import com.selauraclient.launcher.global.LAUNCHER
import com.selauraclient.launcher.global.LIBS
import com.selauraclient.launcher.global.MINECRAFT
import com.selauraclient.launcher.global.Version
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Objects
import java.util.concurrent.Executors
import java.util.zip.ZipFile

data class McInfo(val apks: List<String>, val nativeLibraryDir: String? = null)

private fun String.sanitizePath(): String = "${if (contains("selaura")) "Selaura" else "Minecraft"}/${substringAfter("com.").substringAfter("/")}"

private fun copyFile(from: InputStream, to: File): File {
    to.parentFile?.takeIf { !it.exists() }?.also { require(it.mkdirs()) { "Failed to create directories: ${it.absolutePath}" } }
    from.use { Files.copy(it, to.toPath(), StandardCopyOption.REPLACE_EXISTING) }
    return to
}

data class Launcher(val context: Context, val libsDirs: List<String>, val dexFiles: List<String>, val apks: ArrayList<String>) {
    fun getIntent(): Intent {
        return context.getIntent()
    }
    private fun Context.getIntent(): Intent {
        val pathList = Objects.requireNonNull(classLoader.javaClass.superclass).getDeclaredField("pathList").apply { isAccessible = true }[classLoader]
        val addDexPath = pathList.javaClass.getDeclaredMethod("addDexPath", String::class.java, File::class.java)
        val addNativePath = pathList.javaClass.getDeclaredMethod("addNativePath", MutableCollection::class.java)

        addNativePath.invoke(pathList, libsDirs)
        libsDirs.forEach {
            logs.addLine("added ${it.sanitizePath()} to native path")
        }

        dexFiles.fastForEach {
            if (File(it).setReadOnly()) {
                addDexPath.invoke(pathList, it, null)
                logs.addLine("added ${it.sanitizePath()} to dex path")
            }
        }
        return Intent(context, classLoader.loadClass("$MINECRAFT.Selaura")).apply {
            putStringArrayListExtra("APKS", apks)
            flags = FLAG_ACTIVITY_NO_ANIMATION
        }
    }
}

fun Context.getLauncher(version: Version, status: String, onFinish: (Launcher) -> Unit = {}) {
    if (status == "Installed") {
        getLauncher(onFinish = onFinish)
    } else {
        getLauncher(filesDir.resolve("downloads/${version.versionCode}"), onFinish)
    }
}

fun Context.getLauncher(folder: File? = null, onFinish: (Launcher) -> Unit = {}) {
    Executors.newSingleThreadExecutor().execute {
        try {
            launcherLoading.value = true
            val mcInfo = if (folder != null) {
                folder.listFiles()?.map { it.absolutePath }.run {
                    if (this != null) McInfo(this) else throw Exception("Failed to load Minecraft: No Apk found")
                }
            } else {
                val appInfo = packageManager.getApplicationInfo(MINECRAFT, GET_META_DATA)
                McInfo(
                    mutableListOf<String>().apply {
                        add(appInfo.sourceDir)
                        appInfo.splitSourceDirs?.let { addAll(it) }
                    },
                    appInfo.nativeLibraryDir
                )
            }
            val libApk = mcInfo.apks.firstOrNull { ZipFile(it).contains(LIBS) }
            if (libApk == null) throw Exception("Failed to load Minecraft: Not arm64-v8a")
            val baseApk = mcInfo.apks.firstOrNull { ZipFile(it).contains("classes.dex") }
            
            val cacheDexDir = codeCacheDir.apply { if (deleteRecursively()) logs.addLine("deleted cache") }

            val nativeDir = mcInfo.nativeLibraryDir
            val libsDir = codeCacheDir.resolve(LIBS)
            val libsPaths = mutableListOf<String>(libsDir.absolutePath)

            if (nativeDir == null || File(nativeDir).listFiles()?.isEmpty() == true || !File(nativeDir).name.contains("arm64")) {
                ZipFile(libApk).runOperation({ !it.isDirectory && it.name.startsWith(LIBS) }) { entry, zipFile ->
                    if (!copyFile(zipFile.getInputStream(entry), File(codeCacheDir, entry.name)).exists()) throw Exception("Failed to extract ${entry.name}")
                    logs.addLine("Extracted ${entry.name.removePrefix(LIBS)}")
                }
            } else libsPaths.add(nativeDir)

            dataDir.resolve("Selaura/libSelaura.so").copyTo(libsDir.resolve("libSelaura.so"))

            val dexFiles = mutableListOf(copyFile(assets.open(LAUNCHER), File(cacheDexDir, LAUNCHER)).absolutePath)

            ZipFile(baseApk).runOperation({ it.name.endsWith(".dex") && !it.name.contains("/") }) { entry, zipFile ->
                copyFile(zipFile.getInputStream(entry), File(cacheDexDir, entry.name)).run {
                    dexFiles.add(this.absolutePath)
                }
            }

            mcInfo.apks.forEach { logs.addLine("added ${it.sanitizePath()} to apk list") }

            launcher.value = Launcher(this, libsPaths, dexFiles, ArrayList(mcInfo.apks)).apply { onFinish(this) }
            launcherLoading.value = false
        } catch (e: Exception) {
            e.printStackTrace()
            logs.addLine(if (e.cause != null) e.cause.toString() else e.toString())
        }
    }
}