package com.personal.readreminder.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class AppSettings(val dailyGoal: Int = 10, val resetMinutes: Int = 0, val displayName: String = "")

class SettingsRepository(private val context: Context) {
    private val goalKey = intPreferencesKey("daily_goal")
    private val resetKey = intPreferencesKey("reset_minutes")
    private val nameKey = stringPreferencesKey("display_name")

    val settings: Flow<AppSettings> = context.dataStore.data.map {
        AppSettings(it[goalKey] ?: 10, it[resetKey] ?: 0, it[nameKey] ?: "")
    }

    suspend fun setDailyGoal(pages: Int) { context.dataStore.edit { it[goalKey] = pages } }
}
