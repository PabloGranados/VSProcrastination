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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vsprocrastination.domain.StreakCalculator
import com.example.vsprocrastination.domain.TaskStats
import com.example.vsprocrastination.domain.MotivationalPhrases
import com.example.vsprocrastination.data.model.Task

/**
 * Pantalla de Resumen Semanal.
 * 
 * JUSTIFICACIÓN CIENTÍFICA (Metacognición + Self-Determination Theory):
 * La reflexión sobre el progreso propio (metacognición) mejora
 * el rendimiento futuro. Ver datos concretos de productividad
 * refuerza la sensación de competencia (SDT: Deci & Ryan),
 * que es uno de los 3 pilares de la motivación intrínseca.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklySummaryScreen(
    tasks: List<Task>,
    stats: TaskStats,
    onBack: () -> Unit
) {
    val currentStreak = remember(tasks) { StreakCalculator.calculateCurrentStreak(tasks) }
    val bestStreak = remember(tasks) { StreakCalculator.calculateBestStreak(tasks) }
    val hasCompletedToday = remember(tasks) { StreakCalculator.hasCompletedToday(tasks) }
    
    // Calcular extras
    val completedThisWeek = stats.completedThisWeekCount
    val totalPending = stats.totalPending
    val overdue = stats.overdueCount
    
    // Mensaje de reflexión
    val reflectionPhrase = remember(stats, currentStreak) {
        when {
            currentStreak >= 7 -> "🏆 ¡Una semana completa de racha! Tu constancia es admirable."
            currentStreak >= 3 -> "🔥 Llevas $currentStreak días seguidos. El momentum está de tu lado."
            completedThisWeek >= 10 -> "💪 $completedThisWeek tareas esta semana. ¡Máquina imparable!"
            completedThisWeek >= 5 -> "👏 Buen ritmo. $completedThisWeek tareas completadas esta semana."
            completedThisWeek > 0 -> "🌱 Has empezado. Cada tarea completada cuenta."
            else -> "🎯 Nueva semana, nueva oportunidad. Empieza con una tarea pequeña."
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Resumen") },
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
            // Reflexión principal
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = reflectionPhrase,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Calendario de contribuciones estilo GitHub
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    ContributionCalendar(
                        tasks = tasks
                    )
                }
            }
            
            // Racha
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "🔥 Racha de Productividad",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            value = "$currentStreak",
                            label = "Racha actual",
                            emoji = if (hasCompletedToday) "✅" else "⏳"
                        )
                        StatItem(
                            value = "$bestStreak",
                            label = "Mejor racha",
                            emoji = "🏆"
                        )
                    }
                    
                    if (!hasCompletedToday && currentStreak > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ ¡Completa una tarea hoy para mantener tu racha!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Stats de esta semana
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "📈 Esta Semana",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(value = "$completedThisWeek", label = "Completadas", emoji = "✅")
                        StatItem(value = "$totalPending", label = "Pendientes", emoji = "📋")
                        StatItem(value = "$overdue", label = "Vencidas", emoji = "⚠️")
                    }
                }
            }
            
            // Stats totales
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "📊 Histórico",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            value = "${stats.totalCompletedCount}",
                            label = "Total completadas",
                            emoji = "🏅"
                        )
                        StatItem(
                            value = "${(stats.completionRate * 100).toInt()}%",
                            label = "Tasa completación",
                            emoji = "📊"
                        )
                    }
                    
                    if (stats.totalTimeWorkedTodayMillis > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val minutesToday = (stats.totalTimeWorkedTodayMillis / 60000).toInt()
                        Text(
                            text = "⏱️ $minutesToday minutos enfocados hoy",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Insight psicológico
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "💡 Dato Anti-Procrastinación",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = remember { scienceFacts.random() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, emoji: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 24.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val scienceFacts = listOf(
    "Según Pychyl (2013), la procrastinación no es un problema de gestión del tiempo, sino de gestión emocional. Evitamos tareas que nos generan emociones negativas.",
    "El efecto Zeigarnik (1927) demuestra que las tareas incompletas ocupan más espacio mental que las completadas. Empezar algo, aunque sea poco, reduce la ansiedad.",
    "Steel (2007) encontró que la ecuación de la procrastinación es: Motivación = (Expectativa × Valor) / (Impulsividad × Retraso). Las tareas con deadlines cercanos tienen menos 'retraso', más motivación.",
    "Según Baumeister (2011), la fuerza de voluntad es un recurso limitado. Por eso es mejor hacer las tareas difíciles temprano, cuando tienes más 'combustible' mental.",
    "La investigación de Gollwitzer (1999) sobre 'Implementation Intentions' muestra que decir 'haré X cuando pase Y' duplica las probabilidades de actuar vs. solo tener la intención.",
    "Seinfeld usaba la técnica 'Don't Break the Chain': marcar cada día productivo en un calendario. La cadena visible crea presión positiva para no romperla.",
    "Según Csikszentmihalyi (1990), el estado de 'flow' requiere un balance entre dificultad y habilidad. Tareas demasiado fáciles aburren; demasiado difíciles paralizan.",
    "La ley de Parkinson (1955): 'El trabajo se expande para llenar el tiempo disponible.' Poner deadlines ajustados (incluso artificiales) aumenta la productividad."
)
