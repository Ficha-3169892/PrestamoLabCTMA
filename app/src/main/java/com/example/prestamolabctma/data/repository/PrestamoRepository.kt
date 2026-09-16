package com.example.prestamolabctma.data.repository

import com.example.prestamolabctma.model.*
import java.time.LocalDateTime

interface PrestamoRepository {
    fun obtenerEquipos(): List<Equipo>
    fun obtenerEquipo(id: Int): Equipo?
    fun obtenerSolicitudes(): List<SolicitudPrestamo>
    fun obtenerSolicitud(id: Int): SolicitudPrestamo?
    fun crearSolicitud(solicitud: SolicitudPrestamo): Result<Unit>
    fun cancelarSolicitud(id: Int): Result<Unit>
}

object InMemoryPrestamoRepository : PrestamoRepository {
    private val _equipos = mutableListOf(
        Equipo(1, "PL-001", "Osciloscopio Digital", CategoriaEquipo.ELECTRONICA, EstadoEquipo.DISPONIBLE, "Laboratorio 1"),
        Equipo(2, "PL-002", "Multímetro Fluke", CategoriaEquipo.ELECTRONICA, EstadoEquipo.DISPONIBLE, "Laboratorio 1"),
        Equipo(3, "PL-003", "Taladro Percutor", CategoriaEquipo.HERRAMIENTAS, EstadoEquipo.PRESTADO, "Taller Mecánica"),
        Equipo(4, "PL-004", "Laptop Dell G15", CategoriaEquipo.COMPUTO, EstadoEquipo.DISPONIBLE, "Almacén"),
        Equipo(5, "PL-005", "Kit de Herramientas Red", CategoriaEquipo.HERRAMIENTAS, EstadoEquipo.RESERVADO, "Taller Redes")
    )

    private val _solicitudes = mutableListOf<SolicitudPrestamo>()
    private var nextSolicitudId = 1

    override fun obtenerEquipos(): List<Equipo> = _equipos.toList()
    override fun obtenerEquipo(id: Int): Equipo? = _equipos.find { it.id == id }
    override fun obtenerSolicitudes(): List<SolicitudPrestamo> = _solicitudes.toList()
    override fun obtenerSolicitud(id: Int): SolicitudPrestamo? = _solicitudes.find { it.id == id }

    override fun crearSolicitud(solicitud: SolicitudPrestamo): Result<Unit> {
        val equipo = obtenerEquipo(solicitud.equipoId)
        return if (equipo != null && equipo.estado == EstadoEquipo.DISPONIBLE) {
            val nuevaSolicitud = solicitud.copy(id = nextSolicitudId++, estado = EstadoSolicitud.SOLICITADA)
            _solicitudes.add(nuevaSolicitud)
            actualizarEstadoEquipo(solicitud.equipoId, EstadoEquipo.RESERVADO)
            Result.success(Unit)
        } else {
            Result.failure(Exception("Equipo no disponible"))
        }
    }

    override fun cancelarSolicitud(id: Int): Result<Unit> {
        val solicitud = obtenerSolicitud(id)
        return if (solicitud != null && solicitud.estado == EstadoSolicitud.SOLICITADA) {
            val index = _solicitudes.indexOf(solicitud)
            _solicitudes[index] = solicitud.copy(estado = EstadoSolicitud.CANCELADA)
            actualizarEstadoEquipo(solicitud.equipoId, EstadoEquipo.DISPONIBLE)
            Result.success(Unit)
        } else {
            Result.failure(Exception("No se pudo cancelar"))
        }
    }

    private fun actualizarEstadoEquipo(equipoId: Int, nuevoEstado: EstadoEquipo) {
        val index = _equipos.indexOfFirst { it.id == equipoId }
        if (index != -1) {
            _equipos[index] = _equipos[index].copy(estado = nuevoEstado)
        }
    }
}
