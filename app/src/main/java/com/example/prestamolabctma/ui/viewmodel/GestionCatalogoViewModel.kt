package com.example.prestamolabctma.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prestamolabctma.data.PrestamoRepository
import com.example.prestamolabctma.data.auth.SessionRepository
import com.example.prestamolabctma.model.CategoriaEquipo
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.model.EstadoEquipo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GestionCatalogoUiState(
    val equiposInstructor: List<Equipo> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showFormDialog: Boolean = false,
    val editingEquipo: Equipo? = null,
    val placa: String = "",
    val nombre: String = "",
    val categoria: CategoriaEquipo = CategoriaEquipo.HERRAMIENTAS,
    val descripcion: String = "",
    val estado: EstadoEquipo = EstadoEquipo.DISPONIBLE
)

class GestionCatalogoViewModel(
    private val repository: PrestamoRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GestionCatalogoUiState())
    val uiState: StateFlow<GestionCatalogoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.currentUser.collect { user ->
                if (user != null) {
                    Log.d("EquipoDebug", "GestionCatalogoViewModel: usuario detectado ${user.id}, cargando equipos...")
                    cargarEquiposInstructor(user.id)
                } else {
                    Log.d("EquipoDebug", "GestionCatalogoViewModel: usuario es null en currentUser flow")
                }
            }
        }
    }

    fun cargarEquiposInstructor(userId: String? = sessionRepository.currentUser.value?.id) {
        val id = userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val lista = repository.obtenerEquiposInstructor(id)
                Log.d("EquipoDebug", "cargarEquiposInstructor: obtenidos ${lista.size} equipos para instructor_id=$id")
                _uiState.update { it.copy(equiposInstructor = lista, isLoading = false) }
            } catch (e: Exception) {
                Log.e("EquipoDebug", "cargarEquiposInstructor error: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Error al cargar equipos") }
            }
        }
    }

    fun abrirFormularioCrear() {
        _uiState.update { 
            it.copy(
                showFormDialog = true,
                editingEquipo = null,
                placa = "",
                nombre = "",
                categoria = CategoriaEquipo.HERRAMIENTAS,
                descripcion = "",
                estado = EstadoEquipo.DISPONIBLE,
                errorMessage = null
            )
        }
    }

    fun abrirFormularioEditar(equipo: Equipo) {
        _uiState.update { 
            it.copy(
                showFormDialog = true,
                editingEquipo = equipo,
                placa = equipo.placa,
                nombre = equipo.nombre,
                categoria = equipo.categoria,
                descripcion = equipo.descripcion,
                estado = equipo.estado,
                errorMessage = null
            )
        }
    }

    fun cerrarFormulario() {
        _uiState.update { it.copy(showFormDialog = false, editingEquipo = null, errorMessage = null) }
    }

    fun updatePlaca(placa: String) { _uiState.update { it.copy(placa = placa, errorMessage = null) } }
    fun updateNombre(nombre: String) { _uiState.update { it.copy(nombre = nombre, errorMessage = null) } }
    fun updateCategoria(categoria: CategoriaEquipo) { _uiState.update { it.copy(categoria = categoria) } }
    fun updateDescripcion(desc: String) { _uiState.update { it.copy(descripcion = desc) } }
    fun updateEstado(estado: EstadoEquipo) { _uiState.update { it.copy(estado = estado) } }

    val isFormValid: Boolean
        get() {
            val state = _uiState.value
            return state.placa.isNotBlank() && state.nombre.isNotBlank()
        }

    fun guardarEquipo() {
        val state = _uiState.value
        val user = sessionRepository.currentUser.value
        if (user == null) {
            Log.e("EquipoDebug", "guardarEquipo: user es NULL (no hay usuario logueado en SessionRepository)")
            _uiState.update { it.copy(errorMessage = "Sesión no iniciada o rol no reconocido") }
            return
        }
        if (!isFormValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val equipo = Equipo(
                id = state.editingEquipo?.id ?: "",
                placa = state.placa.trim(),
                nombre = state.nombre.trim(),
                categoria = state.categoria,
                estado = state.estado,
                descripcion = state.descripcion.trim(),
                instructorId = user.id
            )
            Log.d("EquipoDebug", "guardarEquipo: intentando guardar equipo placa=${equipo.placa} para instructor_id=${user.id}")

            val result = if (state.editingEquipo == null) {
                repository.guardarEquipo(equipo, user.id)
            } else {
                repository.actualizarEquipo(equipo, user.id)
            }

            result.fold(
                onSuccess = {
                    Log.d("EquipoDebug", "guardarEquipo: éxito al guardar equipo")
                    _uiState.update { it.copy(isLoading = false, showFormDialog = false, successMessage = "Equipo guardado exitosamente") }
                    cargarEquiposInstructor(user.id)
                },
                onFailure = { error ->
                    Log.e("EquipoDebug", "guardarEquipo: fallo al guardar equipo: ${error.message}", error)
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Error al guardar equipo") }
                }
            )
        }
    }

    fun eliminarEquipo(equipoId: String) {
        val user = sessionRepository.currentUser.value
        if (user == null) {
            Log.e("EquipoDebug", "eliminarEquipo: user es NULL")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.eliminarEquipo(equipoId, user.id)
            result.fold(
                onSuccess = {
                    Log.d("EquipoDebug", "eliminarEquipo: éxito")
                    _uiState.update { it.copy(isLoading = false, successMessage = "Equipo eliminado correctamente") }
                    cargarEquiposInstructor(user.id)
                },
                onFailure = { error ->
                    Log.e("EquipoDebug", "eliminarEquipo error: ${error.message}", error)
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Error al eliminar equipo") }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
