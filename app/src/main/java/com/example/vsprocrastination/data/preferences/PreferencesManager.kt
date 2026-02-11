package com.example.vsprocrastination.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Gestor de preferencias del usuario.
 * Usa DataStore (recomendado sobre SharedPreferences).
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        // Pomodoro
        val POMODORO_DURATION_KEY = intPreferencesKey("pomodoro_duration_minutes")
        
        // Notificaciones
        val NAGGING_ENABLED_KEY = booleanPreferencesKey("nagging_enabled")
        val DEADLINE_REMINDERS_KEY = booleanPreferencesKey("deadline_reminders_enabled")
        
        // Tema
        val DARK_MODE_KEY = stringPreferencesKey("dark_mode") // "system", "light", "dark"
        
        // Defaults
        const val DEFAULT_POMODORO_MINUTES = 25
        const val DEFAULT_DARK_MODE = "system"
    }
    
    // --- Pomodoro Duration ---
    val pomodoroDuration: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[POMODORO_DURATION_KEY] ?: DEFAULT_POMODORO_MINUTES
    }
    
    suspend fun setPomodoroDuration(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[POMODORO_DURATION_KEY] = minutes.coerceIn(5, 90)
        }
    }
    
    // --- Nagging Notifications ---
    val naggingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NAGGING_ENABLED_KEY] ?: true
    }
    
    suspend fun setNaggingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[NAGGING_ENABLED_KEY] = enabled
        }
    }
    
    // --- Deadline Reminders ---
    val deadlineRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DEADLINE_REMINDERS_KEY] ?: true
    }
    
    suspend fun setDeadlineRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DEADLINE_REMINDERS_KEY] = enabled
        }
    }
    
    // --- Dark Mode ---
    val darkMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE_KEY] ?: DEFAULT_DARK_MODE
    }
    
    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = mode
        }
    }
}
