# VS Procrastination

**v2.5.0** — App Android para dejar de procrastinar. Usa principios de psicología conductual para que dejes de postergar y empieces a hacer las cosas.

No es otra lista de tareas. La app decide por ti qué hacer primero, te acompaña mientras lo haces y te molesta si no lo haces.

## Descargar APK

📲 **[Descargar VS Procrastination v2.5.0](releases/VS-Procrastination-v2.5.0.apk)**

> Para instalar: descarga el APK → abre el archivo → permite la instalación desde fuentes desconocidas si tu dispositivo lo pide → listo.

## Novedades en v2.5.0

### 🍅 Sistema Pomodoro Progresivo — Calibración + niveles adaptativos

**El Pomodoro ya no es "25 min para todos".** El nuevo sistema mide tu capacidad real de concentración y te asigna un nivel personalizado que evoluciona contigo.

#### Calibración inicial
- **Cronómetro ascendente**: la primera vez que inicias una sesión, la app te pide trabajar enfocado en algo y presionar "Parar" cuando pierdes el foco
- El tiempo medido determina tu **nivel inicial** automáticamente
- Puedes saltar la calibración (usa 25 min estándar) o recalibrar en cualquier momento desde Ajustes

#### 7 niveles de concentración

| Nivel | Enfoque | Descanso | Sesiones para avanzar |
|-------|---------|----------|-----------------------|
| 🌱 Principiante | 20 min | 3-5 min | 5 |
| 🍅 Clásico | 25 min | 5 min | 5 |
| 📈 Intermedio | 30 min | 5-7 min | 6 |
| 🚀 Avanzado | 40 min | 8-10 min | 6 |
| 🎯 Experto | 50 min | 10 min | 7 |
| 🧠 Deep Work | 60 min | 10-15 min | 8 |
| ⚡ Ultra Focus | 90 min | 20-30 min | ∞ (máximo) |

#### Progresión automática
- Al completar suficientes sesiones exitosas en tu nivel actual, la app sugiere **subir al siguiente nivel**
- Puedes aceptar o quedarte en tu nivel actual
- El contador se resetea si rechazas, para no preguntar en cada sesión

#### Sugerencia de descanso
- Al completar cada sesión Pomodoro, se muestra un **diálogo de descanso recomendado** con el tiempo según tu nivel
- Justificación científica incluida: el cerebro consolida durante el reposo

#### Personalización total
- **Selección manual de nivel**: toca cualquier nivel en la tabla de Ajustes para adoptarlo
- **Tiempos personalizados**: sliders de enfoque (5-90 min) y descanso (1-30 min) para valores libres
- **Recalibración**: repite la medición de foco cuando quieras
- Indicador de nivel y descanso recomendado visible durante la sesión de enfoque

### 🏗️ Arquitectura
- **PomodoroLevel.kt**: modelo de datos con tabla de 7 niveles, lógica de calibración y progresión
- **PreferencesManager**: 6 nuevas preferencias (level, sessionsAtLevel, hasCalibrated, breakDuration, isCustom, totalSessions)
- **MainViewModel**: calibración (start/stop/cancel), progresión automática, personalización manual
- **MainScreen**: CalibrationView (cronómetro ascendente fullscreen), BreakSuggestionDialog, LevelUpDialog
- **SettingsScreen**: sección Pomodoro rediseñada con tabla de niveles, personalización y recalibración
- **FocusModeView**: muestra nivel actual y descanso recomendado durante la sesión

## Novedades anteriores

### 🔔 Reingeniería del sistema de notificaciones (v2.4.0)

### 🔔 Reingeniería del sistema de notificaciones — Menos intrusivo, más inteligente (v2.4.0)

**Problema resuelto: Las notificaciones dejaban de funcionar tras reiniciar el dispositivo.** El BootReceiver tenía `exported="false"` en el manifiesto, impidiendo recibir `BOOT_COMPLETED`. Además, los recordatorios one-shot de deadline no se reprogramaban al reiniciar.

#### Recordatorios escalonados por deadline
- **3 niveles de urgencia** en vez de un solo recordatorio a 1h:
  - 📋 **24h antes** → recordatorio suave (GENTLE) — planifica con anticipación
  - ⏰ **4h antes** → recordatorio medio (PERSISTENT) — hora de actuar
  - 🔴 **1h antes** → recordatorio urgente — última oportunidad
- Cada tier tiene tag único para cancelación individual
- Se reprograman automáticamente tras reinicio del dispositivo

#### Nagging menos agresivo
- Intervalo: **15 min → 3 horas** (reducción de 12x)
- Notificaciones ahora **dismissable** (antes eran ONGOING/no se podían quitar)
- Prioridad reducida: MAX → HIGH
- Categoría: ALARM → REMINDER (no suena como alarma)
- Mensajes menos agresivos, más motivacionales
- Se verifica en BD que la tarea sigue vigente antes de notificar
- Si la tarea ya fue iniciada, no se envía nagging

