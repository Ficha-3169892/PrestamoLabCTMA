package com.example.prestamolabctma.model

import java.time.LocalDateTime

enum class Role {
    APRENDIZ, INSTRUCTOR, CUENTADANTE, ADMIN
}

data class Usuario(
    val id: String, // Documento o UUID
    val nombre: String,
    val correo: String,
    val rol: Role,
    val ficha: String? = null,
    val tieneSanciones: Boolean = false
)

enum class CategoriaEquipo {
    ELECTRONICA, HERRAMIENTAS, MOBILIARIO, COMPUTO, AUDIO_VISUAL
}

enum class EstadoEquipo {
    DISPONIBLE, RESERVADO, PRESTADO, MANTENIMIENTO, REPARACION
}

enum class EstadoSolicitud {
    SOLICITADA, APROBADA, RECHAZADA, ENTREGADA, DEVUELTA, CANCELADA, EN_REVISION
}

enum class EvidenciaSyncEstado {
    LOCAL, SUBIENDO, SINCRONIZADA, FALLIDA
}

data class Equipo(
    val id: String,
    val placa: String,
    val nombre: String,
    val categoria: CategoriaEquipo,
    val estado: EstadoEquipo,
    val ubicacion: String = "Almacén Central",
    val descripcion: String = "",
    val imagenUrl: String? = null,
    val instructorId: String? = null
)

data class SolicitudPrestamo(
    val id: String,
    val equipoId: String?,
    val equipoNombre: String,
    val equipoPlaca: String,
    val equipoCategoria: String,
    val instructorId: String,
    val aprendizId: String,
    val estado: EstadoSolicitud,
    val fechaSolicitud: LocalDateTime = LocalDateTime.now(),
    val fechaAprobacion: LocalDateTime? = null,
    val fechaEntrega: LocalDateTime? = null,
    val fechaDevolucion: LocalDateTime? = null,
    val motivoRechazo: String? = null,
    // Campos de compatibilidad con vistas existentes
    val ambienteDestino: String = "",
    val proposito: String = "",
    val usuarioId: String = aprendizId,
    val evidenciaSyncEstado: EvidenciaSyncEstado? = null
)

data class Novedad(
    val id: Int,
    val equipoId: String,
    val usuarioId: String,
    val descripcion: String,
    val fecha: LocalDateTime = LocalDateTime.now(),
    val esGrave: Boolean = false
)

data class ReporteNovedad(
    val id: String,
    val equipoId: String,
    val equipoNombre: String,
    val equipoPlaca: String,
    val prestamoId: String?,
    val aprendizId: String,
    val instructorId: String,
    val descripcion: String,
    val estado: String = "activo",
    val devolucionSolicitada: Boolean = false,
    val devolucionSolicitadaEn: LocalDateTime? = null,
    val fechaRecepcion: LocalDateTime? = null,
    val fechaReporte: LocalDateTime? = null,
    val fechaResolucion: LocalDateTime? = null,
    val fotos: List<ReporteFoto> = emptyList()
)

data class ReporteFoto(
    val id: String,
    val reporteId: String,
    val storagePath: String,
    val signedUrl: String? = null
)
