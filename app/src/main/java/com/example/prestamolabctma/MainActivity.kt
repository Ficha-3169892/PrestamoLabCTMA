package com.example.prestamolabctma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.prestamolabctma.data.InMemoryPrestamoRepository
import com.example.prestamolabctma.ui.navigation.PrestamoApp
import com.example.prestamolabctma.ui.theme.PrestamoLabCTMATheme
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicialización manual para el prototipo educativo
        val repository = InMemoryPrestamoRepository()
        val viewModel = PrestamoViewModel(repository)

        enableEdgeToEdge()
        setContent {
            PrestamoLabCTMATheme {
                // PrestamoApp integra el Scaffold, BottomBar, SnackbarHost y NavHost
                PrestamoApp(viewModel = viewModel)
            }
        }
    }
}
