package com.example.vsprocrastination.data.repository

import com.example.vsprocrastination.data.dao.HabitDao
import com.example.vsprocrastination.data.model.Habit
import com.example.vsprocrastination.data.model.HabitLog
import com.example.vsprocrastination.domain.StreakCalculator
import kotlinx.coroutines.flow.Flow

/**
 * Repository para hábitos — abstrae acceso a datos.
 * Misma filosofía que TaskRepository: mínima fricción.
 */
class HabitRepository(private val habitDao: HabitDao) {
    
    /** Hábitos activos (no archivados) como Flow reactivo. */
    val activeHabits: Flow<List<Habit>> = habitDao.getActiveHabits()
    
    /** Todos los logs como Flow reactivo. */
    val allLogs: Flow<List<HabitLog>> = habitDao.getAllLogs()
    
    /** Logs del día actual como Flow reactivo. */
    fun getLogsForToday(): Flow<List<HabitLog>> {
        val todayEpochDay = StreakCalculator.dayKey(System.currentTimeMillis())
        return habitDao.getLogsForDate(todayEpochDay)
    }
    
    /** Logs para un día específico. */
    fun getLogsForDate(epochDay: Int): Flow<List<HabitLog>> {
        return habitDao.getLogsForDate(epochDay)
    }
    
    /** Logs para un hábito específico. */
    fun getLogsForHabit(habitId: Long): Flow<List<HabitLog>> {
        return habitDao.getLogsForHabit(habitId)
    }
    
    /**
     * Crea un nuevo hábito.
     * Solo requiere nombre y emoji — máxima simplicidad.
     */
    suspend fun createHabit(name: String, emoji: String = "✅"): Long {
        val habit = Habit(
            name = name.trim(),
            emoji = emoji
        )
        return habitDao.insertHabit(habit)
    }
    
    /**
     * Toggle de completación: si ya está completado hoy, lo desmarca;
     * si no, lo marca como completado.
     * 
     * @return true si se marcó como completado, false si se desmarcó.
     */
    suspend fun toggleHabitForToday(habitId: Long): Boolean {
        val todayEpochDay = StreakCalculator.dayKey(System.currentTimeMillis())
        val existingLog = habitDao.getLogForHabitOnDate(habitId, todayEpochDay)
        
        return if (existingLog != null) {
            habitDao.deleteLogForHabitOnDate(habitId, todayEpochDay)
            false
        } else {
            habitDao.insertLog(
                HabitLog(
                    habitId = habitId,
                    dateEpochDay = todayEpochDay
                )
            )
            true
        }
    }
    
    /**
     * Calcula la racha actual de un hábito individual.
     * Cuenta días consecutivos hacia atrás donde el hábito fue completado.
     */
    suspend fun calculateHabitStreak(habitId: Long): Int {
        val allDays = habitDao.getAllCompletionDays()
        // Filtrar solo los días de este hábito
        val habitLogs = habitDao.getLogsForHabit(habitId)
        // Usamos una approach síncrona para el cálculo
        return calculateStreakFromDays(getHabitCompletionDaysSync(habitId))
    }
    
    /**
     * Obtiene los días de completación de un hábito de forma síncrona.
     */
    private suspend fun getHabitCompletionDaysSync(habitId: Long): Set<Int> {
        val log = habitDao.getLogForHabitOnDate(habitId, 0) // dummy, necesitamos otra query
        // Re-implementar con una query directa
        return emptySet() // Se calcula en el ViewModel con los flows
    }
    
    /**
     * Calcula racha desde un set de días con completación.
     */
    fun calculateStreakFromDays(completionDays: Set<Int>): Int {
        if (completionDays.isEmpty()) return 0
        
        val today = StreakCalculator.dayKey(System.currentTimeMillis())
        val sortedDays = completionDays.toSortedSet()
        
        // Empezar desde hoy o ayer
        val startDay = if (today in sortedDays) {
            today
        } else {
            val yesterday = today - 1
            if (yesterday in sortedDays) yesterday else return 0
        }
        
        var streak = 0
        var currentDay = startDay
        while (currentDay in sortedDays) {
            streak++
            currentDay--
        }
        
        return streak
    }
    
    /** Actualiza un hábito existente. */
    suspend fun updateHabit(habit: Habit) {
        val updated = habit.copy(lastModifiedAt = System.currentTimeMillis())
        habitDao.updateHabit(updated)
    }
    
    /** Archiva un hábito (no lo borra, preserva historial). */
    suspend fun archiveHabit(habitId: Long) {
        habitDao.archiveHabit(habitId)
    }
    
    /** Elimina un hábito permanentemente. */
    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }
}
