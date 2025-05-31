@file:Suppress("unused", "UNUSED_PARAMETER")

package com.mojang.minecraftpe

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NativeActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import java.io.InputStream
import java.io.OutputStream
@SuppressLint("MissingSuperCall")

open class MainActivity : NativeActivity(), View.OnKeyListener, FilePickerManagerHandler {
    external fun nativeShutdown()

    private fun copyFile(var1: InputStream, var2: OutputStream) {
        throw RuntimeException("stub!")
    }

    private fun createAlertDialog(ok: Boolean, cancel: Boolean, cancellable: Boolean) {
        throw RuntimeException("stub!")
    }

    private fun dismissTextWidget() {
        throw RuntimeException("stub!")
    }

    private val inputMethodManager: InputMethodManager
        get() = throw RuntimeException("stub!")

    private val isTextWidgetActive: Boolean
        get() {
            throw RuntimeException("stub!")
        }

    private fun onDialogCanceled() {
        throw RuntimeException("stub!")
    }

    private fun onDialogCompleted() {
        throw RuntimeException("stub!")
    }

    private fun processIntent(param1: Intent) {
        throw RuntimeException("stub!")
    }

    fun acquireMulticast() {
        throw RuntimeException("stub!")
    }

    open fun buyGame() {
        throw RuntimeException("stub!")
    }

    fun calculateAvailableDiskFreeSpace(var1: String?): Long {
        throw RuntimeException("stub!")
    }

    fun checkLicense(): Int {
        throw RuntimeException("stub!")
    }

    fun chromebookCompatibilityIP(): String {
        throw RuntimeException("stub!")
    }

    fun clearLoginInformation() {
        throw RuntimeException("stub!")
    }

    fun copyFromPickedFile(var1: String) {
        throw RuntimeException("stub!")
    }

    fun copyToPickedFile(var1: String) {
        throw RuntimeException("stub!")
    }

    fun createAndroidLaunchIntent(): Intent? {
        throw RuntimeException("stub!")
    }

    fun createUUID(): String {
        throw RuntimeException("stub!")
    }

    fun deviceIdCorrelationStart() {
        throw RuntimeException("stub!")
    }

    override fun dispatchGenericMotionEvent(var1: MotionEvent): Boolean {
        throw RuntimeException("stub!")
    }

    override fun dispatchKeyEvent(var1: KeyEvent): Boolean {
        throw RuntimeException("stub!")
    }

    fun displayDialog(var1: Int) {
        throw RuntimeException("stub!")
    }

    fun getAPIVersion(var1: String): Int {
        throw RuntimeException("stub!")
    }

    val accessToken: String
        get() = throw RuntimeException("stub!")

    fun getAllocatableBytes(var1: String): Long {
        throw RuntimeException("stub!")
    }

    val androidVersion: Int
        get() = throw RuntimeException("stub!")

    val broadcastAddresses: Array<String>
        get() {
            throw RuntimeException("stub!")
        }

    var caretPosition: Int
        get() = throw RuntimeException("stub!")
        set(var1) {
            throw RuntimeException("stub!")
        }

    val clientId: String
        get() = throw RuntimeException("stub!")

    fun getDebugMemoryInfo(var1: String?): Long {
        throw RuntimeException("stub!")
    }

    val deviceModel: String
        get() = throw RuntimeException("stub!")

    val displayHeight: Int
        get() {
            throw RuntimeException("stub!")
        }

    val displayWidth: Int
        get() {
            throw RuntimeException("stub!")
        }

    open fun getExternalStoragePath(): String {
        return getExternalFilesDir(null as String?)!!.absolutePath
    }

    val freeMemory: Long
        get() {
            throw RuntimeException("stub!")
        }

    val iPAddresses: Array<String>
        get() {
            throw RuntimeException("stub!")
        }

    fun getImageData(param1: String?): IntArray {
        throw RuntimeException("stub!")
    }

    val internalStoragePath: String
        get() = throw RuntimeException("stub!")

    val isRunningInBrowserStack: Boolean
        get() = throw RuntimeException("stub!")

    fun getKeyFromKeyCode(var1: Int, var2: Int, var3: Int): Int {
        throw RuntimeException("stub!")
    }

    val keyboardHeight: Float
        get() = throw RuntimeException("stub!")

    val legacyDeviceID: String
        get() = throw RuntimeException("stub!")

    fun getLegacyExternalStoragePath(var1: String): String {
        throw RuntimeException("stub!")
    }

    val locale: String
        get() = throw RuntimeException("stub!")

    val memoryInfo: ActivityManager.MemoryInfo
        get() {
            throw RuntimeException("stub!")
        }

    val memoryLimit: Long
        get() = throw RuntimeException("stub!")

