package com.example.prestamolabctma.data.auth

import android.util.Log
import com.example.prestamolabctma.model.Role
import com.example.prestamolabctma.model.Usuario
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface SessionRepository {
    val currentUser: StateFlow<Usuario?>
    suspend fun login(email: String, password: String): Result<Usuario>
    suspend fun logout()
    suspend fun checkExistingSession()
}

class SessionRepositoryImpl(
    private val authRepository: AuthRepository,
    private val supabaseClient: SupabaseClient
) : SessionRepository {

    private val _currentUser = MutableStateFlow<Usuario?>(null)
    override val currentUser: StateFlow<Usuario?> = _currentUser.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            checkExistingSession()
        }
    }

    override suspend fun checkExistingSession() {
        try {
            val session = supabaseClient.auth.currentSessionOrNull()
            if (session != null) {
                val user = supabaseClient.auth.currentUserOrNull()
                if (user != null) {
                    val userId = user.id
                    val email = user.email ?: ""
                    try {
                        val perfil = supabaseClient.postgrest.from("perfiles")
                            .select {
                                filter { eq("id", userId) }
                            }
                            .decodeSingle<PerfilDto>()

                        val roleEnum = when (perfil.rol.trim().lowercase()) {
                            "instructor" -> Role.INSTRUCTOR
                            else -> Role.APRENDIZ
                        }

                        val usuario = Usuario(
                            id = userId,
                            nombre = perfil.nombre,
                            correo = email,
                            rol = roleEnum
                        )
                        Log.d("AuthDebug", "SessionRepositoryImpl.checkExistingSession -> Usuario restaurado: ${usuario.correo}, Rol: ${usuario.rol}")
                        _currentUser.value = usuario
                    } catch (e: Exception) {
                        Log.d("AuthDebug", "SessionRepositoryImpl.checkExistingSession -> Error perfil: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("AuthDebug", "SessionRepositoryImpl.checkExistingSession -> Error session: ${e.message}")
        }
    }

    override suspend fun login(email: String, password: String): Result<Usuario> {
        val result = authRepository.signIn(email, password)
        result.onSuccess { usuario ->
            Log.d("AuthDebug", "SessionRepositoryImpl.login -> Success. Asignando usuario: ${usuario.correo}, Rol: ${usuario.rol}")
            _currentUser.value = usuario
        }
        result.onFailure { error ->
            Log.d("AuthDebug", "SessionRepositoryImpl.login -> Failure: ${error.message}")
        }
        return result
    }

    override suspend fun logout() {
        try {
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            // ignore
        }
        _currentUser.value = null
    }
}
