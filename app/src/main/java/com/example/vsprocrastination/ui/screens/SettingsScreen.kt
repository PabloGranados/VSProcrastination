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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vsprocrastination.BuildConfig
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
    onClearCompleted: () -> Unit,
    // Sincronización entre dispositivos
    isSignedIn: Boolean = false,
    userEmail: String? = null,
    userName: String? = null,
    isSyncing: Boolean = false,
    lastSyncMessage: String? = null,
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onSyncClick: () -> Unit = {}
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
            
            // === SINCRONIZACIÓN ENTRE DISPOSITIVOS ===
            SettingsSection(title = "🔄 Sincronización") {
                if (isSignedIn) {
                    // Usuario autenticado
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
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
                                Text(
                                    text = userName ?: "Usuario",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = userEmail ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = onSignOutClick) {
                                Text("Cerrar sesión", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = onSyncClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sincronizando...")
                        } else {
                            Text("🔄 Sincronizar ahora")
                        }
                    }
                    
                    if (lastSyncMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = lastSyncMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💡 Tus tareas se sincronizan automáticamente al abrir la app y al hacer cambios.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // No autenticado
                    Text(
                        text = "Sincroniza tus tareas entre tu celular, tablet y cualquier dispositivo Android.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = onSignInClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔗 Iniciar sesión con Google")
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💡 Inicia sesión con la misma cuenta de Google en todos tus dispositivos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
