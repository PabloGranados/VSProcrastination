package com.example.vsprocrastination.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BootReceiver - Reprograma los recordatorios después de un reinicio.
 * 
 * WorkManager ya persiste sus tareas a través de reinicios,
 * pero este receiver asegura que los canales de notificación
 * estén creados y los recordatorios periódicos estén activos.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Recrear canales de notificación
            TaskReminderWorker.createNotificationChannels(context)
            
            // Reprogramar recordatorios periódicos
            TaskReminderWorker.schedulePeriodicReminder(context)
        }
    }
}
