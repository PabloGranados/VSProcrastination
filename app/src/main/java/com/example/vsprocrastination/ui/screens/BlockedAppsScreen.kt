@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.vsprocrastination.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vsprocrastination.service.AppBlockerManager
import com.example.vsprocrastination.service.BlockableApp

/**
 * Pantalla para seleccionar qué apps bloquear durante las sesiones Pomodoro.
 *
 * JUSTIFICACIÓN CIENTÍFICA (Choice Architecture — Thaler & Sunstein, Nudge):
 * Las apps sugeridas aparecen primero con etiqueta "Sugerida" para guiar
 * la decisión sin limitar la libertad. El usuario mantiene el control total.
 *
 * DISEÑO UX:
 * - Las apps de distracción más comunes aparecen primero (pre-selección sugerida)
 * - Barra de búsqueda para encontrar cualquier app instalada
 * - Toggle visual claro (checkmark) para cada app
 * - Contador de apps bloqueadas visible en todo momento
 */
@Composable
fun BlockedAppsScreen(
    blockableApps: List<BlockableApp>,
    blockedPackages: Set<String>,
    hasUsageStatsPermission: Boolean,
    hasOverlayPermission: Boolean,
    appBlockingEnabled: Boolean,
    onToggleApp: (String, Boolean) -> Unit,
    onToggleBlocking: (Boolean) -> Unit,
    onRequestUsageStatsPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(blockableApps, searchQuery) {
        if (searchQuery.isBlank()) blockableApps
        else blockableApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val blockedCount = blockedPackages.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🛡️ Bloqueo de Apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // === HEADER: Estado de permisos ===
            if (!hasUsageStatsPermission || !hasOverlayPermission) {
                item {
                    PermissionsCard(
                        hasUsageStats = hasUsageStatsPermission,
                        hasOverlay = hasOverlayPermission,
                        onRequestUsageStats = onRequestUsageStatsPermission,
                        onRequestOverlay = onRequestOverlayPermission
                    )
                }
            }

            // === TOGGLE PRINCIPAL ===
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (appBlockingEnabled)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bloqueo durante Pomodoro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (appBlockingEnabled)
                                    "✅ Las apps seleccionadas se bloquearán al iniciar una sesión"
                                else
                                    "Desactivado — las apps no se bloquearán",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = appBlockingEnabled,
                            onCheckedChange = onToggleBlocking,
                            enabled = hasUsageStatsPermission && hasOverlayPermission
                        )
                    }
                }
            }

            // === INFO CARD ===
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🧠 ¿Por qué bloquear apps?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Según James Clear (Atomic Habits), agregar fricción a un mal hábito " +
                                    "reduce su frecuencia hasta un 50%. Tu cerebro busca el camino de " +
                                    "menor resistencia — al bloquear apps de distracción, la opción " +
                                    "productiva se convierte en la más fácil.\n\n" +
                                    "Investigadores de UC Irvine encontraron que tras una interrupción " +
                                    "digital se necesitan ~23 minutos para recuperar la concentración profunda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // === CONTADOR + BÚSQUEDA ===
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Apps a bloquear ($blockedCount seleccionadas)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar app...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Buscar") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // === APPS SUGERIDAS (header) ===
            val suggestedApps = filteredApps.filter { it.isSuggested }
            val otherApps = filteredApps.filter { !it.isSuggested }

            if (suggestedApps.isNotEmpty()) {
                item {
                    Text(
                        text = "⚡ Sugeridas para bloquear",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(suggestedApps, key = { it.packageName }) { app ->
                    AppBlockItem(
                        app = app,
                        isBlocked = app.packageName in blockedPackages,
                        onToggle = { blocked -> onToggleApp(app.packageName, blocked) }
                    )
                }
            }

            // === OTRAS APPS ===
            if (otherApps.isNotEmpty()) {
                item {
                    Text(
                        text = "📱 Todas las apps",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(otherApps, key = { it.packageName }) { app ->
                    AppBlockItem(
                        app = app,
                        isBlocked = app.packageName in blockedPackages,
                        onToggle = { blocked -> onToggleApp(app.packageName, blocked) }
                    )
                }
            }

            // Espacio al final
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PermissionsCard(
    hasUsageStats: Boolean,
    hasOverlay: Boolean,
    onRequestUsageStats: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚠️ Permisos necesarios",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Para bloquear apps se necesitan dos permisos especiales. " +
                        "El acto de concederlos manualmente refuerza tu compromiso (Pre-Commitment Strategy).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            if (!hasUsageStats) {
                Button(
                    onClick = onRequestUsageStats,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("📊 Conceder acceso de uso de apps")
                }
            }

            if (!hasOverlay) {
                Button(
                    onClick = onRequestOverlay,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("🪟 Conceder permiso de superposición")
                }
            }
        }
    }
}

@Composable
private fun AppBlockItem(
    app: BlockableApp,
    isBlocked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = { onToggle(!isBlocked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isBlocked) FontWeight.SemiBold else FontWeight.Normal
                )
                if (app.isSuggested) {
                    Text(
                        text = "Fuente común de distracción",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }

            AnimatedVisibility(
                visible = isBlocked,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Bloqueada",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
