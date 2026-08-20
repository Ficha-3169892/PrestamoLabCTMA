package com.example.prestamolabctma.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prestamolabctma.data.repository.InMemoryPrestamoRepository
import com.example.prestamolabctma.data.repository.PrestamoRepository
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.model.EstadoEquipo
import com.example.prestamolabctma.model.SolicitudPrestamo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrestamoUiState(
    val equipos: List<Equipo> = emptyList(),
    val solicitudes: List<SolicitudPrestamo> = emptyList(),
    val mensaje: String? = null,
    val guardando: Boolean = false
)

// Funciones puras de validación
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
        if (_uiState.value.guardando) return // RN-05: Prevenir duplicidad por doble click

        if (!propositoValido(proposito) || !duracionValida(duracionHoras) || ambienteDestino.isBlank()) {
            _uiState.update { it.copy(mensaje = "Datos de solicitud inválidos") }
            return
        }

        val equipo = repository.obtenerEquipo(equipoId)
        // RN-01: Validar que el equipo esté DISPONIBLE
        if (equipo == null || equipo.estado != EstadoEquipo.DISPONIBLE) {
            _uiState.update { it.copy(mensaje = "El equipo no está disponible para préstamo") }
            return
        }

        _uiState.update { it.copy(guardando = true) }

        viewModelScope.launch {
            val nuevaSolicitud = SolicitudPrestamo(
                id = 0,
                equipoId = equipoId,
                ambienteDestino = ambienteDestino,
                proposito = proposito,
                duracionHoras = duracionHoras,
                estado = com.example.prestamolabctma.model.EstadoSolicitud.SOLICITADA
            )

            val exito = repository.crearSolicitud(nuevaSolicitud)
            if (exito) {
                _uiState.update { 
                    it.copy(
                        mensaje = "Solicitud creada con éxito",
                        guardando = false
                    )
                }
                cargarDatos()
            } else {
                _uiState.update { 
                    it.copy(
                        mensaje = "Error al crear la solicitud",
                        guardando = false
                    )
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
            } else {
                _uiState.update { it.copy(mensaje = "No se pudo cancelar la solicitud") }
            }
        }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensaje = null) }
    }
}
