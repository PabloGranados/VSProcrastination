package com.example.vsprocrastination.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.vsprocrastination.MainActivity
import com.example.vsprocrastination.R
import com.example.vsprocrastination.data.database.AppDatabase
import com.example.vsprocrastination.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * TaskReminderWorker - Sistema de recordatorios escalonados con WorkManager.
 *
 * REDISEÑO v2 — MENOS INTRUSIVO, MÁS ESTRATÉGICO:
 * ================================================
 *
 * Recordatorios escalonados por proximidad al deadline:
 *   • 24h antes: Recordatorio suave (GENTLE) — planificación anticipada
 *   • 4h antes:  Recordatorio medio (PERSISTENT) — hora de actuar
 *   • 1h antes:  Recordatorio urgente (PERSISTENT) — última oportunidad
 *
 * Nagging para tareas vencidas:
 *   • Cada 3 HORAS (antes era cada 15 min — demasiado agresivo)
 *   • Notificaciones DISMISSABLE (antes eran ONGOING — no se podían quitar)
 *   • Solo en horas activas (8:00-22:00)
 *
 * JUSTIFICACIÓN PSICOLÓGICA:
 * La "Temporal Motivation Theory" (Steel & König, 2006) muestra que
 * la motivación crece exponencialmente al acercarse el deadline.
 * Los recordatorios escalonados aprovechan esta curva natural.
 *
 * El nagging excesivo (cada 15 min) genera "notification fatigue"
 * y el usuario termina desactivando todas las notificaciones
 * (Pielot & Rello, 2017). Cada 3h es suficiente para mantener
 * la tarea presente sin generar rechazo.
 */
class TaskReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "task_reminder_channel"
        const val NAGGING_CHANNEL_ID = "nagging_reminder_channel"

        const val KEY_TASK_NAME = "task_name"
        const val KEY_TASK_ID = "task_id"
        const val KEY_IS_OVERDUE = "is_overdue"
        const val KEY_NOTIFICATION_TYPE = "notification_type"
        const val KEY_DEADLINE_TIER = "deadline_tier" // "24h", "4h", "1h"

        const val TYPE_GENTLE = "gentle"       // Recordatorio suave (24h antes)
        const val TYPE_PERSISTENT = "persistent" // Recordatorio medio-alto (4h/1h antes)
        const val TYPE_NAGGING = "nagging"       // Para tareas vencidas (cada 3h)

        private const val WORK_TAG_PERIODIC = "periodic_task_reminder"
        private const val WORK_TAG_NAGGING = "nagging_reminder"

        /**
         * DEPRECADO: Usar SmartNotificationWorker en su lugar.
         */
        fun schedulePeriodicReminder(context: Context) {
            // No-op — mantenido solo por compatibilidad
        }

        /**
         * Cancela el recordatorio periódico redundante.
         */
        fun cancelPeriodicReminder(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG_PERIODIC)
        }

        /**
         * Programa recordatorios "nagging" cada 3 HORAS para tareas vencidas.
         * Antes era cada 15 min — causaba notification fatigue.
         *
         * CAMBIOS v2:
         * - Intervalo: 15 min → 3 horas (reducción de 12x)
         * - Notificaciones dismissable (ya no ONGOING)
         * - Prioridad: MAX → HIGH
         */
        fun scheduleNaggingReminder(
            context: Context,
            taskName: String,
            taskId: Long
        ) {
            val request = PeriodicWorkRequestBuilder<TaskReminderWorker>(
                3, TimeUnit.HOURS,    // Cada 3 horas (antes 15 min)
                30, TimeUnit.MINUTES  // Flex interval
            )
                .setInputData(workDataOf(
                    KEY_TASK_NAME to taskName,
                    KEY_TASK_ID to taskId,
                    KEY_IS_OVERDUE to true,
                    KEY_NOTIFICATION_TYPE to TYPE_NAGGING
                ))
                .addTag(WORK_TAG_NAGGING)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "${WORK_TAG_NAGGING}_$taskId",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Programa recordatorios escalonados para un deadline.
         *
         * NUEVO en v2: En vez de un solo recordatorio a 1h del deadline,
         * ahora programa 3 recordatorios con urgencia creciente:
         *
         *   24h antes → GENTLE   (planificación)
         *    4h antes → PERSISTENT (hora de actuar)
         *    1h antes → PERSISTENT (última oportunidad)
         *
         * Cada tier tiene un tag único para cancelación individual.
         */
        fun scheduleDeadlineReminders(
            context: Context,
            taskName: String,
            taskId: Long,
            deadlineMillis: Long
        ) {
            val now = System.currentTimeMillis()
            val workManager = WorkManager.getInstance(context)

            data class Tier(val label: String, val hoursBefore: Long, val type: String)

            val tiers = listOf(
                Tier("24h", 24, TYPE_GENTLE),
                Tier("4h", 4, TYPE_PERSISTENT),
                Tier("1h", 1, TYPE_PERSISTENT)
            )

            tiers.forEach { tier ->
                val delay = deadlineMillis - now - (tier.hoursBefore * 60 * 60 * 1000)
                if (delay > 0) {
                    val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(workDataOf(
                            KEY_TASK_NAME to taskName,
                            KEY_TASK_ID to taskId,
                            KEY_IS_OVERDUE to false,
                            KEY_NOTIFICATION_TYPE to tier.type,
                            KEY_DEADLINE_TIER to tier.label
                        ))
                        .addTag("deadline_${tier.label}_${taskId}")
                        .build()
                    workManager.enqueue(request)
                }
            }
        }

        /**
         * Cancela TODOS los recordatorios escalonados de deadline para una tarea.
         * Incluye tags del sistema anterior (backward compat).
         */
        fun cancelDeadlineReminders(context: Context, taskId: Long) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag("deadline_24h_${taskId}")
            workManager.cancelAllWorkByTag("deadline_4h_${taskId}")
            workManager.cancelAllWorkByTag("deadline_1h_${taskId}")
            // Backward compat: tag del sistema anterior
            workManager.cancelAllWorkByTag("deadline_${taskId}")
        }

        /**
         * DEPRECADO: Usar scheduleDeadlineReminders (plural).
         * Redirige al nuevo método para backward compat.
         */
        fun scheduleDeadlineReminder(
            context: Context,
            taskName: String,
            taskId: Long,
            deadlineMillis: Long
        ) = scheduleDeadlineReminders(context, taskName, taskId, deadlineMillis)

        /**
         * DEPRECADO: Usar cancelDeadlineReminders (plural).
         */
        fun cancelDeadlineReminder(context: Context, taskId: Long) =
            cancelDeadlineReminders(context, taskId)

        /**
         * Cancela recordatorios nagging para una tarea específica.
         */
        fun cancelNaggingReminder(context: Context, taskId: Long) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("${WORK_TAG_NAGGING}_$taskId")
        }

        /**
         * Crea los canales de notificación requeridos.
         */
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)

                // Canal de recordatorios normales (gentle + persistent)
                val reminderChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios de tareas",
                    NotificationManager.IMPORTANCE_DEFAULT // Era HIGH → DEFAULT para menos intrusión
                ).apply {
                    description = "Recordatorios anticipados para tus tareas con deadline"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                }

                // Canal "nagging" — para tareas vencidas (importancia HIGH)
                val naggingChannel = NotificationChannel(
                    NAGGING_CHANNEL_ID,
                    "Recordatorios urgentes",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Recordatorios para tareas que ya pasaron su fecha límite"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 300, 500)
                    enableLights(true)
                    lightColor = android.graphics.Color.RED
                }

                manager.createNotificationChannel(reminderChannel)
                manager.createNotificationChannel(naggingChannel)
            }
        }
    }

    override suspend fun doWork(): Result {
        var taskName = inputData.getString(KEY_TASK_NAME)
        val taskId = inputData.getLong(KEY_TASK_ID, 0)
        val isOverdue = inputData.getBoolean(KEY_IS_OVERDUE, false)
        val type = inputData.getString(KEY_NOTIFICATION_TYPE) ?: TYPE_GENTLE
        val deadlineTier = inputData.getString(KEY_DEADLINE_TIER)

        // ===== HORAS DE SILENCIO (22:00 - 7:59) =====
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (currentHour >= 22 || currentHour < 8) {
            // Para recordatorios one-shot (escalonados de deadline), reprogramar
            // a las 8:00 AM del día siguiente en vez de perder la notificación
            if (type == TYPE_GENTLE || type == TYPE_PERSISTENT) {
                val nextMorning = java.util.Calendar.getInstance().apply {
                    if (currentHour >= 22) add(java.util.Calendar.DAY_OF_YEAR, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 8)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                }
                val delay = nextMorning.timeInMillis - System.currentTimeMillis()
                if (delay > 0 && taskId > 0) {
                    val retryRequest = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(inputData)
                        .addTag("deadline_retry_${taskId}")
                        .build()
                    WorkManager.getInstance(applicationContext).enqueue(retryRequest)
                }
            }
            return Result.success()
        }

        // Verificar preferencias del usuario
        val prefsManager = PreferencesManager(applicationContext)
        val naggingEnabled = prefsManager.naggingEnabled.first()

        if (type == TYPE_NAGGING && !naggingEnabled) {
            return Result.success()
        }

        // Verificar que la tarea aún existe y no está completada
        if (taskId > 0) {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val task = db.taskDao().getTaskById(taskId)
                if (task == null || task.isCompleted) {
                    // Tarea completada o eliminada → cancelar worker y no notificar
                    if (type == TYPE_NAGGING) {
                        // Cancelar el worker periódico de nagging para que no vuelva a dispararse
                        WorkManager.getInstance(applicationContext)
                            .cancelUniqueWork("${WORK_TAG_NAGGING}_$taskId")
                    }
                    // Limpiar notificación previa de esta tarea si quedó visible
                    val notifManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notifManager.cancel(2000 + taskId.toInt())
                    notifManager.cancel(3000 + taskId.toInt())
                    notifManager.cancel(4000 + taskId.toInt())
                    return Result.success()
                }
                // Si es nagging y la tarea ya fue iniciada, cancelar nagging y no molestar
                if (type == TYPE_NAGGING && task.isStarted) {
                    WorkManager.getInstance(applicationContext)
                        .cancelUniqueWork("${WORK_TAG_NAGGING}_$taskId")
                    return Result.success()
                }
                // Actualizar nombre por si cambió
                if (taskName.isNullOrEmpty()) {
                    taskName = task.name
                }
            } catch (_: Exception) {
                // Si no podemos verificar el estado de la tarea, NO enviar notificación
                // Es preferible perder una notificación que enviar una para una tarea eliminada
                return Result.success()
            }
        }

        // Si no se proporcionó nombre, obtener la tarea prioritaria
        if (taskName.isNullOrEmpty() || taskName == "tu tarea más importante") {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val pendingTasks = db.taskDao().getAllTasksSync()
                    .filter { !it.isCompleted }
                val topTask = pendingTasks.maxByOrNull { it.calculatePriorityScore() }
                taskName = topTask?.name ?: return Result.success()
            } catch (_: Exception) {
                taskName = "tu tarea más importante"
            }
        }

        when (type) {
            TYPE_GENTLE -> showGentleNotification(taskName, taskId, deadlineTier)
            TYPE_PERSISTENT -> showPersistentNotification(taskName, taskId, isOverdue, deadlineTier)
            TYPE_NAGGING -> showNaggingNotification(taskName, taskId)
        }

        return Result.success()
    }

    /**
     * Notificación suave — 24h antes del deadline.
     * Tono informativo, no alarmante. Dismissable.
     */
    private fun showGentleNotification(taskName: String, taskId: Long, tier: String?) {
        val (title, body) = when (tier) {
            "24h" -> "📋 Mañana vence: $taskName" to
                "Tienes hasta mañana para completar esta tarea. ¿Puedes planificar un momento hoy para avanzar?\n\n💡 Planificar con anticipación reduce la ansiedad un 40% (Kahneman & Tversky, 1979)."
            else -> "🎯 Siguiente tarea: $taskName" to
                "¿Puedes dedicarle 2 minutos ahora? A veces eso es todo lo que necesitas para entrar en ritmo."
        }

        val notification = baseNotificationBuilder(CHANNEL_ID, taskId)
            .setContentTitle(title)
            .setContentText(body.take(60) + "...")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notifyWithCheck(2000 + taskId.toInt(), notification)
    }

    /**
     * Notificación persistente — 4h y 1h antes del deadline.
     * Más urgente pero aún dismissable (ya no ONGOING).
     */
    private fun showPersistentNotification(taskName: String, taskId: Long, isOverdue: Boolean, tier: String?) {
        val (title, text) = when {
            isOverdue -> "⚠️ Tarea vencida" to
                "\"$taskName\" ya pasó su fecha límite.\n\n🧠 Retrasar tareas vencidas aumenta la ansiedad (Steel, 2007). Solo necesitas 2 minutos para empezar."
            tier == "4h" -> "⏰ $taskName — 4 horas restantes" to
                "Tu deadline se acerca. Es buen momento para empezar o avanzar significativamente.\n\n💪 La motivación crece al acercarse el deadline (Steel & König, 2006)."
            tier == "1h" -> "🔴 ¡Última hora! → $taskName" to
                "Queda 1 hora para el deadline. Si aún no empezaste, este es tu momento.\n\n⚡ Regla de los 5 segundos: actúa antes de que tu cerebro busque excusas."
            else -> "🎯 Es hora de actuar" to
                "📌 Tu próxima tarea: $taskName\n\n💡 «Si tienes que comerte dos sapos, cómete el más grande primero.» — Brian Tracy"
        }

        val notification = baseNotificationBuilder(CHANNEL_ID, taskId)
            .setContentTitle(title)
            .setContentText(text.take(60) + "...")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)  // Dismissable (antes era ONGOING)
            .setPriority(if (tier == "1h") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notifyWithCheck(3000 + taskId.toInt(), notification)
    }

    /**
     * Notificación nagging — para tareas vencidas (cada 3h).
     *
     * CAMBIOS v2:
     * - Ya NO es ONGOING (el usuario puede descartarla)
     * - Prioridad HIGH en vez de MAX
     * - Mensajes menos agresivos, más motivacionales
     * - Se verifica que la tarea siga vigente antes de notificar
     */
    private fun showNaggingNotification(taskName: String, taskId: Long) {
        val messages = listOf(
            "📌 $taskName sigue pendiente. Solo 2 minutos para empezar.",
            "🧠 Tu mente sigue pensando en \"$taskName\". Cierra ese ciclo.",
            "💡 Cada hora que pasa, la ansiedad crece. Empieza $taskName ahora.",
            "⏰ Recordatorio: $taskName está esperándote.",
            "🎯 Pequeño paso: abre la app y empieza $taskName."
        )

        val selectedMessage = messages.random()

        val notification = baseNotificationBuilder(NAGGING_CHANNEL_ID, taskId)
            .setContentTitle("📋 Tarea vencida pendiente")
            .setContentText(selectedMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(selectedMessage))
            .setAutoCancel(true)    // Dismissable (antes ONGOING)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Era MAX
            .setCategory(NotificationCompat.CATEGORY_REMINDER) // Era ALARM
            .build()

        notifyWithCheck(4000 + taskId.toInt(), notification)
    }

    private fun baseNotificationBuilder(channelId: String, taskId: Long): NotificationCompat.Builder {
        val openIntent = PendingIntent.getActivity(
            applicationContext, taskId.toInt(),
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("task_id", taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openIntent)
    }

    private fun notifyWithCheck(id: Int, notification: android.app.Notification) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }
}
