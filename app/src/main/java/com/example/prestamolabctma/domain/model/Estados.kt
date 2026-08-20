package com.example.prestamolabctma.domain.model

enum class CategoriaEquipo {
    ELECTRONICA,
    HERRAMIENTAS_MANUALES,
    COMPUTO,
    MEDITION_ELECTRICA
}

enum class EstadoEquipo {
    DISPONIBLE,
    RESERVADO,
    PRESTADO
}

enum class EstadoSolicitud {
    SOLICITADA,
    APROBADA,
    ENTREGADA,
    DEVUELTA,
    CANCELADA,
    RECHAZADA
}
