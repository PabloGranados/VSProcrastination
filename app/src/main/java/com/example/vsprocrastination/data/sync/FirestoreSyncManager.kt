package com.example.vsprocrastination.data.sync

import com.example.vsprocrastination.data.dao.SubtaskDao
import com.example.vsprocrastination.data.dao.TaskDao
import com.example.vsprocrastination.data.model.Difficulty
import com.example.vsprocrastination.data.model.Priority
import com.example.vsprocrastination.data.model.Subtask
import com.example.vsprocrastination.data.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Gestor de sincronización entre Room (local) y Firestore (nube).
 * 
 * ARQUITECTURA:
 * - Room es la fuente de verdad local (offline-first)
 * - Firestore almacena una copia en la nube por usuario
 * - La sincronización usa merge bidireccional con last-write-wins
 * 
 * ESTRUCTURA FIRESTORE:
 * users/{userId}/tasks/{firebaseId}
 *   ├── name, difficulty, priority, etc.
 *   └── subtasks: [ { name, isCompleted, sortOrder } ]
 * 
 * RESOLUCIÓN DE CONFLICTOS:
 * Si dos dispositivos modifican la misma tarea, gana el que tiene
 * lastModifiedAt más reciente (last-write-wins).
 */
class FirestoreSyncManager(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao
) {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    private val userId: String?
        get() = auth.currentUser?.uid
    
    private fun tasksCollection(uid: String) =
        firestore.collection("users").document(uid).collection("tasks")
    
    // === Estado de autenticación ===
    
    fun isSignedIn(): Boolean = auth.currentUser != null
    
    fun getCurrentUserEmail(): String? = auth.currentUser?.email
    
    fun getCurrentUserName(): String? = auth.currentUser?.displayName
    
    fun getCurrentUserPhotoUrl(): String? = auth.currentUser?.photoUrl?.toString()
    
    // === Sincronización completa ===
    
    /**
     * Sincronización completa: push local → pull remote.
     * Llamar al iniciar sesión o al presionar "Sincronizar".
     */
    suspend fun syncAll(): SyncResult {
        val uid = userId ?: return SyncResult(success = false, message = "No autenticado")
        
        return try {
            val pushed = pushLocalChanges(uid)
            val pulled = pullRemoteChanges(uid)
            SyncResult(
                success = true,
                message = "✅ $pushed subidas, $pulled bajadas",
                pushedCount = pushed,
                pulledCount = pulled
            )
        } catch (e: Exception) {
            SyncResult(success = false, message = "❌ Error: ${e.localizedMessage}")
        }
    }
    
    /**
     * Sube todos los cambios locales a Firestore.
     */
    private suspend fun pushLocalChanges(uid: String): Int {
        val tasks = taskDao.getAllTasksSync()
        var count = 0
        
        for (task in tasks) {
            val subtasks = subtaskDao.getSubtasksForTaskSync(task.id)
            val data = taskToMap(task, subtasks)
            
            if (task.firebaseId != null) {
                // Tarea ya existe en Firestore → actualizar
                tasksCollection(uid).document(task.firebaseId).set(data).await()
            } else {
                // Tarea nueva → crear en Firestore y guardar el ID
                val docRef = tasksCollection(uid).add(data).await()
                taskDao.updateFirebaseId(task.id, docRef.id)
            }
            count++
        }
        return count
    }
    
    /**
     * Descarga cambios remotos de Firestore a Room.
     * Solo actualiza si el remoto es más reciente que el local.
     */
    private suspend fun pullRemoteChanges(uid: String): Int {
        val snapshot = tasksCollection(uid).get().await()
        var count = 0
        
        val localFirebaseIds = taskDao.getAllTasksSync()
            .mapNotNull { it.firebaseId }
            .toSet()
        
        for (doc in snapshot.documents) {
            val firebaseId = doc.id
            
            if (firebaseId in localFirebaseIds) {
                // Tarea existe localmente → merge si remoto es más reciente
                val localTask = taskDao.getTaskByFirebaseId(firebaseId) ?: continue
                val remoteModified = doc.getLong("lastModifiedAt") ?: 0
                
                if (remoteModified > localTask.lastModifiedAt) {
                    val updated = mapToTask(doc).copy(id = localTask.id)
                    taskDao.updateTask(updated)
                    syncSubtasksFromRemote(localTask.id, doc)
                    count++
                }
            } else {
                // Tarea nueva del otro dispositivo → insertar localmente
                val newTask = mapToTask(doc)
                val taskId = taskDao.insertTask(newTask)
                syncSubtasksFromRemote(taskId, doc)
                count++
            }
        }
        
        return count
    }
    
    /**
     * Sincroniza las subtareas de un documento Firestore a Room.
     */
    private suspend fun syncSubtasksFromRemote(localTaskId: Long, doc: DocumentSnapshot) {
        subtaskDao.deleteAllForTask(localTaskId)
        
        @Suppress("UNCHECKED_CAST")
        val subtasksList = doc.get("subtasks") as? List<Map<String, Any>> ?: return
        
        for ((index, sub) in subtasksList.withIndex()) {
            subtaskDao.insertSubtask(
                Subtask(
                    taskId = localTaskId,
                    name = sub["name"] as? String ?: "",
                    isCompleted = sub["isCompleted"] as? Boolean ?: false,
                    sortOrder = (sub["sortOrder"] as? Number)?.toInt() ?: index
                )
            )
        }
    }
    
    // === Sync incremental (una sola tarea) ===
    
    /**
     * Sube una sola tarea a Firestore.
     * Llamar después de crear/editar/completar una tarea.
     */
    suspend fun pushSingleTask(task: Task) {
        val uid = userId ?: return
        
        try {
            val subtasks = subtaskDao.getSubtasksForTaskSync(task.id)
            val data = taskToMap(task, subtasks)
            
            if (task.firebaseId != null) {
                tasksCollection(uid).document(task.firebaseId).set(data).await()
            } else {
                val docRef = tasksCollection(uid).add(data).await()
                taskDao.updateFirebaseId(task.id, docRef.id)
            }
        } catch (_: Exception) {
            // Sync silencioso — no bloquear la UX por error de red
        }
    }
    
    /**
     * Sube una tarea por su ID local.
     */
    suspend fun pushTaskById(taskId: Long) {
        val task = taskDao.getTaskById(taskId) ?: return
        pushSingleTask(task)
    }
    
    /**
     * Elimina una tarea de Firestore.
     */
    suspend fun deleteRemoteTask(firebaseId: String?) {
        val uid = userId ?: return
        firebaseId?.let {
            try {
                tasksCollection(uid).document(it).delete().await()
            } catch (_: Exception) { }
        }
    }
    
    // === Conversiones ===
    
    private fun taskToMap(task: Task, subtasks: List<Subtask>): HashMap<String, Any?> = hashMapOf(
        "name" to task.name,
        "deadlineMillis" to task.deadlineMillis,
        "difficulty" to task.difficulty.name,
        "priority" to task.priority.name,
        "isCompleted" to task.isCompleted,
        "isStarted" to task.isStarted,
        "isQuickTask" to task.isQuickTask,
        "createdAt" to task.createdAt,
        "completedAt" to task.completedAt,
        "totalTimeWorkedMillis" to task.totalTimeWorkedMillis,
        "lastModifiedAt" to task.lastModifiedAt,
        "subtasks" to subtasks.map { sub ->
            hashMapOf(
                "name" to sub.name,
                "isCompleted" to sub.isCompleted,
                "sortOrder" to sub.sortOrder
            )
        }
    )
    
    private fun mapToTask(doc: DocumentSnapshot): Task = Task(
        firebaseId = doc.id,
        name = doc.getString("name") ?: "",
        deadlineMillis = doc.getLong("deadlineMillis"),
        difficulty = try {
            Difficulty.valueOf(doc.getString("difficulty") ?: "EASY")
        } catch (_: Exception) { Difficulty.EASY },
        priority = try {
            Priority.valueOf(doc.getString("priority") ?: "NORMAL")
        } catch (_: Exception) { Priority.NORMAL },
        isCompleted = doc.getBoolean("isCompleted") ?: false,
        isStarted = doc.getBoolean("isStarted") ?: false,
        isQuickTask = doc.getBoolean("isQuickTask") ?: false,
        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
        completedAt = doc.getLong("completedAt"),
        totalTimeWorkedMillis = doc.getLong("totalTimeWorkedMillis") ?: 0,
        lastModifiedAt = doc.getLong("lastModifiedAt") ?: 0
    )
    
    /**
     * Resultado de una operación de sincronización.
     */
    data class SyncResult(
        val success: Boolean,
        val message: String,
        val pushedCount: Int = 0,
        val pulledCount: Int = 0
    )
}
