package com.example.vsprocrastination.data.model

/**
 * Dificultad de la tarea - Simplificado a solo 2 opciones
 * para reducir la "parálisis por decisión" (Ley de Hick).
 * 
 * FÁCIL: Tareas que toman <30 min, baja carga cognitiva
 * DIFÍCIL: Tareas que requieren concentración profunda, >30 min
 */
enum class Difficulty(val weight: Float, val label: String, val emoji: String) {
    EASY(1.0f, "Fácil", "✨"),
    MEDIUM(1.25f, "Media", "⚡"),
    HARD(1.5f, "Difícil", "💪")
}
