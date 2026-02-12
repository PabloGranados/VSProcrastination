package com.example.vsprocrastination.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vsprocrastination.data.model.Difficulty
import com.example.vsprocrastination.data.model.Priority
import com.example.vsprocrastination.data.model.Subtask
import com.example.vsprocrastination.data.model.Task
import com.example.vsprocrastination.ui.viewmodel.MainUiState
import com.example.vsprocrastination.ui.viewmodel.MainViewModel
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla principal de la app.
 * Diseño "cero fricción": muestra UNA tarea principal con botón gigante.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToWeeklySummary: () -> Unit = {},
    onNavigateToHabits: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Mostrar Snackbar con opción Undo cuando se completa una tarea
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.undoCompleteTask()
                SnackbarResult.Dismissed -> viewModel.dismissSnackbar()
            }
        }
    }
    
    // Celebración auto-dismiss después de 2 segundos
    LaunchedEffect(uiState.showCelebration) {
        if (uiState.showCelebration) {
            delay(2500)
            viewModel.dismissCelebration()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isInFocusMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Botón settings pequeño
                    SmallFloatingActionButton(
                        onClick = onNavigateToSettings,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(Icons.Default.Settings, "Ajustes", modifier = Modifier.size(20.dp))
                    }
                    // Botón hábitos
                    SmallFloatingActionButton(
                        onClick = onNavigateToHabits,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text("🔄", modifier = Modifier.size(20.dp), fontSize = 14.sp)
                    }
                    FloatingActionButton(
                        onClick = { viewModel.toggleAddTaskDialog(true) },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, "Agregar tarea")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.isInFocusMode -> FocusModeView(uiState, viewModel)
                uiState.suggestedTask == null -> EmptyState { viewModel.toggleAddTaskDialog(true) }
                else -> MainContent(uiState, viewModel, onNavigateToWeeklySummary)
            }
            
            // Diálogo de agregar tarea
            if (uiState.showAddTaskDialog) {
                TaskDialog(
                    title = "Nueva tarea",
                    initialName = "",
                    initialDifficulty = Difficulty.EASY,
                    initialPriority = Priority.NORMAL,
                    initialDeadlineMillis = null,
                    initialIsQuickTask = false,
                    initialSubtasks = emptyList(),
                    onDismiss = { viewModel.toggleAddTaskDialog(false) },
                    onConfirm = { name, deadline, difficulty, priority, isQuick, subtasks ->
                        viewModel.addTask(name, deadline, difficulty, priority, isQuick, subtasks)
                    }
                )
            }
            
            // Diálogo de editar tarea
            uiState.editingTask?.let { task ->
                var editSubtasks by remember(task.id) { mutableStateOf<List<String>?>(null) }
                LaunchedEffect(task.id) {
                    editSubtasks = viewModel.getSubtasksForTask(task.id).map { it.name }
                }
                
                editSubtasks?.let { subtaskNames ->
                    TaskDialog(
                        title = "Editar tarea",
                        initialName = task.name,
                        initialDifficulty = task.difficulty,
                        initialPriority = task.priority,
                        initialDeadlineMillis = task.deadlineMillis,
                        initialIsQuickTask = task.isQuickTask,
                        initialSubtasks = subtaskNames,
                        onDismiss = { viewModel.cancelEditing() },
                        onConfirm = { name, deadline, difficulty, priority, isQuick, subtasks ->
                            viewModel.saveEditedTask(name, deadline, difficulty, priority, isQuick, subtasks)
                        }
                    )
                }
            }
            
            // Celebración overlay
            if (uiState.showCelebration) {
                CelebrationOverlay()
            }
        }
    }
}

/**
 * Contenido principal con layout adaptativo.
 * Compacto (teléfono) o expandido (tablet/landscape).
 */
@Composable
private fun MainContent(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onNavigateToWeeklySummary: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isExpandedScreen = configuration.screenWidthDp >= 600

    if (isExpandedScreen) {
        ExpandedMainContent(uiState, viewModel, onNavigateToWeeklySummary)
    } else {
        CompactMainContent(uiState, viewModel, onNavigateToWeeklySummary)
    }
}

