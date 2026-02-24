package com.example.vsprocrastination.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.vsprocrastination.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BootReceiver - Reprograma TODOS los recordatorios después de un reinicio.
 * 
 * PROBLEMA QUE RESUELVE:
 * WorkManager persiste sus periodic workers, pero los one-shot workers
 * (recordatorios de deadline escalonados a 24h/4h/1h) se pierden al
 * reiniciar el dispositivo. Este receiver los recrea consultando la BD.
 * 
 * También maneja QUICKBOOT (reinicio rápido en HTC/algunos OEMs chinos).
 * 
 * NOTA: exported="true" es OBLIGATORIO para recibir BOOT_COMPLETED,
 * ya que es un broadcast del sistema (no de la propia app).
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private val BOOT_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in BOOT_ACTIONS) return
        
        val pendingResult = goAsync()
        
        // 1. Recrear canales de notificación (se pierden tras reinicio)
        TaskReminderWorker.createNotificationChannels(context)
        DeadlineCountdownWorker.createNotificationChannel(context)
        
        // 2. Reprogramar workers periódicos
        SmartNotificationWorker.schedule(context)
        DeadlineCountdownWorker.schedule(context)
        
        // 3. Reprogramar recordatorios one-shot para tareas con deadline
        //    y nagging para tareas vencidas (requiere consulta a BD)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val tasks = db.taskDao().getAllTasksSync()
                val now = System.currentTimeMillis()
                
                tasks.filter { !it.isCompleted }.forEach { task ->
                    val deadline = task.deadlineMillis ?: return@forEach
                    
                    if (deadline > now) {
                        // Deadline futuro → programar recordatorios escalonados
                        TaskReminderWorker.scheduleDeadlineReminders(
                            context, task.name, task.id, deadline
                        )
                    } else if (!task.isStarted) {
                        // Tarea vencida no iniciada → nagging (suave, cada 3h)
                        TaskReminderWorker.scheduleNaggingReminder(
                            context, task.name, task.id
                        )
                    }
                }
            } catch (_: Exception) {
                // Los workers periódicos ya cubren lo básico como fallback
            } finally {
                pendingResult.finish()
            }
        }
    }
}
