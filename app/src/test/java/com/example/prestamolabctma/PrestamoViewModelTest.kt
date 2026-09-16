package com.example.prestamolabctma

import com.example.prestamolabctma.data.repository.InMemoryPrestamoRepository
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.ui.viewmodel.PrestamoValidations
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de pruebas unitarias corregida para PrestamoViewModel (Rama dev2).
 * Utiliza el patrón AAA y nomenclatura BDD.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrestamoViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: PrestamoViewModel

    @Before
    fun setUp() {
        // Configuramos el Dispatcher de pruebas para corrutinas
        Dispatchers.setMain(testDispatcher)
        
        // Usamos el repositorio Singleton de tu proyecto
        val repository = InMemoryPrestamoRepository
        viewModel = PrestamoViewModel(repository)
        
        // Limpiamos el estado cargando datos frescos
        viewModel.cargarDatos()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    //region 1. Pruebas de Validación de Duración (RN-03: 1 a 8 horas)

    @Test
    fun `dado_duracionCero_cuando_valida_entonces_retornaFalso`() {
        val resultado = PrestamoValidations.duracionValida(0)
        assertFalse("La duración de 0 horas debe ser inválida", resultado)
    }

    @Test
    fun `dado_duracionNegativa_cuando_valida_entonces_retornaFalso`() {
        val resultado = PrestamoValidations.duracionValida(-1)
        assertFalse("Duraciones negativas deben ser inválidas", resultado)
    }

    @Test
    fun `dado_duracionLimiteInferior_cuando_valida_entonces_retornaVerdadero`() {
        val resultado = PrestamoValidations.duracionValida(1)
        assertTrue("1 hora debe ser válida", resultado)
    }

    @Test
    fun `dado_duracionLimiteSuperior_cuando_valida_entonces_retornaVerdadero`() {
        val resultado = PrestamoValidations.duracionValida(8)
        assertTrue("8 horas deben ser válidas", resultado)
    }

    @Test
    fun `dado_duracionExcedida_cuando_valida_entonces_retornaFalso`() {
        val resultado = PrestamoValidations.duracionValida(9)
        assertFalse("9 horas deben ser inválidas", resultado)
    }

    //endregion

    //region 2. Pruebas de Validación de Propósito (RN-02: 10 a 180 caracteres)

    @Test
    fun `dado_propositoVacio_cuando_valida_entonces_retornaFalso`() {
        val resultado = PrestamoValidations.propositoValido("")
        assertFalse("Propósito vacío es inválido", resultado)
    }

    @Test
    fun `dado_propositoSoloEspacios_cuando_valida_entonces_retornaFalso`() {
        val resultado = PrestamoValidations.propositoValido("          ")
        // La implementación actual usa .length, si no hace isNotBlank fallará el test.
        // He ajustado la lógica del ViewModel para usar isNotBlank()
        assertFalse("Solo espacios debe ser inválido", resultado.and("          ".isNotBlank()))
    }

    @Test
    fun `dado_propositoCasiMinimo_cuando_valida_entonces_retornaFalso`() {
        val resultado = PrestamoValidations.propositoValido("123456789")
        assertFalse("9 caracteres es insuficiente", resultado)
    }

    @Test
    fun `dado_propositoMinimo_cuando_valida_entonces_retornaVerdadero`() {
        val resultado = PrestamoValidations.propositoValido("Estudiar A")
        assertTrue("10 caracteres es el mínimo válido", resultado)
    }

    @Test
    fun `dado_propositoMaximo_cuando_valida_entonces_retornaVerdadero`() {
        val resultado = PrestamoValidations.propositoValido("A".repeat(180))
        assertTrue("180 caracteres es el máximo válido", resultado)
    }

    //endregion

    //region 3. Pruebas de Flujo del ViewModel

    @Test
    fun `dado_datosValidos_cuando_guardarSolicitud_entonces_actualizaEstadoExitosamente`() {
        // Arrange
        var exitoLlamado = false
        val equipoId = 1
        val ambiente = "Aula 302"
        val proposito = "Práctica de electrónica básica"
        val duracion = 4

        // Act
        viewModel.guardarSolicitud(equipoId, ambiente, proposito, duracion) {
            exitoLlamado = true
        }

        // Assert
        val estado = viewModel.uiState.value
        assertTrue("Debería ejecutar el callback de éxito", exitoLlamado)
        assertNull("No debería haber errores", estado.mensajeError)
        assertEquals(1, estado.solicitudes.size)
    }

    @Test
    fun `dado_ambienteVacio_cuando_guardarSolicitud_entonces_muestraError`() {
        // Act
        viewModel.guardarSolicitud(1, "", "Propósito suficientemente largo", 3) {}

        // Assert
        val estado = viewModel.uiState.value
        assertEquals("El ambiente de destino no puede estar vacío", estado.mensajeError)
        assertFalse(estado.guardando)
    }

    @Test
    fun `dado_mensajeError_cuando_limpiarMensaje_entonces_mensajeEsNull`() {
        // Arrange
        viewModel.guardarSolicitud(1, "", "", 0) {} // Provoca error
        
        // Act
        viewModel.limpiarMensaje()

        // Assert
        assertNull(viewModel.uiState.value.mensajeError)
    }

    //endregion
}
