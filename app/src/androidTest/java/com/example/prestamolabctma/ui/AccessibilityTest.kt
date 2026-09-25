package com.example.prestamolabctma.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.prestamolabctma.data.InMemoryPrestamoRepository
import com.example.prestamolabctma.ui.navigation.PrestamoApp
import com.example.prestamolabctma.ui.theme.PrestamoLabCTMATheme
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: PrestamoViewModel
    private lateinit var repository: InMemoryPrestamoRepository

    @Before
    fun setup() {
        repository = InMemoryPrestamoRepository()
        viewModel = PrestamoViewModel(repository)

        composeTestRule.setContent {
            PrestamoLabCTMATheme {
                PrestamoApp(viewModel = viewModel)
            }
        }
    }

    @Test
    fun auditoriaAccesibilidad_pantallaLoginTieneAccionesYTextosAccesibles() {
        // Verificar que el botón de inicio de sesión es clickable y tiene semantics
        composeTestRule.onNodeWithText("Iniciar Sesión")
            .assertExists()
            .assertHasClickAction()

        // Verificar que los campos de entrada tienen etiquetas descriptivas
        composeTestRule.onNodeWithText("Documento o Correo").assertExists()
        composeTestRule.onNodeWithText("Contraseña").assertExists()
    }

    @Test
    fun auditoriaAccesibilidad_catalogoTieneSemanticsListayNavegacion() {
        // Login para ingresar al catálogo
        composeTestRule.onNodeWithText("Documento o Correo").performTextInput("123456")
        composeTestRule.onNodeWithText("Contraseña").performTextInput("123")
        composeTestRule.onNodeWithText("Iniciar Sesión").performClick()

        // Verificar que la barra inferior tiene elementos clickables
        composeTestRule.onNodeWithText("Catálogo")
            .assertExists()
            .assertHasClickAction()

        composeTestRule.onNodeWithText("Mis Préstamos")
            .assertExists()
            .assertHasClickAction()
    }
}
