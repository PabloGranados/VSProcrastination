@file:OptIn(ExperimentalLayoutApi::class)

package com.example.vsprocrastination.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vsprocrastination.BuildConfig
import com.example.vsprocrastination.data.preferences.PreferencesManager
import com.example.vsprocrastination.domain.PomodoroLevel
import com.example.vsprocrastination.domain.PomodoroLevels
import kotlinx.coroutines.launch

/**
 * Pantalla de Ajustes.
 * 
 * JUSTIFICACIÓN CIENTÍFICA (Self-Determination Theory - Autonomía):
 * Dar al usuario control sobre la configuración aumenta la sensación
 * de autonomía, uno de los 3 pilares de la motivación intrínseca.
 * Cuando sientes que controlas la herramienta, la usas más.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    onBack: () -> Unit,
    onClearCompleted: () -> Unit,
    // Pomodoro progresivo
    onCalibrate: () -> Unit = {},
    onSetLevel: (Int) -> Unit = {},
    onSetCustomDuration: (Int, Int) -> Unit = { _, _ -> },
    // Export/Import de datos
    isExportImportInProgress: Boolean = false,
    exportImportMessage: String? = null,
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onDismissExportImportMessage: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    
    val pomodoroDuration by preferencesManager.pomodoroDuration.collectAsState(initial = 25)
    val naggingEnabled by preferencesManager.naggingEnabled.collectAsState(initial = true)
    val deadlineReminders by preferencesManager.deadlineRemindersEnabled.collectAsState(initial = true)
    val darkMode by preferencesManager.darkMode.collectAsState(initial = "system")
    val pomodoroLevelIndex by preferencesManager.pomodoroLevel.collectAsState(initial = 1)
    val pomodoroBreakDuration by preferencesManager.pomodoroBreakDuration.collectAsState(initial = 5)
    val pomodoroIsCustom by preferencesManager.pomodoroIsCustom.collectAsState(initial = false)
    val pomodoroSessionsAtLevel by preferencesManager.pomodoroSessionsAtLevel.collectAsState(initial = 0)
    val pomodoroTotalSessions by preferencesManager.pomodoroTotalSessions.collectAsState(initial = 0)
    val pomodoroHasCalibrated by preferencesManager.pomodoroHasCalibrated.collectAsState(initial = false)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        val screenWidth = LocalConfiguration.current.screenWidthDp
        val horizontalPadding = if (screenWidth >= 840) 80.dp
            else if (screenWidth >= 600) 48.dp else 16.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = horizontalPadding, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === POMODORO PROGRESIVO ===
            SettingsSection(title = "🍅 Pomodoro Progresivo") {
                val currentLevel = PomodoroLevels.getLevel(pomodoroLevelIndex)
                
                // Estado actual
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (pomodoroIsCustom) "Personalizado" else currentLevel.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "⏱ ${pomodoroDuration} min enfoque / ${pomodoroBreakDuration} min descanso",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        if (!pomodoroIsCustom) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sesiones en este nivel: $pomodoroSessionsAtLevel/${currentLevel.sessionsToAdvance.let { if (it == Int.MAX_VALUE) "∞" else it.toString() }} para avanzar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total de sesiones completadas: $pomodoroTotalSessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Tabla de niveles
                Text(
                    text = "Niveles disponibles",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                PomodoroLevels.levels.forEach { level ->
                    val isSelected = !pomodoroIsCustom && pomodoroLevelIndex == level.index
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) 
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) BorderStroke(
                            1.5.dp, MaterialTheme.colorScheme.primary
                        ) else null,
                        shape = RoundedCornerShape(10.dp),
                        onClick = { onSetLevel(level.index) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = level.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = level.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${level.focusMinutes} min",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "⏸ ${level.breakRecommendation}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Personalización manual
                var showCustomDialog by remember { mutableStateOf(false) }
                
                OutlinedButton(
                    onClick = { showCustomDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✏️ Personalizar tiempos")
                }
                
                if (showCustomDialog) {
                    CustomPomodoroDialog(
                        initialFocus = pomodoroDuration,
                        initialBreak = pomodoroBreakDuration,
                        onDismiss = { showCustomDialog = false },
                        onConfirm = { focus, breakMin ->
                            onSetCustomDuration(focus, breakMin)
                            showCustomDialog = false
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Recalibrar
                OutlinedButton(
                    onClick = onCalibrate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🎯 ${if (pomodoroHasCalibrated) "Recalibrar" else "Calibrar"} mi foco")
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💡 La calibración mide tu tiempo de concentración natural y te asigna el nivel ideal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // === NOTIFICACIONES ===
            SettingsSection(title = "🔔 Notificaciones") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Recordatorios insistentes", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Notificaciones cada 15 min para tareas vencidas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = naggingEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { preferencesManager.setNaggingEnabled(enabled) }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Recordatorios de deadline", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Aviso 1 hora antes de la fecha límite",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = deadlineReminders,
                        onCheckedChange = { enabled ->
                            scope.launch { preferencesManager.setDeadlineRemindersEnabled(enabled) }
                        }
                    )
                }
            }
            
            // === APARIENCIA ===
            SettingsSection(title = "🎨 Apariencia") {
                Text("Tema", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "system" to "🔄 Sistema",
                        "light" to "☀️ Claro",
                        "dark" to "🌙 Oscuro"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = darkMode == mode,
                            onClick = {
                                scope.launch { preferencesManager.setDarkMode(mode) }
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
            
            // === RESPALDO Y TRANSFERENCIA ===
            SettingsSection(title = "💾 Respaldo") {
                Text(
                    text = "Exporta tus datos a un archivo JSON para hacer respaldo o transferir a otro dispositivo.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onExportClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExportImportInProgress
                ) {
                    if (isExportImportInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Procesando...")
                    } else {
                        Text("📤 Exportar datos")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExportImportInProgress
                ) {
                    Text("📥 Importar datos")
                }
                
                if (exportImportMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (exportImportMessage.startsWith("✅"))
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exportImportMessage,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissExportImportMessage) {
                                Text("OK")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💡 Al importar, los datos nuevos se agregan sin borrar los existentes. Los duplicados se ignoran.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // === DATOS ===
            SettingsSection(title = "🗑️ Datos") {
                var showConfirmDialog by remember { mutableStateOf(false) }
                
                OutlinedButton(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Limpiar tareas completadas")
                }
                
                Text(
                    text = "Elimina todas las tareas marcadas como completadas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        title = { Text("¿Limpiar completadas?") },
                        text = { Text("Se eliminarán todas las tareas completadas. Esta acción no se puede deshacer.") },
                        confirmButton = {
                            TextButton(onClick = {
                                onClearCompleted()
                                showConfirmDialog = false
                            }) {
                                Text("Eliminar", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
            
            // === CIENCIA ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🧠 Sobre la App",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "VS Procrastination usa principios de psicología cognitiva y conductual:\n\n" +
                                "• Parálisis por Análisis (Schwartz)\n" +
                                "• Eat That Frog (Brian Tracy)\n" +
                                "• Efecto Zeigarnik\n" +
                                "• Ley de Parkinson\n" +
                                "• Matriz Eisenhower\n" +
                                "• Regla de los 2 Minutos (David Allen)\n" +
                                "• Técnica Pomodoro (Cirillo)\n" +
                                "• Don't Break the Chain (Seinfeld)\n" +
                                "• Self-Determination Theory (Deci & Ryan)\n" +
                                "• Priming Psicológico (Bargh)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            // === VERSIÓN ===
            Text(
                text = "VS Procrastination v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * Diálogo para personalizar tiempos de Pomodoro libremente.
 */
@Composable
private fun CustomPomodoroDialog(
    initialFocus: Int,
    initialBreak: Int,
    onDismiss: () -> Unit,
    onConfirm: (focusMinutes: Int, breakMinutes: Int) -> Unit
) {
    var focusMinutes by remember { mutableFloatStateOf(initialFocus.toFloat()) }
    var breakMinutes by remember { mutableFloatStateOf(initialBreak.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personalizar Pomodoro") },
        text = {
            Column {
                Text(
                    text = "Tiempo de enfoque: ${focusMinutes.toInt()} min",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = focusMinutes,
                    onValueChange = { focusMinutes = it },
                    valueRange = 5f..90f,
                    steps = 16  // 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Tiempo de descanso: ${breakMinutes.toInt()} min",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = breakMinutes,
                    onValueChange = { breakMinutes = it },
                    valueRange = 1f..30f,
                    steps = 28
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Referencia rápida de la tabla
                Text(
                    text = "📋 Referencia recomendada:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                PomodoroLevels.levels.forEach { level ->
                    Text(
                        text = "${level.focusMinutes} min → ${level.breakRecommendation} descanso",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(focusMinutes.toInt(), breakMinutes.toInt()) }) {
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
