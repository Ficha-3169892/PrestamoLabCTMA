package com.example.prestamolabctma.data.auth

import com.example.prestamolabctma.model.Usuario
import kotlinx.serialization.Serializable

@Serializable
data class PerfilDto(
    val id: String,
    val nombre: String,
    val rol: String
)

interface AuthRepository {
    suspend fun signUp(email: String, password: String, nombre: String, rol: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Usuario>
}
