package com.example.vsprocrastination.data.dao

import androidx.room.*
import com.example.vsprocrastination.data.model.Habit
import com.example.vsprocrastination.data.model.HabitLog
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones con hábitos y sus logs de completación.
 * Usa Flow para observar cambios reactivamente (igual que TaskDao).
 */
@Dao
interface HabitDao {
    
    // ===== Operaciones con Hábitos =====
    
    /** Obtiene todos los hábitos activos (no archivados) como Flow reactivo. */
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getActiveHabits(): Flow<List<Habit>>
    
    /** Obtiene todos los hábitos (incluidos archivados). */
    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun getAllHabits(): Flow<List<Habit>>
    
    /** Obtiene un hábito por ID. */
    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitById(habitId: Long): Habit?
    
    /** Inserta un nuevo hábito. @return ID del hábito insertado. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long
    
    /** Actualiza un hábito existente. */
    @Update
    suspend fun updateHabit(habit: Habit)
    
    /** Elimina un hábito (y sus logs por CASCADE). */
    @Delete
    suspend fun deleteHabit(habit: Habit)
    
    /** Archiva un hábito (soft delete). */
    @Query("UPDATE habits SET isArchived = 1, lastModifiedAt = :now WHERE id = :habitId")
    suspend fun archiveHabit(habitId: Long, now: Long = System.currentTimeMillis())
    
    // ===== Operaciones con HabitLogs =====
    
    /** Obtiene los logs de un día específico (epoch day). */
    @Query("SELECT * FROM habit_logs WHERE dateEpochDay = :epochDay")
    fun getLogsForDate(epochDay: Int): Flow<List<HabitLog>>
    
    /** Obtiene todos los logs de un hábito específico. */
    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY dateEpochDay DESC")
    fun getLogsForHabit(habitId: Long): Flow<List<HabitLog>>
    
    /** Obtiene todos los logs (para cálculos de calendario/racha). */
    @Query("SELECT * FROM habit_logs ORDER BY dateEpochDay DESC")
    fun getAllLogs(): Flow<List<HabitLog>>
    
    /** Verifica si un hábito ya fue completado en un día específico. */
    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND dateEpochDay = :epochDay LIMIT 1")
    suspend fun getLogForHabitOnDate(habitId: Long, epochDay: Int): HabitLog?
    
    /** Inserta un log de completación. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLog): Long
    
    /** Elimina un log específico (para "desmarcar" un hábito). */
    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND dateEpochDay = :epochDay")
    suspend fun deleteLogForHabitOnDate(habitId: Long, epochDay: Int)
    
    /** Elimina un log por ID. */
    @Delete
    suspend fun deleteLog(log: HabitLog)
    
    /**
     * Conteo de logs por día en un rango (para el mapa de calor).
     * Devuelve pares (dateEpochDay, count).
     */
    @Query("""
        SELECT dateEpochDay, COUNT(*) as count 
        FROM habit_logs 
        WHERE dateEpochDay >= :fromEpochDay 
        GROUP BY dateEpochDay
    """)
    suspend fun getCompletionCountsByDay(fromEpochDay: Int): List<DayCompletionCount>
    
    /**
     * Obtiene los días con al menos un hábito completado (para rachas de hábitos).
     */
    @Query("SELECT DISTINCT dateEpochDay FROM habit_logs ORDER BY dateEpochDay ASC")
    suspend fun getAllCompletionDays(): List<Int>
}

/**
 * Data class auxiliar para el resultado de la query de conteo por día.
 */
data class DayCompletionCount(
    val dateEpochDay: Int,
    val count: Int
)
