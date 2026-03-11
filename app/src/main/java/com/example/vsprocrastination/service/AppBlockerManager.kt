package com.example.vsprocrastination.service

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestiona la lista de apps bloqueadas y el estado del blocker.
 *
 * JUSTIFICACIÓN CIENTÍFICA (Fricción Ambiental — James Clear, Atomic Habits):
 * "Haz los malos hábitos difíciles". Al requerir un esfuerzo consciente
 * para acceder a apps de distracción durante el Pomodoro, se rompe el
 * automatismo del hábito y se fuerza una decisión deliberada.
 *
 * JUSTIFICACIÓN (Pre-Commitment — Ariely, Predictably Irrational):
 * Al activar el blocker ANTES de empezar, el usuario se compromete
 * anticipadamente, como Ulises atándose al mástil.
 */
class AppBlockerManager(private val context: Context) {

    companion object {
        /** Apps populares de distracción sugeridas para bloquear */
        val SUGGESTED_PACKAGES = listOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",        // TikTok
            "com.google.android.youtube",
            "com.twitter.android",
            "com.facebook.katana",
            "com.facebook.orca",               // Messenger
            "com.whatsapp",
            "org.telegram.messenger",
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.pinterest",
            "com.discord",
            "com.netflix.mediaclient",
            "com.amazon.avod",                 // Prime Video
            "com.spotify.music",
            "tv.twitch.android.app"
        )

        private val _isBlockerActive = MutableStateFlow(false)
        val isBlockerActive: StateFlow<Boolean> = _isBlockerActive.asStateFlow()

        fun setBlockerActive(active: Boolean) {
            _isBlockerActive.value = active
        }
    }

    /**
     * Verifica si el permiso PACKAGE_USAGE_STATS está concedido.
     * Este permiso solo se puede otorgar desde Ajustes del sistema.
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Verifica si el permiso SYSTEM_ALERT_WINDOW está concedido.
     */
    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Abre la pantalla de Ajustes para otorgar permiso de Usage Stats.
     */
    fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Abre la pantalla de Ajustes para otorgar permiso de Overlay.
     */
    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Verifica si ambos permisos necesarios están concedidos.
     */
    fun hasAllPermissions(): Boolean {
        return hasUsageStatsPermission() && hasOverlayPermission()
    }

    /**
     * Obtiene la lista de apps instaladas que son lanzables (tienen launcher)
     * y no son apps del sistema, excluyendo nuestra propia app.
     */
    fun getBlockableApps(): List<BlockableApp> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        return resolveInfos
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                // Excluir nuestra app y apps de sistema esenciales
                if (packageName == context.packageName) return@mapNotNull null
                if (isEssentialSystemApp(packageName)) return@mapNotNull null

                try {
                    val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getApplicationInfo(
                            packageName,
                            PackageManager.ApplicationInfoFlags.of(0)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getApplicationInfo(packageName, 0)
                    }
                    BlockableApp(
                        packageName = packageName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        isSuggested = packageName in SUGGESTED_PACKAGES
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .distinctBy { it.packageName }
            .sortedWith(compareByDescending<BlockableApp> { it.isSuggested }.thenBy { it.appName })
    }

    private fun isEssentialSystemApp(packageName: String): Boolean {
        val essentials = setOf(
            "com.android.settings",
            "com.android.systemui",
            "com.android.phone",
            "com.android.dialer",
            "com.android.contacts",
            "com.android.emergency",
            "com.google.android.dialer",
            "com.google.android.contacts",
            "com.samsung.android.dialer",
            "com.samsung.android.contacts"
        )
        return packageName in essentials
    }
}

/**
 * Representa una app que puede ser bloqueada.
 */
data class BlockableApp(
    val packageName: String,
    val appName: String,
    val isSuggested: Boolean = false
)
