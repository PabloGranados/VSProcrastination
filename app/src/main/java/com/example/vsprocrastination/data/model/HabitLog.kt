package com.example.vsprocrastination.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * HabitLog — Registro de completación diaria de un hábito.
 * 
 * Cada fila representa "el usuario completó el hábito X el día Y".
 * Usa epoch days (como StreakCalculator) para garantizar consistencia
 * entre años y zonas horarias.
 * 
 * La combinación (habitId, dateEpochDay) es única — un hábito solo
 * se puede completar una vez por día. El índice compuesto optimiza
 * las queries más frecuentes: "¿qué hábitos se completaron hoy?"
 */
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["dateEpochDay"]),
        Index(value = ["habitId", "dateEpochDay"], unique = true)
    ]
)
data class HabitLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** ID del hábito al que pertenece este log */
    val habitId: Long,
    
    /** Día en epoch days (días desde 1 Jan 1970, misma lógica que StreakCalculator.dayKey) */
    val dateEpochDay: Int,
    
    /** Timestamp exacto de completación (para analytics futuros) */
    val completedAt: Long = System.currentTimeMillis(),
    
    /** Firebase sync field */
    val firebaseId: String? = null
)
