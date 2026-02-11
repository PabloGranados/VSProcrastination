package com.example.vsprocrastination.domain

/**
 * MotivationalPhrases — Frases motivacionales contextuales.
 * 
 * JUSTIFICACIÓN CIENTÍFICA (Bargh et al. - Priming Psicológico):
 * Las palabras que leemos activan inconscientemente conceptos
 * relacionados en nuestro cerebro. Frases sobre acción, enfoque
 * y persistencia "priman" al cerebro para esos comportamientos.
 * 
 * JUSTIFICACIÓN ADICIONAL (Self-Determination Theory - Deci & Ryan):
 * Los mensajes que refuerzan competencia ("puedes hacerlo") y
 * autonomía ("tú elegiste esto") aumentan la motivación intrínseca.
 * 
 * Las frases se categorizan por contexto para máximo impacto.
 */
object MotivationalPhrases {
    
    /**
     * Frases para el modo enfoque (Pomodoro activo).
     * Tono: Calma, determinación, presencia.
     */
    val focusMode = listOf(
        "Estás haciendo un gran trabajo. Enfócate.",
        "Un paso a la vez. Este momento es todo lo que importa.",
        "Tu cerebro está construyendo momentum. No pares.",
        "La disciplina es elegir entre lo que quieres ahora y lo que más quieres.",
        "Cada minuto enfocado es una victoria contra la procrastinación.",
        "No necesitas motivación. Ya estás aquí, eso es suficiente.",
        "El flow empieza cuando dejas de resistirte. Suéltate.",
        "Piensa en cómo te sentirás cuando termines esto.",
        "La parte más difícil ya pasó: empezaste.",
        "Tu yo del futuro te agradece este esfuerzo.",
        "Respira. Enfoca. Avanza.",
        "No tienes que ser perfecto, solo consistente."
    )
    
    /**
     * Frases cuando la tarea es difícil.
     * Tono: Reto, coraje, "eat that frog".
     */
    val hardTask = listOf(
        "💪 Las tareas difíciles primero. Así funciona la gente exitosa.",
        "🐸 Cómete ese sapo. Después todo será más fácil.",
        "⚡ Tu cerebro quiere evitar esto. Demuéstrale que tú mandas.",
        "🏋️ Lo difícil hoy es lo fácil mañana. Entrena tu disciplina.",
        "🔥 Si fuera fácil, ya lo habrías hecho. Hazlo de todos modos.",
        "🧠 Mark Twain: 'Si tu trabajo es comerte un sapo, hazlo a primera hora.'"
    )
    
    /**
     * Frases cuando hay racha activa.
     * Tono: Orgullo, momentum, no romper la cadena.
     */
    fun streakPhrases(streakDays: Int) = listOf(
        "🔥 $streakDays días seguidos. ¡No rompas la cadena!",
        "⚡ Racha de $streakDays días. Tu versión pasada estaría impresionada.",
        "🏆 $streakDays días de disciplina. Eso es carácter, no suerte.",
        "💎 Cada día consecutivo refuerza el hábito. Llevas $streakDays.",
        "🚀 $streakDays días. El momentum es tu mejor aliado."
    )
    
    /**
     * Frases cuando hay tareas vencidas.
     * Tono: Urgencia pero sin culpa, orientado a acción.
     */
    val overdueTasks = listOf(
        "⏰ Tienes tareas vencidas. 5 minutos es todo lo que necesitas para empezar.",
        "📍 No te culpes por el pasado. Actúa ahora.",
        "🎯 La mejor hora para empezar era antes. La segunda mejor es ahora.",
        "⚠️ Cada minuto que pospones aumenta la ansiedad. Rompe el ciclo.",
        "💡 No necesitas terminarla, solo necesitas empezarla."
    )
    
    /**
     * Frases para la regla de 2 minutos.
     * Tono: Energía rápida, impulso.
     */
    val quickTask = listOf(
        "⚡ ¡Menos de 2 minutos! Hazla YA y quítatela de encima.",
        "🏃 Tarea rápida detectada. ¿Por qué posponerla?",
        "✨ David Allen: 'Si toma menos de 2 minutos, hazlo ahora.'",
        "🎯 2 minutos. Sin excusas. Sin planificar. Solo actúa."
    )
    
    /**
     * Frases para cuando no hay tareas (estado vacío).
     * Tono: Celebración, descanso merecido.
     */
    val noTasks = listOf(
        "🎉 ¡Inbox zero! Disfruta el momento.",
        "✅ Todo al día. Tu yo del pasado hizo un gran trabajo.",
        "🧘 Sin tareas pendientes. Respira y recarga.",
        "🌟 Momento perfecto para planificar algo nuevo."
    )
    
    /**
     * Selecciona una frase contextual basada en el estado actual.
     */
    fun getContextualPhrase(
        isHardTask: Boolean = false,
        isQuickTask: Boolean = false,
        streakDays: Int = 0,
        hasOverdue: Boolean = false,
        isFocusMode: Boolean = false
    ): String {
        // Prioridad de contexto: focus > quick > hard > streak > overdue > genérica
        val pool = when {
            isFocusMode -> focusMode
            isQuickTask -> quickTask
            isHardTask -> hardTask
            streakDays >= 2 -> streakPhrases(streakDays)
            hasOverdue -> overdueTasks
            else -> focusMode
        }
        return pool.random()
    }
}
