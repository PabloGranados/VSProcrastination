package com.example.vsprocrastination.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Habit — Hábitos diarios recurrentes.
 * 
 * JUSTIFICACIÓN CIENTÍFICA (B.J. Fogg — "Tiny Habits"):
 * Los hábitos son comportamientos que queremos automatizar.
 * A diferencia de las tareas (que se completan y desaparecen),
 * los hábitos se repiten diariamente y su valor está en la
 * consistencia, no en la completación individual.
 * 
 * JUSTIFICACIÓN ADICIONAL (James Clear — "Atomic Habits"):
 * "You do not rise to the level of your goals. You fall to the
 * level of your systems." Los hábitos SON el sistema.
 * Trackearlos visualmente activa el efecto "Don't Break the Chain"
 * ya implementado en la app con el calendario de actividad.
 * 
 * DISEÑO:
 * - Separado de Task intencionalmente: las tareas se priorizan
 *   y compiten entre sí, los hábitos son todos iguales y diarios.
 * - Sin deadline, sin dificultad, sin prioridad — solo "¿lo hiciste hoy?"
 * - El emoji como ícono reduce fricción vs seleccionar de un catálogo.
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Nombre corto del hábito (ej: "Leer 20 min", "Ejercicio", "Meditar") */
    val name: String,
    
    /** Emoji representativo — visual rápido sin necesidad de íconos vectoriales */
    val emoji: String = "✅",
    
    /** Timestamp de creación */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** Hábito archivado (soft delete) — preserva historial sin mostrarlo activo */
    val isArchived: Boolean = false,
    
    /** Firebase sync fields (preparado para sincronización futura) */
    val firebaseId: String? = null,
    val lastModifiedAt: Long = System.currentTimeMillis()
)
