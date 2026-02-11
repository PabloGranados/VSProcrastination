package com.example.vsprocrastination.data.dao

import androidx.room.*
import com.example.vsprocrastination.data.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones con tareas.
 * Usa Flow para observar cambios reactivamente.
 */
@Dao
interface TaskDao {
    
    /**
     * Obtiene todas las tareas como Flow reactivo.
     * La UI se actualizará automáticamente cuando cambien los datos.
     */
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>
    
    /**
     * Obtiene solo las tareas pendientes (no completadas).
     */
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getPendingTasks(): Flow<List<Task>>
    
    /**
     * Obtiene una tarea por su ID.
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): Task?
    
    /**
     * Inserta una nueva tarea.
     * @return El ID de la tarea insertada
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long
    
    /**
     * Actualiza una tarea existente.
     */
    @Update
    suspend fun updateTask(task: Task)
    
    /**
     * Elimina una tarea.
     */
    @Delete
    suspend fun deleteTask(task: Task)
    
    /**
     * Marca una tarea como iniciada.
     * Para activar el bonus Zeigarnik.
     */
    @Query("UPDATE tasks SET isStarted = 1 WHERE id = :taskId")
    suspend fun markAsStarted(taskId: Long)
    
    /**
     * Marca una tarea como completada.
     */
    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :completedAt WHERE id = :taskId")
    suspend fun markAsCompleted(taskId: Long, completedAt: Long = System.currentTimeMillis())
    
    /**
     * Deshace la completación de una tarea (Undo).
     * Crucial para evitar frustración por taps accidentales.
     */
    @Query("UPDATE tasks SET isCompleted = 0, completedAt = NULL WHERE id = :taskId")
    suspend fun undoComplete(taskId: Long)
    
    /**
     * Actualiza el tiempo trabajado en una tarea (para Pomodoro).
     */
    @Query("UPDATE tasks SET totalTimeWorkedMillis = totalTimeWorkedMillis + :additionalMillis WHERE id = :taskId")
    suspend fun addTimeWorked(taskId: Long, additionalMillis: Long)
    
    /**
     * Elimina todas las tareas completadas (limpieza).
     */
    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks()
    
    /**
     * Obtiene el conteo de tareas pendientes.
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    fun getPendingTasksCount(): Flow<Int>
    
    /**
     * Obtiene tareas pendientes con deadline entre [now] y [cutoff].
     * Query síncrona para uso en Workers (no Flow).
     * Usado por DeadlineCountdownWorker para encontrar tareas a <2h del vencimiento.
     */
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND deadlineMillis IS NOT NULL AND deadlineMillis > :now AND deadlineMillis <= :cutoff")
    suspend fun getTasksWithDeadlineBetween(now: Long, cutoff: Long): List<Task>
    
    // === Queries para sincronización Firebase ===
    
    /**
     * Obtiene todas las tareas de forma síncrona (para sync).
     */
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<Task>
    
    /**
     * Busca una tarea por su Firebase ID.
     */
    @Query("SELECT * FROM tasks WHERE firebaseId = :firebaseId LIMIT 1")
    suspend fun getTaskByFirebaseId(firebaseId: String): Task?
    
    /**
     * Actualiza el Firebase ID de una tarea local.
     */
    @Query("UPDATE tasks SET firebaseId = :firebaseId WHERE id = :taskId")
    suspend fun updateFirebaseId(taskId: Long, firebaseId: String)
    
    /**
     * Actualiza el timestamp de última modificación.
     */
    @Query("UPDATE tasks SET lastModifiedAt = :timestamp WHERE id = :taskId")
    suspend fun updateLastModified(taskId: Long, timestamp: Long = System.currentTimeMillis())
}
