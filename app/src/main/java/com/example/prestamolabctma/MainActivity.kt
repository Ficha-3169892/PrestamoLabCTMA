package com.example.prestamolabctma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.prestamolabctma.ui.navigation.PrestamoApp
import com.example.prestamolabctma.ui.theme.PrestamoLabCTMATheme
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PrestamoViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            PrestamoLabCTMATheme {
                // PrestamoApp integra el Scaffold, BottomBar, SnackbarHost y NavHost
                PrestamoApp(viewModel = viewModel)
            }
        }
    }
}
