package com.example.vsprocrastination.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vsprocrastination.data.model.Habit
import com.example.vsprocrastination.ui.viewmodel.HabitUiState
import com.example.vsprocrastination.ui.viewmodel.HabitViewModel
import com.example.vsprocrastination.ui.viewmodel.HabitWithStatus
import kotlinx.coroutines.delay

/**
 * Pantalla de Habit Tracker — seguimiento de hábitos diarios.
 * 
 * JUSTIFICACIÓN CIENTÍFICA (James Clear — "Atomic Habits"):
 * "Habit tracking is powerful because it leverages multiple Laws
 * of Behavior Change. It simultaneously makes a behavior obvious,
 * attractive, satisfying, and easy."
 * 
 * El simple acto de marcar un checkbox activa:
 * 1. Refuerzo visual inmediato (Tiny Habits — B.J. Fogg)
 * 2. Aversión a la pérdida de racha (Don't Break the Chain — Seinfeld)
 * 3. Efecto de progreso (Progress Principle — Amabile & Kramer)
 * 
 * DISEÑO:
 * - Lista simple con checkboxes grandes (mínima fricción)
 * - Racha visible por hábito (motivación por no romperla)
 * - Progreso del día en la parte superior
 * - Sin complejidad innecesaria: no hay stats, gráficos, ni categorías
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitTrackerScreen(
    viewModel: HabitViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔄 Mis Hábitos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.toggleAddHabitDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Agregar hábito")
            }
        }
    ) { padding ->
        val screenWidth = LocalConfiguration.current.screenWidthDp
        val horizontalPadding = if (screenWidth >= 840) 80.dp
            else if (screenWidth >= 600) 48.dp else 16.dp
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = horizontalPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.habits.isEmpty() -> {
                    EmptyHabitsState { viewModel.toggleAddHabitDialog(true) }
                }
                else -> {
                    HabitsList(uiState, viewModel)
                }
            }
        }
        
        // Diálogo de agregar hábito
        if (uiState.showAddHabitDialog) {
            AddHabitDialog(
                onDismiss = { viewModel.toggleAddHabitDialog(false) },
                onConfirm = { name, emoji -> viewModel.addHabit(name, emoji) }
            )
        }
        
        // Diálogo de editar hábito
        uiState.editingHabit?.let { habit ->
            EditHabitDialog(
                habit = habit,
                onDismiss = { viewModel.cancelEditing() },
                onConfirm = { name, emoji -> viewModel.updateHabit(habit.id, name, emoji) },
                onDelete = { 
                    viewModel.archiveHabit(habit.id)
                    viewModel.cancelEditing()
                }
            )
        }
    }
}

/**
 * Estado vacío con llamada a la acción.
 */
@Composable
private fun EmptyHabitsState(onAddClick: () -> Unit) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = if (screenWidth >= 600) 
                Modifier.widthIn(max = 400.dp).padding(32.dp) 
            else 
                Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔄",
                    fontSize = 56.sp,
                    lineHeight = 64.sp
                )
            }
            Text(
                text = "Sin hábitos aún",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Los hábitos son las tareas que quieres hacer TODOS los días.\n\nAgrega cosas como \"Leer 20 min\", \"Ejercicio\" o \"Meditar\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAddClick) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Agregar primer hábito")
            }
        }
    }
}

/**
 * Lista de hábitos con progreso del día.
 */
@Composable
private fun HabitsList(uiState: HabitUiState, viewModel: HabitViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header con progreso del día
        item {
            DayProgressHeader(
                completedCount = uiState.todayCompletedCount,
                totalCount = uiState.todayTotalCount
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Lista de hábitos
        items(
            items = uiState.habits,
            key = { it.habit.id }
        ) { habitWithStatus ->
            HabitCard(
                habitWithStatus = habitWithStatus,
                onToggle = { viewModel.toggleHabit(habitWithStatus.habit.id) },
                onEdit = { viewModel.startEditingHabit(habitWithStatus.habit) }
            )
        }
        
        // Tip motivacional al final
        item {
            Spacer(modifier = Modifier.height(16.dp))
            MotivationalTip(uiState.todayCompletedCount, uiState.todayTotalCount)
            Spacer(modifier = Modifier.height(80.dp)) // Espacio para FAB
        }
    }
}

/**
 * Header con progreso del día — barra visual + texto.
 */
@Composable
private fun DayProgressHeader(completedCount: Int, totalCount: Int) {
    val progress = if (totalCount > 0) completedCount / totalCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hoy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (completedCount == totalCount && totalCount > 0) 
                        "🎉 ¡Todos!" 
                    else 
                        "$completedCount / $totalCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (completedCount == totalCount && totalCount > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (completedCount == totalCount && totalCount > 0)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/**
 * Card individual de un hábito.
 * Diseño: Emoji grande + nombre + racha + checkbox.
 */
@Composable
private fun HabitCard(
    habitWithStatus: HabitWithStatus,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val habit = habitWithStatus.habit
    val isCompleted = habitWithStatus.isCompletedToday
    val streak = habitWithStatus.currentStreak
    
    // Animación satisfactoria al completar
    val scale by animateFloatAsState(
        targetValue = if (isCompleted) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isCompleted) null else CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Emoji del hábito — en Box de tamaño fijo para evitar recorte
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = habit.emoji,
                    fontSize = 28.sp,
                    lineHeight = 36.sp
                )
            }
            
            // Nombre + racha
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (streak > 0) {
                    Text(
                        text = "🔥 $streak día${if (streak != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Botón editar (sutil)
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            // Checkbox grande
            HabitCheckbox(
                isChecked = isCompleted,
                onToggle = onToggle
            )
        }
    }
}

