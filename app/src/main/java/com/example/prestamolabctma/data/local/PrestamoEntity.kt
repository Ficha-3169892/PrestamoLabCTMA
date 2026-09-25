package com.example.prestamolabctma.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.prestamolabctma.model.EstadoSolicitud

@Entity(tableName = "prestamos")
data class PrestamoEntity(
    @PrimaryKey val id: String,
    val equipoId: String?,
    val equipoNombre: String,
    val equipoPlaca: String,
    val equipoCategoria: String,
    val instructorId: String,
    val aprendizId: String,
    val estado: EstadoSolicitud,
    val fechaSolicitud: String,
    val fechaAprobacion: String?,
    val fechaEntrega: String?,
    val fechaDevolucion: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)