#### Countdown de deadline refinado
- Ventana reducida: **2 horas → 30 minutos** (los recordatorios escalonados cubren las horas previas)
- ONGOING solo cuando faltan **<10 min** (antes <30 min)
- Complementa los recordatorios escalonados en vez de solaparse

#### Notificaciones inteligentes optimizadas
- Intervalo: **3h → 4h** (menos interrupciones)
- **Límite diario de 3 notificaciones** inteligentes (contador con reset automático)
- Canal de recordatorios: IMPORTANCE_HIGH → IMPORTANCE_DEFAULT

#### BootReceiver robusto
- `exported="true"` (obligatorio para broadcasts del sistema)
- Soporte para **QUICKBOOT_POWERON** (HTC, Xiaomi y otros OEMs)
- Usa `goAsync()` con coroutine para consultar la BD sin bloquear
- Reprograma **todos** los recordatorios escalonados de deadline (24h/4h/1h)
- Reprograma nagging para tareas vencidas no iniciadas
- Los workers periódicos (SmartNotification + DeadlineCountdown) se recrean

### 🏗️ Arquitectura
- **TaskReminderWorker**: `scheduleDeadlineReminder()` → `scheduleDeadlineReminders()` (3 tiers)
- **TaskReminderWorker**: `cancelDeadlineReminder()` → `cancelDeadlineReminders()` (cancela todos los tiers + backward compat)
- **SmartNotificationWorker**: contador diario con SharedPreferences para cap de 3/día
- **BootReceiver**: consulta BD en coroutine con `goAsync()` para reprogramar one-shots
- **MainViewModel**: todas las llamadas actualizadas a los nuevos métodos plurales
- Métodos antiguos mantenidos como wrappers deprecated para backward compat

## Novedades anteriores

### 🔓 Open Source Ready — Firebase eliminado (v2.3.0)

### 🔓 Open Source Ready — Firebase eliminado
- **Firebase Auth, Firestore y Analytics completamente removidos**: la app ya no depende de servicios de Google vinculados a una cuenta personal
- **Google Sign-In eliminado**: ya no se requiere `google-services.json` ni credenciales privadas
- **6 dependencias removidas**: firebase-bom, firebase-firestore, firebase-auth, firebase-analytics, play-services-auth, kotlinx-coroutines-play-services
- **Plugin google-services eliminado** de Gradle
- **Permiso INTERNET eliminado**: la app funciona 100% offline
- **Archivo `google-services.json` eliminado** del repositorio

### 💾 Export/Import JSON — Respaldo y transferencia de datos
- **Exportar todos los datos** a un archivo JSON legible: tareas, subtareas, hábitos y logs de hábitos
- **Importar datos** desde un archivo JSON con merge inteligente: agrega datos nuevos sin borrar los existentes
- **Detección de duplicados**: por nombre + fecha de creación, evita insertar la misma tarea/hábito dos veces
- **Storage Access Framework (SAF)**: no requiere permisos de almacenamiento — el usuario elige dónde guardar/leer el archivo
- **Nueva sección "Respaldo" en Ajustes**: botones "Exportar datos" e "Importar datos" con indicador de progreso y mensajes de resultado
- **Formato portátil**: el JSON exportado incluye versión de esquema, nombre de la app y timestamp de exportación
- Ideal para hacer backups, transferir datos entre dispositivos o migrar a un nuevo celular

### 🏗️ Arquitectura
- **ExportImportManager** reemplaza a FirestoreSyncManager como gestor de transferencia de datos
- **TaskRepository simplificado**: eliminadas todas las llamadas a syncManager (push, delete remote)
- **MainViewModel**: auth/sync Firebase reemplazado por `exportData(uri)` / `importData(uri)`
- **MainUiState**: campos `isSignedIn`, `userEmail`, `userName`, `isSyncing` reemplazados por `isExportImportInProgress`, `exportImportMessage`
- **TaskDao**: eliminados `getTaskByFirebaseId()` y `updateFirebaseId()` (queries solo para Firebase)
- **HabitDao**: nuevas queries `getAllHabitsSync()` y `getAllLogsSync()` para exportación
- Campos `firebaseId` mantenidos como legacy en entidades Room para compatibilidad con esquema v5 (no se puede hacer DROP COLUMN en SQLite sin migración destructiva)

