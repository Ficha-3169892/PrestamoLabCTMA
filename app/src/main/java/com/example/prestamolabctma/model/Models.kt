package com.example.prestamolabctma.model

import java.time.LocalDateTime

enum class CategoriaEquipo {
    ELECTRONICA, HERRAMIENTAS, COMPUTO, AUDIO_VISUAL, MOBILIARIO
}

enum class EstadoEquipo {
    DISPONIBLE, RESERVADO, PRESTADO, MANTENIMIENTO
}

enum class EstadoSolicitud {
    SOLICITADA, APROBADA, RECHAZADA, CANCELADA, DEVUELTA
}

data class Equipo(
    val id: Int,
    val placa: String,
    val nombre: String,
    val categoria: CategoriaEquipo,
    val estado: EstadoEquipo,
    val ubicacion: String
)

data class SolicitudPrestamo(
    val id: Int,
    val equipoId: Int,
    val usuarioId: String = "anonymous",
    val ambienteDestino: String,
    val proposito: String,
    val fechaInicio: LocalDateTime = LocalDateTime.now(),
    val duracionHoras: Int,
    val estado: EstadoSolicitud
)
