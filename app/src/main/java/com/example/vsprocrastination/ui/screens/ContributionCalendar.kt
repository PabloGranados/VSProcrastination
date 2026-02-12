package com.example.vsprocrastination.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.sp
import com.example.vsprocrastination.data.model.Task
import java.text.SimpleDateFormat
import java.util.*

/**
 * ContributionCalendar — Mapa de calor de actividad diaria.
 * 
 * JUSTIFICACIÓN CIENTÍFICA (Jerry Seinfeld — "Don't Break the Chain"):
 * La visualización de días consecutivos con actividad crea un
 * compromiso psicológico poderoso. Ver una "cadena" de días coloreados
 * activa la aversión a la pérdida: romper la cadena duele más que
 * el esfuerzo de mantenerla. Los mapas de calor de actividad son un
 * patrón de visualización ampliamente usado para incentivar constancia.
 * 
 * JUSTIFICACIÓN ADICIONAL (B.J. Fogg — "Tiny Habits"):
 * La celebración visual inmediata (cuadro coloreado al completar)
 * refuerza el comportamiento. El calendario actúa como un registro
 * tangible de progreso que combate el "descounting hiperbólico" —
 * la tendencia a subestimar logros pasados.
 */

/**
 * Datos de un día en el calendario.
 */
data class DayActivity(
    val date: Calendar,
    val completedCount: Int,
    val completedTasks: List<String> = emptyList() // Nombres de tareas completadas
)

/**
 * Componente de mapa de calor de actividad.
 * Muestra las últimas 15 semanas de actividad con cuadros coloreados
 * según el número de tareas completadas cada día.
 */
@Composable
fun ContributionCalendar(
    tasks: List<Task>,
    modifier: Modifier = Modifier,
    onDayClick: (DayActivity) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= 600
    val cellSize = if (isExpanded) 18.dp else 14.dp
    val cellSpacing = if (isExpanded) 3.dp else 2.dp
    val labelColumnWidth = if (isExpanded) 24.dp else 20.dp
    val cellStep = cellSize + cellSpacing

    val activityData = remember(tasks) { buildActivityData(tasks) }
    val weeks = remember(activityData) { groupIntoWeeks(activityData) }
    var selectedDay by remember { mutableStateOf<DayActivity?>(null) }
    
    // Auto-scroll al final (semana actual)
    val listState = rememberLazyListState()
    LaunchedEffect(weeks.size) {
        if (weeks.isNotEmpty()) {
            listState.scrollToItem(weeks.size - 1)
        }
    }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Encabezado con leyenda
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📅 Tu Actividad",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            // Leyenda de colores
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
                ActivityLevels.entries.forEach { level ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(level.color())
                    )
                }
                Text(
                    text = "Más",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Etiquetas de días de la semana
        Row {
            // Espacio para alinear con las columnas
            Column(
                modifier = Modifier.width(labelColumnWidth),
                verticalArrangement = Arrangement.spacedBy(cellSpacing)
            ) {
                val dayLabels = listOf("", "L", "", "M", "", "V", "")
                dayLabels.forEach { label ->
                    Box(
                        modifier = Modifier.size(cellSize),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Grid de cuadros de actividad
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                modifier = Modifier.weight(1f)
            ) {
                items(weeks.size) { weekIndex ->
                    val week = weeks[weekIndex]
                    Column(verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
                        week.forEach { day ->
                            val level = getActivityLevel(day.completedCount)
                            val isToday = isToday(day.date)
                            val isSelected = selectedDay?.let {
                                isSameDay(it.date, day.date)
                            } ?: false
                            
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(level.color())
                                    .then(
                                        if (isToday) Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(2.dp)
                                        ) else Modifier
                                    )
                                    .then(
                                        if (isSelected) Modifier.border(
                                            1.5.dp,
                                            MaterialTheme.colorScheme.onSurface,
                                            RoundedCornerShape(2.dp)
                                        ) else Modifier
                                    )
                                    .clickable {
                                        selectedDay = if (isSelected) null else day
                                        onDayClick(day)
                                    }
                            )
                        }
                    }
                }
            }
        }
        
        // Meses debajo del calendario
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = labelColumnWidth, top = 4.dp)
        ) {
            val monthLabels = getMonthLabels(weeks)
            monthLabels.forEach { (label, offset) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = cellStep * offset)
                )
            }
        }
        
        // Detalle del día seleccionado
        selectedDay?.let { day ->
            Spacer(modifier = Modifier.height(8.dp))
            DayDetailCard(day)
        }
    }
}

/**
 * Card con el detalle de un día seleccionado.
 * Muestra qué tareas se completaron ese día.
 */
