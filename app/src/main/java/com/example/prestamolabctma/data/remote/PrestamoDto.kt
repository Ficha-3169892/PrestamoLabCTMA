package com.example.prestamolabctma.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrestamoDto(
    @SerialName("id") val id: String? = null,
    @SerialName("equipo_id") val equipoId: String? = null,
    @SerialName("equipo_nombre") val equipoNombre: String? = null,
    @SerialName("equipo_placa") val equipoPlaca: String? = null,
    @SerialName("equipo_categoria") val equipoCategoria: String? = null,
    @SerialName("instructor_id") val instructorId: String? = null,
    @SerialName("aprendiz_id") val aprendizId: String? = null,
    @SerialName("estado") val estado: String = "solicitado",
    @SerialName("fecha_solicitud") val fechaSolicitud: String? = null,
    @SerialName("fecha_aprobacion") val fechaAprobacion: String? = null,
    @SerialName("fecha_entrega") val fechaEntrega: String? = null,
    @SerialName("fecha_devolucion") val fechaDevolucion: String? = null
)
