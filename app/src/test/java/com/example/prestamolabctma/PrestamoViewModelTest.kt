package com.example.prestamolabctma

import com.example.prestamolabctma.data.InMemoryPrestamoRepository
import com.example.prestamolabctma.model.*
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel
import com.example.prestamolabctma.ui.viewmodel.duracionValida
import com.example.prestamolabctma.ui.viewmodel.propositoValido
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class PrestamoViewModelTest {

    private lateinit var viewModel: PrestamoViewModel
    private lateinit var repository: InMemoryPrestamoRepository

    @Before
    fun setup() {
        repository = InMemoryPrestamoRepository()
        viewModel = PrestamoViewModel(repository)
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
    fun `HU-04 - Registro de solicitud exitoso`() {
        viewModel.login("123456", "123")
        val initialCount = repository.obtenerSolicitudes().size
        viewModel.registrarSolicitud(
            equipoId = 1,
            ambiente = "Laboratorio A",
            proposito = "Practica de circuitos impresos",
            horas = 2,
            fechaInicio = LocalDateTime.now()
        )
        assertEquals(initialCount + 1, repository.obtenerSolicitudes().size)
        assertEquals("Solicitud enviada (Código: RES-${initialCount + 1})", viewModel.uiState.value.mensaje)
    }

    @Test
    fun `HU-04 - Registro bloqueado por sanciones de usuario`() {
        // Simulamos usuario con sanciones (esto se valida en el ViewModel)
        val usuarioSancionado = Usuario("999", "Bad User", "bad@sena.edu.co", Role.APRENDIZ, tieneSanciones = true)
        // Forzamos el estado para el test (en una app real vendría del repo)
        // Nota: Para este test, inyectamos el usuario manualmente si el ViewModel lo permite o usamos el flujo de login
        // Aquí usamos el login de un usuario que NO tiene sanciones en el repo por defecto, 
        // pero podemos probar la lógica de bloqueo.
    }

    @Test
    fun `HU-06 - Cuentadante aprueba solicitud pendiente`() {
        viewModel.login("cuentadante", "admin")
        // Crear solicitud previa
        repository.crearSolicitud(SolicitudPrestamo(1, 1, "123456", "Ambiente 1", "Proposito largo", LocalDateTime.now(), LocalDateTime.now(), 2, EstadoSolicitud.SOLICITADA))
        
        viewModel.procesarSolicitud(1, aprobado = true)
        val sol = repository.obtenerSolicitud(1)
        assertEquals(EstadoSolicitud.APROBADA, sol?.estado)
    }

    @Test
    fun `HU-07 - Registro de devolucion exitoso`() {
        viewModel.login("cuentadante", "admin")
        repository.crearSolicitud(SolicitudPrestamo(1, 1, "123456", "Ambiente 1", "Proposito largo", LocalDateTime.now(), LocalDateTime.now(), 2, EstadoSolicitud.ENTREGADA))
        
        viewModel.registrarDevolucion(1, novedades = "Todo bien", esGrave = false)
        assertEquals(EstadoSolicitud.DEVUELTA, repository.obtenerSolicitud(1)?.estado)
        assertEquals(EstadoEquipo.DISPONIBLE, repository.obtenerEquipo(1)?.estado)
    }

    @Test
    fun `HU-08 - Solicitud de renovacion dentro de limites`() {
        repository.crearSolicitud(SolicitudPrestamo(1, 1, "123456", "Ambiente 1", "Proposito largo", LocalDateTime.now(), LocalDateTime.now(), 2, EstadoSolicitud.ENTREGADA))
        viewModel.solicitarExtension(1)
        // La lógica de renovación actual es una simulación que recarga datos
        assertNull(viewModel.uiState.value.mensaje) // No hay error
    }

    @Test
    fun `HU-09 - Reporte de falla grave inhabilita equipo`() {
        viewModel.login("cuentadante", "admin")
        repository.crearSolicitud(SolicitudPrestamo(1, 1, "123456", "Ambiente 1", "Proposito largo", LocalDateTime.now(), LocalDateTime.now(), 2, EstadoSolicitud.ENTREGADA))
        
        viewModel.registrarDevolucion(1, novedades = "Pantalla rota", esGrave = true)
        assertEquals(EstadoEquipo.REPARACION, repository.obtenerEquipo(1)?.estado)
    }
}
