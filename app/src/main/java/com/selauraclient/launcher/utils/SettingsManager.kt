package com.selauraclient.launcher.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.settings by preferencesDataStore("settings")
@Suppress("unused")
class SettingsManager(private val context: Context, private val scope: CoroutineScope) {

    fun getIntAsFlow(key: String): Flow<Int> = context.settings.data.map { it[intPreferencesKey(key)] ?: 0 }

    fun setInt(key: String, value: Int) {
        scope.launch { context.settings.edit { it[intPreferencesKey(key)] = value } }
    }

    fun getStringAsFlow(key: String): Flow<String> = context.settings.data.map { it[stringPreferencesKey(key)] ?: "" }

    suspend fun getString(key: String): String = getStringAsFlow(key).first()

    fun setString(key: String, value: String) {
        scope.launch { context.settings.edit { it[stringPreferencesKey(key)] = value } }
    }

    fun getBooleanAsFlow(key: String): Flow<Boolean> = context.settings.data.map { it[booleanPreferencesKey(key)] ?: false }

    fun setBoolean(key: String, value: Boolean) {
        scope.launch { context.settings.edit { it[booleanPreferencesKey(key)] = value } }
    }

    fun getLongAsFlow(key: String): Flow<Long> = context.settings.data.map { it[longPreferencesKey(key)] ?: 0 }

    fun setLong(key: String, value: Long) {
        scope.launch { context.settings.edit { it[longPreferencesKey(key)] = value } }
    }
}