/**
 * Checkbox personalizado — más grande y satisfactorio que el default.
 */
@Composable
private fun HabitCheckbox(isChecked: Boolean, onToggle: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isChecked) 
            MaterialTheme.colorScheme.primary 
        else 
            Color.Transparent,
        animationSpec = tween(200),
        label = "checkColor"
    )
    
    val borderColor = if (isChecked)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outline
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isChecked,
            enter = scaleIn(animationSpec = spring(dampingRatio = 0.4f)),
            exit = scaleOut()
        ) {
            Text(
                text = "✓",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

/**
 * Tip motivacional contextual al final de la lista.
 */
@Composable
private fun MotivationalTip(completedCount: Int, totalCount: Int) {
    if (totalCount == 0) return
    
    val message = when {
        completedCount == totalCount -> "\"We are what we repeatedly do. Excellence, then, is not an act, but a habit.\" — Aristóteles"
        completedCount == 0 -> "💡 Tip: Empieza con el hábito más fácil. El momentum hace el resto."
        completedCount < totalCount / 2 -> "💪 Vas bien. Cada hábito completado es un voto por la persona que quieres ser."
        else -> "🔥 ¡Ya casi! Solo ${totalCount - completedCount} más."
    }
    
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}

// ========== DIÁLOGOS ==========

/**
 * Emojis sugeridos para hábitos.
 */
private val SUGGESTED_EMOJIS = listOf(
    "📖", "🏃", "🧘", "💪", "🎵", "✍️", "💧", "🥗",
    "😴", "🚿", "🧹", "📱", "💊", "🌅", "📝", "✅"
)

/**
 * Diálogo para agregar un nuevo hábito.
 * Mínima fricción: solo nombre y emoji.
 */
@Composable
private fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("✅") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo hábito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Campo de nombre
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("¿Qué quieres hacer cada día?") },
                    placeholder = { Text("Ej: Leer 20 min") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            if (name.isNotBlank()) onConfirm(name, selectedEmoji) 
                        }
                    )
                )
                
                // Selector de emoji
                Text(
                    text = "Ícono",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                EmojiSelector(
                    selectedEmoji = selectedEmoji,
                    onEmojiSelected = { selectedEmoji = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedEmoji) },
                enabled = name.isNotBlank()
            ) {
                Text("Agregar")
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
 * Diálogo para editar un hábito existente.
 */
@Composable
private fun EditHabitDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(habit.name) }
    var selectedEmoji by remember { mutableStateOf(habit.emoji) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Archivar hábito?") },
            text = { 
                Text("\"${habit.emoji} ${habit.name}\" ya no aparecerá en tu lista diaria. " +
                     "Tu historial se preserva.") 
            },
            confirmButton = {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Archivar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
        return
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar hábito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del hábito") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Ícono",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                EmojiSelector(
                    selectedEmoji = selectedEmoji,
                    onEmojiSelected = { selectedEmoji = it }
                )
                
                // Botón de archivar
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Archivar hábito")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedEmoji) },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar")
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
 * Grid de emojis seleccionables.
 */
@Composable
private fun EmojiSelector(
    selectedEmoji: String,
    onEmojiSelected: (String) -> Unit
) {
    // Grid de 8 columnas
    val rows = SUGGESTED_EMOJIS.chunked(8)
    
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { emoji ->
                    val isSelected = emoji == selectedEmoji
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onEmojiSelected(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 22.sp,
                            lineHeight = 28.sp
                        )
                    }
                }
            }
        }
    }
}
