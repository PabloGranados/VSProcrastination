package com.example.vsprocrastination.ui.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vsprocrastination.data.database.AppDatabase
import com.example.vsprocrastination.data.model.Difficulty
import com.example.vsprocrastination.data.model.Priority
import com.example.vsprocrastination.data.model.Subtask
import com.example.vsprocrastination.data.model.Task
import com.example.vsprocrastination.data.preferences.PreferencesManager
import com.example.vsprocrastination.data.repository.HabitRepository
import com.example.vsprocrastination.data.repository.TaskRepository
import com.example.vsprocrastination.data.sync.ExportImportManager
import com.example.vsprocrastination.domain.MotivationalPhrases
import com.example.vsprocrastination.domain.PriorityCalculator
import com.example.vsprocrastination.domain.StreakCalculator
import com.example.vsprocrastination.domain.TaskStats
import com.example.vsprocrastination.service.AppLeaveDetector
import com.example.vsprocrastination.service.DeadlineCountdownWorker
import com.example.vsprocrastination.service.FocusService
import com.example.vsprocrastination.service.SmartNotificationWorker
import com.example.vsprocrastination.service.TaskReminderWorker
import com.example.vsprocrastination.service.TimerState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Estado de la UI principal.
 * Inmutable para Compose - cada cambio crea una nueva instancia.
 */
data class MainUiState(
    val suggestedTask: Task? = null,
    val remainingTasks: List<Task> = emptyList(),
    val allTasks: List<Task> = emptyList(),
    val stats: TaskStats = TaskStats(0, 0, 0, 0),
    val isLoading: Boolean = true,
    val isInFocusMode: Boolean = false,
    val timerState: TimerState = TimerState(),
    val showAddTaskDialog: Boolean = false,
    val editingTask: Task? = null,           // Para editar tarea existente
    val skippedTaskIds: Set<Long> = emptySet(), // IDs de tareas "saltadas" temporalmente
    val snackbarMessage: String? = null,
    val lastCompletedTaskId: Long? = null,
    // Nuevas features
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val hasCompletedToday: Boolean = false,
    val showCelebration: Boolean = false,
    val motivationalPhrase: String = "",
    val suggestedTaskSubtasks: List<Subtask> = emptyList(),
    val pomodoroDuration: Int = 25,
    // Export/Import de datos
    val isExportImportInProgress: Boolean = false,
    val exportImportMessage: String? = null,
    // Hábitos (para mapa de calor)
    val habitCompletionsByDay: Map<String, List<String>> = emptyMap()
)

