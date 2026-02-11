package com.example.vsprocrastination.data.model

/**
 * Nivel de prioridad definido por el usuario.
 * Complementa la priorización automática del algoritmo.
 *
 * El usuario marca manualmente qué tan importante considera una tarea,
 * y el algoritmo lo integra en el score final con peso ×2.5.
 *
 * JUSTIFICACIÓN: El algoritmo automático es bueno pero no perfecto.
 * Dar al usuario la opción de "marcar como urgente" le da agencia
 * y control percibido (clave para personas con TDAH).
 */
enum class Priority(val weight: Float, val label: String, val emoji: String) {
    LOW(0.5f, "Baja", "🔽"),
    NORMAL(1.0f, "Normal", "➡️"),
    HIGH(2.0f, "Alta", "🔼"),
    URGENT(3.0f, "Urgente", "🔴")
}
