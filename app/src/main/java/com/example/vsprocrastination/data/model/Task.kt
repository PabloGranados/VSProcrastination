package com.example.vsprocrastination.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * NOTA DE MIGRACIÓN v1→v2:
 * Se añadió el campo 'priority' (Priority enum, default NORMAL).
 * La migración Room agrega la columna con ALTER TABLE.
 * Los datos existentes se preservan con prioridad NORMAL.
 */

/**
 * Entidad Task - Diseñada para mínima fricción cognitiva.
 * 
 * ALGORITMO DE PRIORIZACIÓN (Matriz Eisenhower Oculta):
 * =====================================================
 * Score = (Urgencia × 2) + (Dificultad × 1.5) + Bonus
 * 
 * Donde:
 * - Urgencia: Inversamente proporcional al tiempo restante hasta el deadline
 *   • Sin deadline = urgencia base de 0.5 (prioridad media-baja)
 *   • Deadline hoy = urgencia 10 (máxima)
 *   • Deadline mañana = urgencia 8
 *   • Deadline en 7+ días = urgencia 2
 * 
 * - Dificultad: Las tareas DIFÍCILES tienen un bonus de 1.5x
 *   Justificación psicológica: El cerebro con TDAH tiende a evitar
 *   tareas difíciles. Al darles mayor peso, forzamos su aparición
 *   temprano cuando hay más energía (efecto "Eat That Frog" de Brian Tracy).
 * 
 * - Bonus de "Tarea Iniciada": +3 puntos si ya se empezó
 *   Justificación: Efecto Zeigarnik - las tareas incompletas
 *   permanecen en la mente. Priorizar continuarlas reduce ansiedad.
 */
@Entity(tableName = "tasks")
@TypeConverters(TaskConverters::class)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    
    // Deadline opcional - null significa "sin fecha límite"
    val deadlineMillis: Long? = null,
    
    val difficulty: Difficulty = Difficulty.EASY,
    
    // Prioridad definida por el usuario (v2)
    val priority: Priority = Priority.NORMAL,
    
    // Estados de la tarea
    val isCompleted: Boolean = false,
    val isStarted: Boolean = false,  // Para el bonus Zeigarnik
    
    // Regla de los 2 minutos (David Allen, GTD):
    // Si la tarea toma menos de 2 minutos, hazla YA.
    val isQuickTask: Boolean = false,
    
    // Timestamps para analytics futuros
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    
    // Tiempo trabajado en la tarea (para Pomodoro)
    val totalTimeWorkedMillis: Long = 0,
    
    // Firebase sync fields (v4)
    val firebaseId: String? = null,
    val lastModifiedAt: Long = System.currentTimeMillis()
) {
    /**
     * Calcula el score de prioridad de la tarea.
     * Mayor score = mayor prioridad = aparece primero.
     */
    fun calculatePriorityScore(currentTimeMillis: Long = System.currentTimeMillis()): Float {
        if (isCompleted) return -1f // Tareas completadas van al final
        
        val urgencyScore = calculateUrgencyScore(currentTimeMillis)
        val difficultyScore = difficulty.weight
        val priorityScore = priority.weight
        val startedBonus = if (isStarted) 3f else 0f
        val quickBonus = if (isQuickTask) 5f else 0f  // Regla 2 min: prioridad alta
        
        // Fórmula v2: Urgencia×2 + Dificultad×1.5 + Prioridad×2.5 + Zeigarnik + QuickTask
        return (urgencyScore * 2f) + (difficultyScore * 1.5f) + (priorityScore * 2.5f) + startedBonus + quickBonus
    }
    
    /**
     * Calcula la urgencia basada en el tiempo restante hasta el deadline.
     * Usa una curva exponencial para crear más "presión" cerca del deadline.
     */
    private fun calculateUrgencyScore(currentTimeMillis: Long): Float {
        val deadline = deadlineMillis ?: return 0.5f // Sin deadline = prioridad media-baja
        
        val timeRemainingMillis = deadline - currentTimeMillis
        val hoursRemaining = timeRemainingMillis / (1000f * 60f * 60f)
        
        return when {
            hoursRemaining <= 0 -> 15f      // ¡VENCIDA! Máxima urgencia
            hoursRemaining <= 2 -> 12f      // Menos de 2 horas
            hoursRemaining <= 6 -> 10f      // Menos de 6 horas
            hoursRemaining <= 24 -> 8f      // Hoy
            hoursRemaining <= 48 -> 6f      // Mañana
            hoursRemaining <= 72 -> 4f      // En 3 días
            hoursRemaining <= 168 -> 2f     // Esta semana
            else -> 1f                       // Más de una semana
        }
    }
    
    /**
     * Texto amigable para mostrar el deadline.
     */
    fun getDeadlineDisplayText(currentTimeMillis: Long = System.currentTimeMillis()): String {
        val deadline = deadlineMillis ?: return "Sin fecha límite"
        
        val timeRemainingMillis = deadline - currentTimeMillis
        val hoursRemaining = timeRemainingMillis / (1000f * 60f * 60f)
        val minutesRemaining = timeRemainingMillis / (1000f * 60f)
        
        // Formatear hora del deadline
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = deadline
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        val timeStr = if (hour != 23 || minute != 59) {
            " ${String.format("%02d:%02d", hour, minute)}"
        } else ""
        
        return when {
            hoursRemaining <= 0 -> "⚠️ ¡VENCIDA!"
            minutesRemaining <= 60 -> "🔥 ${minutesRemaining.toInt()} min"
            hoursRemaining <= 2 -> "🔥 ${hoursRemaining.toInt()}h$timeStr"
            hoursRemaining <= 24 -> "⏰ Hoy$timeStr"
            hoursRemaining <= 48 -> "📅 Mañana$timeStr"
            hoursRemaining <= 72 -> "📅 En 2-3 días$timeStr"
            hoursRemaining <= 168 -> "📆 Esta semana"
            else -> "📆 Más de una semana"
        }
    }
}

/**
 * Converters para Room - permite guardar los enums Difficulty y Priority.
 */
class TaskConverters {
    @TypeConverter
    fun fromDifficulty(difficulty: Difficulty): String = difficulty.name
    
    @TypeConverter
    fun toDifficulty(value: String): Difficulty = Difficulty.valueOf(value)
    
    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name
    
    @TypeConverter
    fun toPriority(value: String): Priority = Priority.valueOf(value)
}