/**
 * ViewModel principal de la app.
 * Integra FocusService, notificaciones y gestión de tareas.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = TaskRepository(database.taskDao(), database.subtaskDao())
    private val habitRepository = HabitRepository(database.habitDao())
    private val exportImportManager = ExportImportManager(database.taskDao(), database.subtaskDao(), database.habitDao())
    val preferencesManager = PreferencesManager(application)
    private val context: Context get() = getApplication()
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    // Flow dedicado para IDs saltados — combinado reactivamente con allTasks
    private val _skippedTaskIds = MutableStateFlow<Set<Long>>(emptySet())
    
    init {
        observeTasks()
        observeHabitData()
        observeFocusService()
        observePreferences()
        initializeNotifications()
    }
    
    /**
     * Observa cambios en las tareas y actualiza el estado de la UI.
     * Respeta las tareas "saltadas" temporalmente.
     */
    private fun observeTasks() {
        viewModelScope.launch {
            combine(repository.allTasks, _skippedTaskIds) { tasks, skippedIds ->
                Pair(tasks, skippedIds)
            }.collect { (tasks, skippedIds) ->
                val currentTime = System.currentTimeMillis()
                
                // Filtrar tareas saltadas para la sugerencia
                val tasksForSuggestion = tasks.filter { it.id !in skippedIds }
                val suggested = PriorityCalculator.getSuggestedTask(tasksForSuggestion, currentTime)
                
                // Remaining incluye las saltadas (aparecen en la lista secundaria)
                val remaining = PriorityCalculator.getTasksSortedByPriority(tasks, true, currentTime)
                    .filter { it.id != suggested?.id }
                val stats = PriorityCalculator.getQuickStats(tasks)
                
                // Programar nagging para tareas vencidas
                scheduleOverdueReminders(tasks)
                
                // Calcular rachas
                val currentStreak = StreakCalculator.calculateCurrentStreak(tasks)
                val bestStreak = StreakCalculator.calculateBestStreak(tasks)
                val hasCompletedToday = StreakCalculator.hasCompletedToday(tasks)
                
                // Frase motivacional contextual
                val phrase = MotivationalPhrases.getContextualPhrase(
                    isHardTask = suggested?.difficulty == Difficulty.HARD,
                    isQuickTask = suggested?.isQuickTask == true,
                    streakDays = currentStreak,
                    hasOverdue = stats.hasOverdueTasks
                )
                
                // Cargar subtareas de la tarea sugerida
                val subtasks = if (suggested != null) {
                    repository.getSubtasksForTaskSync(suggested.id)
                } else emptyList()
                
                _uiState.update { current ->
                    current.copy(
                        suggestedTask = suggested,
                        remainingTasks = remaining,
                        allTasks = tasks,
                        stats = stats,
                        isLoading = false,
                        skippedTaskIds = skippedIds,
                        currentStreak = currentStreak,
                        bestStreak = bestStreak,
                        hasCompletedToday = hasCompletedToday,
                        motivationalPhrase = phrase,
                        suggestedTaskSubtasks = subtasks
                    )
                }
            }
        }
    }
    
    /**
     * Observa datos de hábitos para integrarlos en el mapa de calor.
     * Los hábitos completados aparecen en el ContributionCalendar.
     */
    private fun observeHabitData() {
        viewModelScope.launch {
            combine(
                habitRepository.activeHabits,
                habitRepository.allLogs
            ) { habits, logs ->
                val habitMap = habits.associateBy { it.id }
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                
                // Convertir logs a Map<"yyyy-MM-dd", List<habitName>>
                logs.groupBy { it.dateEpochDay }
                    .mapKeys { (epochDay, _) ->
                        val cal = java.util.Calendar.getInstance()
                        cal.timeInMillis = epochDay.toLong() * 24 * 60 * 60 * 1000L
                        dateFormat.format(cal.time)
                    }
                    .mapValues { (_, dayLogs) ->
                        dayLogs.mapNotNull { log ->
                            habitMap[log.habitId]?.let { "${it.emoji} ${it.name}" }
                        }
                    }
            }.collect { habitsByDay ->
                _uiState.update { it.copy(habitCompletionsByDay = habitsByDay) }
            }
        }
    }
    
    /**
     * Observa el FocusService para sincronizar el timer con la UI.
     */
    private fun observeFocusService() {
        viewModelScope.launch {
            FocusService.timerState.collect { timerState ->
                _uiState.update { current ->
                    current.copy(timerState = timerState)
                }
                
                // Si el timer se completó, registrar el tiempo
                if (timerState.isComplete && timerState.taskId > 0) {
                    repository.addTimeWorked(timerState.taskId, timerState.totalMillis)
                }
            }
        }
        
        viewModelScope.launch {
            FocusService.isRunning.collect { isRunning ->
                _uiState.update { current ->
                    current.copy(isInFocusMode = isRunning)
                }
            }
        }
    }
    
    /**
     * Observa cambios en preferencias del usuario.
     */
    private fun observePreferences() {
        viewModelScope.launch {
            preferencesManager.pomodoroDuration.collect { duration ->
                _uiState.update { it.copy(pomodoroDuration = duration) }
            }
        }
    }
    
    /**
     * Inicializa canales de notificación y recordatorios periódicos.
     */
    private fun initializeNotifications() {
        TaskReminderWorker.createNotificationChannels(context)
        // Cancelar el viejo recordatorio periódico redundante (era cada 2h,
        // pero SmartNotificationWorker ya cubre esta función mejor)
        TaskReminderWorker.cancelPeriodicReminder(context)
        // Countdown estilo Duolingo para tareas a <2h del deadline
        DeadlineCountdownWorker.createNotificationChannel(context)
        DeadlineCountdownWorker.schedule(context)
        // Sistema de notificaciones inteligente basado en ciencia conductual
        // Es el único worker periódico general: cada 3h con ventanas circadianas
        SmartNotificationWorker.schedule(context)
    }
    
    /**
     * Programa recordatorios insistentes para tareas vencidas.
     * Solo si el usuario tiene nagging habilitado.
     */
    private fun scheduleOverdueReminders(tasks: List<Task>) {
        viewModelScope.launch {
            val naggingEnabled = preferencesManager.naggingEnabled.first()
            if (!naggingEnabled) return@launch
            
            val now = System.currentTimeMillis()
            tasks.filter { 
                !it.isCompleted && !it.isStarted &&
                it.deadlineMillis != null && it.deadlineMillis < now 
            }.forEach { task ->
                TaskReminderWorker.scheduleNaggingReminder(context, task.name, task.id)
            }
        }
    }
    
    /**
     * Agrega una nueva tarea con mínima fricción.
     */
    fun addTask(
        name: String,
        deadlineMillis: Long? = null,
        difficulty: Difficulty = Difficulty.EASY,
        priority: Priority = Priority.NORMAL,
        isQuickTask: Boolean = false,
        subtaskNames: List<String> = emptyList()
    ) {
        if (name.isBlank()) return
        
        viewModelScope.launch {
            val taskId = repository.createTask(name, deadlineMillis, difficulty, priority, isQuickTask, subtaskNames)
            
            // Programar recordatorio de deadline si existe
            if (deadlineMillis != null) {
                TaskReminderWorker.scheduleDeadlineReminder(
                    context, name, taskId, deadlineMillis
                )
            }
            
            _uiState.update { it.copy(showAddTaskDialog = false) }
        }
    }
    
    /**
     * Inicia la tarea sugerida y entra en Modo Enfoque.
     * Lanza el FocusService como Foreground Service.
     */
    fun startSuggestedTask(durationMinutes: Int = _uiState.value.pomodoroDuration) {
        val task = _uiState.value.suggestedTask ?: return
        
        viewModelScope.launch {
            repository.startTask(task.id)
            
            // Cancelar notificaciones nagging y countdown para esta tarea
            TaskReminderWorker.cancelNaggingReminder(context, task.id)
            DeadlineCountdownWorker.cancelNotification(context, task.id)
            
            // Limpiar notificación de "vuelve a la app" si existe
            val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifManager.cancel(AppLeaveDetector.NOTIFICATION_ID_RETURN)
            
            // Iniciar Foreground Service
            FocusService.startFocus(context, task.name, task.id, durationMinutes)
        }
    }
    
    /**
     * Marca la tarea actual como completada con soporte para Undo.
     */
    fun completeCurrentTask() {
        val task = _uiState.value.suggestedTask ?: return
        
        viewModelScope.launch {
            // Detener servicio si está corriendo
            if (_uiState.value.isInFocusMode) {
                FocusService.stopFocus(context)
            }
            
            // Cancelar notificaciones de esta tarea
            TaskReminderWorker.cancelNaggingReminder(context, task.id)
            TaskReminderWorker.cancelDeadlineReminder(context, task.id)
            DeadlineCountdownWorker.cancelNotification(context, task.id)
            
            val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifManager.cancel(AppLeaveDetector.NOTIFICATION_ID_RETURN)
            
            repository.completeTask(task.id)
            
            // Mostrar Snackbar + celebración
            _uiState.update { 
                it.copy(
                    snackbarMessage = "✅ \"${task.name}\" completada",
                    lastCompletedTaskId = task.id,
                    showCelebration = true
                )
            }
        }
    }
    
    /**
     * Deshace la última tarea completada (Undo).
     * Previene frustración por taps accidentales.
     */
    fun undoCompleteTask() {
        val taskId = _uiState.value.lastCompletedTaskId ?: return
        
        viewModelScope.launch {
            repository.undoCompleteTask(taskId)
            _uiState.update { 
                it.copy(
                    snackbarMessage = null,
                    lastCompletedTaskId = null
                )
            }
        }
    }
    
    /**
     * Limpia el mensaje del Snackbar.
     */
    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null, lastCompletedTaskId = null) }
    }
    
    /**
     * Oculta la animación de celebración.
     */
    fun dismissCelebration() {
        _uiState.update { it.copy(showCelebration = false) }
    }
    
    /**
     * "Salta" la tarea sugerida temporalmente.
     * JUSTIFICACIÓN PSICOLÓGICA (TDAH):
     * El algoritmo no es perfecto. Si la tarea sugerida te genera
     * demasiada ansiedad en este momento, bloquearla solo empeora las cosas.
     * "Saltar" te da agencia (control percibido) sin eliminar la tarea.
     * Las tareas saltadas reaparecen al reiniciar la app.
     */
    fun skipSuggestedTask() {
        val task = _uiState.value.suggestedTask ?: return
        _skippedTaskIds.update { it + task.id }
    }
    
    /**
     * Resetea las tareas saltadas (al abrir la app).
     */
    fun resetSkippedTasks() {
        _skippedTaskIds.value = emptySet()
    }
    
    /**
     * Abre el diálogo de edición con los datos de la tarea.
     */
    fun startEditingTask(task: Task) {
        _uiState.update { it.copy(editingTask = task) }
    }
    
    /**
     * Guarda los cambios de edición de una tarea.
     */
    fun saveEditedTask(
        name: String,
        deadlineMillis: Long?,
        difficulty: Difficulty,
        priority: Priority,
        isQuickTask: Boolean = false,
        subtaskNames: List<String> = emptyList()
    ) {
        val task = _uiState.value.editingTask ?: return
        
        viewModelScope.launch {
            val updated = task.copy(
                name = name.trim(),
                deadlineMillis = deadlineMillis,
                difficulty = difficulty,
                priority = priority,
                isQuickTask = isQuickTask
            )
            repository.updateTask(updated)
            repository.updateSubtasks(task.id, subtaskNames)
            
            // Re-programar deadline reminder si cambió
            TaskReminderWorker.cancelDeadlineReminder(context, task.id)
            if (deadlineMillis != null) {
                TaskReminderWorker.scheduleDeadlineReminder(
                    context, name, task.id, deadlineMillis
                )
            }
            
            _uiState.update { it.copy(editingTask = null) }
        }
    }
    
    /**
     * Cancela la edición.
     */
    fun cancelEditing() {
        _uiState.update { it.copy(editingTask = null) }
    }
    
    /**
     * Cancela el modo enfoque sin completar la tarea.
     */
    fun cancelFocusMode() {
        viewModelScope.launch {
            val timerState = _uiState.value.timerState
            
            // Registrar el tiempo trabajado si fue significativo (>1 min)
            if (timerState.taskId > 0) {
                val timeWorked = timerState.totalMillis - timerState.remainingMillis
                if (timeWorked > 60000) {
                    repository.addTimeWorked(timerState.taskId, timeWorked)
                }
            }
            
            FocusService.stopFocus(context)
            
            val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifManager.cancel(AppLeaveDetector.NOTIFICATION_ID_RETURN)
        }
    }
    
    /**
     * Elimina una tarea.
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            TaskReminderWorker.cancelNaggingReminder(context, task.id)
            TaskReminderWorker.cancelDeadlineReminder(context, task.id)
            DeadlineCountdownWorker.cancelNotification(context, task.id)
            repository.deleteTask(task)
        }
    }
    
    /**
     * Muestra/oculta el diálogo de agregar tarea.
     */
    fun toggleAddTaskDialog(show: Boolean) {
        _uiState.update { it.copy(showAddTaskDialog = show) }
    }
    
    /**
     * Limpia todas las tareas completadas.
     */
    fun clearCompletedTasks() {
        viewModelScope.launch {
            repository.clearCompletedTasks()
        }
    }
    
    /**
     * Llamado cuando la app pasa a background durante modo enfoque.
     * Envía un broadcast al AppLeaveDetector.
     */
    fun onAppPaused() {
        if (_uiState.value.isInFocusMode) {
            val intent = android.content.Intent(AppLeaveDetector.ACTION_APP_LEFT)
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        }
    }
    
    /**
     * Llamado cuando la app vuelve a primer plano.
     * Limpia la notificación de "vuelve a la app".
     */
    fun onAppResumed() {
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.cancel(AppLeaveDetector.NOTIFICATION_ID_RETURN)
    }
    
    /**
     * Toggle una subtarea completada/pendiente.
     */
    fun toggleSubtask(subtaskId: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleSubtask(subtaskId, completed)
            // Recargar subtareas de la tarea sugerida
            val suggested = _uiState.value.suggestedTask ?: return@launch
            val subtasks = repository.getSubtasksForTaskSync(suggested.id)
            _uiState.update { it.copy(suggestedTaskSubtasks = subtasks) }
        }
    }
    
    /**
     * Obtiene subtareas de una tarea (para edición).
     */
    suspend fun getSubtasksForTask(taskId: Long): List<Subtask> {
        return repository.getSubtasksForTaskSync(taskId)
    }
    
    // === Export/Import de datos ===
    
    /**
     * Exporta todos los datos a un archivo JSON.
     * @param uri URI del archivo destino (obtenido via SAF ACTION_CREATE_DOCUMENT)
     */
    fun exportData(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportImportInProgress = true) }
            val result = exportImportManager.exportToJson(context, uri)
            _uiState.update {
                it.copy(
                    isExportImportInProgress = false,
                    exportImportMessage = result.message
                )
            }
        }
    }
    
    /**
     * Importa datos desde un archivo JSON.
     * Hace merge: no borra datos existentes, solo agrega nuevos.
     * @param uri URI del archivo origen (obtenido via SAF ACTION_OPEN_DOCUMENT)
     */
    fun importData(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportImportInProgress = true) }
            val result = exportImportManager.importFromJson(context, uri)
            _uiState.update {
                it.copy(
                    isExportImportInProgress = false,
                    exportImportMessage = result.message
                )
            }
        }
    }
    
    /**
     * Limpia el mensaje de export/import.
     */
    fun dismissExportImportMessage() {
        _uiState.update { it.copy(exportImportMessage = null) }
    }
}
