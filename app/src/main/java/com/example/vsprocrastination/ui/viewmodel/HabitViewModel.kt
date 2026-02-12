package com.example.vsprocrastination.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vsprocrastination.data.database.AppDatabase
import com.example.vsprocrastination.data.model.Habit
import com.example.vsprocrastination.data.model.HabitLog
import com.example.vsprocrastination.data.repository.HabitRepository
import com.example.vsprocrastination.domain.StreakCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Estado de la UI del Habit Tracker.
 */
data class HabitUiState(
    val habits: List<HabitWithStatus> = emptyList(),
    val todayCompletedCount: Int = 0,
    val todayTotalCount: Int = 0,
    val showAddHabitDialog: Boolean = false,
    val editingHabit: Habit? = null,
    val isLoading: Boolean = true
)

/**
 * Un hábito con su estado de completación de hoy y su racha.
 */
data class HabitWithStatus(
    val habit: Habit,
    val isCompletedToday: Boolean,
    val currentStreak: Int
)

/**
 * ViewModel para el Habit Tracker.
 * 
 * Sigue la misma arquitectura que MainViewModel:
 * AndroidViewModel + StateFlow + Repository pattern.
 */
class HabitViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = HabitRepository(database.habitDao())
    
    private val _uiState = MutableStateFlow(HabitUiState())
    val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()
    
    init {
        observeHabits()
    }
    
    /**
     * Observa hábitos y logs reactivamente.
     * Combina los flows de hábitos activos y logs de hoy
     * para generar el estado completo de la UI.
     */
    private fun observeHabits() {
        viewModelScope.launch {
            combine(
                repository.activeHabits,
                repository.allLogs
            ) { habits, allLogs ->
                val todayEpochDay = StreakCalculator.dayKey(System.currentTimeMillis())
                val todayLogs = allLogs.filter { it.dateEpochDay == todayEpochDay }
                val todayCompletedIds = todayLogs.map { it.habitId }.toSet()
                
                // Calcular racha por hábito
                val habitsWithStatus = habits.map { habit ->
                    val habitLogDays = allLogs
                        .filter { it.habitId == habit.id }
                        .map { it.dateEpochDay }
                        .toSet()
                    
                    HabitWithStatus(
                        habit = habit,
                        isCompletedToday = habit.id in todayCompletedIds,
                        currentStreak = repository.calculateStreakFromDays(habitLogDays)
                    )
                }
                
                HabitUiState(
                    habits = habitsWithStatus,
                    todayCompletedCount = todayCompletedIds.count { id -> 
                        habits.any { it.id == id }
                    },
                    todayTotalCount = habits.size,
                    showAddHabitDialog = _uiState.value.showAddHabitDialog,
                    editingHabit = _uiState.value.editingHabit,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    /** Toggle completación de un hábito hoy. */
    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            repository.toggleHabitForToday(habitId)
        }
    }
    
    /** Crea un nuevo hábito. */
    fun addHabit(name: String, emoji: String = "✅") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createHabit(name, emoji)
            _uiState.update { it.copy(showAddHabitDialog = false) }
        }
    }
    
    /** Actualiza un hábito existente. */
    fun updateHabit(habitId: Long, name: String, emoji: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val habit = database.habitDao().getHabitById(habitId) ?: return@launch
            repository.updateHabit(habit.copy(name = name.trim(), emoji = emoji))
            _uiState.update { it.copy(editingHabit = null) }
        }
    }
    
    /** Archiva un hábito (soft delete). */
    fun archiveHabit(habitId: Long) {
        viewModelScope.launch {
            repository.archiveHabit(habitId)
        }
    }
    
    /** Abre/cierra el diálogo de agregar hábito. */
    fun toggleAddHabitDialog(show: Boolean) {
        _uiState.update { it.copy(showAddHabitDialog = show) }
    }
    
    /** Abre el diálogo de edición para un hábito. */
    fun startEditingHabit(habit: Habit) {
        _uiState.update { it.copy(editingHabit = habit) }
    }
    
    /** Cierra el diálogo de edición. */
    fun cancelEditing() {
        _uiState.update { it.copy(editingHabit = null) }
    }
}
