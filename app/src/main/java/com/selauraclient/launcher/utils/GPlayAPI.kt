package com.selauraclient.launcher.utils

import android.accounts.AuthenticatorException
import com.aurora.gplayapi.Constants
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.selauraclient.launcher.global.Data.authData
import com.selauraclient.launcher.global.MINECRAFT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ApkData(
    val url: String,
    val name: String,
    val size: Long
)

suspend fun getApks(versionCode: Int): List<ApkData> = withContext(Dispatchers.IO) {
    PurchaseHelper( authData.value ?: throw AuthenticatorException("No login info")).purchase(
        MINECRAFT,
        versionCode,
        0,
        patchFormat = Constants.PatchFormat.GZIPPED_GDIFF
    ).map { ApkData(it.url, it.name, it.size) }
}