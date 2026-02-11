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
import java.util.concurrent.TimeUnit

/**
 * TaskReminderWorker - Notificaciones "Pegajosas" con WorkManager.
 *
 * JUSTIFICACIÓN TÉCNICA:
 * WorkManager garantiza que las notificaciones se entreguen incluso si
 * la app está cerrada. Usa JobScheduler (API 21+) o AlarmManager como fallback.
 * 
 * JUSTIFICACIÓN PSICOLÓGICA:
 * Las notificaciones que no se pueden descartar fácilmente implementan
 * el "Implementation Intention" (Gollwitzer, 1999) - son recordatorios
 * contextuales que anclan la tarea al momento presente.
 * 
 * La notificación usa ONGOING para que no se descarte con swipe.
 * Solo desaparece cuando el usuario abre la app e inicia la tarea.
 */
class TaskReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    
    companion object {
        const val CHANNEL_ID = "task_reminder_channel"
        const val NAGGING_CHANNEL_ID = "nagging_reminder_channel"
        
        const val KEY_TASK_NAME = "task_name"
        const val KEY_TASK_ID = "task_id"
        const val KEY_IS_OVERDUE = "is_overdue"
        const val KEY_NOTIFICATION_TYPE = "notification_type"
        
        const val TYPE_GENTLE = "gentle"       // Recordatorio suave
        const val TYPE_PERSISTENT = "persistent" // No se puede descartar
        const val TYPE_NAGGING = "nagging"       // Repetitivo e insistente
        
        private const val WORK_TAG_PERIODIC = "periodic_task_reminder"
        private const val WORK_TAG_NAGGING = "nagging_reminder"
        
        /**
         * Programa un recordatorio periódico cada 2 horas.
         * "¿Ya empezaste tu tarea más importante?"
         */
        fun schedulePeriodicReminder(context: Context) {
            val request = PeriodicWorkRequestBuilder<TaskReminderWorker>(
                2, TimeUnit.HOURS,
                30, TimeUnit.MINUTES // Flex interval
            )
                .setInputData(workDataOf(
                    KEY_NOTIFICATION_TYPE to TYPE_PERSISTENT
                ))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false) // Incluso con batería baja
                        .build()
                )
                .addTag(WORK_TAG_PERIODIC)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
        
        /**
         * Programa un recordatorio "insistente" (nagging) cada 15 minutos.
         * Se activa cuando hay tareas vencidas.
         */
        fun scheduleNaggingReminder(
            context: Context,
            taskName: String,
            taskId: Long
        ) {
            val request = PeriodicWorkRequestBuilder<TaskReminderWorker>(
                15, TimeUnit.MINUTES
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
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }
        
        /**
         * Programa un recordatorio one-shot para un deadline específico.
         */
        fun scheduleDeadlineReminder(
            context: Context,
            taskName: String,
            taskId: Long,
            deadlineMillis: Long
        ) {
            val now = System.currentTimeMillis()
            val delay = deadlineMillis - now - (60 * 60 * 1000) // 1 hora antes del deadline
            
            if (delay <= 0) return // Ya pasó o falta muy poco
            
            val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(
                    KEY_TASK_NAME to taskName,
                    KEY_TASK_ID to taskId,
                    KEY_IS_OVERDUE to false,
                    KEY_NOTIFICATION_TYPE to TYPE_PERSISTENT
                ))
                .addTag("deadline_${taskId}")
                .build()
            
            WorkManager.getInstance(context).enqueue(request)
        }
        
        /**
         * Cancela recordatorios nagging para una tarea específica.
         * Se llama cuando la tarea se inicia o completa.
         */
        fun cancelNaggingReminder(context: Context, taskId: Long) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("${WORK_TAG_NAGGING}_$taskId")
        }
        
        /**
         * Cancela el recordatorio de deadline para una tarea.
         */
        fun cancelDeadlineReminder(context: Context, taskId: Long) {
            WorkManager.getInstance(context)
                .cancelAllWorkByTag("deadline_${taskId}")
        }
        
        /**
         * Crea los canales de notificación requeridos.
         * Llamar una vez al iniciar la app.
         */
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                
                // Canal de recordatorios normales
                val reminderChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios de tareas",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Recordatorios para que inicies tus tareas"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                }
                
                // Canal "nagging" (insistente) - máxima importancia
                val naggingChannel = NotificationChannel(
                    NAGGING_CHANNEL_ID,
                    "Recordatorios urgentes",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Recordatorios insistentes para tareas vencidas"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                    enableLights(true)
                    lightColor = android.graphics.Color.RED
                }
                
                manager.createNotificationChannel(reminderChannel)
                manager.createNotificationChannel(naggingChannel)
            }
        }
    }
    
    override fun doWork(): Result {
        val taskName = inputData.getString(KEY_TASK_NAME) ?: "tu tarea más importante"
        val taskId = inputData.getLong(KEY_TASK_ID, 0)
        val isOverdue = inputData.getBoolean(KEY_IS_OVERDUE, false)
        val type = inputData.getString(KEY_NOTIFICATION_TYPE) ?: TYPE_GENTLE
        
        when (type) {
            TYPE_GENTLE -> showGentleNotification(taskName, taskId)
            TYPE_PERSISTENT -> showPersistentNotification(taskName, taskId, isOverdue)
            TYPE_NAGGING -> showNaggingNotification(taskName, taskId)
        }
        
        return Result.success()
    }
    
    /**
     * Notificación suave - se puede descargar.
     */
    private fun showGentleNotification(taskName: String, taskId: Long) {
        val notification = baseNotificationBuilder(CHANNEL_ID, taskId)
            .setContentTitle("📋 ¿Ya empezaste?")
            .setContentText(taskName)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notifyWithCheck(2000 + taskId.toInt(), notification)
    }
    
    /**
     * Notificación persistente - NO se puede descartar con swipe.
     * Solo desaparece cuando el usuario actúa.
     */
    private fun showPersistentNotification(taskName: String, taskId: Long, isOverdue: Boolean) {
        val title = if (isOverdue) "⚠️ ¡TAREA VENCIDA!" else "🎯 Es hora de actuar"
        val text = if (isOverdue) {
            "$taskName ya pasó su fecha límite. Ábrela AHORA."
        } else {
            "Tu próxima tarea: $taskName. Toca para empezar."
        }
        
        val notification = baseNotificationBuilder(CHANNEL_ID, taskId)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)  // <-- CLAVE: No se descarta con swipe
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        notifyWithCheck(3000 + taskId.toInt(), notification)
    }
    
    /**
     * Notificación nagging - insistente, con vibración fuerte.
     * Para tareas vencidas que el usuario no ha empezado.
     */
    private fun showNaggingNotification(taskName: String, taskId: Long) {
        val messages = listOf(
            "🔴 $taskName sigue sin empezar...",
            "⏰ ¿Cuánto más vas a esperar? → $taskName",
            "💀 La procrastinación gana si no abres esto → $taskName",
            "🧠 Solo necesitas 2 minutos para empezar: $taskName",
            "🚨 RECORDATORIO: $taskName no se va a hacer sola"
        )
        
        val notification = baseNotificationBuilder(NAGGING_CHANNEL_ID, taskId)
            .setContentTitle("⚠️ ¡Acción requerida!")
            .setContentText(messages.random())
            .setStyle(NotificationCompat.BigTextStyle().bigText(messages.random()))
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
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