### 🎨 Corrección de UI — Habit Tracker (v2.2.1)
- **Selector de emojis adaptativo**: grid de 8 columnas fijas reemplazado por layout responsivo — 4 columnas en teléfonos (portrait) y 8 en tablets. Usa `weight(1f)` + `aspectRatio(1f)` para distribución equitativa sin desbordamiento
- **Emojis ya no se amontonan**: el grid anterior (8×40dp = 348dp) desbordaba el `AlertDialog` en pantallas < 360dp — ahora se adapta a cualquier ancho
- **Fondo visual en emojis**: todos los emojis del selector tienen fondo `surfaceVariant` sutil, haciéndolos visibles como botones tapeables (antes sólo el seleccionado tenía fondo)
- **Diálogos scrolleables**: los diálogos de "Nuevo hábito" y "Editar hábito" ahora tienen `verticalScroll` — el contenido ya no se corta en pantallas pequeñas
- **HabitCard optimizado para portrait**: emoji circle 44dp, checkbox 40dp, botón editar 36dp, spacing reducido — ~24dp extra de espacio horizontal para el nombre del hábito
- **Animación de completado arreglada**: la escala al marcar un hábito era un no-op (`1f → 1f`), ahora hay feedback visual sutil (`1f → 1.02f`)
- Bordes redondeados de 12dp en los emojis del selector para mejor apariencia

### 🔔 Notificaciones — Menos spam + horas de silencio (v2.2.1)
- **Eliminado worker redundante**: `TaskReminderWorker` periódico (cada 2h) ya no se programa — duplicaba lo que SmartNotificationWorker ya hace mejor
- **Al abrir la app se cancela** el worker periódico viejo activamente con `cancelPeriodicReminder()` 
- **SmartNotificationWorker de cada 1h a cada 3h**: ~5 notificaciones útiles/día en lugar de ~14
- **Política UPDATE**: el nuevo intervalo toma efecto inmediato sin reinstalar
- **Horas de silencio**: TaskReminderWorker y SmartNotificationWorker callan de 22:00 a 7:59; DeadlineCountdownWorker de 23:00 a 6:59
- Justificación: las notificaciones nocturnas interrumpen el sueño y generan asociación negativa con la app (Exelmans & Van den Bulck, 2016)

### 🔄 Habit Tracker (v2.2.0)

### 🔄 Habit Tracker — Seguimiento de hábitos diarios
- **Nueva pantalla de hábitos**: accesible desde el botón 🔄 en la pantalla principal
- **Crea hábitos** con nombre y emoji personalizado (16 emojis sugeridos: 📖🏃🧘💪🎵✍️💧🥗😴🚿🧹📱💊🌅📝✅)
- **Un tap para completar**: checkbox circular animado, sin fricción
- **Racha por hábito**: cada hábito muestra su racha individual (🔥 5 días)
- **Progreso del día**: barra animada con conteo "3/5" y celebración "🎉 ¡Todos!" al completar todos
- **Editar y archivar**: soft delete que preserva todo el historial
- **Integrado con el mapa de calor**: los hábitos completados aparecen en el ContributionCalendar junto con las tareas, sumando actividad diaria
- **Detalle expandido**: al tocar un día en el calendario, ahora muestra tareas Y hábitos completados
- **Tips motivacionales** contextuales al pie de la lista (Aristóteles, James Clear)
- **Diseño responsive**: padding adaptativo para tablets (compact/medium/expanded)
- Justificación: Atomic Habits (James Clear) + Tiny Habits (B.J. Fogg) + Don't Break the Chain (Seinfeld)

### 🏗️ Arquitectura
- **Entidades separadas**: `Habit` y `HabitLog` como tablas Room independientes de `Task` — los hábitos no compiten con las tareas en el algoritmo de priorización
- **Migración Room v4→v5**: tablas `habits` y `habit_logs` con índices optimizados y foreign key CASCADE
- **HabitViewModel** independiente del MainViewModel — cada pantalla gestiona su propio estado
- **HabitRepository** con toggle atómico de completación diaria y cálculo de rachas

## Novedades anteriores

### � Corrección de bugs críticos (v2.1.3)
- **Rachas ya no se rompen en Año Nuevo**: corregido el cálculo de `dayKey()` que usaba `year*1000+dayOfYear` — ahora usa epoch days para garantizar consecutividad entre años
- **Las preferencias de notificación ahora funcionan**: los toggles de "nagging" y "recordatorios de deadline" en Ajustes realmente desactivan las notificaciones (antes eran cosméticos)
- **Notificaciones consistentes**: corregido bug donde la notificación compacta y expandida mostraban mensajes diferentes
- **Colisión de notificaciones resuelta**: `AppLeaveDetector` y `DeadlineCountdownWorker` ya no comparten el mismo ID de notificación
- **Limpieza de tareas completadas funciona correctamente**
- **Versión dinámica**: la pantalla de Ajustes muestra la versión real desde `BuildConfig` en lugar de un texto fijo

