package com.example.prestamolabctma.data.repository

import com.example.prestamolabctma.domain.model.CategoriaEquipo
import com.example.prestamolabctma.domain.model.Equipo
import com.example.prestamolabctma.domain.model.EstadoEquipo
import com.example.prestamolabctma.domain.model.EstadoSolicitud
import com.example.prestamolabctma.domain.model.SolicitudPrestamo

/**
 * Repositorio persistente en memoria (Singleton).
 * Mantiene la coherencia de los datos entre pantallas.
 */
object InMemoryPrestamoRepository : PrestamoRepository {

    private val equipos = mutableListOf(
        Equipo(1, "Multímetro Digital", CategoriaEquipo.MEDITION_ELECTRICA, EstadoEquipo.DISPONIBLE),
        Equipo(2, "Osciloscopio 50MHz", CategoriaEquipo.MEDITION_ELECTRICA, EstadoEquipo.DISPONIBLE),
        Equipo(3, "Juego de Destornilladores Dieléctricos", CategoriaEquipo.HERRAMIENTAS_MANUALES, EstadoEquipo.DISPONIBLE),
        Equipo(4, "Soldador de Estaño 60W", CategoriaEquipo.ELECTRONICA, EstadoEquipo.DISPONIBLE),
        Equipo(5, "Fuente de Poder Regulable", CategoriaEquipo.ELECTRONICA, EstadoEquipo.DISPONIBLE),
        Equipo(6, "Pinza Amperimétrica", CategoriaEquipo.MEDITION_ELECTRICA, EstadoEquipo.DISPONIBLE)
    )

    private val solicitudes = mutableListOf<SolicitudPrestamo>()
    private var nextSolicitudId = 1

    override fun obtenerEquipos(): List<Equipo> = synchronized(this) { equipos.toList() }
    override fun obtenerEquipo(id: Int): Equipo? = synchronized(this) { equipos.find { it.id == id } }
    override fun obtenerSolicitudes(): List<SolicitudPrestamo> = synchronized(this) { solicitudes.toList() }
    override fun obtenerSolicitud(id: Int): SolicitudPrestamo? = synchronized(this) { solicitudes.find { it.id == id } }

    override fun crearSolicitud(solicitud: SolicitudPrestamo): Result<Unit> = synchronized(this) {
        val equipoIndex = equipos.indexOfFirst { it.id == solicitud.equipoId }
        
        if (equipoIndex == -1) return Result.failure(Exception("Equipo no encontrado"))

        val equipo = equipos[equipoIndex]
        if (equipo.estado != EstadoEquipo.DISPONIBLE) {
            return Result.failure(Exception("RN-01: El equipo ya no está disponible"))
        }

        // RN-06: Actualizar estado del equipo a RESERVADO
        equipos[equipoIndex] = equipo.copy(estado = EstadoEquipo.RESERVADO)

        val nuevaSolicitud = solicitud.copy(
            id = nextSolicitudId++,
            estado = EstadoSolicitud.SOLICITADA
        )
        solicitudes.add(nuevaSolicitud)

        return Result.success(Unit)
    }

    override fun cancelarSolicitud(id: Int): Result<Unit> = synchronized(this) {
        val solicitudIndex = solicitudes.indexOfFirst { it.id == id }
        if (solicitudIndex == -1) return Result.failure(Exception("Solicitud no encontrada"))

        val solicitud = solicitudes[solicitudIndex]
        if (solicitud.estado != EstadoSolicitud.SOLICITADA) {
            return Result.failure(Exception("Solo se pueden cancelar solicitudes en estado SOLICITADA"))
        }

        solicitudes[solicitudIndex] = solicitud.copy(estado = EstadoSolicitud.CANCELADA)

        val equipoIndex = equipos.indexOfFirst { it.id == solicitud.equipoId }
        if (equipoIndex != -1) {
            equipos[equipoIndex] = equipos[equipoIndex].copy(estado = EstadoEquipo.DISPONIBLE)
        }

        return Result.success(Unit)
    }
}
