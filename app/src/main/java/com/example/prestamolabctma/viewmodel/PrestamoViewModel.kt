package com.example.prestamolabctma.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prestamolabctma.data.repository.InMemoryPrestamoRepository
import com.example.prestamolabctma.data.repository.PrestamoRepository
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.model.EstadoEquipo
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.model.SolicitudPrestamo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class PrestamoUiState(
    val equipos: List<Equipo> = emptyList(),
    val solicitudes: List<SolicitudPrestamo> = emptyList(),
    val mensaje: String? = null,
    val guardando: Boolean = false
)

fun propositoValido(texto: String): Boolean = texto.length in 10..180
fun duracionValida(horas: Int): Boolean = horas in 1..8

class PrestamoViewModel(
    private val repository: PrestamoRepository = InMemoryPrestamoRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrestamoUiState())
    val uiState: StateFlow<PrestamoUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        _uiState.update {
            it.copy(
                equipos = repository.obtenerEquipos(),
                solicitudes = repository.obtenerSolicitudes()
            )
        }
    }

    fun registrarSolicitud(
        equipoId: Int,
        ambienteDestino: String,
        proposito: String,
        duracionHoras: Int
    ) {
        if (_uiState.value.guardando) return

        if (!propositoValido(proposito) || !duracionValida(duracionHoras) || ambienteDestino.isBlank()) {
            _uiState.update { it.copy(mensaje = "Datos de solicitud inválidos") }
            return
        }

        val equipo = repository.obtenerEquipo(equipoId)
        if (equipo == null || equipo.estado != EstadoEquipo.DISPONIBLE) {
            _uiState.update { it.copy(mensaje = "El equipo no está disponible para préstamo") }
            return
        }

        _uiState.update { it.copy(guardando = true) }

        viewModelScope.launch {
            val nuevaSolicitud = SolicitudPrestamo(
                id = 0,
                equipoId = equipoId,
                usuarioId = "anonymous", // ID temporal para compatibilidad
                ambienteDestino = ambienteDestino,
                proposito = proposito,
                fechaInicio = LocalDateTime.now(),
                duracionHoras = duracionHoras,
                estado = EstadoSolicitud.SOLICITADA
            )

            val exito = repository.crearSolicitud(nuevaSolicitud)
            if (exito) {
                _uiState.update { 
                    it.copy(mensaje = "Solicitud creada con éxito", guardando = false)
                }
                cargarDatos()
            } else {
                _uiState.update { 
                    it.copy(mensaje = "Error al crear la solicitud", guardando = false)
                }
            }
        }
    }

    fun cancelarSolicitud(solicitudId: Int) {
        viewModelScope.launch {
            val exito = repository.cancelarSolicitud(solicitudId)
            if (exito) {
                _uiState.update { it.copy(mensaje = "Solicitud cancelada") }
                cargarDatos()
            }
        }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensaje = null) }
    }
}
