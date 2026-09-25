package com.example.prestamolabctma.data.auth

import android.util.Log
import com.example.prestamolabctma.model.Role
import com.example.prestamolabctma.model.Usuario
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : AuthRepository {
    override suspend fun signUp(email: String, password: String, nombre: String, rol: String): Result<Unit> {
        return try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = buildJsonObject {
                    put("nombre", nombre)
                    put("rol", rol)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val message = e.message ?: ""
            val userFriendlyMessage = when {
                message.contains("already registered", ignoreCase = true) ||
                message.contains("already exists", ignoreCase = true) ||
                message.contains("User already registered", ignoreCase = true) -> 
                    "El correo electrónico ya se encuentra registrado."
                message.contains("Network", ignoreCase = true) ||
                message.contains("Unable to resolve host", ignoreCase = true) ->
                    "Error de conexión. Verifique su red e intente de nuevo."
                else -> "Error en el registro: ${e.localizedMessage ?: "Intente nuevamente"}"
            }
            Result.failure(Exception(userFriendlyMessage, e))
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Usuario> {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val currentUser = supabaseClient.auth.currentUserOrNull() 
                ?: return Result.failure(Exception("No se pudo obtener el usuario autenticado."))
            
            val userId = currentUser.id
            val userEmail = currentUser.email ?: email

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
                correo = userEmail,
                rol = roleEnum
            )
            Log.d("AuthDebug", "AuthRepositoryImpl.signIn -> Usuario: ${usuario.correo}, Rol: ${usuario.rol}")

            Result.success(usuario)
        } catch (e: Exception) {
            val message = e.message ?: ""
            val userFriendlyMessage = when {
                message.contains("Invalid login credentials", ignoreCase = true) ||
                message.contains("Invalid Grant", ignoreCase = true) ||
                message.contains("invalid", ignoreCase = true) ||
                message.contains("Email not confirmed", ignoreCase = true) -> 
                    "Correo o contraseña incorrectos."
                message.contains("Network", ignoreCase = true) ||
                message.contains("Unable to resolve host", ignoreCase = true) ->
                    "Error de conexión. Verifique su red e intente de nuevo."
                else -> "Error al iniciar sesión: ${e.localizedMessage ?: "Intente nuevamente"}"
            }
            Result.failure(Exception(userFriendlyMessage, e))
        }
    }
}
