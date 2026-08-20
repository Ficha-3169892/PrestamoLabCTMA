package com.example.prestamolabctma.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.prestamolabctma.data.PrestamoRepository
import com.example.prestamolabctma.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PrestamoUiState(
    val equipos: List<Equipo> = emptyList(),
    val solicitudes: List<SolicitudPrestamo> = emptyList(),
    val mensaje: String? = null,
    val guardando: Boolean = false
)

fun propositoValido(texto: String) = texto.length in 10..180
fun duracionValida(horas: Int) = horas in 1..8

class PrestamoViewModel(private val repository: PrestamoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PrestamoUiState())
    val uiState: StateFlow<PrestamoUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        _uiState.update { it.copy(
            equipos = repository.obtenerEquipos(),
            solicitudes = repository.obtenerSolicitudes()
        )}
    }

    fun registrarSolicitud(equipoId: Int, ambiente: String, proposito: String, horas: Int) {
        if (!propositoValido(proposito) || !duracionValida(horas)) {
            _uiState.update { it.copy(mensaje = "Datos inválidos") }
            return
        }

        val equipo = repository.obtenerEquipo(equipoId)
        if (equipo?.estado != EstadoEquipo.DISPONIBLE) {
            _uiState.update { it.copy(mensaje = "Equipo no disponible") }
            return
        }

        val existeActiva = repository.obtenerSolicitudes().any { it.equipoId == equipoId && it.estado == EstadoSolicitud.SOLICITADA }
        if (existeActiva) {
            _uiState.update { it.copy(mensaje = "Ya existe una solicitud") }
            return
        }

        _uiState.update { it.copy(guardando = true) }
        val id = (repository.obtenerSolicitudes().maxOfOrNull { it.id } ?: 0) + 1
        val sol = SolicitudPrestamo(id, equipoId, ambiente, proposito, horas, EstadoSolicitud.SOLICITADA)
        
        repository.crearSolicitud(sol)
        _uiState.update { it.copy(guardando = false, mensaje = "Solicitud creada") }
        cargarDatos()
    }

    fun cancelarSolicitud(id: Int) {
        repository.cancelarSolicitud(id)
        _uiState.update { it.copy(mensaje = "Solicitud cancelada") }
        cargarDatos()
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensaje = null) }
    }
}
