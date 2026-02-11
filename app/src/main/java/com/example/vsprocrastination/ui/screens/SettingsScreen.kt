package com.example.vsprocrastination.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vsprocrastination.data.preferences.PreferencesManager
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
    onClearCompleted: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    val pomodoroDuration by preferencesManager.pomodoroDuration.collectAsState(initial = 25)
    val naggingEnabled by preferencesManager.naggingEnabled.collectAsState(initial = true)
    val deadlineReminders by preferencesManager.deadlineRemindersEnabled.collectAsState(initial = true)
    val darkMode by preferencesManager.darkMode.collectAsState(initial = "system")
    
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === POMODORO ===
            SettingsSection(title = "🍅 Pomodoro") {
                Text(
                    text = "Duración de la sesión de enfoque",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(15, 25, 30, 45).forEach { minutes ->
                        FilterChip(
                            selected = pomodoroDuration == minutes,
                            onClick = {
                                scope.launch { preferencesManager.setPomodoroDuration(minutes) }
                            },
                            label = { Text("${minutes}m") }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💡 25 min es el estándar Pomodoro. Si te cuesta concentrarte, prueba 15 min.",
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
