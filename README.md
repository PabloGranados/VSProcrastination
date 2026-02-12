# VS Procrastination

**v2.1.3** — App Android para dejar de procrastinar. Usa principios de psicología conductual para que dejes de postergar y empieces a hacer las cosas.

No es otra lista de tareas. La app decide por ti qué hacer primero, te acompaña mientras lo haces y te molesta si no lo haces.

## Descargar APK

📲 **[Descargar VS Procrastination v2.1.3](releases/VS-Procrastination-v2.1.3.apk)**

> Para instalar: descarga el APK → abre el archivo → permite la instalación desde fuentes desconocidas si tu dispositivo lo pide → listo.

## Novedades en v2.1.3

### 🐛 Corrección de bugs críticos
- **Rachas ya no se rompen en Año Nuevo**: corregido el cálculo de `dayKey()` que usaba `year*1000+dayOfYear` — ahora usa epoch days para garantizar consecutividad entre años
- **Las preferencias de notificación ahora funcionan**: los toggles de "nagging" y "recordatorios de deadline" en Ajustes realmente desactivan las notificaciones (antes eran cosméticos)
- **Notificaciones consistentes**: corregido bug donde la notificación compacta y expandida mostraban mensajes diferentes
- **Colisión de notificaciones resuelta**: `AppLeaveDetector` y `DeadlineCountdownWorker` ya no comparten el mismo ID de notificación
- **Limpieza de tareas completadas sincroniza con Firebase**: al borrar tareas completadas, ahora también se eliminan de Firestore
- **Versión dinámica**: la pantalla de Ajustes muestra la versión real desde `BuildConfig` en lugar de un texto fijo

### 🛡️ Seguridad y estabilidad
- **TaskConverters a prueba de crashes**: valores corruptos en la BD ya no causan crash — devuelven defaults seguros
- **Logs condicionales**: `Log.w()` solo se ejecuta en builds de debug, no en release
- **ProGuard configurado**: reglas para Room, Firebase, Coroutines, DataStore y enums
- **Sincronización thread-safe**: añadido Mutex para evitar sync concurrentes en Firestore
- **Room Schema Export habilitado**: permite verificar integridad de migraciones futuras

### 🔧 Mejoras de código
- **Código duplicado eliminado**: Workers ahora reutilizan `StreakCalculator` en lugar de duplicar la lógica
- **Botón "CONTINUAR" corregido**: ya no muestra "25 min" fijo ignorando la configuración del usuario

## Novedades anteriores

### 📱 Diseño responsive para tablets y landscape (v2.1.2)
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

### 🔄 Sincronización entre dispositivos (v2.0)
- **Tus tareas en todos tus dispositivos**: celular, tablet, cualquier Android
- Inicia sesión con Google y tus tareas se sincronizan automáticamente
- Sincronización al abrir la app y al hacer cambios (crear, editar, completar, eliminar)
- Botón "Sincronizar ahora" en Ajustes para forzar sincronización manual
- Resolución de conflictos automática (gana la versión más reciente)
- Funciona offline: si no hay internet, los datos se guardan localmente y se sincronizan después

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
| Técnica Pomodoro (Cirillo) | Modo Enfoque con timer configurable |
| Temporal Motivation Theory (Steel) | Countdown visible en notificaciones al acercarse el deadline |
| Compromiso público (Cialdini) | La notificación persistente del timer actúa como compromiso visible |
| Implementation Intentions (Gollwitzer) | Notificaciones contextuales que anclan la tarea al momento presente |
| Ley de Fogg | Entrada de tareas con mínima fricción |
| Ley de Hick | Máximo 3-4 opciones en cada selector |
| Don't Break the Chain (Seinfeld) | Mapa de calor de actividad con historial de 15 semanas |
| Ritmos Circadianos | Notificaciones adaptadas a la hora del día (cortisol matutino, bajón post-prandial, pico vespertino) |
| Planning Fallacy (Kahneman) | Reflexión nocturna para planificar el día siguiente |

## Features

### Gestión de tareas

- Crear tareas con nombre, dificultad, prioridad y deadline (fecha + hora)
- Subtareas simples (hasta 4 por tarea) con checklist integrado
- Marcar tareas como "rápidas" para la regla de los 2 minutos
- Editar cualquier tarea tocándola en la lista
- Undo al completar por si fue un tap accidental
- Saltar la tarea sugerida sin eliminarla

### Modo Enfoque (Pomodoro)

- Timer configurable (15 a 60 minutos) como Foreground Service
- Cronómetro regresivo nativo en la notificación, visible fuera de la app en tiempo real
- Barra de progreso en la notificación
- Botón "Detener" directamente desde la notificación
- Detección de salida de la app durante sesión activa con notificación para que vuelvas
- Registro automático del tiempo trabajado por tarea

### Notificaciones inteligentes

