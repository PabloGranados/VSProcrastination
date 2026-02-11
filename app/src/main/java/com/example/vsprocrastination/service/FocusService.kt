package com.example.vsprocrastination.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.vsprocrastination.MainActivity
import com.example.vsprocrastination.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FocusService - Foreground Service para el modo Deep Work.
 * 
 * JUSTIFICACIÓN TÉCNICA:
 * Un Foreground Service garantiza que el timer siga corriendo
 * incluso si la app va a background. Android no lo mata porque
 * muestra una notificación persistente (requisito del OS).
 * 
 * JUSTIFICACIÓN PSICOLÓGICA:
 * La notificación persistente con el timer actúa como
 * "compromiso público" (teoría de Cialdini) - es más difícil
 * abandonar la tarea cuando hay evidencia visible de que empezaste.
 */
class FocusService : Service() {
    
    companion object {
        const val CHANNEL_ID = "focus_timer_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "com.example.vsprocrastination.START_FOCUS"
        const val ACTION_STOP = "com.example.vsprocrastination.STOP_FOCUS"
        const val ACTION_COMPLETE = "com.example.vsprocrastination.COMPLETE_TASK"
        
        const val EXTRA_TASK_NAME = "task_name"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_DURATION_MINUTES = "duration_minutes"
        
        // Estado compartido para que la UI observe
        private val _timerState = MutableStateFlow(TimerState())
        val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
        
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
        
        fun startFocus(context: Context, taskName: String, taskId: Long, durationMinutes: Int = 25) {
            val intent = Intent(context, FocusService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TASK_NAME, taskName)
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopFocus(context: Context) {
            val intent = Intent(context, FocusService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    
    private var taskName: String = ""
    private var taskId: Long = 0
    private var totalDurationMillis: Long = 0
    private var startTimeMillis: Long = 0
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "Tarea"
                taskId = intent.getLongExtra(EXTRA_TASK_ID, 0)
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 25)
                totalDurationMillis = durationMinutes * 60 * 1000L
                startTimer()
            }
            ACTION_STOP -> {
                stopTimer()
                stopSelf()
            }
            ACTION_COMPLETE -> {
                stopTimer()
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    private fun startTimer() {
        startTimeMillis = System.currentTimeMillis()
        _isRunning.value = true
        
        val notification = buildNotification(totalDurationMillis)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTimeMillis
                val remaining = totalDurationMillis - elapsed
                
                if (remaining <= 0) {
                    // Timer completado
                    _timerState.value = TimerState(
                        remainingMillis = 0,
                        totalMillis = totalDurationMillis,
                        taskName = taskName,
                        taskId = taskId,
                        isComplete = true
                    )
                    showCompletionNotification()
                    _isRunning.value = false
                    stopSelf()
                    break
                }
                
                _timerState.value = TimerState(
                    remainingMillis = remaining,
                    totalMillis = totalDurationMillis,
                    taskName = taskName,
                    taskId = taskId,
                    isComplete = false
                )
                
                // Actualizar notificación cada segundo para barra de progreso fluida
                // (el cronómetro nativo maneja el texto del timer automáticamente)
                updateNotification(remaining)
                
                delay(1000)
            }
        }
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
        val elapsed = System.currentTimeMillis() - startTimeMillis
        
        _timerState.value = TimerState(
            remainingMillis = 0,
            totalMillis = totalDurationMillis,
            taskName = taskName,
            taskId = taskId,
            isComplete = false,
            elapsedWhenStopped = elapsed
        )
        _isRunning.value = false
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Modo Enfoque",
                NotificationManager.IMPORTANCE_LOW // Sin sonido, pero siempre visible
            ).apply {
                description = "Mantiene el timer de enfoque activo"
                setShowBadge(true)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun buildNotification(remainingMillis: Long): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FocusService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val progressPercent = ((1 - remainingMillis.toFloat() / totalDurationMillis) * 100).toInt()
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 Modo Enfoque: $taskName")
            .setContentText("Sesión de enfoque en progreso")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // No se puede deslizar para descartar
            .setSilent(true)
            // Cronómetro live nativo del sistema — cuenta regresiva visible
            // aunque el usuario salga de la app (API 24+)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(System.currentTimeMillis() + remainingMillis)
            .setShowWhen(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Detener",
                stopIntent
            )
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(100, progressPercent, false)
            .build()
    }
    
    private fun updateNotification(remainingMillis: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(remainingMillis))
    }
    
    private fun showCompletionNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎉 ¡Sesión completada!")
            .setContentText("Terminaste tu sesión de enfoque para: $taskName")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID + 1, notification)
    }
    
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

/**
 * Estado del timer compartido entre Service y UI.
 */
data class TimerState(
    val remainingMillis: Long = 0,
    val totalMillis: Long = 0,
    val taskName: String = "",
    val taskId: Long = 0,
    val isComplete: Boolean = false,
    val elapsedWhenStopped: Long = 0
) {
    val minutes: Int get() = (remainingMillis / 1000 / 60).toInt()
    val seconds: Int get() = ((remainingMillis / 1000) % 60).toInt()
    val progress: Float get() = if (totalMillis > 0) 1f - (remainingMillis.toFloat() / totalMillis) else 0f
    val formattedTime: String get() = String.format("%02d:%02d", minutes, seconds)
}
