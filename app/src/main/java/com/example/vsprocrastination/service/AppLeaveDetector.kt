package com.example.vsprocrastination.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.vsprocrastination.MainActivity
import com.example.vsprocrastination.R

/**
 * AppLeaveDetector - Detecta cuando el usuario sale de la app en Modo Enfoque.
 * 
 * JUSTIFICACIÓN PSICOLÓGICA:
 * El "Accountability Partner Digital" - cuando sabes que alguien
 * (o algo) te está observando, es más difícil romper el compromiso.
 * 
 * Este BroadcastReceiver se activa con USER_PRESENT (desbloqueo)
 * para verificar si el usuario abandonó la app durante una sesión
 * de enfoque y enviarle una notificación de "vuelta al trabajo".
 */
class AppLeaveDetector : BroadcastReceiver() {
    
    companion object {
        const val NOTIFICATION_ID_RETURN = 5000
        const val ACTION_APP_LEFT = "com.example.vsprocrastination.APP_LEFT"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        // Solo actuar si hay una sesión de enfoque activa
        if (!FocusService.isRunning.value) return
        
        val timerState = FocusService.timerState.value
        if (timerState.taskName.isEmpty()) return
        
        when (intent.action) {
            Intent.ACTION_USER_PRESENT, ACTION_APP_LEFT -> {
                showReturnNotification(context, timerState.taskName)
            }
        }
    }
    
    /**
     * Notificación "de vuelta al trabajo" - persistente.
     */
    private fun showReturnNotification(context: Context, taskName: String) {
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val messages = listOf(
            "🔙 Vuelve a tu sesión de enfoque",
            "⏱️ Tu timer sigue corriendo... ¡No lo desperdicies!",
            "🧠 2 minutos más de enfoque. Tú puedes.",
            "🎯 $taskName te espera. Solo un poco más."
        )
        
        val notification = NotificationCompat.Builder(context, TaskReminderWorker.NAGGING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠️ ¡Saliste de la app!")
            .setContentText(messages.random())
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "${messages.random()}\n\nTu sesión de enfoque para \"$taskName\" sigue activa."
            ))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setAutoCancel(false)
            .build()
        
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_RETURN, notification)
    }
}