@Composable
private fun DayDetailCard(day: DayActivity) {
    val dateFormat = remember { SimpleDateFormat("EEEE d 'de' MMMM", Locale("es")) }
    val dateStr = dateFormat.format(day.date.time)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = dateStr.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            if (day.completedCount > 0) {
                Text(
                    text = "✅ ${day.completedCount} tarea${if (day.completedCount > 1) "s" else ""} completada${if (day.completedCount > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (day.completedTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    day.completedTasks.take(5).forEach { taskName ->
                        Text(
                            text = "  • $taskName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    if (day.completedTasks.size > 5) {
                        Text(
                            text = "  ...y ${day.completedTasks.size - 5} más",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                Text(
                    text = "Sin actividad este día",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Niveles de actividad con colores (estilo GitHub).
 */
enum class ActivityLevels {
    NONE, LOW, MEDIUM, HIGH, VERY_HIGH;
    
    @Composable
    fun color(): Color {
        return when (this) {
            NONE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            LOW -> Color(0xFFFFE0B2)       // Ámbar claro
            MEDIUM -> Color(0xFFFFB74D)    // Ámbar medio
            HIGH -> Color(0xFFF57C00)      // Naranja oscuro
            VERY_HIGH -> Color(0xFFE65100) // Naranja profundo
        }
    }
}

/**
 * Determina el nivel de actividad según la cantidad de tareas completadas.
 */
private fun getActivityLevel(count: Int): ActivityLevels {
    return when {
        count == 0 -> ActivityLevels.NONE
        count == 1 -> ActivityLevels.LOW
        count == 2 -> ActivityLevels.MEDIUM
        count <= 4 -> ActivityLevels.HIGH
        else -> ActivityLevels.VERY_HIGH
    }
}

/**
 * Construye datos de actividad para los últimos 105 días (15 semanas).
 */
private fun buildActivityData(tasks: List<Task>): List<DayActivity> {
    val completedTasks = tasks.filter { it.completedAt != null }
    
    // Agrupar tareas por día
    val tasksByDay = mutableMapOf<String, MutableList<Task>>()
    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    completedTasks.forEach { task ->
        val dayKey = dayFormat.format(Date(task.completedAt!!))
        tasksByDay.getOrPut(dayKey) { mutableListOf() }.add(task)
    }
    
    // Generar últimos 105 días
    val days = mutableListOf<DayActivity>()
    val calendar = Calendar.getInstance()
    
    // Retroceder al inicio de la semana actual + 14 semanas atrás
    val today = Calendar.getInstance()
    calendar.time = today.time
    
    // Ir al domingo de esta semana
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    calendar.add(Calendar.DAY_OF_YEAR, -(dayOfWeek - Calendar.SUNDAY))
    
    // Retroceder 14 semanas
    calendar.add(Calendar.WEEK_OF_YEAR, -14)
    
    // Generar 15 semanas (105 días)
    val endDate = Calendar.getInstance()
    endDate.add(Calendar.DAY_OF_YEAR, 1) // incluir hoy
    
    while (calendar.before(endDate)) {
        val dayKey = dayFormat.format(calendar.time)
        val tasksForDay = tasksByDay[dayKey] ?: emptyList()
        
        days.add(
            DayActivity(
                date = calendar.clone() as Calendar,
                completedCount = tasksForDay.size,
                completedTasks = tasksForDay.map { it.name }
            )
        )
        
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }
    
    return days
}

/**
 * Agrupa los días en semanas (columnas de 7 días, Domingo a Sábado).
 */
private fun groupIntoWeeks(days: List<DayActivity>): List<List<DayActivity>> {
    if (days.isEmpty()) return emptyList()
    
    val weeks = mutableListOf<MutableList<DayActivity>>()
    var currentWeek = mutableListOf<DayActivity>()
    
    days.forEach { day ->
        val dayOfWeek = day.date.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY && currentWeek.isNotEmpty()) {
            weeks.add(currentWeek)
            currentWeek = mutableListOf()
        }
        currentWeek.add(day)
    }
    
    if (currentWeek.isNotEmpty()) {
        weeks.add(currentWeek)
    }
    
    return weeks
}

/**
 * Genera las etiquetas de meses para mostrar debajo del calendario.
 */
private fun getMonthLabels(weeks: List<List<DayActivity>>): List<Pair<String, Int>> {
    if (weeks.isEmpty()) return emptyList()
    
    val labels = mutableListOf<Pair<String, Int>>()
    val monthNames = listOf(
        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    )
    var lastMonth = -1
    
    weeks.forEachIndexed { index, week ->
        val firstDay = week.first()
        val month = firstDay.date.get(Calendar.MONTH)
        if (month != lastMonth) {
            labels.add(Pair(monthNames[month], if (labels.isEmpty()) 0 else 1))
            lastMonth = month
        }
    }
    
    return labels
}

private fun isToday(cal: Calendar): Boolean {
    val today = Calendar.getInstance()
    return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
