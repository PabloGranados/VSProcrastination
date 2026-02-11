package com.example.vsprocrastination

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vsprocrastination.data.preferences.PreferencesManager
import com.example.vsprocrastination.ui.screens.MainScreen
import com.example.vsprocrastination.ui.screens.SettingsScreen
import com.example.vsprocrastination.ui.screens.WeeklySummaryScreen
import com.example.vsprocrastination.ui.theme.VSProcrastinationTheme
import com.example.vsprocrastination.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    private lateinit var preferencesManager: PreferencesManager
    
    // Solicitar permiso de notificaciones (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Las notificaciones son críticas, pero la app funciona sin ellas */ }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        preferencesManager = PreferencesManager(this)
        
        setContent {
            val darkMode by preferencesManager.darkMode.collectAsState(initial = "system")
            
            VSProcrastinationTheme(overrideDarkMode = darkMode) {
                val navController = rememberNavController()
                val uiState by viewModel.uiState.collectAsState()
                
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToWeeklySummary = { navController.navigate("weekly_summary") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            preferencesManager = viewModel.preferencesManager,
                            onBack = { navController.popBackStack() },
                            onClearCompleted = { viewModel.clearCompletedTasks() }
                        )
                    }
                    composable("weekly_summary") {
                        WeeklySummaryScreen(
                            tasks = uiState.allTasks,
                            stats = uiState.stats,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Notificar al ViewModel que la app pasó a background
        viewModel.onAppPaused()
    }
    
    override fun onResume() {
        super.onResume()
        // Limpiar notificación de "vuelve a la app"
        viewModel.onAppResumed()
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}