package com.example.prestamolabctma.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para el recurso "préstamos" diseñado para PostgREST (Supabase).
 * Los nombres de los campos coinciden con la convención snake_case de base de datos.
 */
@Serializable
data class PrestamoDto(
    @SerialName("id") val id: Int,
    @SerialName("equipo_id") val equipoId: Int,
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("ambiente_destino") val ambienteDestino: String,
    @SerialName("proposito") val proposito: String,
    @SerialName("fecha_solicitud") val fechaSolicitud: String, // ISO-8601
    @SerialName("fecha_inicio") val fechaInicio: String,       // ISO-8601
    @SerialName("duracion_horas") val duracionHoras: Int,
    @SerialName("estado") val estado: String,
    @SerialName("motivo_rechazo") val motivoRechazo: String? = null,
    @SerialName("novedad_devolucion") val novedadDevolucion: String? = null,
    @SerialName("renovaciones") val renovaciones: Int = 0
)
