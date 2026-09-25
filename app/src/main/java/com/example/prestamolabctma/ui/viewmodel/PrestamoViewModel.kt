package com.example.prestamolabctma.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prestamolabctma.data.PrestamoRepository
import com.example.prestamolabctma.data.ReporteNovedadRepository
import com.example.prestamolabctma.data.auth.SessionRepository
import com.example.prestamolabctma.data.datastore.UserPreferencesRepository
import com.example.prestamolabctma.model.*
import com.example.prestamolabctma.util.LocationHelper
import com.example.prestamolabctma.util.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.LocalDateTime

sealed class RefreshState {
    object Inactiva : RefreshState()
    object EnCurso : RefreshState()
    object Exitosa : RefreshState()
    data class Fallida(val error: String) : RefreshState()
}

/**
 * Estados explícitos de la interfaz (Loading, Content, Empty, Error, Operation)
 */
sealed class UiStatus {
    object Loading : UiStatus()
    data class Content(val totalItems: Int) : UiStatus()
    object Empty : UiStatus()
    data class Error(val mensaje: String) : UiStatus()
    data class Operation(val descripcion: String) : UiStatus()
}

/**
 * Estado específico para la gestión de evidencia en la UI
 */
data class EvidenciaUiState(
    val fotosUris: List<Uri> = emptyList(),
    val procesando: Boolean = false,
    val mensajeError: String? = null
)

data class PrestamoUiState(
    val usuarioLogueado: Usuario? = null,
    val equipos: List<Equipo> = emptyList(),
    val solicitudes: List<SolicitudPrestamo> = emptyList(),
    val reportesInstructor: List<ReporteNovedad> = emptyList(),
    val mensaje: String? = null,
    val guardando: Boolean = false,
    val filtroCategoria: CategoriaEquipo? = null,
    val filtroBusqueda: String = "",
    val lastUpdated: Long? = null,
    val evidenciaEstado: EvidenciaUiState = EvidenciaUiState(),
    val status: UiStatus = UiStatus.Loading,
    val ultimaUbicacionRegistrada: String? = null
)

fun propositoValido(texto: String) = texto.length in 10..180
fun duracionValida(horas: Int) = horas in 1..8

