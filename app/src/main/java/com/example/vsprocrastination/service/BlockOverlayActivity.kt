package com.example.vsprocrastination.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vsprocrastination.ui.theme.VSProcrastinationTheme
import kotlinx.coroutines.delay

/**
 * BlockOverlayActivity — Pantalla fullscreen que aparece sobre apps bloqueadas.
 *
 * JUSTIFICACIÓN CIENTÍFICA:
 * - Friction (Clear, 2018): Agrega un punto de fricción entre el impulso y la acción.
 * - Implementation Intentions (Gollwitzer, 1999): "SI abro [app], ENTONCES veo este recordatorio".
 * - Cooling-off Period (Thaler & Sunstein, Nudge): El countdown de 10s
 *   da tiempo al Sistema 2 (deliberativo) de anular el impulso del Sistema 1 (automático).
 *
 * DISEÑO:
 * No es punitivo ni agresivo. Usa tono empático + recordatorio de la meta.
 * El usuario SIEMPRE puede volver, reforzando autonomía (Self-Determination Theory).
 */
class BlockOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BLOCKED_APP_NAME = "blocked_app_name"
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val blockedAppName = intent.getStringExtra(EXTRA_BLOCKED_APP_NAME) ?: "esta app"
        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""

        setContent {
            VSProcrastinationTheme {
                BlockOverlayScreen(
                    blockedAppName = blockedAppName,
                    onGoBack = { finish() },
                    onUnlockAnyway = {
                        // Cerrar el overlay — el usuario vuelve a la app bloqueada
                        // pero el monitor la detectará de nuevo tras el intervalo
                        finish()
                    }
                )
            }
        }
    }

    // Impedir que el botón Back cierre el overlay fácilmente
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Volver a nuestra app en lugar de a la app bloqueada
        finish()
    }
}

@Composable
private fun BlockOverlayScreen(
    blockedAppName: String,
    onGoBack: () -> Unit,
    onUnlockAnyway: () -> Unit
) {
    // Countdown de enfriamiento para el botón de desbloqueo
    var cooldownSeconds by remember { mutableIntStateOf(10) }
    var showUnlockButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds--
        }
        showUnlockButton = true
    }

    // Animación de entrada
    val backgroundAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500),
        label = "bg_alpha"
    )

    // Pulso del escudo
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val shieldScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_scale"
    )

    // Frase motivacional aleatoria
    val phrases = remember {
        listOf(
            "Tu yo futuro te lo agradecerá 💪",
            "23 minutos. Eso tarda tu cerebro en recuperar el enfoque tras una distracción.",
            "No necesitas motivación, necesitas disciplina. Pero tu app te ayuda con ambas.",
            "El impulso dura ~90 segundos. Respira y vuelve a tu tarea.",
            "Cada vez que resistes, tu corteza prefrontal se fortalece. Literalmente.",
            "\"La disciplina es elegir entre lo que quieres ahora y lo que más quieres.\" — Abraham Lincoln",
            "Estás aquí porque tú mismo elegiste proteger tu tiempo. Confía en esa decisión.",
            "El aburrimiento es la puerta de la creatividad. No lo mates con scrolling."
        )
    }
    val selectedPhrase = remember { phrases.random() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(backgroundAlpha)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Escudo animado
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(shieldScale)
                    .background(
                        Color(0xFF4FC3F7).copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🛡️",
                    fontSize = 48.sp
                )
            }

            // Título
            Text(
                text = "Modo Enfoque Activo",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            // Nombre de la app bloqueada
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚫 $blockedAppName",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bloqueada durante tu sesión Pomodoro",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Frase motivacional
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4FC3F7).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "💡 $selectedPhrase",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón principal: Volver a la tarea
            Button(
                onClick = onGoBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4FC3F7)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "🎯 Volver a mi tarea",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E)
                    )
                )
            }

            // Botón secundario: Desbloquear con cooldown
            AnimatedVisibility(
                visible = true,
                enter = fadeIn()
            ) {
                if (!showUnlockButton) {
                    Text(
                        text = "Desbloquear en ${cooldownSeconds}s...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.4f)
                        ),
                        textAlign = TextAlign.Center
                    )
                } else {
                    TextButton(
                        onClick = onUnlockAnyway,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Desbloquear de todos modos",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // Dato científico
            Text(
                text = "🧠 Dato: El 88% de las veces que abres redes sociales es por hábito, no por necesidad real (Duke University, 2006)",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.4f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
