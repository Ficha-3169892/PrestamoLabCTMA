package com.example.prestamolabctma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prestamolabctma.data.repository.PrestamoRepository
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.model.SolicitudPrestamo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrestamoUiState(
    val equipos: List<Equipo> = emptyList(),
    val solicitudes: List<SolicitudPrestamo> = emptyList(),
    val mensajeError: String? = null,
    val guardando: Boolean = false
)

object PrestamoValidations {
    fun ambienteValido(ambiente: String): Boolean = ambiente.isNotBlank()
    fun propositoValido(proposito: String): Boolean = proposito.length in 10..180
    fun duracionValida(horas: Int): Boolean = horas in 1..8
}

class PrestamoViewModel(
    private val repository: PrestamoRepository
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
                solicitudes = repository.obtenerSolicitudes(),
                mensajeError = null
            )
        }
    }

    fun guardarSolicitud(
        equipoId: Int,
        ambiente: String,
        proposito: String,
        duracion: Int,
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.guardando) return

        _uiState.update { it.copy(guardando = true, mensajeError = null) }

        viewModelScope.launch {
            val error = when {
                !PrestamoValidations.ambienteValido(ambiente) -> "El ambiente de destino no puede estar vacío"
                !PrestamoValidations.propositoValido(proposito) -> "El propósito debe tener entre 10 y 180 caracteres"
                !PrestamoValidations.duracionValida(duracion) -> "La duración debe ser entre 1 y 8 horas"
                else -> null
            }

            if (error != null) {
                _uiState.update { it.copy(guardando = false, mensajeError = error) }
                return@launch
            }

            val nuevaSolicitud = SolicitudPrestamo(
                id = 0,
                equipoId = equipoId,
                ambienteDestino = ambiente,
                proposito = proposito,
                duracionHoras = duracion,
                estado = EstadoSolicitud.SOLICITADA
            )

            val resultado = repository.crearSolicitud(nuevaSolicitud)

            if (resultado.isSuccess) {
                cargarDatos()
                _uiState.update { it.copy(guardando = false) }
                onSuccess()
            } else {
                _uiState.update { 
                    it.copy(
                        guardando = false, 
                        mensajeError = resultado.exceptionOrNull()?.message ?: "Error al crear la solicitud"
                    )
                }
            }
        }
    }

    fun cancelarSolicitud(solicitudId: Int) {
        viewModelScope.launch {
            val resultado = repository.cancelarSolicitud(solicitudId)
            if (resultado.isSuccess) {
                cargarDatos()
            } else {
                _uiState.update { 
                    it.copy(mensajeError = resultado.exceptionOrNull()?.message ?: "Error al cancelar solicitud")
                }
            }
        }
    }
    
    fun limpiarMensaje() {
        _uiState.update { it.copy(mensajeError = null) }
    }
}