### 🛡️ Seguridad y estabilidad (v2.1.3)
- **TaskConverters a prueba de crashes**: valores corruptos en la BD ya no causan crash — devuelven defaults seguros
- **Logs condicionales**: `Log.w()` solo se ejecuta en builds de debug, no en release
- **ProGuard configurado**: reglas para Room, Firebase, Coroutines, DataStore y enums
- **Sincronización thread-safe**: añadido Mutex para evitar operaciones concurrentes
- **Room Schema Export habilitado**: permite verificar integridad de migraciones futuras

### 🔧 Mejoras de código (v2.1.3)
- **Código duplicado eliminado**: Workers ahora reutilizan `StreakCalculator` en lugar de duplicar la lógica
- **Botón "CONTINUAR" corregido**: ya no muestra "25 min" fijo ignorando la configuración del usuario

### �📱 Diseño responsive para tablets y landscape (v2.1.2)
- **Layout de dos paneles** en tablets y modo horizontal: tarea hero a la izquierda, cola de tareas a la derecha
- **Modo Enfoque adaptativo**: en landscape muestra información de la tarea y timer lado a lado, aprovechando el espacio horizontal
- **Padding dinámico** en todas las pantallas: Ajustes y Resumen Semanal se adaptan al ancho disponible (compact < 600dp, medium 600-840dp, expanded > 840dp)
- **Estado vacío centrado** con ancho máximo para legibilidad en pantallas grandes
- **Calendario de actividad responsive**: celdas más grandes (18dp) y espaciado ampliado en tablets

### 🎨 Mejoras de diseño
- **Paleta del calendario renovada**: colores ámbar/naranja que armonizan con el tema de la app (antes usaba verdes genéricos)
- **Calendario como mapa de calor propio**: diseño visual original con esquinas más redondeadas (3dp) y mejor legibilidad
- **Versión correcta** mostrada en la pantalla de Ajustes (antes mostraba v2.0)
- **Corrección de inconsistencias tipográficas** en toda la app

### 📅 Calendario de actividad con mapa de calor (v2.1)
- **Grid visual de actividad** de las últimas 15 semanas con cuadros coloreados en 5 niveles
- Toca cualquier día para ver **los nombres de las tareas** que completaste ese día
- Borde especial destaca el día de hoy y leyenda de colores (Menos → Más)
- Reemplaza la barra de progreso genérica en la pantalla principal
- También aparece en el Resumen Semanal para ver el historial completo
- Justificación: "Don't Break the Chain" (Seinfeld) + Tiny Habits (B.J. Fogg)

### 🔔 Sistema de notificaciones inteligente (v2.1)
- **Notificaciones basadas en ritmo circadiano**: diferentes mensajes según la hora del día
  - **Mañana (8-10h)**: Arrancada con la tarea prioritaria + dato sobre cortisol/deep work
  - **Mediodía (12-14h)**: Motivación para tareas rápidas durante la caída post-prandial
  - **Tarde (16-18h)**: Revisión del progreso + segundo pico de energía
  - **Noche (20-22h)**: Reflexión + incentivo para planificar el día siguiente
- **Protección de racha**: si llevas 2+ días y no has completado nada hoy, aviso urgente
- **Alertas de deadline**: notificación inmediata si hay tareas venciendo en <4 horas
- Cada notificación incluye **datos científicos reales** (Pychyl, Steel, Baumeister, Gollwitzer, Kahneman)
- Los recordatorios periódicos ahora muestran **el nombre exacto de tu tarea prioritaria** en vez de texto genérico

### � Respaldo y transferencia (v2.3.0, reemplaza Sincronización v2.0)
- **Export/Import JSON** reemplaza la sincronización con Firebase
- Exporta e importa tareas, subtareas, hábitos y logs
- Merge inteligente: no borra datos existentes al importar
- Detección de duplicados por nombre + fecha de creación
- Sin necesidad de cuenta de Google ni conexión a internet

## Cómo funciona

La pantalla principal muestra **una sola tarea** — la más urgente e importante según un algoritmo de priorización automático. Nada de listas largas ni parálisis por decisión.

### Algoritmo de priorización

Cada tarea recibe un score numérico:

```
Score = (Urgencia x 2) + (Dificultad x 1.5) + (Prioridad x 2.5) + Bonus Zeigarnik + Bonus Quick
```

- **Urgencia** — se calcula a partir de la cercanía del deadline. Tareas vencidas tienen score máximo.
- **Dificultad** — las tareas difíciles suben primero ("Eat That Frog", Brian Tracy).
- **Prioridad** — 4 niveles manuales: Baja, Normal, Alta, Urgente.
- **Bonus Zeigarnik** (+3) — tareas ya iniciadas se priorizan porque el cerebro no las suelta.
- **Bonus Quick** (+5) — tareas marcadas como "rápidas" (regla de los 2 minutos) suben de posición para resolverlas de inmediato.

