package com.example.prestamolabctma.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.model.EvidenciaSyncEstado

@Entity(tableName = "prestamos")
data class PrestamoEntity(
    @PrimaryKey val id: Int,
    val equipoId: Int,
    val usuarioId: String,
    val ambienteDestino: String,
    val proposito: String,
    val fechaSolicitud: String, // Guardado como ISO String
    val fechaInicio: String,
    val duracionHoras: Int,
    val estado: EstadoSolicitud,
    val motivoRechazo: String?,
    val novedadDevolucion: String?,
    val renovaciones: Int,
    val lastUpdated: Long = System.currentTimeMillis(),
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
