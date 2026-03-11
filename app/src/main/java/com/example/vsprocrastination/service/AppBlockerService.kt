package com.example.vsprocrastination.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.vsprocrastination.R
import com.example.vsprocrastination.data.preferences.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * AppBlockerService — Monitorea la app en foreground y muestra overlay si está bloqueada.
 *
 * JUSTIFICACIÓN CIENTÍFICA:
 * - Stimulus Control (Thorndike, 2012): Eliminar el acceso a estímulos
 *   de distracción es más eficaz que resistirlos con fuerza de voluntad.
 * - Ego Depletion (Baumeister, 2011): Cada decisión de "no abrir TikTok"
 *   consume autocontrol limitado. El bloqueo elimina la decisión.
 * - Flow State Protection (Csikszentmihalyi, 1990): Una interrupción
 *   digital requiere ~23 min para recuperar la concentración (Mark et al., 2008).
 *
 * MECANISMO:
 * Usa UsageStatsManager para polling de la app en foreground cada 1.5s.
 * Si detecta una app bloqueada, lanza BlockOverlayActivity como overlay.
 */
class AppBlockerService : Service() {

    companion object {
        const val CHANNEL_ID = "app_blocker_channel"
        private const val NOTIFICATION_ID = 2001
        private const val POLL_INTERVAL_MS = 1500L

        const val ACTION_START_BLOCKING = "com.example.vsprocrastination.START_BLOCKING"
        const val ACTION_STOP_BLOCKING = "com.example.vsprocrastination.STOP_BLOCKING"

        fun startBlocking(context: Context) {
            val intent = Intent(context, AppBlockerService::class.java).apply {
                action = ACTION_START_BLOCKING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopBlocking(context: Context) {
            val intent = Intent(context, AppBlockerService::class.java).apply {
                action = ACTION_STOP_BLOCKING
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
                // Service may not be running
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitorJob: Job? = null
    private var blockedPackages: Set<String> = emptySet()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_BLOCKING -> {
                startMonitoring()
            }
            ACTION_STOP_BLOCKING -> {
                stopMonitoring()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        AppBlockerManager.setBlockerActive(true)

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            // Cargar la lista de apps bloqueadas
            val preferencesManager = PreferencesManager(this@AppBlockerService)
            blockedPackages = preferencesManager.blockedApps.first()

            if (blockedPackages.isEmpty()) {
                stopMonitoring()
                stopSelf()
                return@launch
            }

            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            while (isActive) {
                val foregroundPackage = getForegroundPackage(usageStatsManager)
                if (foregroundPackage != null && foregroundPackage in blockedPackages) {
                    showBlockOverlay(foregroundPackage)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Obtiene el paquete de la app actualmente en foreground.
     * Usa UsageStatsManager con una ventana de 5 segundos.
     */
    private fun getForegroundPackage(usageStatsManager: UsageStatsManager): String? {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 5000

        val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
        var lastForegroundPackage: String? = null

        val event = android.app.usage.UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                lastForegroundPackage = event.packageName
            }
        }

        return lastForegroundPackage
    }

    /**
     * Lanza la BlockOverlayActivity sobre la app bloqueada.
     */
    private fun showBlockOverlay(blockedPackage: String) {
        val pm = packageManager
        val appName = try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(
                    blockedPackage,
                    android.content.pm.PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(blockedPackage, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            blockedPackage
        }

        val intent = Intent(this, BlockOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(BlockOverlayActivity.EXTRA_BLOCKED_APP_NAME, appName)
            putExtra(BlockOverlayActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
        }
        startActivity(intent)
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        AppBlockerManager.setBlockerActive(false)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bloqueador de Apps",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Monitoreo de apps durante el modo enfoque"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Bloqueador activo")
            .setContentText("Apps de distracción bloqueadas durante tu sesión de enfoque")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        stopMonitoring()
        serviceScope.cancel()
        super.onDestroy()
    }
}