### Base psicológica

| Principio | Cómo se aplica |
|---|---|
| Parálisis por análisis (Schwartz) | Solo se muestra una tarea sugerida a la vez |
| Eat That Frog (Brian Tracy) | Las tareas difíciles pesan más en el score |
| Efecto Zeigarnik | Las tareas iniciadas se priorizan automáticamente |
| Ley de Parkinson | Deadlines con fecha y hora exacta generan urgencia real |
| Regla de los 2 minutos (David Allen) | Tareas rápidas se marcan y priorizan para eliminarlas primero |
| Técnica Pomodoro (Cirillo) | Modo Enfoque progresivo con calibración + 7 niveles adaptativos |
| Temporal Motivation Theory (Steel) | Countdown visible en notificaciones al acercarse el deadline |
| Compromiso público (Cialdini) | La notificación persistente del timer actúa como compromiso visible |
| Implementation Intentions (Gollwitzer) | Notificaciones contextuales que anclan la tarea al momento presente |
| Ley de Fogg | Entrada de tareas con mínima fricción |
| Ley de Hick | Máximo 3-4 opciones en cada selector |
| Don't Break the Chain (Seinfeld) | Mapa de calor de actividad con historial de 15 semanas |
| Ritmos Circadianos | Notificaciones adaptadas a la hora del día (cortisol matutino, bajón post-prandial, pico vespertino) |
| Planning Fallacy (Kahneman) | Reflexión nocturna para planificar el día siguiente |
| Atomic Habits (James Clear) | Habit Tracker con checkboxes diarios y rachas por hábito |
| Tiny Habits (B.J. Fogg) | Celebración visual inmediata al completar un hábito |

## Features

### Gestión de tareas

- Crear tareas con nombre, dificultad, prioridad y deadline (fecha + hora)
- Subtareas simples (hasta 4 por tarea) con checklist integrado
- Marcar tareas como "rápidas" para la regla de los 2 minutos
- Editar cualquier tarea tocándola en la lista
- Undo al completar por si fue un tap accidental
- Saltar la tarea sugerida sin eliminarla

### Modo Enfoque (Pomodoro Progresivo)

- **Calibración inicial**: cronómetro ascendente para medir tu tiempo de concentración natural
- **7 niveles progresivos**: de 20 min (Principiante) a 90 min (Ultra Focus) con descansos recomendados
- **Progresión automática**: al completar suficientes sesiones, la app sugiere subir de nivel
- **Sugerencia de descanso**: al completar cada sesión, muestra el descanso recomendado para tu nivel
- **Personalización total**: elige nivel manualmente, ajusta tiempos con sliders, o recalibra cuando quieras
- Timer como Foreground Service con cronómetro regresivo nativo en la notificación
- Barra de progreso en la notificación con botón "Detener"
- Indicador de nivel y descanso recomendado visible durante la sesión
- Detección de salida de la app durante sesión activa con notificación para que vuelvas
- Registro automático del tiempo trabajado por tarea

### Notificaciones inteligentes

- **Sistema circadiano**: notificaciones adaptadas a la hora del día (mañana, mediodía, tarde, noche) con contenido científico específico
- Notificaciones inteligentes cada 4 horas (máximo 3 por día) con **el nombre real de tu tarea prioritaria**
- **Recordatorios escalonados** por deadline: 24h antes (suave), 4h antes (medio), 1h antes (urgente)
- Todas las notificaciones son **dismissable** (el usuario puede descartarlas)
- Modo nagging cada 3 horas para tareas vencidas, con mensajes motivacionales
- **Countdown estilo Duolingo**: cuando una tarea está a menos de 30 minutos de vencer, aparece una notificación con cronómetro regresivo en tiempo real. Se vuelve pegajosa cuando quedan menos de 10 minutos.
- Protección de racha: aviso urgente si tienes racha activa y no has completado nada hoy
- Alertas de deadline inminente (<4 horas) con motivación contextual
- Datos científicos integrados en cada notificación (Steel, Pychyl, Baumeister, Gollwitzer, Kahneman)
- **Reprogramación completa tras reinicio**: BootReceiver reprograma todos los recordatorios escalonados, nagging y workers periódicos

### Rachas y motivación

- Racha de días consecutivos completando al menos una tarea
- Mapa de calor de actividad también visible en el Resumen Semanal
- Animación de celebración al completar una tarea (confeti)
- Frases motivacionales contextuales: cambian según si la tarea es difícil, rápida, si llevas racha, o si hay tareas vencidas
- Resumen semanal con estadísticas, reflexión adaptativa y datos curiosos sobre procrastinación

