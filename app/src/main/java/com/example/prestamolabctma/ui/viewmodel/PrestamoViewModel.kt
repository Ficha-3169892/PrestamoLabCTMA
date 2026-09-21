package com.example.prestamolabctma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prestamolabctma.data.PrestamoRepository
import com.example.prestamolabctma.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import java.io.InputStream

sealed class RefreshState {
    object Inactiva : RefreshState()
    object EnCurso : RefreshState()
    object Exitosa : RefreshState()
    data class Fallida(val error: String) : RefreshState()
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
    val evidenciaEstado: EvidenciaUiState = EvidenciaUiState()
)

fun propositoValido(texto: String) = texto.length in 10..180
fun duracionValida(horas: Int) = horas in 1..8

class PrestamoViewModel(private val repository: PrestamoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PrestamoUiState())
    val uiState: StateFlow<PrestamoUiState> = _uiState.asStateFlow()

    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Inactiva)
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.obtenerSolicitudesFlow().collect { lista ->
                _uiState.update { it.copy(solicitudes = lista, lastUpdated = System.currentTimeMillis()) }
            }
        }
        cargarEquipos()
    }

    private fun cargarEquipos() {
        _uiState.update { it.copy(equipos = repository.obtenerEquipos()) }
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshState.value = RefreshState.EnCurso
            try {
                val result = repository.refreshPrestamos()
                result.fold(
                    onSuccess = { _refreshState.value = RefreshState.Exitosa },
                    onFailure = { _refreshState.value = RefreshState.Fallida(it.message ?: "Error") }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _refreshState.value = RefreshState.Fallida(e.message ?: "Error inesperado")
            }
        }
    }

    // --- GESTIÓN DE EVIDENCIA (Semana 9) ---

    /**
     * Procesa la imagen seleccionada o capturada, validando tamaño y persistiendo localmente.
     */
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

    /**
     * Vincula la evidencia actual a una solicitud de préstamo y dispara la sincronización.
     */
    fun confirmarYSubirEvidencia(solicitudId: Int) {
        val estadoActual = _uiState.value.evidenciaEstado
        val uri = estadoActual.uriPreview ?: return

        viewModelScope.launch {
            repository.vincularEvidenciaASolicitud(
                solicitudId, 
                uri, 
                mimeType = "image/jpeg", 
                tamano = 0 // En una app real pasaríamos el tamaño real
            )
            
            val result = repository.subirEvidenciaAlServidor(solicitudId)
            result.onFailure { error ->
                _uiState.update { it.copy(mensaje = "Sincronización fallida. Se reintentará luego.") }
            }
        }
    }

    fun eliminarEvidencia() {
        _uiState.update { it.copy(evidenciaEstado = EvidenciaUiState()) }
    }

    // --- LÓGICA DE NEGOCIO EXISTENTE (RESTAURADA) ---

    fun login(identificador: String, contrasena: String): Boolean {
        if (identificador.isBlank() || contrasena.isBlank()) {
            _uiState.update { it.copy(mensaje = "Documento o contraseña incorrectos") }
            return false
        }
        val usuario = repository.validarUsuario(identificador, contrasena)
        if (usuario != null) {
            _uiState.update { it.copy(usuarioLogueado = usuario, mensaje = "Bienvenido ${usuario.nombre}") }
            refresh()
            return true
        } else {
            _uiState.update { it.copy(mensaje = "Documento o contraseña incorrectos") }
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
        
        _uiState.update { it.copy(guardando = true) }
        val id = (_uiState.value.solicitudes.maxOfOrNull { it.id } ?: 0) + 1
        val sol = SolicitudPrestamo(
            id = id, equipoId = equipoId, usuarioId = usuario.id,
            ambienteDestino = ambiente, proposito = proposito,
            fechaSolicitud = LocalDateTime.now(), fechaInicio = fechaInicio,
            duracionHoras = horas, estado = EstadoSolicitud.SOLICITADA
        )
        repository.crearSolicitud(sol)
        _uiState.update { it.copy(guardando = false, mensaje = "Solicitud enviada (Código: RES-$id)") }
    }

    fun procesarSolicitud(solicitudId: Int, aprobado: Boolean, motivo: String? = null) {
        val nuevoEstado = if (aprobado) EstadoSolicitud.APROBADA else EstadoSolicitud.RECHAZADA
        repository.actualizarEstadoSolicitud(solicitudId, nuevoEstado, motivo)
        cargarEquipos()
    }

    fun registrarDevolucion(solicitudId: Int, novedades: String?, esGrave: Boolean) {
        val solicitud = repository.obtenerSolicitud(solicitudId) ?: return
        val estadoFinal = if (esGrave) EstadoSolicitud.EN_REVISION else EstadoSolicitud.DEVUELTA
        repository.actualizarEstadoSolicitud(solicitudId, estadoFinal)
        if (esGrave) repository.actualizarEstadoEquipo(solicitud.equipoId, EstadoEquipo.REPARACION)
        cargarEquipos()
    }

    fun solicitarExtension(solicitudId: Int) {
        val solicitud = repository.obtenerSolicitud(solicitudId) ?: return
        if (solicitud.renovaciones >= 1) {
            _uiState.update { it.copy(mensaje = "Máximo 1 renovación permitida") }
            return
        }
        cargarEquipos()
    }

    fun setFiltroCategoria(categoria: CategoriaEquipo?) { _uiState.update { it.copy(filtroCategoria = categoria) } }
    fun setFiltroBusqueda(texto: String) { _uiState.update { it.copy(filtroBusqueda = texto) } }
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
