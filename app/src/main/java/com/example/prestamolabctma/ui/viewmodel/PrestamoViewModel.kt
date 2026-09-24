package com.example.prestamolabctma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prestamolabctma.data.PrestamoRepository
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
    val uriPreview: String? = null,
    val estadoSync: EvidenciaSyncEstado? = null,
    val mensajeError: String? = null,
    val procesando: Boolean = false
)

data class PrestamoUiState(
    val usuarioLogueado: Usuario? = null,
    val equipos: List<Equipo> = emptyList(),
    val solicitudes: List<SolicitudPrestamo> = emptyList(),
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
    private val userPreferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrestamoUiState())
    val uiState: StateFlow<PrestamoUiState> = _uiState.asStateFlow()

    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Inactiva)
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    init {
        // Colectar solicitudes reactivamente
        viewModelScope.launch {
            repository.obtenerSolicitudesFlow().collect { lista ->
                _uiState.update { current ->
                    val newStatus = if (lista.isEmpty() && current.equipos.isEmpty()) UiStatus.Empty else UiStatus.Content(lista.size)
                    current.copy(solicitudes = lista, lastUpdated = System.currentTimeMillis(), status = newStatus)
                }
            }
        }

        // Colectar catálogo de equipos reactivamente
        viewModelScope.launch {
            repository.obtenerEquiposFlow().collect { equipos ->
                _uiState.update { current ->
                    val newStatus = if (equipos.isEmpty() && current.solicitudes.isEmpty()) UiStatus.Empty else UiStatus.Content(equipos.size)
                    current.copy(equipos = equipos, status = newStatus)
                }
            }
        }

        // Colectar preferencias de usuario si DataStore está presente
        userPreferencesRepository?.let { prefsRepo ->
            viewModelScope.launch {
                prefsRepo.userPreferencesFlow.collect { prefs ->
                    val catEnum = prefs.categoriaFiltro?.let {
                        try { CategoriaEquipo.valueOf(it) } catch (e: Exception) { null }
                    }
                    _uiState.update { it.copy(filtroBusqueda = prefs.busquedaFiltro, filtroCategoria = catEnum) }
                }
            }
        }

        cargarEquipos()
    }

    fun cargarEquipos() {
        viewModelScope.launch {
            val lista = repository.obtenerEquipos()
            if (lista.isNotEmpty()) {
                _uiState.update { it.copy(equipos = lista, status = UiStatus.Content(lista.size)) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshState.value = RefreshState.EnCurso
            try {
                val result = repository.refreshPrestamos()
                result.fold(
                    onSuccess = {
                        _refreshState.value = RefreshState.Exitosa
                    },
                    onFailure = {
                        _refreshState.value = RefreshState.Fallida(it.message ?: "Error")
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _refreshState.value = RefreshState.Fallida(e.message ?: "Error inesperado")
            }
        }
    }

    // --- GESTIÓN DE EVIDENCIA (Semana 9) ---

    fun adjuntarEvidencia(inputStream: InputStream, fileName: String, mimeType: String, size: Long) {
        val MAX_SIZE = 5 * 1024 * 1024 // 5MB
        
        if (size > MAX_SIZE) {
            _uiState.update { it.copy(
                evidenciaEstado = it.evidenciaEstado.copy(mensajeError = "El archivo es demasiado grande (máx 5MB)")
            )}
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(evidenciaEstado = it.evidenciaEstado.copy(procesando = true)) }
            
            val result = repository.guardarEvidenciaLocal(inputStream, fileName)
            result.fold(
                onSuccess = { path ->
                    _uiState.update { it.copy(
                        evidenciaEstado = EvidenciaUiState(uriPreview = path, estadoSync = EvidenciaSyncEstado.LOCAL)
                    )}
                },
                onFailure = { error ->
                    _uiState.update { it.copy(
                        evidenciaEstado = it.evidenciaEstado.copy(mensajeError = "Error al guardar localmente: ${error.message}", procesando = false)
                    )}
                }
            )
        }
    }

    fun confirmarYSubirEvidencia(solicitudId: Int) {
        val estadoActual = _uiState.value.evidenciaEstado
        val uri = estadoActual.uriPreview ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(status = UiStatus.Operation("Subiendo evidencia fotográfica")) }
            repository.vincularEvidenciaASolicitud(
                solicitudId, 
                uri, 
                mimeType = "image/jpeg", 
                tamano = 0
            )
            
            val result = repository.subirEvidenciaAlServidor(solicitudId)
            result.onFailure {
                _uiState.update { current -> current.copy(mensaje = "Sincronización fallida. Se reintentará luego.") }
            }
            _uiState.update { current -> current.copy(status = UiStatus.Content(current.solicitudes.size)) }
        }
    }

    fun eliminarEvidencia() {
        _uiState.update { it.copy(evidenciaEstado = EvidenciaUiState()) }
    }

    // --- GESTIÓN DE UBICACIÓN GPS Y NOTIFICACIONES (Semana 9) ---

    fun registrarDevolucionConUbicacion(
        solicitudId: Int,
        novedades: String?,
        esGrave: Boolean,
        locationHelper: LocationHelper
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = UiStatus.Operation("Obteniendo ubicación GPS para confirmación")) }
            
            var lat: Double? = null
            var lng: Double? = null
            var ts: Long? = null

            val locResult = locationHelper.obtenerUbicacionPuntual()
            locResult.onSuccess { ubicacion ->
                lat = ubicacion.latitud
                lng = ubicacion.longitud
                ts = ubicacion.timestamp
                _uiState.update { current ->
                    current.copy(ultimaUbicacionRegistrada = "Lat: ${ubicacion.latitud}, Lng: ${ubicacion.longitud}")
                }
            }.onFailure {
                _uiState.update { current ->
                    current.copy(mensaje = "Devolución registrada sin GPS (No disponible o permiso denegado)")
                }
            }

            val solicitud = repository.obtenerSolicitud(solicitudId)
            if (solicitud != null) {
                val solicitudConUbicacion = solicitud.copy(
                    latitud = lat,
                    longitud = lng,
                    ubicacionTimestamp = ts
                )
                repository.crearSolicitud(solicitudConUbicacion)
            }

            val estadoFinal = if (esGrave) EstadoSolicitud.EN_REVISION else EstadoSolicitud.DEVUELTA
            repository.actualizarEstadoSolicitud(solicitudId, estadoFinal, novedades)
            if (esGrave && solicitud != null) {
                repository.actualizarEstadoEquipo(solicitud.equipoId, EstadoEquipo.REPARACION)
            }
            cargarEquipos()
            _uiState.update { current -> current.copy(status = UiStatus.Content(current.solicitudes.size)) }
        }
    }

    fun activarRecordatorioNotificacion(solicitudId: Int, notificationHelper: NotificationHelper) {
        viewModelScope.launch {
            val solicitud = repository.obtenerSolicitud(solicitudId) ?: return@launch
            val equipo = repository.obtenerEquipo(solicitud.equipoId)
            val equipoNombre = equipo?.nombre ?: "Equipo de Laboratorio"

            val enviado = notificationHelper.mostrarRecordatorioDevolucion(
                solicitudId = solicitudId,
                equipoNombre = equipoNombre,
                ambiente = solicitud.ambienteDestino
            )

            if (enviado) {
                _uiState.update { it.copy(mensaje = "Recordatorio de devolución activado correctamente") }
            } else {
                _uiState.update { it.copy(mensaje = "No se enviaron notificaciones (permiso denegado)") }
            }
        }
    }

    // --- LÓGICA DE NEGOCIO Y AUTENTICACIÓN ---

    fun login(identificador: String, contrasena: String): Boolean {
        if (identificador.isBlank() || contrasena.isBlank()) {
            _uiState.update { it.copy(mensaje = "Documento o contraseña incorrectos", status = UiStatus.Error("Credenciales vacías")) }
            return false
        }
        val usuario = repository.validarUsuario(identificador, contrasena)
        if (usuario != null) {
            _uiState.update { it.copy(usuarioLogueado = usuario, mensaje = "Bienvenido ${usuario.nombre}", status = UiStatus.Content(it.solicitudes.size)) }
            userPreferencesRepository?.let { prefs ->
                viewModelScope.launch { prefs.guardarUltimoRolUsado(usuario.rol.name) }
            }
            refresh()
            return true
        } else {
            _uiState.update { it.copy(mensaje = "Documento o contraseña incorrectos", status = UiStatus.Error("Credenciales inválidas")) }
            return false
        }
    }

    fun registrarSolicitud(equipoId: Int, ambiente: String, proposito: String, horas: Int, fechaInicio: LocalDateTime) {
        val usuario = _uiState.value.usuarioLogueado ?: return
        if (usuario.tieneSanciones) {
            _uiState.update { it.copy(mensaje = "El aprendiz tiene devoluciones pendientes o sanciones") }
            return
        }
        if (!duracionValida(horas)) {
            _uiState.update { it.copy(mensaje = "La duración máxima permitida es de 8 horas") }
            return
        }
        
        _uiState.update { it.copy(guardando = true, status = UiStatus.Operation("Guardando solicitud")) }
        val id = (_uiState.value.solicitudes.maxOfOrNull { it.id } ?: 0) + 1
        val sol = SolicitudPrestamo(
            id = id, equipoId = equipoId, usuarioId = usuario.id,
            ambienteDestino = ambiente, proposito = proposito,
            fechaSolicitud = LocalDateTime.now(), fechaInicio = fechaInicio,
            duracionHoras = horas, estado = EstadoSolicitud.SOLICITADA
        )
        repository.crearSolicitud(sol)
        _uiState.update { current -> current.copy(guardando = false, mensaje = "Solicitud enviada (Código: RES-$id)", status = UiStatus.Content(current.solicitudes.size + 1)) }
    }

    fun procesarSolicitud(solicitudId: Int, aprobado: Boolean, motivo: String? = null) {
        val nuevoEstado = if (aprobado) EstadoSolicitud.APROBADA else EstadoSolicitud.RECHAZADA
        repository.actualizarEstadoSolicitud(solicitudId, nuevoEstado, motivo)
        cargarEquipos()
    }

    fun registrarDevolucion(solicitudId: Int, novedades: String?, esGrave: Boolean) {
        viewModelScope.launch {
            val solicitud = repository.obtenerSolicitud(solicitudId) ?: return@launch
            val estadoFinal = if (esGrave) EstadoSolicitud.EN_REVISION else EstadoSolicitud.DEVUELTA
            repository.actualizarEstadoSolicitud(solicitudId, estadoFinal, novedades)
            if (esGrave) repository.actualizarEstadoEquipo(solicitud.equipoId, EstadoEquipo.REPARACION)
            cargarEquipos()
        }
    }

    fun solicitarExtension(solicitudId: Int) {
        viewModelScope.launch {
            val solicitud = repository.obtenerSolicitud(solicitudId) ?: return@launch
            if (solicitud.renovaciones >= 1) {
                _uiState.update { it.copy(mensaje = "Máximo 1 renovación permitida") }
                return@launch
            }
            cargarEquipos()
        }
    }

    fun setFiltroCategoria(categoria: CategoriaEquipo?) {
        _uiState.update { it.copy(filtroCategoria = categoria) }
        userPreferencesRepository?.let { prefs ->
            viewModelScope.launch { prefs.guardarCategoriaFiltro(categoria?.name) }
        }
    }

    fun setFiltroBusqueda(texto: String) {
        _uiState.update { it.copy(filtroBusqueda = texto) }
        userPreferencesRepository?.let { prefs ->
            viewModelScope.launch { prefs.guardarBusquedaFiltro(texto) }
        }
    }

    fun obtenerEquiposFiltrados(): List<Equipo> {
        val state = _uiState.value
        return state.equipos.filter { 
            (it.nombre.contains(state.filtroBusqueda, true) || it.placa.contains(state.filtroBusqueda, true)) &&
            (state.filtroCategoria == null || it.categoria == state.filtroCategoria)
        }
    }

    fun limpiarMensaje() { _uiState.update { it.copy(mensaje = null) } }
    fun resetRefreshState() { _refreshState.value = RefreshState.Inactiva }
}
