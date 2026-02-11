package com.example.vsprocrastination.domain

import com.example.vsprocrastination.data.model.Task

/**
 * PriorityCalculator - El cerebro de la app anti-procrastinación.
 * 
 * PRINCIPIOS PSICOLÓGICOS APLICADOS:
 * ==================================
 * 
 * 1. PARÁLISIS POR ANÁLISIS (Barry Schwartz - "The Paradox of Choice"):
 *    Problema: Demasiadas opciones = ninguna decisión.
 *    Solución: Devolvemos UNA sola tarea. Sin listas largas.
 * 
 * 2. EAT THAT FROG (Brian Tracy):
 *    Problema: Evitamos tareas difíciles hasta el final del día.
 *    Solución: Las tareas HARD tienen mayor peso, aparecen primero.
 * 
 * 3. EFECTO ZEIGARNIK (Bluma Zeigarnik):
 *    Problema: Las tareas iniciadas pero no terminadas causan ansiedad.
 *    Solución: Bonus de +3 puntos a tareas ya iniciadas.
 * 
 * 4. LEY DE PARKINSON:
 *    Problema: El trabajo se expande para llenar el tiempo disponible.
 *    Solución: Prioridad exponencial cerca del deadline.
 * 
 * 5. PRINCIPIO DE URGENCIA (Matriz Eisenhower):
 *    Urgente + Importante = HACER AHORA
 *    Importante + No Urgente = Programar
 *    Urgente + No Importante = Delegar (no aplica en app personal)
 *    Ni Urgente + Ni Importante = Eliminar
 */
object PriorityCalculator {
    
    /**
     * Obtiene LA tarea que debes hacer AHORA.
     * 
     * @param tasks Lista de todas las tareas pendientes
     * @param currentTimeMillis Tiempo actual para calcular urgencias
     * @return La única tarea sugerida, o null si no hay tareas pendientes
     * 
     * Esta función elimina la "parálisis por decisión" al devolver
     * exactamente UNA tarea - la más importante según el algoritmo.
     */
    fun getSuggestedTask(
        tasks: List<Task>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Task? {
        return tasks
            .filter { !it.isCompleted }
            .maxByOrNull { it.calculatePriorityScore(currentTimeMillis) }
    }
    
    /**
     * Obtiene la lista de tareas ordenadas por prioridad.
     * Usada para mostrar la lista secundaria (discreta) debajo de la tarea principal.
     * 
     * @param tasks Lista de todas las tareas
     * @param excludeCompleted Si true, excluye las tareas completadas
     * @return Lista ordenada de mayor a menor prioridad
     */
    fun getTasksSortedByPriority(
        tasks: List<Task>,
        excludeCompleted: Boolean = true,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<Task> {
        return tasks
            .filter { !excludeCompleted || !it.isCompleted }
            .sortedByDescending { it.calculatePriorityScore(currentTimeMillis) }
    }
    
    /**
     * Obtiene las tareas restantes (excluyendo la sugerida).
     * Para mostrar en la lista secundaria discreta.
     */
    fun getRemainingTasks(
        tasks: List<Task>,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<Task> {
        val suggested = getSuggestedTask(tasks, currentTimeMillis)
        return getTasksSortedByPriority(tasks, true, currentTimeMillis)
            .filter { it.id != suggested?.id }
    }
    
    /**
     * Calcula estadísticas rápidas para feedback visual.
     */
    fun getQuickStats(tasks: List<Task>): TaskStats {
        val pending = tasks.filter { !it.isCompleted }
        val completed = tasks.filter { it.isCompleted }
        val overdue = pending.filter { 
            it.deadlineMillis != null && it.deadlineMillis < System.currentTimeMillis() 
        }
        val completedToday = completed.filter { 
            it.completedAt != null && isToday(it.completedAt)
        }
        val completedThisWeek = completed.filter {
            it.completedAt != null && isThisWeek(it.completedAt)
        }
        
        return TaskStats(
            totalPending = pending.size,
            overdueCount = overdue.size,
            completedTodayCount = completedToday.size,
            totalTimeWorkedTodayMillis = completedToday.sumOf { it.totalTimeWorkedMillis },
            completedThisWeekCount = completedThisWeek.size,
            totalCompletedCount = completed.size,
            totalTasksCount = tasks.size
        )
    }
    
    private fun isToday(timestamp: Long): Boolean {
        val cal = java.util.Calendar.getInstance()
        val todayYear = cal.get(java.util.Calendar.YEAR)
        val todayDay = cal.get(java.util.Calendar.DAY_OF_YEAR)
        
        cal.timeInMillis = timestamp
        return cal.get(java.util.Calendar.YEAR) == todayYear &&
               cal.get(java.util.Calendar.DAY_OF_YEAR) == todayDay
    }
    
    private fun isThisWeek(timestamp: Long): Boolean {
        val cal = java.util.Calendar.getInstance()
        val thisYear = cal.get(java.util.Calendar.YEAR)
        val thisWeek = cal.get(java.util.Calendar.WEEK_OF_YEAR)
        
        cal.timeInMillis = timestamp
        return cal.get(java.util.Calendar.YEAR) == thisYear &&
               cal.get(java.util.Calendar.WEEK_OF_YEAR) == thisWeek
    }
}

/**
 * Estadísticas rápidas para mostrar en la UI.
 */
data class TaskStats(
    val totalPending: Int,
    val overdueCount: Int,
    val completedTodayCount: Int,
    val totalTimeWorkedTodayMillis: Long,
    val completedThisWeekCount: Int = 0,
    val totalCompletedCount: Int = 0,
    val totalTasksCount: Int = 0
) {
    val hasOverdueTasks: Boolean get() = overdueCount > 0
    val completionRate: Float get() = if (totalTasksCount > 0) totalCompletedCount.toFloat() / totalTasksCount else 0f
    
    fun getCompletedTodayText(): String {
        return if (completedTodayCount > 0) {
            "✅ $completedTodayCount completadas hoy"
        } else {
            "Aún no has completado tareas hoy"
        }
    }
    
    fun getProgressSummary(): String {
        return buildString {
            append("$completedThisWeekCount esta semana")
            if (totalCompletedCount > 0) {
                append(" · $totalCompletedCount total")
            }
        }
    }
}