### Configuración

- **Pomodoro progresivo**: tabla de 7 niveles, selección manual, personalización con sliders, recalibración
- Toggle de notificaciones nagging y recordatorios de deadline
- Tema: claro, oscuro o automático del sistema
- Export/Import JSON para respaldo y transferencia entre dispositivos
- Limpiar tareas completadas

### Habit Tracker

- Crear hábitos diarios con nombre y emoji personalizado
- Checkbox circular animado para completar/desmarcar hábitos
- Racha individual por hábito (días consecutivos)
- Progreso del día con barra animada y celebración al completar todos
- Archivar hábitos preservando historial
- Tips motivacionales contextuales según el progreso del día
- Integración con el mapa de calor (los hábitos suman actividad diaria)

### Progreso y calendario

- **Mapa de calor de actividad** con 15 semanas de historial
- Cuadros coloreados en 5 niveles de ámbar/naranja según tareas completadas por día
- Detalle al tocar un día: muestra los nombres de las tareas completadas
- Borde especial para el día actual
- Contadores: tareas completadas hoy, esta semana, total pendientes
- Indicador de tareas vencidas
- Racha actual y mejor racha

## Tech stack

- Kotlin 2.0 + Jetpack Compose con Material Design 3
- Room 2.6 con migraciones (v1 → v2 → v3 → v4 → v5)
- Export/Import JSON para respaldo y transferencia de datos (Storage Access Framework)
- WorkManager 2.9 para notificaciones programadas y workers periódicos
- Foreground Service con cronómetro nativo para el timer
- Navigation Compose 2.8 para navegación entre pantallas
- DataStore Preferences 1.1 para configuración del usuario
- MVVM con StateFlow + combine
- BroadcastReceivers para detección de salida de app y boot del dispositivo

## Estructura del proyecto

```
app/src/main/java/com/example/vsprocrastination/
├── data/
│   ├── model/
│   │   ├── Task.kt              # Entidad principal con cálculo de score
│   │   ├── Subtask.kt           # Subtareas con FK a Task
│   │   ├── Habit.kt             # Hábitos diarios recurrentes
│   │   ├── HabitLog.kt          # Registro de completación diaria
│   │   ├── Difficulty.kt        # Enum: EASY, MEDIUM, HARD
│   │   └── Priority.kt          # Enum: LOW, NORMAL, HIGH, URGENT
│   ├── dao/
│   │   ├── TaskDao.kt           # Queries Room (Flow reactivo + suspend)
│   │   ├── SubtaskDao.kt        # CRUD subtareas
│   │   └── HabitDao.kt          # CRUD hábitos + logs de completación
│   ├── database/
│   │   └── AppDatabase.kt       # Singleton Room, migraciones v1-v5
│   ├── preferences/
│   │   └── PreferencesManager.kt # DataStore para settings
│   ├── repository/
│   │   ├── TaskRepository.kt    # Capa de abstracción sobre DAOs
│   │   └── HabitRepository.kt   # Lógica de hábitos: toggle, rachas, CRUD
│   └── sync/
│       └── ExportImportManager.kt # Export/Import JSON via SAF
├── domain/
│   ├── PriorityCalculator.kt    # Algoritmo de priorización + stats
│   ├── StreakCalculator.kt      # Cálculo de rachas consecutivas
│   ├── MotivationalPhrases.kt   # Frases contextuales por categoría
│   └── PomodoroLevel.kt         # 7 niveles progresivos + calibración + progresión
├── service/
│   ├── FocusService.kt          # Foreground Service (Pomodoro + cronómetro nativo)
│   ├── TaskReminderWorker.kt    # WorkManager (recordatorios escalonados 24h/4h/1h + nagging 3h)
│   ├── SmartNotificationWorker.kt # Notificaciones circadianas (4h, máx 3/día)
│   ├── DeadlineCountdownWorker.kt # Countdown <30min estilo Duolingo
│   ├── AppLeaveDetector.kt      # BroadcastReceiver (salida de app)
│   └── BootReceiver.kt          # Reprograma TODO tras reboot (workers + one-shots + nagging)
├── ui/
│   ├── screens/
│   │   ├── MainScreen.kt        # Pantalla principal + CalibrationView + BreakDialog + LevelUpDialog
│   │   ├── HabitTrackerScreen.kt # Pantalla de hábitos diarios
│   │   ├── ContributionCalendar.kt # Mapa de calor de actividad (tareas + hábitos)
│   │   ├── SettingsScreen.kt    # Configuración (Pomodoro progresivo + padding adaptativo)
│   │   └── WeeklySummaryScreen.kt # Resumen semanal (padding adaptativo)
│   ├── viewmodel/
│   │   ├── MainViewModel.kt     # Estado central de la app
│   │   └── HabitViewModel.kt    # Estado del Habit Tracker
│   └── theme/
│       └── Theme.kt             # Tema Material 3 con soporte dark mode
└── MainActivity.kt              # NavHost con 4 rutas
```