    val platformDpi: Int
        get() {
            throw RuntimeException("stub!")
        }

    fun getPlatformStringVar(var1: Int): String? {
        throw RuntimeException("stub!")
    }

    val profileId: String
        get() = throw RuntimeException("stub!")

    val profileName: String
        get() = throw RuntimeException("stub!")

    fun getProp(var1: String): String {
        throw RuntimeException("stub!")
    }

    val screenHeight: Int
        get() {
            throw RuntimeException("stub!")
        }

    val screenWidth: Int
        get() {
            throw RuntimeException("stub!")
        }

    fun getSecureStorageKey(var1: String?): String {
        throw RuntimeException("stub!")
    }

    var textBoxBackend: String?
        get() = throw RuntimeException("stub!")
        set(var1) {
            throw RuntimeException("stub!")
        }

    val timeFromProcessStart: Long
        get() = throw RuntimeException("stub!")

    val totalMemory: Long
        get() {
            throw RuntimeException("stub!")
        }

    val usedMemory: Long
        get() {
            throw RuntimeException("stub!")
        }

    fun hasBuyButtonWhenInvalidLicense(): Boolean {
        throw RuntimeException("stub!")
    }

    fun hasHardwareChanged(): Boolean {
        throw RuntimeException("stub!")
    }

    fun hasHardwareKeyboard(): Boolean {
        throw RuntimeException("stub!")
    }

    fun hasWriteExternalStoragePermission(): Boolean {
        throw RuntimeException("stub!")
    }

    fun hideKeyboard() {
        throw RuntimeException("stub!")
    }

    fun initializeMulticast() {
        throw RuntimeException("stub!")
    }

    fun initiateUserInput(var1: Int) {
        throw RuntimeException("stub!")
    }

    val isAndroidAmazon: Boolean
        external get

    val isAndroidChromebook: Boolean
        external get

    val isAndroidTrial: Boolean
        external get

    val isBrazeEnabled: Boolean
        external get

    val isChromebook: Boolean
        get() = throw RuntimeException("stub!")

    protected open val isDemo: Boolean
        get() = throw RuntimeException("stub!")

    val isEduMode: Boolean
        external get

    val isFirstSnooperStart: Boolean
        get() = throw RuntimeException("stub!")

    val isHardwareKeyboardHidden: Boolean
        get() {
            throw RuntimeException("stub!")
        }

    val isMulticastHeld: Boolean
        get() {
            throw RuntimeException("stub!")
        }

    val isPreview: Boolean
        external get

    val isPublishBuild: Boolean
        external get

    val isTTSEnabled: Boolean
        get() {
            throw RuntimeException("stub!")
        }

    val isTTSInstalled: Boolean
        get() {
            throw RuntimeException("stub!")
        }

    val isTablet: Boolean
        get() {
            throw RuntimeException("stub!")
        }

    val isTestInfrastructureDisabled: Boolean
        external get

    val isTextToSpeechInProgress: Boolean
        get() {
            throw RuntimeException("stub!")
        }

    fun launchUri(var1: String?) {
        throw RuntimeException("stub!")
    }

    fun lockCursor() {
        throw RuntimeException("stub!")
    }

    override fun onActivityResult(var1: Int, var2: Int, var3: Intent?) {
        throw RuntimeException("stub!")
    }

    public override fun onCreate(bundle: Bundle?) {
        throw RuntimeException("stub!")
    }

    override fun onDestroy() {
        throw RuntimeException("stub!")
    }

    override fun onKey(var1: View, var2: Int, var3: KeyEvent): Boolean {
        throw RuntimeException("stub!")
    }

    override fun onKeyDown(var1: Int, var2: KeyEvent): Boolean {
        throw RuntimeException("stub!")
    }

    override fun onKeyMultiple(var1: Int, var2: Int, var3: KeyEvent): Boolean {
        throw RuntimeException("stub!")
    }

    override fun onKeyUp(var1: Int, var2: KeyEvent): Boolean {
        throw RuntimeException("stub!")
    }

    public override fun onNewIntent(var1: Intent) {
        throw RuntimeException("stub!")
    }
    external fun nativeSuspend()

    override fun onPause() {
        throw RuntimeException("stub!")
    }

    fun onPickFileSuccess(var1: Boolean) {
        throw RuntimeException("stub!")
    }

    override fun onRequestPermissionsResult(var1: Int, var2: Array<String>, var3: IntArray) {
        throw RuntimeException("stub!")
    }

    override fun onResume() {
        throw RuntimeException("stub!")
    }

    override fun onStart() {
        throw RuntimeException("stub!")
    }

    override fun onStop() {
        throw RuntimeException("stub!")
    }

