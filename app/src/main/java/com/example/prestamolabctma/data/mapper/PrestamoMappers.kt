package com.example.prestamolabctma.data.mapper

import com.example.prestamolabctma.data.local.EquipoEntity
import com.example.prestamolabctma.data.local.PrestamoEntity
import com.example.prestamolabctma.data.remote.PrestamoDto
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.model.SolicitudPrestamo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

private fun parseDateSafely(dateStr: String?): LocalDateTime? {
    if (dateStr.isNullOrBlank()) return null
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
                            null
                        }
                    }
                }
            }
        }
    }
}

fun parseEstado(estadoStr: String?): EstadoSolicitud {
    return when (estadoStr?.trim()?.lowercase()) {
        "aprobado" -> EstadoSolicitud.APROBADA
        "rechazado" -> EstadoSolicitud.RECHAZADA
        "entregado" -> EstadoSolicitud.ENTREGADA
        "devuelto" -> EstadoSolicitud.DEVUELTA
        else -> EstadoSolicitud.SOLICITADA
    }
}

fun estadoToString(estado: EstadoSolicitud): String {
    return when (estado) {
        EstadoSolicitud.APROBADA -> "aprobado"
        EstadoSolicitud.RECHAZADA -> "rechazado"
        EstadoSolicitud.ENTREGADA -> "entregado"
        EstadoSolicitud.DEVUELTA -> "devuelto"
        else -> "solicitado"
    }
}

fun PrestamoDto.toEntity(): PrestamoEntity {
    return PrestamoEntity(
        id = id ?: UUID.randomUUID().toString(),
        equipoId = equipoId,
        equipoNombre = equipoNombre ?: "",
        equipoPlaca = equipoPlaca ?: "",
        equipoCategoria = equipoCategoria ?: "",
        instructorId = instructorId ?: "",
        aprendizId = aprendizId ?: "",
        estado = parseEstado(estado),
        fechaSolicitud = fechaSolicitud ?: LocalDateTime.now().format(formatter),
        fechaAprobacion = fechaAprobacion,
        fechaEntrega = fechaEntrega,
        fechaDevolucion = fechaDevolucion
    )
}

fun PrestamoEntity.toDomain(): SolicitudPrestamo {
    return SolicitudPrestamo(
        id = id,
        equipoId = equipoId,
        equipoNombre = equipoNombre,
        equipoPlaca = equipoPlaca,
        equipoCategoria = equipoCategoria,
        instructorId = instructorId,
        aprendizId = aprendizId,
        estado = estado,
        fechaSolicitud = parseDateSafely(fechaSolicitud) ?: LocalDateTime.now(),
        fechaAprobacion = parseDateSafely(fechaAprobacion),
        fechaEntrega = parseDateSafely(fechaEntrega),
        fechaDevolucion = parseDateSafely(fechaDevolucion),
        usuarioId = aprendizId,
        ambienteDestino = equipoNombre,
        proposito = equipoPlaca
    )
}

fun SolicitudPrestamo.toEntity(): PrestamoEntity {
    return PrestamoEntity(
        id = id,
        equipoId = equipoId,
        equipoNombre = equipoNombre,
        equipoPlaca = equipoPlaca,
        equipoCategoria = equipoCategoria,
        instructorId = instructorId,
        aprendizId = aprendizId,
        estado = estado,
        fechaSolicitud = fechaSolicitud.format(formatter),
        fechaAprobacion = fechaAprobacion?.format(formatter),
        fechaEntrega = fechaEntrega?.format(formatter),
        fechaDevolucion = fechaDevolucion?.format(formatter)
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
        descripcion = descripcion,
        imagenUrl = imagenUrl,
        instructorId = instructorId
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
        descripcion = descripcion,
        imagenUrl = imagenUrl,
        instructorId = instructorId
    )
}
