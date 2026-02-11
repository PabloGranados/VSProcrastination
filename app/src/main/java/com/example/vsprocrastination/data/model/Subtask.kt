package com.example.vsprocrastination.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Subtarea — Descomposición de tareas grandes.
 * 
 * JUSTIFICACIÓN CIENTÍFICA (Bandura, 1997 - Self-Efficacy Theory):
 * Las tareas grandes generan "parálisis de inicio" porque el cerebro
 * percibe baja autoeficacia ("no puedo con esto").
 * Dividir en pasos pequeños aumenta la percepción de control
 * y genera micro-recompensas al completar cada paso.
 * 
 * JUSTIFICACIÓN ADICIONAL (Locke & Latham - Goal-Setting Theory):
 * Las metas específicas y alcanzables producen mejor rendimiento
 * que las metas vagas o demasiado ambiciosas.
 */
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class Subtask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val name: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    // Firebase sync field (v4)
    val firebaseId: String? = null
)
