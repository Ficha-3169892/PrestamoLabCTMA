package com.example.prestamolabctma.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReporteNovedadDto(
    @SerialName("id") val id: String? = null,
    @SerialName("equipo_id") val equipoId: String,
    @SerialName("equipo_nombre") val equipoNombre: String,
    @SerialName("equipo_placa") val equipoPlaca: String,
    @SerialName("prestamo_id") val prestamoId: String? = null,
    @SerialName("aprendiz_id") val aprendizId: String,
    @SerialName("instructor_id") val instructorId: String,
    @SerialName("descripcion") val descripcion: String,
    @SerialName("estado") val estado: String = "activo",
    @SerialName("devolucion_solicitada") val devolucionSolicitada: Boolean = false,
    @SerialName("devolucion_solicitada_en") val devolucionSolicitadaEn: String? = null,
    @SerialName("fecha_recepcion") val fechaRecepcion: String? = null,
    @SerialName("fecha_reporte") val fechaReporte: String? = null,
    @SerialName("fecha_resolucion") val fechaResolucion: String? = null
)

@Serializable
data class ReporteFotoDto(
    @SerialName("id") val id: String? = null,
    @SerialName("reporte_id") val reporteId: String,
    @SerialName("storage_path") val storagePath: String
)