- **Sistema circadiano**: notificaciones adaptadas a la hora del día (mañana, mediodía, tarde, noche) con contenido científico específico
- Recordatorio periódico cada 2 horas con **el nombre real de tu tarea prioritaria** (ya no dice genérico)
- Notificaciones persistentes (no se descartan con swipe)
- Modo nagging cada 15 minutos para tareas vencidas, con mensajes directos y rotantes
- Recordatorio 1 hora antes del deadline
- **Countdown estilo Duolingo**: cuando una tarea está a menos de 2 horas de vencer, aparece una notificación con cronómetro regresivo en tiempo real que cuenta atrás hasta el deadline. Se vuelve pegajosa cuando quedan menos de 30 minutos.
- Protección de racha: aviso urgente si tienes racha activa y no has completado nada hoy
- Alertas de deadline inminente (<4 horas) con motivación contextual
- Datos científicos integrados en cada notificación (Steel, Pychyl, Baumeister, Gollwitzer, Kahneman)
- Reprogramación automática tras reinicio del dispositivo

### Rachas y motivación

- Racha de días consecutivos completando al menos una tarea
- Mapa de calor de actividad también visible en el Resumen Semanal
- Animación de celebración al completar una tarea (confeti)
- Frases motivacionales contextuales: cambian según si la tarea es difícil, rápida, si llevas racha, o si hay tareas vencidas
- Resumen semanal con estadísticas, reflexión adaptativa y datos curiosos sobre procrastinación

### Configuración

- Duración del Pomodoro ajustable
- Toggle de notificaciones nagging y recordatorios de deadline
- Tema: claro, oscuro o automático del sistema
- Sincronización con Google (nuevo en v2.0)
- Limpiar tareas completadas

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
- Room 2.6 con migraciones (v1 → v2 → v3 → v4)
- Firebase Auth + Firestore para sincronización entre dispositivos
- Google Sign-In para autenticación
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
│   │   ├── Difficulty.kt        # Enum: EASY, MEDIUM, HARD
│   │   └── Priority.kt          # Enum: LOW, NORMAL, HIGH, URGENT
│   ├── dao/
│   │   ├── TaskDao.kt           # Queries Room (Flow reactivo + suspend)
│   │   └── SubtaskDao.kt        # CRUD subtareas
│   ├── database/
│   │   └── AppDatabase.kt       # Singleton Room, migraciones v1-v4
│   ├── preferences/
│   │   └── PreferencesManager.kt # DataStore para settings
│   ├── repository/
│   │   └── TaskRepository.kt    # Capa de abstracción sobre DAOs
│   └── sync/
│       └── FirestoreSyncManager.kt # Sincronización Room ↔ Firestore
├── domain/
│   ├── PriorityCalculator.kt    # Algoritmo de priorización + stats
│   ├── StreakCalculator.kt      # Cálculo de rachas consecutivas
│   └── MotivationalPhrases.kt   # Frases contextuales por categoría
├── service/
│   ├── FocusService.kt          # Foreground Service (Pomodoro + cronómetro nativo)
│   ├── TaskReminderWorker.kt    # WorkManager (3 niveles de notificación + consulta BD)
│   ├── SmartNotificationWorker.kt # Notificaciones circadianas basadas en ciencia
│   ├── DeadlineCountdownWorker.kt # Countdown <2h estilo Duolingo
│   ├── AppLeaveDetector.kt      # BroadcastReceiver (salida de app)
│   └── BootReceiver.kt          # Reprograma workers tras reboot
├── ui/
│   ├── screens/
│   │   ├── MainScreen.kt        # Pantalla principal (layout adaptativo compact/expanded)
│   │   ├── ContributionCalendar.kt # Mapa de calor de actividad (responsive)
│   │   ├── SettingsScreen.kt    # Configuración (padding adaptativo)
│   │   └── WeeklySummaryScreen.kt # Resumen semanal (padding adaptativo)
│   ├── viewmodel/
│   │   └── MainViewModel.kt     # Estado central de la app
│   └── theme/
│       └── Theme.kt             # Tema Material 3 con soporte dark mode
└── MainActivity.kt              # NavHost con 3 rutas
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
- `INTERNET` — sincronización con Firebase

## Changelog

### v2.1.3 (febrero 2026)
- Corregido bug crítico: rachas se rompían en cambio de año (dayKey ahora usa epoch days)
- Workers respetan preferencias de notificación del usuario (nagging/deadline)
- Corregido doble random() en notificaciones nagging y AppLeaveDetector
- Resuelto colisión de Notification IDs entre AppLeaveDetector y DeadlineCountdownWorker
- clearCompletedTasks ahora elimina también las tareas remotas en Firestore
- Versión en Ajustes usa BuildConfig.VERSION_NAME dinámicamente
- TaskConverters con manejo seguro de valores inválidos (no crashea)
- Eliminado código duplicado en SmartNotificationWorker (usa StreakCalculator)
- Sincronización protegida con Mutex contra ejecuciones concurrentes
- Logs condicionales: Log.w solo en BuildConfig.DEBUG
- ProGuard configurado para Room, Firebase, Coroutines, DataStore y enums
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
- Sincronización entre dispositivos con Firebase (Auth + Firestore)
- Google Sign-In para vincular cuenta
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
cp app/build/outputs/apk/release/app-release.apk releases/VS-Procrastination-v2.1.3.apk
```

Para el APK de debug (con firma automática):

```bash
./gradlew assembleDebug
```

El APK de debug queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Licencia

Pablo Daniel Granados Martínez.