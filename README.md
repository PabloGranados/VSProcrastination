# VS Procrastination

**v2.0** — App Android para dejar de procrastinar. Usa principios de psicología conductual para que dejes de postergar y empieces a hacer las cosas.

No es otra lista de tareas. La app decide por ti qué hacer primero, te acompaña mientras lo haces y te molesta si no lo haces.

## Descargar APK

📲 **[Descargar VS Procrastination v2.0](releases/VS-Procrastination-v2.0.apk)**

> Para instalar: descarga el APK → abre el archivo → permite la instalación desde fuentes desconocidas si tu dispositivo lo pide → listo.

## Novedades en v2.0

### 🔄 Sincronización entre dispositivos
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

### Notificaciones

- Recordatorio periódico cada 2 horas vía WorkManager
- Notificaciones persistentes (no se descartan con swipe)
- Modo nagging cada 15 minutos para tareas vencidas, con mensajes directos y rotantes
- Recordatorio 1 hora antes del deadline
- **Countdown estilo Duolingo**: cuando una tarea está a menos de 2 horas de vencer, aparece una notificación con cronómetro regresivo en tiempo real que cuenta atrás hasta el deadline. Se vuelve pegajosa cuando quedan menos de 30 minutos.
- Reprogramación automática tras reinicio del dispositivo

### Rachas y motivación

- Racha de días consecutivos completando al menos una tarea
- Animación de celebración al completar una tarea (confeti)
- Frases motivacionales contextuales: cambian según si la tarea es difícil, rápida, si llevas racha, o si hay tareas vencidas
- Resumen semanal con estadísticas, reflexión adaptativa y datos curiosos sobre procrastinación

### Configuración

- Duración del Pomodoro ajustable
- Toggle de notificaciones nagging y recordatorios de deadline
- Tema: claro, oscuro o automático del sistema
- Sincronización con Google (nuevo en v2.0)
- Limpiar tareas completadas

### Progreso

- Barra de progreso global con porcentaje
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
│   ├── TaskReminderWorker.kt    # WorkManager (3 niveles de notificación)
│   ├── DeadlineCountdownWorker.kt # Countdown <2h estilo Duolingo
│   ├── AppLeaveDetector.kt      # BroadcastReceiver (salida de app)
│   └── BootReceiver.kt          # Reprograma workers tras reboot
├── ui/
│   ├── screens/
│   │   ├── MainScreen.kt        # Pantalla principal
│   │   ├── SettingsScreen.kt    # Configuración
│   │   └── WeeklySummaryScreen.kt # Resumen semanal
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
cp app/build/outputs/apk/release/app-release.apk releases/VS-Procrastination-v2.0.apk
```

Para el APK de debug (con firma automática):

```bash
./gradlew assembleDebug
```

El APK de debug queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Licencia

Pablo Daniel Granados Martínez.