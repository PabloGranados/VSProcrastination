package com.example.vsprocrastination.domain

/**
 * Sistema de niveles progresivos para Pomodoro.
 *
 * JUSTIFICACIÓN PSICOLÓGICA:
 * La técnica Pomodoro clásica de 25 min no funciona para todos.
 * Las personas con TDAH pueden necesitar empezar con sesiones más cortas,
 * y los que ya tienen buena capacidad de concentración se benefician
 * de sesiones más largas (deep work de Cal Newport).
 *
 * Este sistema:
 * 1. Calibra el punto de partida midiendo el foco natural del usuario
 * 2. Asigna un nivel inicial basado en esa medición
 * 3. Progresa gradualmente a sesiones más largas conforme se completan sesiones
 * 4. Permite personalización manual en cualquier momento
 */
data class PomodoroLevel(
    val index: Int,
    val focusMinutes: Int,
    val breakMinMin: Int,
    val breakMaxMin: Int,
    val label: String,
    val description: String,
    val sessionsToAdvance: Int // Sesiones exitosas para avanzar al siguiente nivel
) {
    val breakRecommendation: String
        get() = if (breakMinMin == breakMaxMin) "${breakMinMin} min"
                else "$breakMinMin-$breakMaxMin min"

    val breakMidpointMinutes: Int
        get() = (breakMinMin + breakMaxMin) / 2
}

object PomodoroLevels {

    val levels = listOf(
        PomodoroLevel(0, 20, 3, 5, "Principiante", "Ideal para empezar a construir el hábito", 5),
        PomodoroLevel(1, 25, 5, 5, "Clásico", "El estándar Pomodoro de Francesco Cirillo", 5),
        PomodoroLevel(2, 30, 5, 7, "Intermedio", "Un paso más allá del clásico", 6),
        PomodoroLevel(3, 40, 8, 10, "Avanzado", "Para sesiones de trabajo más profundo", 6),
        PomodoroLevel(4, 50, 10, 10, "Experto", "Concentración extendida con descanso adecuado", 7),
        PomodoroLevel(5, 60, 10, 15, "Deep Work", "Una hora completa de enfoque intenso", 8),
        PomodoroLevel(6, 90, 20, 30, "Ultra Focus", "El máximo — sesiones de flujo profundo", Int.MAX_VALUE)
    )

    val maxLevelIndex = levels.size - 1

    /**
     * Dado un tiempo de enfoque medido en el cronómetro de calibración,
     * determina el nivel inicial recomendado.
     *
     * Lógica: se asigna al nivel cuyo focusMinutes sea ≤ al tiempo medido.
     * Si el usuario aguantó 35 min → nivel 2 (30 min), no nivel 3 (40 min),
     * porque es mejor empezar cómodo y progresar.
     */
    fun levelForCalibrationMinutes(measuredMinutes: Int): PomodoroLevel {
        // Buscar el nivel más alto cuyo focusMinutes sea ≤ al medido
        val matched = levels.lastOrNull { it.focusMinutes <= measuredMinutes }
        return matched ?: levels.first()
    }

    /**
     * Dado un nivel actual, determina el siguiente nivel.
     * Retorna null si ya es el máximo.
     */
    fun nextLevel(currentIndex: Int): PomodoroLevel? {
        return levels.getOrNull(currentIndex + 1)
    }

    /**
     * Retorna el nivel por índice, o el primero si no existe.
     */
    fun getLevel(index: Int): PomodoroLevel {
        return levels.getOrElse(index) { levels.first() }
    }

    /**
     * Encuentra el nivel más cercano a una duración personalizada.
     */
    fun closestLevel(focusMinutes: Int): PomodoroLevel {
        return levels.minByOrNull { kotlin.math.abs(it.focusMinutes - focusMinutes) }
            ?: levels.first()
    }
}
