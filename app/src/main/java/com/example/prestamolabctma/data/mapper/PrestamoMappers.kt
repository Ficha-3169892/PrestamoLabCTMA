package com.example.prestamolabctma.data.mapper

import com.example.prestamolabctma.data.local.EquipoEntity
import com.example.prestamolabctma.data.local.PrestamoEntity
import com.example.prestamolabctma.data.remote.PrestamoDto
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.model.EvidenciaSyncEstado
import com.example.prestamolabctma.model.SolicitudPrestamo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

private fun parseDateSafely(dateStr: String?): LocalDateTime {
    if (dateStr.isNullOrBlank()) return LocalDateTime.now()
    return try {
        LocalDateTime.parse(dateStr, formatter)
    } catch (e: Exception) {
        try {
            LocalDateTime.parse(dateStr)
        } catch (e2: Exception) {
            try {
                ZonedDateTime.parse(dateStr).toLocalDateTime()
            } catch (e3: Exception) {
                try {
                    val pattern1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    LocalDateTime.parse(dateStr, pattern1)
                } catch (e4: Exception) {
                    try {
                        val pattern2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        LocalDateTime.parse(dateStr, pattern2)
                    } catch (e5: Exception) {
                        try {
                            val pattern3 = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            LocalDate.parse(dateStr, pattern3).atStartOfDay()
                        } catch (e6: Exception) {
                            LocalDateTime.now()
                        }
                    }
                }
            }
        }
    }
}

fun PrestamoDto.toEntity(): PrestamoEntity {
    return PrestamoEntity(
        id = id,
        equipoId = equipoId,
        usuarioId = usuarioId,
        ambienteDestino = ambienteDestino,
        proposito = proposito,
        fechaSolicitud = fechaSolicitud,
        fechaInicio = fechaInicio,
        duracionHoras = duracionHoras,
        estado = try { EstadoSolicitud.valueOf(estado) } catch (e: Exception) { EstadoSolicitud.EN_REVISION },
        motivoRechazo = motivoRechazo,
        novedadDevolucion = novedadDevolucion,
        renovaciones = renovaciones,
        evidenciaSyncEstado = EvidenciaSyncEstado.SINCRONIZADA
    )
}

fun PrestamoEntity.toDomain(): SolicitudPrestamo {
    return SolicitudPrestamo(
        id = id,
        equipoId = equipoId,
        usuarioId = usuarioId,
        ambienteDestino = ambienteDestino,
        proposito = proposito,
        fechaSolicitud = parseDateSafely(fechaSolicitud),
        fechaInicio = parseDateSafely(fechaInicio),
        duracionHoras = duracionHoras,
        estado = estado,
        motivoRechazo = motivoRechazo,
        novedadDevolucion = novedadDevolucion,
        renovaciones = renovaciones,
        evidenciaUri = evidenciaUri,
        evidenciaMimeType = evidenciaMimeType,
        evidenciaTamano = evidenciaTamano,
        evidenciaSyncEstado = evidenciaSyncEstado,
        latitud = latitud,
        longitud = longitud,
        ubicacionTimestamp = ubicacionTimestamp
    )
}

fun SolicitudPrestamo.toEntity(): PrestamoEntity {
    return PrestamoEntity(
        id = id,
        equipoId = equipoId,
        usuarioId = usuarioId,
        ambienteDestino = ambienteDestino,
        proposito = proposito,
        fechaSolicitud = fechaSolicitud.format(formatter),
        fechaInicio = fechaInicio.format(formatter),
        duracionHoras = duracionHoras,
        estado = estado,
        motivoRechazo = motivoRechazo,
        novedadDevolucion = novedadDevolucion,
        renovaciones = renovaciones,
        evidenciaUri = evidenciaUri,
        evidenciaMimeType = evidenciaMimeType,
        evidenciaTamano = evidenciaTamano,
        evidenciaSyncEstado = evidenciaSyncEstado,
        latitud = latitud,
        longitud = longitud,
        ubicacionTimestamp = ubicacionTimestamp
    )
}

fun EquipoEntity.toDomain(): Equipo {
    return Equipo(
        id = id,
        placa = placa,
        nombre = nombre,
        categoria = categoria,
        estado = estado,
        ubicacion = ubicacion,
        observaciones = observaciones,
        imagenUrl = imagenUrl
    )
}

fun Equipo.toEntity(): EquipoEntity {
    return EquipoEntity(
        id = id,
        placa = placa,
        nombre = nombre,
        categoria = categoria,
        estado = estado,
        ubicacion = ubicacion,
        observaciones = observaciones,
        imagenUrl = imagenUrl
    )
}
