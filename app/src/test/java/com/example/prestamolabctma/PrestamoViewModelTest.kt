package com.example.prestamolabctma

import com.example.prestamolabctma.data.InMemoryPrestamoRepository
import com.example.prestamolabctma.model.*
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel
import com.example.prestamolabctma.ui.viewmodel.duracionValida
import com.example.prestamolabctma.ui.viewmodel.propositoValido
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class PrestamoViewModelTest {

    private lateinit var viewModel: PrestamoViewModel
    private lateinit var repository: InMemoryPrestamoRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryPrestamoRepository()
        viewModel = PrestamoViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- PRUEBAS DE VALIDACIÓN (Básicas) ---

    @Test
    fun `HU-VALIDACION - Validar duracion y proposito`() {
        assertTrue(duracionValida(4))
        assertFalse(duracionValida(10))
        assertTrue(propositoValido("Prestamo para clase de electronica"))
        assertFalse(propositoValido("Corto"))
    }

    // --- PRUEBAS DE HISTORIAS DE USUARIO ---

    @Test
    fun `HU-01 - Login exitoso con credenciales validas`() {
        val exito = viewModel.login("123456", "cualquiera")
        assertTrue(exito)
        assertEquals("Juan Perez", viewModel.uiState.value.usuarioLogueado?.nombre)
    }

    @Test
    fun `HU-01 - Login fallido con campos vacios`() {
        val exito = viewModel.login("", "")
        assertFalse(exito)
        assertEquals("Documento o contraseña incorrectos", viewModel.uiState.value.mensaje)
    }

    @Test
    fun `HU-02 - Filtrado de catalogo por busqueda de nombre`() {
        viewModel.setFiltroBusqueda("Osciloscopio")
        val filtrados = viewModel.obtenerEquiposFiltrados()
        assertTrue(filtrados.any { it.nombre.contains("Osciloscopio") })
        assertFalse(filtrados.any { it.nombre.contains("Taladro") })
    }

    @Test
    fun `HU-02 - Filtrado de catalogo por categoria`() {
        viewModel.setFiltroCategoria(CategoriaEquipo.HERRAMIENTAS)
        val filtrados = viewModel.obtenerEquiposFiltrados()
        assertTrue(filtrados.all { it.categoria == CategoriaEquipo.HERRAMIENTAS })
    }

    @Test
    fun `HU-04 - Registro de solicitud exitoso`() = runTest {
        viewModel.login("123456", "123")
        val initialCount = repository.obtenerSolicitudes().size
        viewModel.registrarSolicitud(
            equipoId = 1,
            ambiente = "Laboratorio A",
            proposito = "Practica de circuitos impresos",
            horas = 2,
            fechaInicio = LocalDateTime.now()
        )
        testScheduler.advanceUntilIdle()
        assertEquals(initialCount + 1, repository.obtenerSolicitudes().size)
        assertEquals("Solicitud enviada (Código: RES-${initialCount + 1})", viewModel.uiState.value.mensaje)
    }

    @Test
    fun `HU-06 - Cuentadante aprueba solicitud pendiente`() = runTest {
        viewModel.login("cuentadante", "admin")
        repository.crearSolicitud(SolicitudPrestamo(1, 1, "123456", "Ambiente 1", "Proposito largo", LocalDateTime.now(), LocalDateTime.now(), 2, EstadoSolicitud.SOLICITADA))
        
        viewModel.procesarSolicitud(1, aprobado = true)
        testScheduler.advanceUntilIdle()
        val sol = repository.obtenerSolicitud(1)
        assertEquals(EstadoSolicitud.APROBADA, sol?.estado)
    }

    @Test
    fun `HU-07 - Registro de devolucion exitoso`() = runTest {
        viewModel.login("cuentadante", "admin")
        repository.crearSolicitud(SolicitudPrestamo(1, 1, "123456", "Ambiente 1", "Proposito largo", LocalDateTime.now(), LocalDateTime.now(), 2, EstadoSolicitud.ENTREGADA))
        
        viewModel.registrarDevolucion(1, novedades = "Todo bien", esGrave = false)
        testScheduler.advanceUntilIdle()
        assertEquals(EstadoSolicitud.DEVUELTA, repository.obtenerSolicitud(1)?.estado)
        assertEquals(EstadoEquipo.DISPONIBLE, repository.obtenerEquipo(1)?.estado)
    }

    @Test
    fun `HU-08 - Solicitud de renovacion dentro de limites`() = runTest {
        repository.crearSolicitud(SolicitudPrestamo(1, 1, "123456", "Ambiente 1", "Proposito largo", LocalDateTime.now(), LocalDateTime.now(), 2, EstadoSolicitud.ENTREGADA))
        viewModel.solicitarExtension(1)
        testScheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.mensaje)
    }

    @Test
    fun `HU-09 - Reporte de falla grave inhabilita equipo`() = runTest {
        viewModel.login("cuentadante", "admin")
        repository.crearSolicitud(SolicitudPrestamo(1, 1, "123456", "Ambiente 1", "Proposito largo", LocalDateTime.now(), LocalDateTime.now(), 2, EstadoSolicitud.ENTREGADA))
        
        viewModel.registrarDevolucion(1, novedades = "Pantalla rota", esGrave = true)
        testScheduler.advanceUntilIdle()
        assertEquals(EstadoEquipo.REPARACION, repository.obtenerEquipo(1)?.estado)
    }

    // --- SEMANA 09: PRUEBAS DE EVIDENCIA FOTOGRÁFICA ---

    @Test
    fun `Semana 09 - Adjuntar evidencia valida actualiza estado con URI local`() = runTest {
        val content = "fake image content".toByteArray()
        val inputStream = ByteArrayInputStream(content)
        
        viewModel.adjuntarEvidencia(inputStream, "test.jpg", "image/jpeg", content.size.toLong())
        
        val evidenceState = viewModel.uiState.value.evidenciaEstado
        assertNotNull(evidenceState.uriPreview)
        assertTrue(evidenceState.uriPreview!!.contains("test.jpg"))
        assertEquals(EvidenciaSyncEstado.LOCAL, evidenceState.estadoSync)
        assertNull(evidenceState.mensajeError)
    }

    @Test
    fun `Semana 09 - Adjuntar evidencia de mas de 5MB reporta error`() = runTest {
        val largeSize = 6 * 1024 * 1024L
        val inputStream = ByteArrayInputStream(ByteArray(0))
        
        viewModel.adjuntarEvidencia(inputStream, "big.jpg", "image/jpeg", largeSize)
        
        val evidenceState = viewModel.uiState.value.evidenciaEstado
        assertNull(evidenceState.uriPreview)
        assertEquals("El archivo es demasiado grande (máx 5MB)", evidenceState.mensajeError)
    }

    @Test
    fun `Semana 09 - Confirmar evidencia vincula a prestamo y cambia a sincronizada`() = runTest {
        // 1. Crear solicitud
        repository.crearSolicitud(SolicitudPrestamo(1, 1, "123456", "Amb 1", "Proposito largo", LocalDateTime.now(), LocalDateTime.now(), 2, EstadoSolicitud.SOLICITADA))
        
        // 2. Adjuntar localmente
        val content = "image".toByteArray()
        viewModel.adjuntarEvidencia(ByteArrayInputStream(content), "photo.jpg", "image/jpeg", content.size.toLong())
        
        // 3. Confirmar y subir
        viewModel.confirmarYSubirEvidencia(1)
        testScheduler.advanceUntilIdle()
        
        val solicitud = repository.obtenerSolicitud(1)
        assertNotNull(solicitud?.evidenciaUri)
        assertEquals(EvidenciaSyncEstado.SINCRONIZADA, solicitud?.evidenciaSyncEstado)
    }

    @Test
    fun `Semana 09 - Eliminar evidencia limpia el estado del ViewModel`() {
        val content = "image".toByteArray()
        viewModel.adjuntarEvidencia(ByteArrayInputStream(content), "photo.jpg", "image/jpeg", content.size.toLong())
        
        viewModel.eliminarEvidencia()
        
        val evidenceState = viewModel.uiState.value.evidenciaEstado
        assertNull(evidenceState.uriPreview)
        assertNull(evidenceState.estadoSync)
    }
}
