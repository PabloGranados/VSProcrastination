package com.example.vsprocrastination.data.sync

import android.content.Context
import android.net.Uri
import com.example.vsprocrastination.data.dao.HabitDao
import com.example.vsprocrastination.data.dao.SubtaskDao
import com.example.vsprocrastination.data.dao.TaskDao
import com.example.vsprocrastination.data.model.Difficulty
import com.example.vsprocrastination.data.model.Habit
import com.example.vsprocrastination.data.model.HabitLog
import com.example.vsprocrastination.data.model.Priority
import com.example.vsprocrastination.data.model.Subtask
import com.example.vsprocrastination.data.model.Task
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gestor de exportación/importación de datos en formato JSON.
 *
 * ARQUITECTURA:
 * - Usa Storage Access Framework (SAF) → no requiere permisos de almacenamiento
 * - Exporta tareas, subtareas, hábitos y logs de hábitos
 * - Al importar hace MERGE: no borra datos existentes, solo agrega nuevos
 * - Formato JSON legible y portátil
 *
 * ESTRUCTURA DEL ARCHIVO:
 * {
 *   "version": 1,
 *   "appName": "VS Procrastination",
 *   "exportedAt": "2026-02-23T14:30:00",
 *   "tasks": [...],
 *   "subtasks": [...],
 *   "habits": [...],
 *   "habitLogs": [...]
 * }
 */
