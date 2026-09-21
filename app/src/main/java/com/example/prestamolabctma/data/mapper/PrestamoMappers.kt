package com.example.prestamolabctma.data.mapper

import com.example.prestamolabctma.data.local.PrestamoEntity
import com.example.prestamolabctma.data.remote.PrestamoDto
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.model.EvidenciaSyncEstado
import com.example.prestamolabctma.model.SolicitudPrestamo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

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
        // Al descargar del API, asumimos que si hay una evidencia vinculada (URL), ya está sincronizada
        // Sin embargo, el DTO actual no tiene campos de evidencia. 
        // Si el API devolviera una URL de imagen, la mapearíamos aquí.
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
        fechaSolicitud = LocalDateTime.parse(fechaSolicitud, formatter),
        fechaInicio = LocalDateTime.parse(fechaInicio, formatter),
        duracionHoras = duracionHoras,
        estado = estado,
        motivoRechazo = motivoRechazo,
        novedadDevolucion = novedadDevolucion,
        renovaciones = renovaciones,
        evidenciaUri = evidenciaUri,
        evidenciaMimeType = evidenciaMimeType,
        evidenciaTamano = evidenciaTamano,
        evidenciaSyncEstado = evidenciaSyncEstado
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
        evidenciaSyncEstado = evidenciaSyncEstado
    )
}
