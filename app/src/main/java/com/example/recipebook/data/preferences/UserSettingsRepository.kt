package com.example.recipebook.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_settings")

data class UserSettings(
    val isAutoCacheEnabled: Boolean = true,
    val isHistoryEnabled: Boolean = true,
    val cacheTtlHours: Int = 24
)

@Singleton
open class UserSettingsRepository {

    private val context: Context?

    // Основной конструктор для Hilt
    @Inject
    constructor(@ApplicationContext context: Context) {
        this.context = context
    }

    // Конструктор для Unit-тестов (без вызова Android Context)
    constructor(forTest: Boolean) {
        this.context = null
    }

    private object Keys {
        val AUTO_CACHE = booleanPreferencesKey("auto_cache_enabled")
        val HISTORY = booleanPreferencesKey("history_enabled")
        val CACHE_TTL_HOURS = intPreferencesKey("cache_ttl_hours")
    }

    open val settingsFlow: Flow<UserSettings>
        get() = context?.dataStore?.data?.map { pref ->
            UserSettings(
                isAutoCacheEnabled = pref[Keys.AUTO_CACHE] ?: true,
                isHistoryEnabled = pref[Keys.HISTORY] ?: true,
                cacheTtlHours = pref[Keys.CACHE_TTL_HOURS] ?: 24
            )
        } ?: flowOf(UserSettings())

    open suspend fun setAutoCache(enabled: Boolean) {
        context?.dataStore?.edit { it[Keys.AUTO_CACHE] = enabled }
    }

    open suspend fun setHistoryEnabled(enabled: Boolean) {
        context?.dataStore?.edit { it[Keys.HISTORY] = enabled }
    }

    open suspend fun setCacheTtlHours(hours: Int) {
        context?.dataStore?.edit { it[Keys.CACHE_TTL_HOURS] = hours }
    }
}