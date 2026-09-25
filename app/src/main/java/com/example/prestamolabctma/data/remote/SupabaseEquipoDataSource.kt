package com.example.prestamolabctma.data.remote

import android.util.Log
import com.example.prestamolabctma.model.CategoriaEquipo
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.model.EstadoEquipo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EquipoDto(
    @SerialName("id") val id: String? = null,
    @SerialName("placa") val placa: String,
    @SerialName("nombre") val nombre: String,
    @SerialName("categoria") val categoria: String,
    @SerialName("estado") val estado: String,
    @SerialName("ubicacion") val ubicacion: String = "Almacén Central",
    @SerialName("descripcion") val descripcion: String = "",
    @SerialName("imagen_url") val imagen_url: String? = null,
    @SerialName("instructor_id") val instructor_id: String? = null
)

class SupabaseEquipoDataSource(
    private val supabaseClient: SupabaseClient
) {
    suspend fun getAllEquipos(): List<Equipo> {
        return try {
            val dtos = supabaseClient.postgrest.from("equipos")
                .select()
                .decodeList<EquipoDto>()
            Log.d("EquipoDebug", "SupabaseEquipoDataSource.getAllEquipos: encontrados ${dtos.size} equipos en Supabase")
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseEquipoDataSource.getAllEquipos error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getAvailableEquipos(): List<Equipo> {
        return try {
            val dtos = supabaseClient.postgrest.from("equipos")
                .select {
                    filter {
                        eq("estado", "disponible")
                    }
                }
                .decodeList<EquipoDto>()
            Log.d("EquipoDebug", "SupabaseEquipoDataSource.getAvailableEquipos: encontrados ${dtos.size} equipos disponibles")
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseEquipoDataSource.getAvailableEquipos error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getEquiposByInstructor(instructorId: String): List<Equipo> {
        return try {
            Log.d("EquipoDebug", "SupabaseEquipoDataSource.getEquiposByInstructor: consultando para instructorId=$instructorId")
            val dtos = supabaseClient.postgrest.from("equipos")
                .select {
                    filter {
                        eq("instructor_id", instructorId)
                    }
                }
                .decodeList<EquipoDto>()
            Log.d("EquipoDebug", "SupabaseEquipoDataSource.getEquiposByInstructor: encontrados ${dtos.size} equipos para instructorId=$instructorId")
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseEquipoDataSource.getEquiposByInstructor error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun insertEquipo(equipo: Equipo, instructorId: String): Result<Unit> {
        return try {
            val dto = EquipoDto(
                id = if (equipo.id.isBlank()) null else equipo.id,
                placa = equipo.placa,
                nombre = equipo.nombre,
                categoria = equipo.categoria.name.lowercase(),
                estado = equipo.estado.name.lowercase(),
                ubicacion = equipo.ubicacion,
                descripcion = equipo.descripcion,
                imagen_url = equipo.imagenUrl,
                instructor_id = instructorId
            )
            supabaseClient.postgrest.from("equipos").insert(dto)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseEquipoDataSource.insertEquipo error: ${e.message}", e)
            val msg = e.message ?: ""
            val userMsg = if (msg.contains("unique", ignoreCase = true) || msg.contains("duplicate", ignoreCase = true)) {
                "La placa '${equipo.placa}' ya se encuentra registrada."
            } else {
                "Error al crear el equipo: ${e.localizedMessage ?: "Intente nuevamente"}"
            }
            Result.failure(Exception(userMsg, e))
        }
    }

    suspend fun updateEquipo(equipo: Equipo, instructorId: String): Result<Unit> {
        return try {
            val dto = EquipoDto(
                id = equipo.id,
                placa = equipo.placa,
                nombre = equipo.nombre,
                categoria = equipo.categoria.name.lowercase(),
                estado = equipo.estado.name.lowercase(),
                ubicacion = equipo.ubicacion,
                descripcion = equipo.descripcion,
                imagen_url = equipo.imagenUrl,
                instructor_id = instructorId
            )
            supabaseClient.postgrest.from("equipos").update(dto) {
                filter {
                    eq("id", equipo.id)
                    eq("instructor_id", instructorId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseEquipoDataSource.updateEquipo error: ${e.message}", e)
            Result.failure(Exception("Error al actualizar el equipo: ${e.localizedMessage ?: "Intente nuevamente"}", e))
        }
    }

    suspend fun deleteEquipo(equipoId: String, instructorId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest.from("equipos").delete {
                filter {
                    eq("id", equipoId)
                    eq("instructor_id", instructorId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseEquipoDataSource.deleteEquipo error: ${e.message}", e)
            Result.failure(Exception("Error al eliminar el equipo: ${e.localizedMessage ?: "Intente nuevamente"}", e))
        }
    }
}

private fun EquipoDto.toDomain(): Equipo {
    return Equipo(
        id = id ?: "",
        placa = placa,
        nombre = nombre,
        categoria = try { CategoriaEquipo.valueOf(categoria.uppercase()) } catch (e: Exception) { CategoriaEquipo.HERRAMIENTAS },
        estado = try { EstadoEquipo.valueOf(estado.uppercase()) } catch (e: Exception) { EstadoEquipo.DISPONIBLE },
        ubicacion = ubicacion,
        descripcion = descripcion,
        imagenUrl = imagen_url,
        instructorId = instructor_id
    )
}
