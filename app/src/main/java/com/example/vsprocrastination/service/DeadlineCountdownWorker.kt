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
import java.util.concurrent.TimeUnit

/**
 * DeadlineCountdownWorker - Notificaciones cuenta regresiva estilo Duolingo.
 *
 * JUSTIFICACIÓN PSICOLÓGICA:
 * "Temporal Motivation Theory" (Steel & König, 2006):
 * La motivación aumenta exponencialmente a medida que el deadline se acerca.
 * Mostrar un countdown visual amplifica este efecto natural. Es el mismo
 * principio que usa Duolingo: presión temporal visible genera acción.
 *
 * Se ejecuta cada 15 minutos y busca tareas con deadline en las próximas 2 horas.
 * Muestra notificaciones persistentes con cronómetro regresivo nativo del sistema.
 */
class DeadlineCountdownWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val CHANNEL_ID = "deadline_countdown_channel"
        private const val WORK_TAG = "deadline_countdown_scanner"
        private const val NOTIFICATION_BASE_ID = 5000
        private const val TWO_HOURS_MILLIS = 2 * 60 * 60 * 1000L
        
        /**
         * Programa el scanner periódico cada 15 minutos.
         * Busca tareas cuyo deadline está a menos de 2 horas.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DeadlineCountdownWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES // Flex interval
            )
                .addTag(WORK_TAG)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
        
        /**
         * Cancela el scanner periódico.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG)
        }
        
        /**
         * Cancela la notificación countdown de una tarea específica.
         * Llamar cuando la tarea se completa o inicia.
         */
        fun cancelNotification(context: Context, taskId: Long) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(NOTIFICATION_BASE_ID + taskId.toInt())
        }
        
        /**
         * Crea el canal de notificación para countdown de deadlines.
         */
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Cuenta regresiva de deadlines",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas cuando una tarea está a menos de 2 horas de vencer"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                    enableLights(true)
                    lightColor = android.graphics.Color.parseColor("#FF6B00")
                }
                
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        }
    }
    
    /**
     * Mensajes de urgencia rotantes — estilo Duolingo.
     * Generan presión temporal sin ser agresivos.
     */
    private val urgencyMessages = listOf(
        "⏰ ¡Se acaba el tiempo!",
        "🔥 ¡El deadline se acerca!",
        "⚡ ¡Ahora o nunca!",
        "🚨 ¡No dejes que venza!",
        "💪 ¡Aún puedes lograrlo!",
        "🏃 ¡Corre, aún hay tiempo!"
    )
    
    private val motivationalSubtext = listOf(
        "Empieza ahora y termina a tiempo",
        "2 minutos para empezar es todo lo que necesitas",
        "Tu yo del futuro te lo agradecerá",
        "No pierdas tu racha de productividad",
        "La procrastinación es el enemigo, no la tarea",
        "Pequeños pasos llevan a grandes logros"
    )
    
    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val taskDao = db.taskDao()
        
        val now = System.currentTimeMillis()
        val cutoff = now + TWO_HOURS_MILLIS
        
        val urgentTasks = taskDao.getTasksWithDeadlineBetween(now, cutoff)
        
        if (urgentTasks.isEmpty()) {
            return Result.success()
        }
        
        urgentTasks.forEach { task ->
            showCountdownNotification(task.id, task.name, task.deadlineMillis!!)
        }
        
        return Result.success()
    }
    
    /**
     * Muestra notificación con cronómetro regresivo nativo.
     * El sistema Android actualiza el timer automáticamente —
     * el usuario ve la cuenta regresiva en tiempo real sin consumir batería.
     */
    private fun showCountdownNotification(taskId: Long, taskName: String, deadlineMillis: Long) {
        val remaining = deadlineMillis - System.currentTimeMillis()
        val minutesLeft = (remaining / 1000 / 60).toInt()
        
        val openIntent = PendingIntent.getActivity(
            applicationContext, taskId.toInt(),
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("task_id", taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = urgencyMessages.random()
        val timeText = when {
            minutesLeft < 10 -> "🔴 ¡Menos de 10 minutos!"
            minutesLeft < 15 -> "⚠️ ¡Menos de 15 minutos!"
            minutesLeft < 30 -> "¡Solo $minutesLeft minutos!"
            minutesLeft < 60 -> "¡Menos de 1 hora!"
            else -> "¡Menos de 2 horas!"
        }
        val subtitle = motivationalSubtext.random()
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$title $taskName")
            .setContentText("$timeText • $subtitle")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$timeText\n📌 $taskName\n\n💡 $subtitle")
            )
            .setContentIntent(openIntent)
            .setOngoing(minutesLeft < 30) // Pegajosa cuando queda poco
            .setAutoCancel(minutesLeft >= 30)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // Cronómetro regresivo nativo del sistema
            // Cuenta atrás en tiempo real hasta el deadline
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(deadlineMillis)
            .setShowWhen(true)
            .build()
        
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_BASE_ID + taskId.toInt(), notification)
    }
}
