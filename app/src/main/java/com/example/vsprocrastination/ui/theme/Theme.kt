package com.example.vsprocrastination.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Orange80,
    secondary = BlueGrey80,
    tertiary = Green80,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    primaryContainer = Color(0xFF33200A),
    onPrimaryContainer = Orange80,
    errorContainer = Color(0xFF4E0000),
    onErrorContainer = Color(0xFFFFB4A9)
)

private val LightColorScheme = lightColorScheme(
    primary = Orange40,
    secondary = BlueGrey40,
    tertiary = Green40,
    background = Color(0xFFFFFBF5),
    surface = Color(0xFFFFFBF5),
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF3E2700),
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF410001)
)

@Composable
fun VSProcrastinationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    overrideDarkMode: String = "system", // "system", "light", "dark"
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val effectiveDarkTheme = when (overrideDarkMode) {
        "dark" -> true
        "light" -> false
        else -> darkTheme
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        effectiveDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Colorear la barra de estado
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !effectiveDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}