package com.example.prestamolabctma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.prestamolabctma.navigation.AppNavigation
import com.example.prestamolabctma.ui.theme.PrestamoLabCTMATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrestamoLabCTMATheme {
                AppNavigation()
            }
        }
    }
}
