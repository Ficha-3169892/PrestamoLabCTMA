package com.example.prestamolabctma.model

import java.time.LocalDateTime

enum class Role {
    APRENDIZ, INSTRUCTOR, CUENTADANTE, ADMIN
}

data class Usuario(
    val id: String, // Documento
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
    SOLICITADA, APROBADA, ENTREGADA, DEVUELTA, CANCELADA, RECHAZADA, EN_REVISION
}

enum class EvidenciaSyncEstado {
    LOCAL, SUBIENDO, SINCRONIZADA, FALLIDA
}

data class Equipo(
    val id: Int,
    val placa: String,
    val nombre: String,
    val categoria: CategoriaEquipo,
    val estado: EstadoEquipo,
    val ubicacion: String = "Almacén Central",
    val observaciones: String = "",
    val imagenUrl: String? = null
)

data class SolicitudPrestamo(
    val id: Int,
    val equipoId: Int,
    val usuarioId: String,
    val ambienteDestino: String,
    val proposito: String,
    val fechaSolicitud: LocalDateTime = LocalDateTime.now(),
    val fechaInicio: LocalDateTime,
    val duracionHoras: Int,
    val estado: EstadoSolicitud,
    val motivoRechazo: String? = null,
    val novedadDevolucion: String? = null,
    val renovaciones: Int = 0,
    // Semana 9: Evidencia fotográfica
    val evidenciaUri: String? = null,
    val evidenciaMimeType: String? = null,
    val evidenciaTamano: Long? = null,
    val evidenciaSyncEstado: EvidenciaSyncEstado? = null,
    // Semana 9: Capacidad física adicional - Ubicación GPS en entrega/devolución
    val latitud: Double? = null,
    val longitud: Double? = null,
    val ubicacionTimestamp: Long? = null
)

data class Novedad(
    val id: Int,
    val equipoId: Int,
    val usuarioId: String,
    val descripcion: String,
    val fecha: LocalDateTime = LocalDateTime.now(),
    val esGrave: Boolean = false
)