class ExportImportManager(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val habitDao: HabitDao
) {
    companion object {
        private const val SCHEMA_VERSION = 1
        private const val APP_NAME = "VS Procrastination"
    }

    /**
     * Exporta todos los datos a un archivo JSON.
     * @param context Context para acceder al ContentResolver
     * @param uri URI del archivo destino (obtenido via SAF ACTION_CREATE_DOCUMENT)
     * @return Resultado con estadísticas de la exportación
     */
    suspend fun exportToJson(context: Context, uri: Uri): ExportImportResult {
        return try {
            val tasks = taskDao.getAllTasksSync()
            val allSubtasks = mutableListOf<Subtask>()
            for (task in tasks) {
                allSubtasks.addAll(subtaskDao.getSubtasksForTaskSync(task.id))
            }

            // Obtener hábitos y logs de forma sincrónica
            val habits = habitDao.getAllHabitsSync()
            val habitLogs = habitDao.getAllLogsSync()

            val json = JSONObject().apply {
                put("version", SCHEMA_VERSION)
                put("appName", APP_NAME)
                put("exportedAt", formatTimestamp(System.currentTimeMillis()))
                put("tasks", tasksToJsonArray(tasks))
                put("subtasks", subtasksToJsonArray(allSubtasks))
                put("habits", habitsToJsonArray(habits))
                put("habitLogs", habitLogsToJsonArray(habitLogs))
            }

            // Escribir al archivo via ContentResolver
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toString(2).toByteArray(Charsets.UTF_8))
            } ?: return ExportImportResult(
                success = false,
                message = "❌ No se pudo abrir el archivo para escritura"
            )

            ExportImportResult(
                success = true,
                message = "✅ Exportadas ${tasks.size} tareas, ${habits.size} hábitos",
                taskCount = tasks.size,
                habitCount = habits.size
            )
        } catch (e: Exception) {
            ExportImportResult(
                success = false,
                message = "❌ Error al exportar: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Importa datos desde un archivo JSON.
     * Usa merge: agrega tareas/hábitos nuevos sin borrar los existentes.
     * Detecta duplicados por nombre + createdAt para evitar insertarlos dos veces.
     *
     * @param context Context para acceder al ContentResolver
     * @param uri URI del archivo origen (obtenido via SAF ACTION_OPEN_DOCUMENT)
     * @return Resultado con estadísticas de la importación
     */
    suspend fun importFromJson(context: Context, uri: Uri): ExportImportResult {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: return ExportImportResult(
                success = false,
                message = "❌ No se pudo leer el archivo"
            )

            val json = JSONObject(jsonString)
            val version = json.optInt("version", 0)

            if (version < 1) {
                return ExportImportResult(
                    success = false,
                    message = "❌ Formato de archivo no reconocido"
                )
            }

            var tasksImported = 0
            var habitsImported = 0

            // === Importar tareas ===
            val tasksArray = json.optJSONArray("tasks") ?: JSONArray()
            val existingTasks = taskDao.getAllTasksSync()
            val existingTaskKeys = existingTasks.map { "${it.name}|${it.createdAt}" }.toSet()

            // Mapeo de ID viejo → ID nuevo (para subtareas)
            val taskIdMap = mutableMapOf<Long, Long>()

            for (i in 0 until tasksArray.length()) {
                val taskJson = tasksArray.getJSONObject(i)
                val key = "${taskJson.getString("name")}|${taskJson.getLong("createdAt")}"

                if (key !in existingTaskKeys) {
                    val oldId = taskJson.getLong("id")
                    val task = jsonToTask(taskJson)
                    val newId = taskDao.insertTask(task)
                    taskIdMap[oldId] = newId
                    tasksImported++
                }
            }

            // === Importar subtareas (de tareas nuevas) ===
            val subtasksArray = json.optJSONArray("subtasks") ?: JSONArray()
            for (i in 0 until subtasksArray.length()) {
                val subJson = subtasksArray.getJSONObject(i)
                val oldTaskId = subJson.getLong("taskId")
                val newTaskId = taskIdMap[oldTaskId] ?: continue // Solo para tareas recién importadas
                val subtask = jsonToSubtask(subJson, newTaskId)
                subtaskDao.insertSubtask(subtask)
            }

            // === Importar hábitos ===
            val habitsArray = json.optJSONArray("habits") ?: JSONArray()
            val existingHabits = habitDao.getAllHabitsSync()
            val existingHabitKeys = existingHabits.map { "${it.name}|${it.createdAt}" }.toSet()

            val habitIdMap = mutableMapOf<Long, Long>()

            for (i in 0 until habitsArray.length()) {
                val habitJson = habitsArray.getJSONObject(i)
                val key = "${habitJson.getString("name")}|${habitJson.getLong("createdAt")}"

                if (key !in existingHabitKeys) {
                    val oldId = habitJson.getLong("id")
                    val habit = jsonToHabit(habitJson)
                    val newId = habitDao.insertHabit(habit)
                    habitIdMap[oldId] = newId
                    habitsImported++
                }
            }

            // === Importar logs de hábitos (de hábitos nuevos) ===
            val logsArray = json.optJSONArray("habitLogs") ?: JSONArray()
            for (i in 0 until logsArray.length()) {
                val logJson = logsArray.getJSONObject(i)
                val oldHabitId = logJson.getLong("habitId")
                val newHabitId = habitIdMap[oldHabitId] ?: continue
                val log = jsonToHabitLog(logJson, newHabitId)
                habitDao.insertLog(log)
            }

            ExportImportResult(
                success = true,
                message = "✅ Importadas $tasksImported tareas, $habitsImported hábitos",
                taskCount = tasksImported,
                habitCount = habitsImported
            )
        } catch (e: Exception) {
            ExportImportResult(
                success = false,
                message = "❌ Error al importar: ${e.localizedMessage}"
            )
        }
    }

    // === Serialización a JSON ===

    private fun tasksToJsonArray(tasks: List<Task>): JSONArray {
        val array = JSONArray()
        for (task in tasks) {
            array.put(JSONObject().apply {
                put("id", task.id)
                put("name", task.name)
                put("deadlineMillis", task.deadlineMillis ?: JSONObject.NULL)
                put("difficulty", task.difficulty.name)
                put("priority", task.priority.name)
                put("isCompleted", task.isCompleted)
                put("isStarted", task.isStarted)
                put("isQuickTask", task.isQuickTask)
                put("createdAt", task.createdAt)
                put("completedAt", task.completedAt ?: JSONObject.NULL)
                put("totalTimeWorkedMillis", task.totalTimeWorkedMillis)
                put("lastModifiedAt", task.lastModifiedAt)
            })
        }
        return array
    }

    private fun subtasksToJsonArray(subtasks: List<Subtask>): JSONArray {
        val array = JSONArray()
        for (sub in subtasks) {
            array.put(JSONObject().apply {
                put("id", sub.id)
                put("taskId", sub.taskId)
                put("name", sub.name)
                put("isCompleted", sub.isCompleted)
                put("sortOrder", sub.sortOrder)
            })
        }
        return array
    }

    private fun habitsToJsonArray(habits: List<Habit>): JSONArray {
        val array = JSONArray()
        for (habit in habits) {
            array.put(JSONObject().apply {
                put("id", habit.id)
                put("name", habit.name)
                put("emoji", habit.emoji)
                put("createdAt", habit.createdAt)
                put("isArchived", habit.isArchived)
            })
        }
        return array
    }

    private fun habitLogsToJsonArray(logs: List<HabitLog>): JSONArray {
        val array = JSONArray()
        for (log in logs) {
            array.put(JSONObject().apply {
                put("id", log.id)
                put("habitId", log.habitId)
                put("dateEpochDay", log.dateEpochDay)
                put("completedAt", log.completedAt)
            })
        }
        return array
    }

    // === Deserialización desde JSON ===

    private fun jsonToTask(json: JSONObject): Task = Task(
        // id = 0 → Room autogenera nuevo ID
        name = json.getString("name"),
        deadlineMillis = if (json.isNull("deadlineMillis")) null else json.getLong("deadlineMillis"),
        difficulty = try {
            Difficulty.valueOf(json.optString("difficulty", "EASY"))
        } catch (_: Exception) { Difficulty.EASY },
        priority = try {
            Priority.valueOf(json.optString("priority", "NORMAL"))
        } catch (_: Exception) { Priority.NORMAL },
        isCompleted = json.optBoolean("isCompleted", false),
        isStarted = json.optBoolean("isStarted", false),
        isQuickTask = json.optBoolean("isQuickTask", false),
        createdAt = json.optLong("createdAt", System.currentTimeMillis()),
        completedAt = if (json.isNull("completedAt")) null else json.optLong("completedAt"),
        totalTimeWorkedMillis = json.optLong("totalTimeWorkedMillis", 0),
        lastModifiedAt = json.optLong("lastModifiedAt", System.currentTimeMillis())
    )

    private fun jsonToSubtask(json: JSONObject, newTaskId: Long): Subtask = Subtask(
        // id = 0 → Room autogenera nuevo ID
        taskId = newTaskId,
        name = json.getString("name"),
        isCompleted = json.optBoolean("isCompleted", false),
        sortOrder = json.optInt("sortOrder", 0)
    )

    private fun jsonToHabit(json: JSONObject): Habit = Habit(
        // id = 0 → Room autogenera nuevo ID
        name = json.getString("name"),
        emoji = json.optString("emoji", "✅"),
        createdAt = json.optLong("createdAt", System.currentTimeMillis()),
        isArchived = json.optBoolean("isArchived", false)
    )

    private fun jsonToHabitLog(json: JSONObject, newHabitId: Long): HabitLog = HabitLog(
        // id = 0 → Room autogenera nuevo ID
        habitId = newHabitId,
        dateEpochDay = json.getInt("dateEpochDay"),
        completedAt = json.optLong("completedAt", System.currentTimeMillis())
    )

    private fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}

/**
 * Resultado de una operación de exportación o importación.
 */
data class ExportImportResult(
    val success: Boolean,
    val message: String,
    val taskCount: Int = 0,
    val habitCount: Int = 0
)
