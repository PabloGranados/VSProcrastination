package com.example.vsprocrastination.service

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
import com.example.vsprocrastination.data.model.Task
import com.example.vsprocrastination.data.preferences.PreferencesManager
import com.example.vsprocrastination.domain.StreakCalculator
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * SmartNotificationWorker — Sistema de notificaciones inteligente basado en ciencia.
 * 
 * JUSTIFICACIÓN CIENTÍFICA:
 * ========================
 * 
 * 1. RITMOS CIRCADIANOS (Monk & Folkard, 1992):
 *    La productividad cognitiva sigue un patrón durante el día.
 *    Las notificaciones se adaptan al momento del día para máximo impacto:
 *    - Mañana (8-10): Energía alta → tareas difíciles
 *    - Mediodía (12-14): Bajón post-prandial → tareas rápidas
 *    - Tarde (16-18): Segundo pico → revisión y planificación
 *    - Noche (20-22): Reflexión → registrar pendientes del día siguiente
 * 
 * 2. IMPLEMENTATION INTENTIONS (Gollwitzer, 1999):
 *    "Si [situación], entonces [acción]" duplica la probabilidad de actuar.
 *    Las notificaciones formulan la acción específica, no solo un recordatorio genérico.
 * 
 * 3. EFECTO ZEIGARNIK + COMMITMENT DEVICES (Rogers et al., 2015):
 *    Recordar tareas iniciadas pero incompletas genera tensión psicológica
 *    que motiva a completarlas. Las notificaciones explotan esto.
 * 
 * 4. PRIMING PSICOLÓGICO (Bargh et al., 1996):
 *    Incluir datos científicos en las notificaciones "prima" al usuario
 *    hacia comportamientos productivos inconscientemente.
 * 
 * 5. PLANNING FALLACY (Kahneman & Tversky, 1979):
 *    Las personas subestiman el tiempo que les toma hacer cosas.
 *    Las notificaciones nocturnas incentivan planificar el día siguiente
 *    para contrarrestar este sesgo.
 */
class SmartNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val WORK_TAG = "smart_notification_scheduler"
        const val NOTIFICATION_ID_SMART = 7000
        
        const val KEY_NOTIFICATION_CATEGORY = "notification_category"
        const val CATEGORY_MORNING_KICKSTART = "morning_kickstart"
        const val CATEGORY_MIDDAY_QUICK = "midday_quick"
        const val CATEGORY_AFTERNOON_REVIEW = "afternoon_review"
        const val CATEGORY_EVENING_PLAN = "evening_plan"
        const val CATEGORY_SCIENCE_FACT = "science_fact"
        const val CATEGORY_STREAK_RISK = "streak_risk"
        const val CATEGORY_SMART_PERIODIC = "smart_periodic"
        
        /**
         * Programa el sistema de notificaciones inteligente.
         * Ejecuta cada 4 horas (antes 3h) y decide qué tipo de notificación enviar.
         * Límite: máximo 3 notificaciones inteligentes por día.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SmartNotificationWorker>(
                4, TimeUnit.HOURS,    // Cada 4 horas (antes 3h)
                1, TimeUnit.HOURS     // Flex interval de 1h
            )
                .setInputData(workDataOf(
                    KEY_NOTIFICATION_CATEGORY to CATEGORY_SMART_PERIODIC
                ))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .addTag(WORK_TAG)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
        
        /**
         * Cancela el sistema de notificaciones inteligente.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG)
        }
    }
    
    // ====================================================================
    // BANCO DE MENSAJES CIENTÍFICOS POR CATEGORÍA
    // ====================================================================
    
    /**
     * Mañana (8-10): Arrancada del día con la tarea más pesada.
     * Basado en "Eat That Frog" (Brian Tracy) + Cronobiología.
     */
    private fun morningMessages(topTask: String, pendingCount: Int) = listOf(
        "🌅 Tu cortisol está al máximo ahora — es tu momento de mayor enfoque.\n\n🎯 Tu tarea prioritaria: $topTask\n\n💡 Dato: El pico de cortisol matutino (8-10 AM) mejora la concentración un 30% (Monk, 1992).",
        "🐸 Brian Tracy: «Cómete el sapo más grande primero».\n\n📌 Tu sapo de hoy: $topTask\n\nTienes $pendingCount tareas pendientes. Empieza con esta y el resto será más fácil.",
        "🧠 Tu cerebro tiene ~4 horas de trabajo profundo al día (Newport, \"Deep Work\").\n\n⏰ No las desperdicies — tu tarea más importante:\n$topTask",
        "☀️ Buenos días. Tienes $pendingCount cosas por hacer.\n\n🎯 El algoritmo recomienda empezar con: $topTask\n\n💡 Investigación: Hacer la tarea difícil primero reduce la ansiedad del día un 40%.",
        "🔬 Baumeister (2011): La fuerza de voluntad es máxima por la mañana y se agota durante el día.\n\n💪 Aprovéchala ahora → $topTask"
    )
    
    /**
     * Mediodía (12-14): Motivar con tareas rápidas durante el bajón post-comida.
     */
    private fun middayMessages(quickTaskCount: Int, topTask: String) = listOf(
        "⚡ Después de comer el cerebro baja la guardia.\n\n🎯 Truco científico: Haz tareas cortas ahora para mantener el momentum.\n\nTu siguiente: $topTask",
        "🍽️ El bajón post-prandial es real (Sleep Med Rev, 2014).\n\n💡 Solución: Tareas de 2 minutos. ${if (quickTaskCount > 0) "Tienes $quickTaskCount tareas rápidas esperando." else "Tu siguiente paso: $topTask"}",
        "⏰ Regla de los 2 minutos (David Allen, GTD):\n«Si algo toma menos de 2 min, hazlo ahora.»\n\n📋 ¿Puedes avanzar algo rápido con: $topTask?",
        "🧪 Microproductividad: Completar tareas pequeñas entre comidas libera dopamina y re-activa tu enfoque.\n\n🎯 Siguiente tarea: $topTask"
    )
    
    /**
     * Tarde (16-18): Segundo aire — revisar progreso y cerrar pendientes.
     */
    private fun afternoonMessages(completedToday: Int, pendingCount: Int, topTask: String) = listOf(
        "📊 Llevas $completedToday tarea${if (completedToday != 1) "s" else ""} completada${if (completedToday != 1) "s" else ""} hoy.\n\n${if (completedToday > 0) "👏 ¡Buen ritmo!" else "⚡ Aún hay tiempo."} Quedan $pendingCount pendientes.\n\n🎯 Siguiente: $topTask",
        "🕐 Segundo pico de energía: 16-18h (Ritmo circadiano de la temperatura corporal).\n\n📌 Aprovéchalo para: $topTask\n\n${if (pendingCount <= 2) "¡Estás a punto de cerrar el día!" else ""}",
        "🧠 Efecto Zeigarnik: Las tareas que empezaste pero no terminaste ocupan espacio mental.\n\n¿Puedes cerrar algo antes de que acabe el día?\n🎯 $topTask",
        "📈 El progreso visible aumenta la motivación (Amabile & Kramer, 2011).\n\nHoy: $completedToday completadas${if (completedToday > 0) " ✅" else ""}. ¿Sumamos una más?\n🎯 $topTask"
    )
    
    /**
     * Noche (20-22): Reflexión y planificación para mañana.
     * Combate la "Planning Fallacy" de Kahneman.
     */
    private fun eveningMessages(completedToday: Int, pendingCount: Int) = listOf(
        "🌙 Reflexión del día: Completaste $completedToday tarea${if (completedToday != 1) "s" else ""}.\n\n${if (completedToday > 0) "✅ ¡Bien hecho!" else "📝 Mañana será mejor."}\n\n💡 Kahneman: Planificar la noche anterior reduce la \"Planning Fallacy\". Abre la app y revisa tus pendientes para mañana.",
        "📝 Los más productivos planifican el día siguiente antes de dormir (Cal Newport, \"Deep Work\").\n\n📋 Tienes $pendingCount pendientes. ¿Quieres revisarlos y priorizar para mañana?",
        "🧘 Cierre del día:\n✅ Completadas hoy: $completedToday\n📋 Pendientes: $pendingCount\n\n💡 Escribir las tareas de mañana libera memoria de trabajo y mejora el sueño (Scullin et al., 2018, Journal of Experimental Psychology).",
        "🌃 Descanso merecido — pero planificar 5 min ahora te ahorrará 30 min mañana.\n\n📌 La inercia del sueño dificulta decidir por la mañana. Deja tus tareas listas ahora.\n\nPendientes: $pendingCount",
        "🔬 Estudio (2018, J. Experimental Psychology): Escribir los pendientes del día siguiente antes de dormir reduce el tiempo para conciliar el sueño.\n\n📝 Abre la app, revisa tus $pendingCount tareas y duerme tranquilo."
    )
    
    /**
     * Datos científicos sueltos para notificaciones de refuerzo.
     */
    private val scienceNuggets = listOf(
        "🧠 Dato: El cerebro humano consume 20% de la energía del cuerpo. Procrastinar gasta esa energía en preocupación, no en acción (Pychyl, 2013).",
        "📊 Investigación: Las personas que escriben sus metas tienen 42% más probabilidad de lograrlas (Dominican University, 2015).",
        "⚡ Regla de los 5 segundos (Mel Robbins): Cuando tengas un impulso de actuar, hazlo antes de que pasen 5 segundos. El cerebro sabotea después.",
        "🧪 Efecto Premack: Vincula una tarea desagradable con una recompensa inmediata. Ej: «Después de esta tarea, me tomo un café.» (Premack, 1965)",
        "🔬 Ley de Yerkes-Dodson: Un nivel moderado de estrés mejora el rendimiento. Los deadlines ajustados pueden ser tu aliado, no tu enemigo.",
        "💡 La técnica Pomodoro funciona porque fragmenta el trabajo en bloques cognitivamente manejables de 25 min (Cirillo, 1980s). Tu cerebro procesa mejor en sprints.",
        "🧠 Neurociencia: Completar tareas libera dopamina en el núcleo accumbens. Cada ✅ literalmente te hace sentir bien — tu cerebro quiere más.",
        "📈 Efecto del progreso (Amabile & Kramer, 2011): De todos los impulsores de motivación laboral, el #1 es sentir que avanzas. Cada tarea completada cuenta.",
        "💪 Grit (Angela Duckworth, 2016): La perseverancia y la pasión importan más que el talento. Cada día que vuelves a esta app, estás fortaleciendo tu grit."
    )
    
    /**
     * Mensajes de protección de racha.
     */
    private fun streakMessages(streakDays: Int) = listOf(
        "🔥 ¡Racha de $streakDays días en riesgo!\n\nAún no has completado ninguna tarea hoy. Solo necesitas completar UNA para mantener tu cadena.\n\n🧠 Seinfeld: «Don't break the chain» — Cada día roto duele más que el esfuerzo de mantenerla.",
        "⚠️ Tu racha de $streakDays días se pierde si no completas algo hoy.\n\n💡 Dato: La aversión a la pérdida (Kahneman & Tversky) es 2x más fuerte que el deseo de ganar. ¡No pierdas tu racha!",
        "🏆 $streakDays días de productividad consecutiva.\n\n¿De verdad quieres que hoy sea día cero?\n\nCompleta una tarea — cualquiera — y mantén el momentum."
    )
    
    override suspend fun doWork(): Result {
        // ===== HORAS DE SILENCIO (22:00 - 7:59) =====
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour >= 22 || hour < 8) {
            return Result.success()
        }
        
        // ===== LÍMITE DIARIO: MÁX 3 NOTIFICACIONES INTELIGENTES =====
        // Evita notification fatigue incluso en días donde el worker
        // se ejecuta más veces de lo esperado.
        if (hasReachedDailyCap()) {
            return Result.success()
        }
        
        // Verificar preferencias del usuario antes de notificar
        val prefsManager = PreferencesManager(applicationContext)
        val naggingEnabled = prefsManager.naggingEnabled.first()
        
        val db = AppDatabase.getDatabase(applicationContext)
        val taskDao = db.taskDao()
        val tasks = taskDao.getAllTasksSync()
        
        val pendingTasks = tasks.filter { !it.isCompleted }
        val todayKey = StreakCalculator.dayKey(System.currentTimeMillis())
        val completedToday = tasks.filter { 
            it.isCompleted && it.completedAt != null && 
            StreakCalculator.dayKey(it.completedAt) == todayKey 
        }
        
        // Obtener la tarea top por prioridad
        val topTask = pendingTasks
            .maxByOrNull { it.calculatePriorityScore() }
        val topTaskName = topTask?.name ?: "tu tarea más importante"
        val quickTaskCount = pendingTasks.count { it.isQuickTask }
        
        // Calcular racha usando StreakCalculator (sin duplicar lógica)
        val hasCompletedToday = completedToday.isNotEmpty()
        val streakDays = StreakCalculator.calculateCurrentStreak(tasks)
        
        // Si no hay tareas pendientes, no molestar
        if (pendingTasks.isEmpty()) return Result.success()
        
        // Decidir qué notificación enviar según hora del día y contexto
        val (title, body) = when {
            // Protección de racha: prioridad máxima si es >2 días y no se completó hoy
            // Solo si nagging está habilitado
            naggingEnabled && !hasCompletedToday && streakDays >= 2 && hour >= 18 -> {
                "🔥 ¡Tu racha está en peligro!" to streakMessages(streakDays).random()
            }
            // Mañana: 8-10 AM
            hour in 8..10 -> {
                "🌅 Tu plan del día" to morningMessages(topTaskName, pendingTasks.size).random()
            }
            // Mediodía: 12-14
            hour in 12..14 -> {
                "⚡ Momento perfecto" to middayMessages(quickTaskCount, topTaskName).random()
            }
            // Tarde: 16-18
            hour in 16..18 -> {
                "📊 Tu progreso de hoy" to afternoonMessages(completedToday.size, pendingTasks.size, topTaskName).random()
            }
            // Noche: 20-22
            hour in 20..22 -> {
                "🌙 Reflexión del día" to eveningMessages(completedToday.size, pendingTasks.size).random()
            }
            // Horas no programadas: dato científico aleatorio (con baja probabilidad)
            else -> {
                // Solo enviar en estas horas si hay tareas urgentes
                val hasUrgent = pendingTasks.any { 
                    it.deadlineMillis != null && 
                    it.deadlineMillis - System.currentTimeMillis() < 4 * 60 * 60 * 1000 
                }
                if (hasUrgent) {
                    "🎯 Tarea urgente" to "⏰ \"$topTaskName\" necesita tu atención pronto.\n\n${scienceNuggets.random()}"
                } else {
                    // No enviar notificación en horas no activas
                    return Result.success()
                }
            }
        }
        
        showSmartNotification(title, body, topTask?.id ?: 0)
        incrementDailyCounter()
        
        return Result.success()
    }
    
    // ====================================================================
    // CONTROL DE LÍMITE DIARIO
    // ====================================================================
    
    /**
     * Verifica si ya se alcanzó el límite de 3 notificaciones inteligentes hoy.
     * Usa SharedPreferences (no DataStore) por simplicidad en el Worker.
     */
    private fun hasReachedDailyCap(): Boolean {
        val prefs = applicationContext.getSharedPreferences("smart_notif_prefs", Context.MODE_PRIVATE)
        val todayKey = getTodayKey()
        val lastDay = prefs.getString("last_notif_day", "") ?: ""
        val count = prefs.getInt("daily_notif_count", 0)
        return lastDay == todayKey && count >= 3
    }
    
    /**
     * Incrementa el contador diario de notificaciones inteligentes.
     */
    private fun incrementDailyCounter() {
        val prefs = applicationContext.getSharedPreferences("smart_notif_prefs", Context.MODE_PRIVATE)
        val todayKey = getTodayKey()
        val lastDay = prefs.getString("last_notif_day", "") ?: ""
        prefs.edit().apply {
            if (lastDay == todayKey) {
                putInt("daily_notif_count", prefs.getInt("daily_notif_count", 0) + 1)
            } else {
                putString("last_notif_day", todayKey)
                putInt("daily_notif_count", 1)
            }
            apply()
        }
    }
    
    private fun getTodayKey(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
    
    private fun showSmartNotification(title: String, body: String, taskId: Long) {
        val openIntent = PendingIntent.getActivity(
            applicationContext, NOTIFICATION_ID_SMART,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("task_id", taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, TaskReminderWorker.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body.take(60) + "...")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Era implícitamente DEFAULT, ahora explícito
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_SMART, notification)
    }
    
}