/**
 * Layout compacto (teléfono vertical): columna única.
 */
@Composable
private fun CompactMainContent(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onNavigateToWeeklySummary: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp)
    ) {
        // Header con stats
        item {
            StatsHeader(uiState, onNavigateToWeeklySummary)
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // Calendario de contribuciones (estilo GitHub)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    ContributionCalendar(
                        tasks = uiState.allTasks,
                        habitCompletionsByDay = uiState.habitCompletionsByDay
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Mini stats compactos debajo del calendario
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Hoy: ${uiState.stats.completedTodayCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Semana: ${uiState.stats.completedThisWeekCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Total: ${uiState.stats.totalCompletedCount}/${uiState.stats.totalTasksCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Tarea principal (Hero Section)
        uiState.suggestedTask?.let { task ->
            item {
                // Frase motivacional contextual
                if (uiState.motivationalPhrase.isNotEmpty()) {
                    Text(
                        text = uiState.motivationalPhrase,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
                
                SuggestedTaskCard(
                    task = task,
                    subtasks = uiState.suggestedTaskSubtasks,
                    onStartClick = { viewModel.startSuggestedTask() },
                    onCompleteClick = { viewModel.completeCurrentTask() },
                    onSkipClick = { viewModel.skipSuggestedTask() },
                    onEditClick = { viewModel.startEditingTask(task) },
                    onSubtaskToggle = { id, completed -> viewModel.toggleSubtask(id, completed) }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        // Lista secundaria (discreta)
        if (uiState.remainingTasks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Después de esta (${uiState.remainingTasks.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.skippedTaskIds.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.resetSkippedTasks() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                "Resetear saltadas",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(uiState.remainingTasks, key = { it.id }) { task ->
                SecondaryTaskItem(
                    task = task,
                    isSkipped = task.id in uiState.skippedTaskIds,
                    onDelete = { viewModel.deleteTask(task) },
                    onEdit = { viewModel.startEditingTask(task) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Layout expandido (tablet/landscape): dos paneles.
 * Izquierda: tarea hero + calendario.
 * Derecha: cola de tareas restantes.
 */
@Composable
private fun ExpandedMainContent(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onNavigateToWeeklySummary: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Header completo
        StatsHeader(uiState, onNavigateToWeeklySummary)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Panel izquierdo: tarea hero + calendario
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Frase motivacional
                if (uiState.motivationalPhrase.isNotEmpty()) {
                    Text(
                        text = uiState.motivationalPhrase,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Tarea sugerida hero
                uiState.suggestedTask?.let { task ->
                    SuggestedTaskCard(
                        task = task,
                        subtasks = uiState.suggestedTaskSubtasks,
                        onStartClick = { viewModel.startSuggestedTask() },
                        onCompleteClick = { viewModel.completeCurrentTask() },
                        onSkipClick = { viewModel.skipSuggestedTask() },
                        onEditClick = { viewModel.startEditingTask(task) },
                        onSubtaskToggle = { id, completed -> viewModel.toggleSubtask(id, completed) }
                    )
                }

                // Calendario de contribuciones
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        ContributionCalendar(
                            tasks = uiState.allTasks,
                            habitCompletionsByDay = uiState.habitCompletionsByDay
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Hoy: ${uiState.stats.completedTodayCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Semana: ${uiState.stats.completedThisWeekCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Total: ${uiState.stats.totalCompletedCount}/${uiState.stats.totalTasksCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(72.dp))
            }

            // Panel derecho: cola de tareas
            LazyColumn(
                modifier = Modifier.weight(0.45f),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                if (uiState.remainingTasks.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cola de tareas (${uiState.remainingTasks.size}):",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.skippedTaskIds.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.resetSkippedTasks() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Resetear saltadas", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(uiState.remainingTasks, key = { it.id }) { task ->
                        SecondaryTaskItem(
                            task = task,
                            isSkipped = task.id in uiState.skippedTaskIds,
                            onDelete = { viewModel.deleteTask(task) },
                            onEdit = { viewModel.startEditingTask(task) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📭", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No hay más tareas en cola",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Header con estadísticas rápidas.
 */
@Composable
private fun StatsHeader(uiState: MainUiState, onNavigateToWeeklySummary: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "VS Procrastination",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = uiState.stats.getCompletedTodayText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Racha
            if (uiState.currentStreak > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    onClick = onNavigateToWeeklySummary
                ) {
                    Text(
                        text = "🔥 ${uiState.currentStreak}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            if (uiState.stats.hasOverdueTasks) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "⚠️ ${uiState.stats.overdueCount}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/**
 * Card principal con la tarea sugerida.
 * Diseño "Hero" para captar atención total.
 */
@Composable
private fun SuggestedTaskCard(
    task: Task,
    subtasks: List<Subtask> = emptyList(),
    onStartClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onSkipClick: () -> Unit,
    onEditClick: () -> Unit,
    onSubtaskToggle: (Long, Boolean) -> Unit = { _, _ -> }
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top row: label + edit icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (task.isStarted) "📍 CONTINÚA CON ESTO" 
                           else if (task.isQuickTask) "⚡ RÁPIDA — MENOS DE 2 MINUTOS"
                           else "🎯 TU SIGUIENTE PASO",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Nombre de la tarea (grande y centrado)
            Text(
                text = task.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Info de deadline y dificultad
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.getDeadlineDisplayText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                DifficultyBadge(task.difficulty)
                PriorityBadge(task.priority)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Subtareas (si existen)
            if (subtasks.isNotEmpty()) {
                val completedCount = subtasks.count { it.isCompleted }
                Text(
                    text = "Pasos ($completedCount/${subtasks.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                subtasks.forEach { subtask ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSubtaskToggle(subtask.id, !subtask.isCompleted) }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = subtask.isCompleted,
                            onCheckedChange = { onSubtaskToggle(subtask.id, it) },
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = subtask.name,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (subtask.isCompleted)
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Botón gigante "EMPEZAR AHORA"
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (task.isStarted) "▶️ CONTINUAR" else "🚀 EMPEZAR AHORA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Botones secundarios: Completar y Saltar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onCompleteClick) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ya la completé")
                }
                
                TextButton(onClick = onSkipClick) {
                    Text("⏭️ Saltar esta vez")
                }
            }
        }
    }
}

/**
 * Badge de dificultad reutilizable.
 */
@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    Surface(
        color = when (difficulty) {
            Difficulty.HARD -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
            Difficulty.MEDIUM -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            Difficulty.EASY -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "${difficulty.emoji} ${difficulty.label}",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = when (difficulty) {
                Difficulty.HARD -> MaterialTheme.colorScheme.error
                Difficulty.MEDIUM -> MaterialTheme.colorScheme.secondary
                Difficulty.EASY -> MaterialTheme.colorScheme.tertiary
            }
        )
    }
}

/**
 * Badge de prioridad reutilizable.
 */
@Composable
private fun PriorityBadge(priority: Priority) {
    if (priority == Priority.NORMAL) return // No mostrar badge para NORMAL
    Surface(
        color = when (priority) {
            Priority.URGENT -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
            Priority.HIGH -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            Priority.LOW -> MaterialTheme.colorScheme.surfaceVariant
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "${priority.emoji} ${priority.label}",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = when (priority) {
                Priority.URGENT -> MaterialTheme.colorScheme.error
                Priority.HIGH -> MaterialTheme.colorScheme.primary
                Priority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

// ProgressSection reemplazada por ContributionCalendar integrado directamente en MainContent

/**
 * Item de tarea secundaria con badge de dificultad y edición al tocar.
 */
@Composable
private fun SecondaryTaskItem(
    task: Task,
    isSkipped: Boolean = false,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = if (isSkipped) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    DifficultyBadge(task.difficulty)
                    PriorityBadge(task.priority)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = task.getDeadlineDisplayText(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isSkipped) {
                        Text(
                            text = "⏭️ Saltada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Vista del Modo Enfoque (Pomodoro activo).
 */
@Composable
private fun FocusModeView(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val timerState = uiState.timerState
    val progress = timerState.progress
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val focusPhrase = remember { 
        com.example.vsprocrastination.domain.MotivationalPhrases.getContextualPhrase(isFocusMode = true) 
    }
    val taskName = timerState.taskName.ifEmpty { 
        uiState.suggestedTask?.name ?: "Tarea" 
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLandscape) {
            // Landscape: layout horizontal de dos paneles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Izquierda: información de la tarea
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "🔒 MODO ENFOQUE",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = taskName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = focusPhrase,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }

                // Derecha: timer + controles
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = timerState.formattedTime,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 72.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(6.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.cancelFocusMode() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pausar")
                        }
                        Button(
                            onClick = { viewModel.completeCurrentTask() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("¡Terminé!")
                        }
                    }
                }
            }
        } else {
        // Portrait: layout vertical
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .padding(32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "🔒 MODO ENFOQUE",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = taskName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Barra de progreso circular implícita
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(6.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.2f),
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Timer gigante
            Text(
                text = timerState.formattedTime,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 72.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Frase motivacional contextual
            Text(
                text = focusPhrase,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Botones
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.cancelFocusMode() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pausar")
                }
                
                Button(
                    onClick = { viewModel.completeCurrentTask() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("¡Terminé!")
                }
            }
        }
        }
    }
}

/**
 * Estado vacío cuando no hay tareas.
 */
@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
    Column(
        modifier = Modifier
            .widthIn(max = 480.dp)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉",
            fontSize = 64.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "¡No tienes tareas pendientes!",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Agrega algo que necesites hacer.\nYo te digo qué hacer primero.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Agregar tarea")
        }
    }
    }
}

/**
 * Estado de carga.
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Diálogo reutilizable para crear y editar tareas.
 * Incluye: dificultad (3 niveles), prioridad (4 niveles),
 * fecha con DatePicker y hora con TimePicker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDialog(
    title: String,
    initialName: String,
    initialDifficulty: Difficulty,
    initialPriority: Priority,
    initialDeadlineMillis: Long?,
    initialIsQuickTask: Boolean = false,
    initialSubtasks: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (name: String, deadline: Long?, difficulty: Difficulty, priority: Priority, isQuickTask: Boolean, subtasks: List<String>) -> Unit
) {
    var taskName by remember { mutableStateOf(initialName) }
    var selectedDifficulty by remember { mutableStateOf(initialDifficulty) }
    var selectedPriority by remember { mutableStateOf(initialPriority) }
    var isQuickTask by remember { mutableStateOf(initialIsQuickTask) }
    var hasDeadline by remember { mutableStateOf(initialDeadlineMillis != null) }
    var deadlineOption by remember { mutableIntStateOf(
        if (initialDeadlineMillis == null) 0
        else {
            val hoursUntil = (initialDeadlineMillis - System.currentTimeMillis()) / (1000 * 60 * 60)
            when {
                hoursUntil <= 24 -> 0
                hoursUntil <= 48 -> 1
                hoursUntil <= 168 -> 2
                else -> 3
            }
        }
    ) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var customDeadlineMillis by remember { mutableStateOf(initialDeadlineMillis) }
    
    // Hora personalizada: extraer de deadline existente o default 23:59
    val initialCal = java.util.Calendar.getInstance().apply {
        if (initialDeadlineMillis != null) timeInMillis = initialDeadlineMillis
    }
    var selectedHour by remember { mutableIntStateOf(initialCal.get(java.util.Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(initialCal.get(java.util.Calendar.MINUTE)) }
    var hasCustomTime by remember { mutableStateOf(initialDeadlineMillis != null && 
        !(initialCal.get(java.util.Calendar.HOUR_OF_DAY) == 23 && initialCal.get(java.util.Calendar.MINUTE) == 59)) }
    
    // Subtareas
    var subtaskList by remember { mutableStateOf(initialSubtasks.toMutableList()) }
    var newSubtaskText by remember { mutableStateOf("") }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDeadlineMillis ?: System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = selectedHour,
        initialMinute = selectedMinute,
        is24Hour = true
    )
    
    // DatePicker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // DatePicker devuelve millis en UTC — extraer fecha en UTC
                        val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                        utcCal.timeInMillis = millis
                        val year = utcCal.get(java.util.Calendar.YEAR)
                        val month = utcCal.get(java.util.Calendar.MONTH)
                        val day = utcCal.get(java.util.Calendar.DAY_OF_MONTH)

                        // Construir deadline en zona horaria local
                        val cal = java.util.Calendar.getInstance()
                        cal.set(java.util.Calendar.YEAR, year)
                        cal.set(java.util.Calendar.MONTH, month)
                        cal.set(java.util.Calendar.DAY_OF_MONTH, day)
                        cal.set(java.util.Calendar.HOUR_OF_DAY, if (hasCustomTime) selectedHour else 23)
                        cal.set(java.util.Calendar.MINUTE, if (hasCustomTime) selectedMinute else 59)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        customDeadlineMillis = cal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // TimePicker Dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Hora límite") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    hasCustomTime = true
                    showTimePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                // Campo de texto — lo más importante
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("¿Qué necesitas hacer?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Dificultad — 3 opciones
                Text(
                    text = "Dificultad:",
                    style = MaterialTheme.typography.labelMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Difficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = selectedDifficulty == diff,
                            onClick = { selectedDifficulty = diff },
                            label = { Text("${diff.emoji} ${diff.label}") }
                        )
                    }
                }
                
                // Prioridad — 4 opciones
                Text(
                    text = "Prioridad:",
                    style = MaterialTheme.typography.labelMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Priority.entries.forEach { prio ->
                        FilterChip(
                            selected = selectedPriority == prio,
                            onClick = { selectedPriority = prio },
                            label = { Text(prio.emoji, fontSize = 14.sp) }
                        )
                    }
                }
                // Label de prioridad seleccionada
                Text(
                    text = selectedPriority.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                // Deadline — toggle + opciones rápidas + fecha/hora personalizada
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isQuickTask,
                        onCheckedChange = { isQuickTask = it }
                    )
                    Text("⚡ Menos de 2 minutos")
                }
                if (isQuickTask) {
                    Text(
                        text = "David Allen (GTD): 'Si toma menos de 2 minutos, hazlo ahora.' Se priorizará automáticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Subtareas
                Text(
                    text = "Pasos (opcional):",
                    style = MaterialTheme.typography.labelMedium
                )
                subtaskList.forEachIndexed { index, subtask ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${index + 1}.", modifier = Modifier.padding(end = 4.dp))
                        Text(
                            text = subtask,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { subtaskList = subtaskList.toMutableList().also { it.removeAt(index) } },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, "Eliminar", modifier = Modifier.size(14.dp))
                        }
                    }
                }
                if (subtaskList.size < 4) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            label = { Text("Agregar paso...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        IconButton(
                            onClick = {
                                if (newSubtaskText.isNotBlank()) {
                                    subtaskList = (subtaskList + newSubtaskText.trim()).toMutableList()
                                    newSubtaskText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, "Agregar")
                        }
                    }
                }
                
                // Fecha límite
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasDeadline,
                        onCheckedChange = { hasDeadline = it }
                    )
                    Text("Tiene fecha límite")
                }
                
                if (hasDeadline) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = deadlineOption == 0,
                            onClick = { deadlineOption = 0 },
                            label = { Text("Hoy") }
                        )
                        FilterChip(
                            selected = deadlineOption == 1,
                            onClick = { deadlineOption = 1 },
                            label = { Text("Mañana") }
                        )
                        FilterChip(
                            selected = deadlineOption == 2,
                            onClick = { deadlineOption = 2 },
                            label = { Text("7 días") }
                        )
                    }
                    
                    // Fecha personalizada
                    OutlinedButton(
                        onClick = {
                            deadlineOption = 3
                            showDatePicker = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (deadlineOption == 3 && customDeadlineMillis != null) {
                                val cal = java.util.Calendar.getInstance()
                                cal.timeInMillis = customDeadlineMillis!!
                                "📅 ${cal.get(java.util.Calendar.DAY_OF_MONTH)}/${cal.get(java.util.Calendar.MONTH) + 1}/${cal.get(java.util.Calendar.YEAR)}"
                            } else {
                                "📅 Elegir fecha..."
                            }
                        )
                    }
                    
                    // Hora personalizada
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (hasCustomTime) {
                                "⏰ ${String.format("%02d:%02d", selectedHour, selectedMinute)}"
                            } else {
                                "⏰ Establecer hora..."
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val effectiveHour = if (hasCustomTime) selectedHour else 23
                    val effectiveMinute = if (hasCustomTime) selectedMinute else 59
                    
                    val deadline = if (hasDeadline) {
                        when (deadlineOption) {
                            0 -> {
                                val cal = java.util.Calendar.getInstance()
                                cal.set(java.util.Calendar.HOUR_OF_DAY, effectiveHour)
                                cal.set(java.util.Calendar.MINUTE, effectiveMinute)
                                cal.set(java.util.Calendar.SECOND, 0)
                                cal.timeInMillis
                            }
                            1 -> {
                                val cal = java.util.Calendar.getInstance()
                                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                                cal.set(java.util.Calendar.HOUR_OF_DAY, effectiveHour)
                                cal.set(java.util.Calendar.MINUTE, effectiveMinute)
                                cal.set(java.util.Calendar.SECOND, 0)
                                cal.timeInMillis
                            }
                            2 -> {
                                val cal = java.util.Calendar.getInstance()
                                cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
                                cal.set(java.util.Calendar.HOUR_OF_DAY, effectiveHour)
                                cal.set(java.util.Calendar.MINUTE, effectiveMinute)
                                cal.set(java.util.Calendar.SECOND, 0)
                                cal.timeInMillis
                            }
                            3 -> {
                                // Aplicar hora al customDeadlineMillis
                                customDeadlineMillis?.let { millis ->
                                    val cal = java.util.Calendar.getInstance()
                                    cal.timeInMillis = millis
                                    cal.set(java.util.Calendar.HOUR_OF_DAY, effectiveHour)
                                    cal.set(java.util.Calendar.MINUTE, effectiveMinute)
                                    cal.set(java.util.Calendar.SECOND, 0)
                                    cal.timeInMillis
                                }
                            }
                            else -> null
                        }
                    } else null
                    
                    onConfirm(taskName, deadline, selectedDifficulty, selectedPriority, isQuickTask, subtaskList)
                },
                enabled = taskName.isNotBlank()
            ) {
                Text(if (initialName.isEmpty()) "Agregar" else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Overlay de celebración al completar una tarea.
 * 
 * JUSTIFICACIÓN CIENTÍFICA (BJ Fogg - "Tiny Habits"):
 * La celebración inmediata después de un comportamiento lo refuerza.
 * El cerebro libera dopamina al recibir feedback positivo,
 * creando una asociación positiva con la acción de completar tareas.
 */
@Composable
private fun CelebrationOverlay() {
    val emojis = remember { listOf("🎉", "⭐", "✨", "🏆", "💪", "🔥", "🎊", "💎") }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Emojis flotantes
        emojis.forEachIndexed { index, emoji ->
            val infiniteTransition = rememberInfiniteTransition(label = "celebration_$index")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -300f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1500 + (index * 200),
                        easing = LinearOutSlowInEasing
                    )
                ),
                label = "offsetY_$index"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1500 + (index * 200))
                ),
                label = "alpha_$index"
            )
            
            Text(
                text = emoji,
                fontSize = 32.sp,
                modifier = Modifier
                    .offset(
                        x = ((index * 47 - 150) % 300).dp,
                        y = offsetY.dp
                    )
                    .alpha(alpha)
            )
        }
        
        // Texto central
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
            label = "scale"
        )
        Text(
            text = "🎉 ¡Tarea completada!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.scale(scale)
        )
    }
}
