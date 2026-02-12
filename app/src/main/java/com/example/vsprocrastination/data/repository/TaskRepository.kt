package com.example.vsprocrastination.data.repository

import com.example.vsprocrastination.data.dao.SubtaskDao
import com.example.vsprocrastination.data.dao.TaskDao
import com.example.vsprocrastination.data.model.Difficulty
import com.example.vsprocrastination.data.model.Priority
import com.example.vsprocrastination.data.model.Subtask
import com.example.vsprocrastination.data.model.Task
import com.example.vsprocrastination.data.sync.FirestoreSyncManager
import kotlinx.coroutines.flow.Flow

/**
 * Repository para abstraer el acceso a datos.
 * Facilita testing y posibles fuentes de datos futuras (sync con backend, etc).
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    val syncManager: FirestoreSyncManager? = null
) {
    
    /**
     * Todas las tareas como Flow reactivo.
     */
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    
    /**
     * Solo tareas pendientes.
     */
    val pendingTasks: Flow<List<Task>> = taskDao.getPendingTasks()
    
    /**
     * Conteo de tareas pendientes para badges/notificaciones.
     */
    val pendingTasksCount: Flow<Int> = taskDao.getPendingTasksCount()
    
    /**
     * Crea una nueva tarea con mínima fricción.
     * Solo requiere el nombre, todo lo demás es opcional.
     */
    suspend fun createTask(
        name: String,
        deadlineMillis: Long? = null,
        difficulty: Difficulty = Difficulty.EASY,
        priority: Priority = Priority.NORMAL,
        isQuickTask: Boolean = false,
        subtaskNames: List<String> = emptyList()
    ): Long {
        val task = Task(
            name = name.trim(),
            deadlineMillis = deadlineMillis,
            difficulty = difficulty,
            priority = priority,
            isQuickTask = isQuickTask
        )
        val taskId = taskDao.insertTask(task)
        
        // Insertar subtareas si las hay
        if (subtaskNames.isNotEmpty()) {
            val subtasks = subtaskNames.mapIndexed { index, subtaskName ->
                Subtask(taskId = taskId, name = subtaskName.trim(), sortOrder = index)
            }
            subtaskDao.insertSubtasks(subtasks)
        }
        
        // Sync incremental a Firebase
        syncManager?.pushTaskById(taskId)
        
        return taskId
    }
    
    /**
     * Marca tarea como iniciada (bonus Zeigarnik se activa).
     */
    suspend fun startTask(taskId: Long) {
        taskDao.markAsStarted(taskId)
    }
    
    /**
     * Marca tarea como completada.
     */
    suspend fun completeTask(taskId: Long) {
        taskDao.markAsCompleted(taskId)
        taskDao.updateLastModified(taskId)
        syncManager?.pushTaskById(taskId)
    }
    
    /**
     * Deshace la completación de una tarea (Undo).
     */
    suspend fun undoCompleteTask(taskId: Long) {
        taskDao.undoComplete(taskId)
    }
    
    /**
     * Registra tiempo trabajado (para sesiones Pomodoro).
     */
    suspend fun addTimeWorked(taskId: Long, millis: Long) {
        taskDao.addTimeWorked(taskId, millis)
    }
    
    /**
     * Actualiza una tarea existente.
     */
    suspend fun updateTask(task: Task) {
        val updated = task.copy(lastModifiedAt = System.currentTimeMillis())
        taskDao.updateTask(updated)
        syncManager?.pushSingleTask(updated)
    }
    
    /**
     * Elimina una tarea.
     */
    suspend fun deleteTask(task: Task) {
        syncManager?.deleteRemoteTask(task.firebaseId)
        taskDao.deleteTask(task)
    }
    
    /**
     * Limpia tareas completadas (local y remota).
     */
    suspend fun clearCompletedTasks() {
        // Primero borrar de Firestore las tareas completadas que tengan firebaseId
        val completedTasks = taskDao.getAllTasksSync().filter { it.isCompleted }
        completedTasks.forEach { task ->
            if (task.firebaseId != null) {
                syncManager?.deleteRemoteTask(task.firebaseId)
            }
        }
        taskDao.deleteCompletedTasks()
    }
    
    /**
     * Obtiene una tarea por ID.
     */
    suspend fun getTaskById(taskId: Long): Task? {
        return taskDao.getTaskById(taskId)
    }
    
    // === Subtareas ===
    
    fun getSubtasksForTask(taskId: Long): Flow<List<Subtask>> {
        return subtaskDao.getSubtasksForTask(taskId)
    }
    
    suspend fun getSubtasksForTaskSync(taskId: Long): List<Subtask> {
        return subtaskDao.getSubtasksForTaskSync(taskId)
    }
    
    suspend fun addSubtask(taskId: Long, name: String, sortOrder: Int = 0): Long {
        return subtaskDao.insertSubtask(Subtask(taskId = taskId, name = name.trim(), sortOrder = sortOrder))
    }
    
    suspend fun toggleSubtask(subtaskId: Long, completed: Boolean) {
        subtaskDao.setCompleted(subtaskId, completed)
    }
    
    suspend fun deleteSubtask(subtask: Subtask) {
        subtaskDao.deleteSubtask(subtask)
    }
    
    suspend fun updateSubtasks(taskId: Long, subtaskNames: List<String>) {
        subtaskDao.deleteAllForTask(taskId)
        if (subtaskNames.isNotEmpty()) {
            val subtasks = subtaskNames.mapIndexed { index, name ->
                Subtask(taskId = taskId, name = name.trim(), sortOrder = index)
            }
            subtaskDao.insertSubtasks(subtasks)
        }
    }
    
    suspend fun getSubtaskProgress(taskId: Long): Pair<Int, Int> {
        val total = subtaskDao.getSubtaskCount(taskId)
        val completed = subtaskDao.getCompletedSubtaskCount(taskId)
        return Pair(completed, total)
    }
}
