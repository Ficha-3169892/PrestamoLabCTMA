package com.example.prestamolabctma.ui.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prestamolabctma.data.auth.AuthRepository
import com.example.prestamolabctma.data.auth.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val selectedRole: String = "aprendiz", // "aprendiz" o "instructor"
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isRegistered: Boolean = false,
    // Login state
    val loginEmail: String = "",
    val loginPassword: String = "",
    val isLoginLoading: Boolean = false,
    val loginError: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun updateNombre(nombre: String) {
        _uiState.update { it.copy(nombre = nombre, errorMessage = null) }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun updateRole(role: String) {
        _uiState.update { it.copy(selectedRole = role, errorMessage = null) }
    }

    fun updateLoginEmail(email: String) {
        _uiState.update { it.copy(loginEmail = email, loginError = null) }
    }

    fun updateLoginPassword(password: String) {
        _uiState.update { it.copy(loginPassword = password, loginError = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null, loginError = null) }
    }

    val isFormValid: Boolean
        get() {
            val state = _uiState.value
            val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(state.email).matches()
            val isPasswordValid = state.password.length >= 8
            val isNombreValid = state.nombre.isNotBlank()
            val isRoleValid = state.selectedRole == "aprendiz" || state.selectedRole == "instructor"
            return isNombreValid && isEmailValid && isPasswordValid && isRoleValid
        }

    val isLoginValid: Boolean
        get() {
            val state = _uiState.value
            return state.loginEmail.isNotBlank() && state.loginPassword.isNotBlank()
        }

    fun register(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!isFormValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signUp(
                email = state.email.trim(),
                password = state.password,
                nombre = state.nombre.trim(),
                rol = state.selectedRole
            )
            result.fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = "¡Registro exitoso! Por favor inicia sesión.",
                            isRegistered = true
                        )
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Error desconocido en el registro"
                        )
                    }
                }
            )
        }
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!isLoginValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoginLoading = true, loginError = null) }
            val result = sessionRepository.login(state.loginEmail.trim(), state.loginPassword)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoginLoading = false, loginError = null) }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoginLoading = false,
                            loginError = error.message ?: "Credenciales inválidas"
                        )
                    }
                }
            )
        }
    }
}