## Build

Requiere Android Studio Ladybug o superior. minSdk 24, targetSdk 36.

```bash
./gradlew assembleDebug
```

## Permisos

- `POST_NOTIFICATIONS` — recordatorios y countdown
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` — timer del Modo Enfoque
- `VIBRATE` — notificaciones nagging y countdown
- `RECEIVE_BOOT_COMPLETED` — reprogramar workers tras reinicio
- `WAKE_LOCK` — WorkManager interno
## Changelog

### v2.5.0 (marzo 2026)
- **Sistema Pomodoro progresivo**: calibración inicial + 7 niveles adaptativos
- Cronómetro ascendente de calibración para medir foco natural del usuario
- Tabla de niveles: Principiante (20 min) → Ultra Focus (90 min) con descansos recomendados
- Progresión automática: sugiere subir de nivel tras completar suficientes sesiones
- Diálogo de descanso recomendado al completar cada sesión Pomodoro
- Selección manual de nivel en Ajustes (tabla completa con tap para elegir)
- Personalización libre con sliders: enfoque 5-90 min, descanso 1-30 min
- Botón de calibración/recalibración en Ajustes
- Indicador de nivel y descanso visible durante sesión de enfoque
- Contador de sesiones por nivel y total
- PomodoroLevel.kt: modelo de datos, lógica de calibración y progresión
- PreferencesManager: 6 nuevas preferencias persistentes (DataStore)
- MainScreen: CalibrationView, BreakSuggestionDialog, LevelUpDialog
- SettingsScreen: sección Pomodoro completamente rediseñada
- FocusModeView: badge de nivel en portrait y landscape
- Primera sesión redirige a calibración si no se ha hecho
- versionCode 9 → 10, versionName 2.4.0 → 2.5.0

### v2.4.0 (febrero 2026)
- **Fix crítico**: BootReceiver con `exported="true"` — las notificaciones ahora sobreviven al reinicio del dispositivo
- BootReceiver soporta QUICKBOOT_POWERON (HTC, Xiaomi, etc.)
- BootReceiver reprograma recordatorios one-shot de deadline consultando la BD con goAsync()
- Recordatorios escalonados: 24h (gentle), 4h (persistent), 1h (urgent) antes del deadline
- Nagging reducido de cada 15 min a cada 3 horas
- Notificaciones nagging ahora dismissable (antes ONGOING)
- Nagging: prioridad MAX → HIGH, categoría ALARM → REMINDER
- Verificación en BD de que la tarea sigue vigente antes de notificar
- Nagging no se envía si la tarea ya fue iniciada
- DeadlineCountdownWorker: ventana de 2h → 30 min
- DeadlineCountdownWorker: ONGOING solo <10 min (antes <30 min)
- SmartNotificationWorker: intervalo de 3h → 4h
- SmartNotificationWorker: límite diario de 3 notificaciones inteligentes
- Canal de recordatorios: IMPORTANCE_HIGH → IMPORTANCE_DEFAULT
- Mensajes de nagging menos agresivos, más motivacionales
- Métodos scheduleDeadlineReminder/cancelDeadlineReminder redirigen a versiones plurales
- versionCode 8 → 9, versionName 2.3.0 → 2.4.0

### v2.3.0 (febrero 2026)
- **Firebase completamente eliminado**: Auth, Firestore, Analytics y Google Sign-In removidos
- **Nuevo sistema Export/Import JSON**: respaldo y transferencia de datos sin cuenta de Google
- Plugin google-services y 6 dependencias removidas
- Permiso INTERNET eliminado (app 100% offline)
- ExportImportManager reemplaza FirestoreSyncManager
- TaskRepository simplificado (sin llamadas a syncManager)
- MainViewModel: auth/sync → export/import
- SettingsScreen: sección "Sincronización" → "Respaldo"
- MainActivity: signInLauncher → exportLauncher/importLauncher (SAF)
- HabitDao: nuevas queries sincrónicas para exportación
- TaskDao: eliminados getTaskByFirebaseId() y updateFirebaseId()
- Archivo google-services.json eliminado del repositorio
- Campos firebaseId mantenidos como legacy para compatibilidad Room v5

### v2.2.1 (febrero 2026)
- Selector de emojis adaptativo: 4 columnas en portrait, 8 en tablets (weight + aspectRatio)
- Corregido desbordamiento del grid de emojis en AlertDialog en pantallas < 360dp
- Fondo visual en todos los emojis del selector (antes solo el seleccionado tenía fondo)
- Diálogos de hábitos ahora son scrolleables (verticalScroll)
- HabitCard optimizado: emoji 44dp, checkbox 40dp, edit 36dp, spacing reducido
- Animación de completado arreglada (escala 1f → 1.02f)
- Bordes redondeados 12dp en emojis del selector
- Eliminado worker redundante TaskReminderWorker periódico
- SmartNotificationWorker de cada 1h a cada 3h
- Horas de silencio en notificaciones (22:00–07:59 / 23:00–06:59)

### v2.2.0 (febrero 2026)
- Habit Tracker: nueva pantalla de seguimiento de hábitos diarios
- Crear hábitos con nombre y emoji personalizado
- Toggle de completación diaria con checkbox animado
- Racha individual por hábito (días consecutivos)
- Progreso del día con barra animada y celebración
- Archivar hábitos (soft delete preservando historial)
- Integración con el mapa de calor: hábitos suman actividad en ContributionCalendar
- Detalle de día expandido muestra tareas y hábitos completados
- Tips motivacionales contextuales según progreso
- Nueva entidad Room: Habit + HabitLog con migración v4→v5
- HabitDao con índices optimizados y constraint UNIQUE (habitId, dateEpochDay)
- HabitRepository con toggle atómico y cálculo de rachas
- HabitViewModel independiente con StateFlow reactivo
- Botón de navegación 🔄 en pantalla principal
- Diseño responsive para tablets
- NavHost actualizado a 4 rutas

### v2.1.3 (febrero 2026)
- Corregido bug crítico: rachas se rompían en cambio de año (dayKey ahora usa epoch days)
- Workers respetan preferencias de notificación del usuario (nagging/deadline)
- Corregido doble random() en notificaciones nagging y AppLeaveDetector
- Resuelto colisión de Notification IDs entre AppLeaveDetector y DeadlineCountdownWorker
- clearCompletedTasks simplificado
- Versión en Ajustes usa BuildConfig.VERSION_NAME dinámicamente
- TaskConverters con manejo seguro de valores inválidos (no crashea)
- Eliminado código duplicado en SmartNotificationWorker (usa StreakCalculator)
- Sincronización protegida con Mutex contra ejecuciones concurrentes
- Logs condicionales: Log.w solo en BuildConfig.DEBUG
- ProGuard configurado para Room, Coroutines, DataStore y enums
- Room exportSchema habilitado con directorio de schemas
- buildConfig = true habilitado para acceso a BuildConfig.VERSION_NAME
- Botón "CONTINUAR" sin minutos hardcodeados

### v2.1.2 (febrero 2026)
- Diseño responsive completo para tablets y modo landscape
- Layout de dos paneles en pantalla principal para pantallas ≥ 600dp
- Modo Enfoque con layout horizontal en landscape
- Padding dinámico en Ajustes y Resumen Semanal según ancho de pantalla
- Calendario de actividad con celdas adaptativas (14dp compacto / 18dp expandido)
- Paleta de colores del calendario cambiada a ámbar/naranja (acorde al tema)
- Mapa de calor de actividad con diseño visual propio
- Corregida versión mostrada en pantalla de Ajustes
- Estado vacío centrado con ancho máximo para tablets

### v2.1 (febrero 2026)
- Mapa de calor de actividad con 15 semanas de historial
- Detalle de tareas completadas por día al tocar el calendario
- Sistema de notificaciones inteligente basado en ritmo circadiano
- Notificaciones muestran el nombre real de la tarea prioritaria
- SmartNotificationWorker con motivación científica según hora del día
- Protección de racha y alertas de deadline inminente
- Reemplazada barra de progreso genérica por el mapa de calor en pantalla principal

### v2.0 (febrero 2026)
- Sincronización entre dispositivos con Firebase (removido en v2.3.0)
- Google Sign-In para vincular cuenta (removido en v2.3.0)
- Sync automático al abrir la app y al modificar tareas
- Sync manual desde Ajustes
- Migración de base de datos v3 → v4
- Mejoras menores de estabilidad

### v1.0
- Lanzamiento inicial
- Algoritmo de priorización automático
- Modo Enfoque (Pomodoro) con Foreground Service
- Subtareas, rachas, frases motivacionales
- Notificaciones persistentes, nagging y countdown estilo Duolingo
- Resumen semanal

## Generar APK

Para generar el APK de release:

```bash
./gradlew assembleRelease
```

El APK se genera en `app/build/outputs/apk/release/`. Cópialo a la carpeta `releases/` y renómbralo:

```bash
cp app/build/outputs/apk/release/app-release.apk releases/VS-Procrastination-v2.5.0.apk
```

Para el APK de debug (con firma automática):

```bash
./gradlew assembleDebug
```

El APK de debug queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Licencia

Pablo Daniel Granados Martínez.