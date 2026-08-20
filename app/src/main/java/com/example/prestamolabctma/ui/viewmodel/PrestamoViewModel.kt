package com.example.prestamolabctma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prestamolabctma.data.repository.PrestamoRepository
import com.example.prestamolabctma.domain.model.EstadoSolicitud
import com.example.prestamolabctma.domain.model.SolicitudPrestamo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PrestamoViewModel(
    private val repository: PrestamoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrestamoUiState())
    val uiState: StateFlow<PrestamoUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        val equipos = repository.obtenerEquipos()
        val solicitudes = repository.obtenerSolicitudes()
        _uiState.update { 
            it.copy(
                equipos = equipos,
                solicitudes = solicitudes,
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
        // RN-05: Ignorar si ya se está guardando
        if (_uiState.value.guardando) return

        // Bloquear ejecuciones dobles
        _uiState.update { it.copy(guardando = true, mensajeError = null) }

        viewModelScope.launch {
            // Validaciones (RN-02, RN-03, RN-04)
            val error = when {
                !PrestamoValidations.ambienteValido(ambiente) -> "El ambiente de destino no puede estar vacío (RN-02)"
                !PrestamoValidations.propositoValido(proposito) -> "El propósito debe tener entre 10 y 180 caracteres (RN-03)"
                !PrestamoValidations.duracionValida(duracion) -> "La duración debe ser entre 1 y 8 horas (RN-04)"
                else -> null
            }

            if (error != null) {
                _uiState.update { it.copy(guardando = false, mensajeError = error) }
                return@launch
            }

            val nuevaSolicitud = SolicitudPrestamo(
                id = 0, // El repo asignará el ID real
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
                        mensajeError = resultado.exceptionOrNull()?.message ?: "Error desconocido al crear solicitud"
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
}