class PrestamoViewModel(
    private val repository: PrestamoRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val sessionRepository: SessionRepository,
    private val reporteNovedadRepository: ReporteNovedadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrestamoUiState())
    val uiState: StateFlow<PrestamoUiState> = _uiState.asStateFlow()

    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Inactiva)
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    init {
        // Colectar sesión actual desde SessionRepository
        viewModelScope.launch {
            sessionRepository.currentUser.collect { usuario ->
                Log.d("AuthDebug", "PrestamoViewModel.init -> collect currentUser: ${usuario?.correo}, Rol: ${usuario?.rol}")
                _uiState.update { it.copy(usuarioLogueado = usuario) }
                cargarEquiposParaUsuario(usuario)
                if (usuario != null) {
                    Log.d("AuthDebug", "PrestamoViewModel: usuario autenticado detectado, ejecutando cargarSolicitudesRemotas y refresh")
                    cargarSolicitudesRemotas()
                    refresh()
                }
                if (usuario?.rol == Role.INSTRUCTOR) {
                    cargarReportesInstructor(usuario.id)
                }
            }
        }

        // Colectar equipos reactivamente
        viewModelScope.launch {
            repository.obtenerEquiposFlow().collect { lista ->
                val usuario = _uiState.value.usuarioLogueado
                val equiposFiltrados = if (usuario?.rol == Role.INSTRUCTOR) {
                    lista.filter { it.instructorId == usuario.id }
                } else {
                    lista.filter { it.estado == EstadoEquipo.DISPONIBLE }
                }
                Log.d("EquipoDebug", "PrestamoViewModel.obtenerEquiposFlow collect: total=${lista.size}, filtrados=${equiposFiltrados.size}")
                _uiState.update { current ->
                    val newStatus = if (equiposFiltrados.isEmpty() && current.solicitudes.isEmpty()) UiStatus.Empty else UiStatus.Content(equiposFiltrados.size)
                    current.copy(equipos = equiposFiltrados, status = newStatus)
                }
            }
        }

        // Colectar solicitudes reactivamente
        viewModelScope.launch {
            repository.obtenerSolicitudesFlow().collect { lista ->
                _uiState.update { current ->
                    val newStatus = if (lista.isEmpty() && current.equipos.isEmpty()) UiStatus.Empty else UiStatus.Content(lista.size)
                    current.copy(solicitudes = lista, lastUpdated = System.currentTimeMillis(), status = newStatus)
                }
            }
        }

        // Colectar preferencias de usuario si DataStore está presente
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { prefs ->
                val catEnum = prefs.categoriaFiltro?.let {
                    try { CategoriaEquipo.valueOf(it) } catch (e: Exception) { null }
                }
                _uiState.update { it.copy(filtroBusqueda = prefs.busquedaFiltro, filtroCategoria = catEnum) }
            }
        }

        cargarEquiposParaUsuario()
    }

    fun cargarEquiposParaUsuario(usuario: Usuario? = _uiState.value.usuarioLogueado) {
        viewModelScope.launch {
            Log.d("EquipoDebug", "PrestamoViewModel.cargarEquiposParaUsuario: usuario=${usuario?.correo}, rol=${usuario?.rol}, id=${usuario?.id}")
            val lista = if (usuario?.rol == Role.INSTRUCTOR) {
                Log.d("EquipoDebug", "PrestamoViewModel: rol INSTRUCTOR -> llamando a obtenerEquiposInstructor(${usuario.id})")
                repository.obtenerEquiposInstructor(usuario.id)
            } else {
                Log.d("EquipoDebug", "PrestamoViewModel: rol APRENDIZ/NULL -> llamando a obtenerEquiposDisponibles()")
                repository.obtenerEquiposDisponibles()
            }
            Log.d("EquipoDebug", "PrestamoViewModel: equipos obtenidos para UI = ${lista.size}")
            _uiState.update { it.copy(equipos = lista, status = if (lista.isEmpty()) UiStatus.Empty else UiStatus.Content(lista.size)) }
        }
    }

    fun cargarSolicitudesRemotas() {
        viewModelScope.launch {
            try {
                val lista = repository.obtenerSolicitudes()
                _uiState.update { it.copy(solicitudes = lista) }
            } catch (e: Exception) {
                Log.e("EquipoDebug", "cargarSolicitudesRemotas error: ${e.message}", e)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshState.value = RefreshState.EnCurso
            try {
                val usuario = _uiState.value.usuarioLogueado
                cargarEquiposParaUsuario(usuario)
                cargarSolicitudesRemotas()
                if (usuario?.rol == Role.INSTRUCTOR) {
                    cargarReportesInstructor(usuario.id)
                }

                val result = repository.refreshPrestamos()
                result.fold(
                    onSuccess = {
                        _refreshState.value = RefreshState.Exitosa
                    },
                    onFailure = { err ->
                        _refreshState.value = RefreshState.Fallida(err.localizedMessage ?: "Error desconocido")
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _refreshState.value = RefreshState.Fallida(e.localizedMessage ?: "Error desconocido")
            }
        }
    }

    // --- FLUJO DE PRÉSTAMOS Y NOVEDADES (APRENDIZ E INSTRUCTOR) ---

    fun solicitarPrestamo(equipo: Equipo) {
        val usuario = _uiState.value.usuarioLogueado ?: return
        if (usuario.rol != Role.APRENDIZ) return

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            val tieneNovedad = reporteNovedadRepository.tieneNovedadActiva(equipo.id)
            if (tieneNovedad) {
                _uiState.update { it.copy(guardando = false, mensaje = "El equipo tiene una novedad activa y no puede ser solicitado.") }
                return@launch
            }

            val result = repository.solicitarPrestamo(equipo, usuario.id)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(guardando = false, mensaje = "¡Solicitud de préstamo enviada con éxito!") }
                    cargarSolicitudesRemotas()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(guardando = false, mensaje = err.message ?: "Error al solicitar préstamo") }
                }
            )
        }
    }

    fun aprobarPrestamo(prestamoId: String, equipoId: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            val result = repository.aprobarPrestamo(prestamoId, equipoId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(guardando = false, mensaje = "Préstamo aprobado correctamente") }
                    cargarSolicitudesRemotas()
                    cargarEquiposParaUsuario()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(guardando = false, mensaje = err.message ?: "Error al aprobar préstamo") }
                }
            )
        }
    }

    fun rechazarPrestamo(prestamoId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            val result = repository.rechazarPrestamo(prestamoId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(guardando = false, mensaje = "Préstamo rechazado") }
                    cargarSolicitudesRemotas()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(guardando = false, mensaje = err.message ?: "Error al rechazar préstamo") }
                }
            )
        }
    }

    fun entregarPrestamo(prestamoId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            val result = repository.entregarPrestamo(prestamoId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(guardando = false, mensaje = "Préstamo marcado como entregado") }
                    cargarSolicitudesRemotas()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(guardando = false, mensaje = err.message ?: "Error al marcar entregado") }
                }
            )
        }
    }

    fun devolverPrestamo(prestamoId: String, equipoId: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            val result = repository.devolverPrestamo(prestamoId, equipoId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(guardando = false, mensaje = "Devolución registrada correctamente") }
                    cargarSolicitudesRemotas()
                    cargarEquiposParaUsuario()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(guardando = false, mensaje = err.message ?: "Error al registrar devolución") }
                }
            )
        }
    }

    fun cargarReportesInstructor(instructorId: String) {
        viewModelScope.launch {
            Log.d("EquipoDebug", "PrestamoViewModel.cargarReportesInstructor: cargando para instructorId=$instructorId")
            val lista = reporteNovedadRepository.obtenerReportesInstructor(instructorId)
            Log.d("EquipoDebug", "PrestamoViewModel.cargarReportesInstructor: encontrados ${lista.size} reportes activos")
            _uiState.update { it.copy(reportesInstructor = lista) }
        }
    }

    fun solicitarDevolucionReporte(reporteId: String, instructorId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            val beforeCount = _uiState.value.reportesInstructor.size
            Log.d("EquipoDebug", "solicitarDevolucionReporte ANTES: reportesInstructor count=$beforeCount")

            val result = reporteNovedadRepository.solicitarDevolucion(reporteId)
            result.fold(
                onSuccess = {
                    val listaActualizada = reporteNovedadRepository.obtenerReportesInstructor(instructorId)
                    Log.d("EquipoDebug", "solicitarDevolucionReporte DESPUÉS: reportesInstructor count=${listaActualizada.size}")
                    _uiState.update { it.copy(guardando = false, reportesInstructor = listaActualizada, mensaje = "Devolución solicitada al aprendiz") }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(guardando = false, mensaje = err.message ?: "Error al solicitar devolución") }
                }
            )
        }
    }

    fun marcarRecibidoReporte(reporteId: String, prestamoId: String?, equipoId: String?, instructorId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            val beforeCount = _uiState.value.reportesInstructor.size
            Log.d("EquipoDebug", "marcarRecibidoReporte ANTES: reportesInstructor count=$beforeCount")

            val result = reporteNovedadRepository.marcarRecibido(reporteId, prestamoId, equipoId)
            result.fold(
                onSuccess = {
                    val listaActualizada = reporteNovedadRepository.obtenerReportesInstructor(instructorId)
                    Log.d("EquipoDebug", "marcarRecibidoReporte DESPUÉS: reportesInstructor count=${listaActualizada.size}")
                    _uiState.update { it.copy(guardando = false, reportesInstructor = listaActualizada, mensaje = "Equipo marcado como recibido en mantenimiento") }
                    cargarSolicitudesRemotas()
                    cargarEquiposParaUsuario()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(guardando = false, mensaje = err.message ?: "Error al marcar recibido") }
                }
            )
        }
    }

    fun actualizarEstadoEquipoInstructor(equipo: Equipo, nuevoEstado: EstadoEquipo) {
        val usuario = _uiState.value.usuarioLogueado ?: return
        if (usuario.rol != Role.INSTRUCTOR) return

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            val equipoActualizado = equipo.copy(estado = nuevoEstado)
            val result = repository.actualizarEquipo(equipoActualizado, usuario.id)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(guardando = false, mensaje = "Estado actualizado a $nuevoEstado") }
                    cargarEquiposParaUsuario(usuario)
                    if (usuario.rol == Role.INSTRUCTOR) {
                        cargarReportesInstructor(usuario.id)
                    }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(guardando = false, mensaje = err.message ?: "Error al actualizar estado") }
                }
            )
        }
    }

    fun agregarFoto(uri: Uri) {
        _uiState.update { 
            it.copy(evidenciaEstado = it.evidenciaEstado.copy(fotosUris = it.evidenciaEstado.fotosUris + uri)) 
        }
    }

    fun eliminarFoto(uri: Uri) {
        _uiState.update { 
            it.copy(evidenciaEstado = it.evidenciaEstado.copy(fotosUris = it.evidenciaEstado.fotosUris - uri)) 
        }
    }

    fun crearReporteNovedad(
        solicitudId: String,
        descripcion: String,
        context: Context,
        onSuccess: () -> Unit
    ) {
        if (descripcion.isBlank()) return
        val solicitud = _uiState.value.solicitudes.find { it.id == solicitudId } ?: return
        val equipo = _uiState.value.equipos.find { it.id == solicitud.equipoId } ?: Equipo(
            id = solicitud.equipoId ?: "",
            placa = solicitud.equipoPlaca,
            nombre = solicitud.equipoNombre,
            categoria = try { CategoriaEquipo.valueOf(solicitud.equipoCategoria.uppercase()) } catch (e: Exception) { CategoriaEquipo.HERRAMIENTAS },
            estado = EstadoEquipo.DISPONIBLE,
            instructorId = solicitud.instructorId
        )

        val uris = _uiState.value.evidenciaEstado.fotosUris
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, evidenciaEstado = it.evidenciaEstado.copy(procesando = true)) }
            
            val fotosBytes = mutableListOf<Pair<String, ByteArray>>()
            for ((index, uri) in uris.withIndex()) {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        fotosBytes.add("foto_${System.currentTimeMillis()}_$index.jpg" to bytes)
                    }
                } catch (e: Exception) {
                    Log.e("EquipoDebug", "Error leyendo bytes de foto $uri: ${e.message}")
                }
            }

            val result = reporteNovedadRepository.crearReporte(solicitud, equipo, descripcion, fotosBytes)
            result.fold(
                onSuccess = { count ->
                    Log.d("EquipoDebug", "crearReporteNovedad éxito con $count fotos")
                    _uiState.update { 
                        it.copy(
                            guardando = false, 
                            mensaje = "Reporte enviado con éxito ($count fotos adjuntas)",
                            evidenciaEstado = EvidenciaUiState()
                        ) 
                    }
                    onSuccess()
                },
                onFailure = { err ->
                    Log.e("EquipoDebug", "crearReporteNovedad error: ${err.message}", err)
                    _uiState.update { 
                        it.copy(
                            guardando = false, 
                            mensaje = err.message ?: "Error al desvincular o enviar reporte",
                            evidenciaEstado = it.evidenciaEstado.copy(procesando = false)
                        ) 
                    }
                }
            )
        }
    }

    suspend fun obtenerReportesAprendiz(aprendizId: String): List<ReporteNovedad> {
        return reporteNovedadRepository.obtenerReportesAprendiz(aprendizId)
    }

    suspend fun obtenerReportesInstructor(instructorId: String): List<ReporteNovedad> {
        return reporteNovedadRepository.obtenerReportesInstructor(instructorId)
    }

    fun resolverReporte(reporteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true) }
            val result = reporteNovedadRepository.resolverReporte(reporteId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(guardando = false, mensaje = "Reporte marcado como resuelto.") }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(guardando = false, mensaje = err.message ?: "Error al resolver reporte") }
                }
            )
        }
    }

    fun registrarDevolucionConUbicacion(id: Int, novedad: String?, esGrave: Boolean, locationHelper: LocationHelper) {
        viewModelScope.launch {
            val locResult = locationHelper.obtenerUbicacionPuntual()
            val ubicacionData = locResult.getOrNull()
            val lat = ubicacionData?.latitud
            val lon = ubicacionData?.longitud
            _uiState.update { 
                it.copy(
                    mensaje = "Devolución registrada correctamente",
                    ultimaUbicacionRegistrada = if (lat != null && lon != null) "Lat: $lat, Lon: $lon" else "Ubicación no disponible"
                ) 
            }
        }
    }

    fun activarRecordatorioNotificacion(solicitudId: Int, notificationHelper: NotificationHelper) {
        viewModelScope.launch {
            notificationHelper.mostrarRecordatorioDevolucion(solicitudId, "Equipo", "Ambiente")
            _uiState.update { it.copy(mensaje = "Recordatorio de devolución activado correctamente") }
        }
    }

    fun setFiltroBusqueda(texto: String) {
        _uiState.update { it.copy(filtroBusqueda = texto) }
        viewModelScope.launch { userPreferencesRepository.guardarBusquedaFiltro(texto) }
    }

    fun setFiltroCategoria(categoria: CategoriaEquipo?) {
        _uiState.update { it.copy(filtroCategoria = categoria) }
        val catName = categoria?.name ?: ""
        viewModelScope.launch { userPreferencesRepository.guardarCategoriaFiltro(catName) }
    }

    fun obtenerEquiposFiltrados(): List<Equipo> {
        val state = _uiState.value
        return state.equipos.filter { 
            (it.nombre.contains(state.filtroBusqueda, true) || it.placa.contains(state.filtroBusqueda, true)) &&
            (state.filtroCategoria == null || it.categoria == state.filtroCategoria)
        }
    }

    suspend fun obtenerNombreUsuario(userId: String): String {
        return repository.obtenerNombreUsuario(userId)
    }

    fun logout() {
        viewModelScope.launch {
            try {
                sessionRepository.logout()
                Log.d("EquipoDebug", "PrestamoViewModel.logout: sesión cerrada exitosamente")
            } catch (e: Exception) {
                Log.e("EquipoDebug", "PrestamoViewModel.logout error: ${e.message}", e)
            }
            _uiState.update { PrestamoUiState() }
        }
    }

    fun limpiarMensaje() { _uiState.update { it.copy(mensaje = null) } }
    fun resetRefreshState() { _refreshState.value = RefreshState.Inactiva }
}
