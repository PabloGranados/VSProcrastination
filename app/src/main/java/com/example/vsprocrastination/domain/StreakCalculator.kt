package com.example.vsprocrastination.domain

import com.example.vsprocrastination.data.model.Task
import java.util.Calendar

/**
 * StreakCalculator — Calcula rachas de productividad.
 * 
 * JUSTIFICACIÓN CIENTÍFICA (B.F. Skinner - Refuerzo Operante):
 * El refuerzo intermitente (variable ratio) es el más resistente
 * a la extinción. Las rachas crean un "efecto de compromiso":
 * romper una racha de 7 días duele más que perder un día individual.
 * 
 * JUSTIFICACIÓN ADICIONAL (Jerry Seinfeld - "Don't Break the Chain"):
 * Marcar días consecutivos crea momentum psicológico.
 * La visualización de la racha actúa como recompensa social interna.
 */
object StreakCalculator {
    
    /**
     * Calcula la racha actual de días consecutivos con al menos
     * una tarea completada.
     * 
     * @return Número de días consecutivos (0 si hoy no hay tareas completadas)
     */
    fun calculateCurrentStreak(tasks: List<Task>): Int {
        val completedTasks = tasks.filter { it.completedAt != null }
        if (completedTasks.isEmpty()) return 0
        
        // Obtener los días únicos (solo fecha, sin hora) con tareas completadas
        val daysWithCompletions = completedTasks
            .mapNotNull { it.completedAt }
            .map { dayKey(it) }
            .toSortedSet()
        
        if (daysWithCompletions.isEmpty()) return 0
        
        // Empezar desde hoy y contar hacia atrás
        val today = dayKey(System.currentTimeMillis())
        
        // Si hoy no hay completaciones, verificar si ayer hubo (racha aún activa)
        val startDay = if (today in daysWithCompletions) {
            today
        } else {
            val yesterday = today - 1
            if (yesterday in daysWithCompletions) yesterday else return 0
        }
        
        var streak = 0
        var currentDay = startDay
        while (currentDay in daysWithCompletions) {
            streak++
            currentDay--
        }
        
        return streak
    }
    
    /**
     * Calcula la mejor racha histórica.
     */
    fun calculateBestStreak(tasks: List<Task>): Int {
        val completedTasks = tasks.filter { it.completedAt != null }
        if (completedTasks.isEmpty()) return 0
        
        val daysWithCompletions = completedTasks
            .mapNotNull { it.completedAt }
            .map { dayKey(it) }
            .toSortedSet()
        
        if (daysWithCompletions.isEmpty()) return 0
        
        var bestStreak = 1
        var currentStreak = 1
        val sortedDays = daysWithCompletions.toList()
        
        for (i in 1 until sortedDays.size) {
            if (sortedDays[i] == sortedDays[i - 1] + 1) {
                currentStreak++
                bestStreak = maxOf(bestStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        
        return bestStreak
    }
    
    /**
     * ¿Se completó al menos una tarea hoy?
     * Útil para saber si la racha está "asegurada" hoy.
     */
    fun hasCompletedToday(tasks: List<Task>): Boolean {
        val today = dayKey(System.currentTimeMillis())
        return tasks.any { it.completedAt != null && dayKey(it.completedAt) == today }
    }
    
    /**
     * Convierte timestamp en un "día clave" usando epoch days.
     * Permite comparar días consecutivos correctamente (incluso entre años).
     */
    fun dayKey(timestamp: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        // Usar días desde epoch: year*366+dayOfYear sería incorrecto en cambio de año.
        // Epoch days = total de días desde 1 Jan 1970, garantiza consecutividad.
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return (cal.timeInMillis / (24 * 60 * 60 * 1000L)).toInt()
    }
}
