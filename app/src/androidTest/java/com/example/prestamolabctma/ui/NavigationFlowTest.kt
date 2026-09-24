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
class NavigationFlowTest {

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
    fun flujoNavegacionPrincipal_loginYCatalogoADetalle() {
        // 1. En pantalla Login, realizar ingreso
        composeTestRule.onNodeWithText("Documento o Correo").performTextInput("123456")
        composeTestRule.onNodeWithText("Contraseña").performTextInput("123")
        composeTestRule.onNodeWithText("Iniciar Sesión").performClick()

        // 2. Verificar que se navega al Catálogo de Equipos
        composeTestRule.onNodeWithText("Catálogo de Equipos").assertExists()
        composeTestRule.onNodeWithText("Osciloscopio Digital").assertExists()

        // 3. Hacer clic en un equipo para ir a Detalle
        composeTestRule.onNodeWithText("Osciloscopio Digital").performClick()

        // 4. Verificar que se muestra la pantalla de Detalle de equipo
        composeTestRule.onNodeWithText("Detalle del Equipo").assertExists()
        composeTestRule.onNodeWithText("Solicitar Préstamo").assertExists()

        // 5. Hacer clic en "Solicitar Préstamo"
        composeTestRule.onNodeWithText("Solicitar Préstamo").performClick()

        // 6. Verificar que está en el formulario de solicitud
        composeTestRule.onNodeWithText("Ambiente/Destino").assertExists()
    }
}
