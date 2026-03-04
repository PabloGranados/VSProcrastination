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
        val POMODORO_LEVEL_KEY = intPreferencesKey("pomodoro_level_index")
        val POMODORO_SESSIONS_AT_LEVEL_KEY = intPreferencesKey("pomodoro_sessions_at_level")
        val POMODORO_HAS_CALIBRATED_KEY = booleanPreferencesKey("pomodoro_has_calibrated")
        val POMODORO_BREAK_DURATION_KEY = intPreferencesKey("pomodoro_break_duration_minutes")
        val POMODORO_IS_CUSTOM_KEY = booleanPreferencesKey("pomodoro_is_custom")
        val POMODORO_TOTAL_SESSIONS_KEY = intPreferencesKey("pomodoro_total_sessions")
        
        // Notificaciones
        val NAGGING_ENABLED_KEY = booleanPreferencesKey("nagging_enabled")
        val DEADLINE_REMINDERS_KEY = booleanPreferencesKey("deadline_reminders_enabled")
        
        // Tema
        val DARK_MODE_KEY = stringPreferencesKey("dark_mode") // "system", "light", "dark"
        
        // Sync
        val SYNC_ENABLED_KEY = booleanPreferencesKey("sync_enabled")
        val LAST_SYNC_TIMESTAMP_KEY = longPreferencesKey("last_sync_timestamp")
        
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
    
    // --- Pomodoro Level (sistema progresivo) ---
    val pomodoroLevel: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[POMODORO_LEVEL_KEY] ?: 1 // Default: nivel 1 (25 min clásico)
    }
    
    suspend fun setPomodoroLevel(levelIndex: Int) {
        context.dataStore.edit { prefs ->
            prefs[POMODORO_LEVEL_KEY] = levelIndex.coerceIn(0, 6)
        }
    }
    
    val pomodoroSessionsAtLevel: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[POMODORO_SESSIONS_AT_LEVEL_KEY] ?: 0
    }
    
    suspend fun setPomodoroSessionsAtLevel(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[POMODORO_SESSIONS_AT_LEVEL_KEY] = count
        }
    }
    
    suspend fun incrementPomodoroSessionsAtLevel() {
        context.dataStore.edit { prefs ->
            val current = prefs[POMODORO_SESSIONS_AT_LEVEL_KEY] ?: 0
            prefs[POMODORO_SESSIONS_AT_LEVEL_KEY] = current + 1
        }
    }
    
    val pomodoroHasCalibrated: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[POMODORO_HAS_CALIBRATED_KEY] ?: false
    }
    
    suspend fun setPomodoroHasCalibrated(calibrated: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[POMODORO_HAS_CALIBRATED_KEY] = calibrated
        }
    }
    
    val pomodoroBreakDuration: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[POMODORO_BREAK_DURATION_KEY] ?: 5
    }
    
    suspend fun setPomodoroBreakDuration(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[POMODORO_BREAK_DURATION_KEY] = minutes.coerceIn(1, 30)
        }
    }
    
    val pomodoroIsCustom: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[POMODORO_IS_CUSTOM_KEY] ?: false
    }
    
    suspend fun setPomodoroIsCustom(custom: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[POMODORO_IS_CUSTOM_KEY] = custom
        }
    }
    
    val pomodoroTotalSessions: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[POMODORO_TOTAL_SESSIONS_KEY] ?: 0
    }
    
    suspend fun incrementPomodoroTotalSessions() {
        context.dataStore.edit { prefs ->
            val current = prefs[POMODORO_TOTAL_SESSIONS_KEY] ?: 0
            prefs[POMODORO_TOTAL_SESSIONS_KEY] = current + 1
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
    
    // --- Sync ---
    val syncEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SYNC_ENABLED_KEY] ?: false
    }
    
    suspend fun setSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SYNC_ENABLED_KEY] = enabled
        }
    }
    
    val lastSyncTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_SYNC_TIMESTAMP_KEY] ?: 0L
    }
    
    suspend fun setLastSyncTimestamp(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            prefs[LAST_SYNC_TIMESTAMP_KEY] = timestamp
        }
    }
}