    override fun onWindowFocusChanged(var1: Boolean) {
        throw RuntimeException("stub!")
    }

    fun openAndroidAppSettings() {
        throw RuntimeException("stub!")
    }

    fun openFile() {
        throw RuntimeException("stub!")
    }

    fun pickImage(var1: Long) {
        throw RuntimeException("stub!")
    }

    fun postScreenshotToFacebook(var1: String?, var2: Int, var3: Int, var4: IntArray?) {
        throw RuntimeException("stub!")
    }

    fun quit() {
        throw RuntimeException("stub!")
    }

    fun releaseMulticast() {
        throw RuntimeException("stub!")
    }

    fun requestIntegrityToken(var1: String?) {
        throw RuntimeException("stub!")
    }

    fun requestStoragePermission(var1: Int) {
        throw RuntimeException("stub!")
    }

    fun resumeGameplayUpdates() {
        throw RuntimeException("stub!")
    }

    fun runNativeCallbackOnUiThread(var1: Long) {
        throw RuntimeException("stub!")
    }

    fun saveFile(var1: String?) {
        throw RuntimeException("stub!")
    }

    fun setCachedDeviceId(var1: String?) {
        throw RuntimeException("stub!")
    }

    fun setClipboard(var1: String?) {
        throw RuntimeException("stub!")
    }

    fun setFileDialogCallback(var1: Long) {
        throw RuntimeException("stub!")
    }

    fun setIsPowerVR(var1: Boolean) {
        throw RuntimeException("stub!")
    }

    fun setKeepScreenOnFlag(var1: Boolean) {
        throw RuntimeException("stub!")
    }

    fun setLoginInformation(var1: String?, var2: String?, var3: String?, var4: String?) {
        throw RuntimeException("stub!")
    }

    fun setMulticastReferenceCounting(var1: Boolean) {
        throw RuntimeException("stub!")
    }

    fun setRefreshToken(var1: String?) {
        throw RuntimeException("stub!")
    }

    fun setSecureStorageKey(var1: String?, var2: String?) {
        throw RuntimeException("stub!")
    }

    fun setSession(var1: String?) {
        throw RuntimeException("stub!")
    }

    fun setTextToSpeechEnabled(var1: Boolean) {
        throw RuntimeException("stub!")
    }

    fun setVolume(var1: Float) {
        throw RuntimeException("stub!")
    }

    fun setupKeyboardViews(var1: String?, var2: Int, var3: Boolean, var4: Boolean, var5: Boolean) {
        throw RuntimeException("stub!")
    }

    fun share(var1: String?, var2: String?, var3: String?) {
        throw RuntimeException("stub!")
    }

    fun shareFile(var1: String?, var2: String?, var3: String) {
        throw RuntimeException("stub!")
    }

    fun showKeyboard(var1: String?, var2: Int, var3: Boolean, var4: Boolean, var5: Boolean) {
        throw RuntimeException("stub!")
    }

    fun startTextToSpeech(var1: String?) {
        throw RuntimeException("stub!")
    }

    fun statsTrackEvent(var1: String?, var2: String?) {
        throw RuntimeException("stub!")
    }

    fun statsUpdateUserData(var1: String?, var2: String?) {
        throw RuntimeException("stub!")
    }

    fun stopTextToSpeech() {
        throw RuntimeException("stub!")
    }

    fun supportsSizeQuery(var1: String): Boolean {
        throw RuntimeException("stub!")
    }

    fun suspendGameplayUpdates() {
        throw RuntimeException("stub!")
    }

    fun throwRuntimeExceptionFromNative(var1: String?) {
        throw RuntimeException("stub!")
    }

    fun tick() {
        throw RuntimeException("stub!")
    }

    fun unlockCursor() {
        throw RuntimeException("stub!")
    }

    fun updateLocalization(var1: String?, var2: String?) {
        throw RuntimeException("stub!")
    }

    fun vibrate(var1: Int) {
        throw RuntimeException("stub!")
    }

    inner class HeadsetConnectionReceiver(val t: MainActivity) :
        BroadcastReceiver() {
        override fun onReceive(var1: Context, var2: Intent) {
            throw RuntimeException("stub!")
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("HandlerLeak")
    internal inner class IncomingHandler(val t: MainActivity) : Handler() {
        override fun handleMessage(var1: Message) {
            throw RuntimeException("stub!")
        }
    }


    companion object {
        fun saveScreenshot(param0: String?, param1: Int, param2: Int, param3: IntArray?) {
            throw RuntimeException("stub!")
        }
    }

    override fun startPickerActivity(intent: Intent?, i: Int) {
        throw RuntimeException("stub!")
    }
}

internal interface FilePickerManagerHandler {
    fun startPickerActivity(intent: Intent?, i: Int)
}