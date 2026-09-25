package com.example.prestamolabctma.data.remote

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SupabasePrestamoDataSource(
    private val supabaseClient: SupabaseClient
) {
    suspend fun getPrestamos(): List<PrestamoDto> {
        return try {
            val user = supabaseClient.auth.currentUserOrNull()
            Log.d("EquipoDebug", "SupabasePrestamoDataSource.getPrestamos: currentUser=${user?.id}, email=${user?.email}")
            val dtos = supabaseClient.postgrest.from("prestamos")
                .select()
                .decodeList<PrestamoDto>()
            Log.d("EquipoDebug", "SupabasePrestamoDataSource.getPrestamos: obtenidos ${dtos.size} préstamos")
            dtos
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabasePrestamoDataSource.getPrestamos error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun createPrestamo(dto: PrestamoDto): Result<Unit> {
        return try {
            Log.d("EquipoDebug", "SupabasePrestamoDataSource.createPrestamo: creando préstamo dto=$dto")
            supabaseClient.postgrest.from("prestamos").insert(dto)
            Log.d("EquipoDebug", "SupabasePrestamoDataSource.createPrestamo: creado exitosamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabasePrestamoDataSource.createPrestamo error: ${e.message}", e)
            Result.failure(Exception("Error al solicitar préstamo: ${e.localizedMessage}", e))
        }
    }

    suspend fun updatePrestamoEstado(
        prestamoId: String, 
        nuevoEstado: String, 
        timestampField: String?, 
        equipoIdToUpdateEstado: String?, 
        nuevoEstadoEquipo: String?
    ): Result<Unit> {
        return try {
            val nowStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            Log.d("EquipoDebug", "SupabasePrestamoDataSource.updatePrestamoEstado: id=$prestamoId, estado=$nuevoEstado, time=$nowStr")
            
            supabaseClient.postgrest.from("prestamos").update(
                buildJsonObject {
                    put("estado", nuevoEstado)
                    if (timestampField != null) {
                        put(timestampField, nowStr)
                    }
                }
            ) {
                filter {
                    eq("id", prestamoId)
                }
            }

            if (equipoIdToUpdateEstado != null && nuevoEstadoEquipo != null) {
                Log.d("EquipoDebug", "SupabasePrestamoDataSource: actualizando equipo equipo_id=$equipoIdToUpdateEstado a estado=$nuevoEstadoEquipo")
                supabaseClient.postgrest.from("equipos").update(
                    buildJsonObject {
                        put("estado", nuevoEstadoEquipo)
                    }
                ) {
                    filter {
                        eq("id", equipoIdToUpdateEstado)
                    }
                }
            }

            Log.d("EquipoDebug", "SupabasePrestamoDataSource.updatePrestamoEstado: transición completada")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabasePrestamoDataSource.updatePrestamoEstado error: ${e.message}", e)
            Result.failure(Exception("Error al actualizar estado del préstamo: ${e.localizedMessage}", e))
        }
    }
}
