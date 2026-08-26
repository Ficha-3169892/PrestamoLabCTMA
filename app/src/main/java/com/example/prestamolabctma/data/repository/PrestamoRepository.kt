package com.example.prestamolabctma.data.repository

import com.example.prestamolabctma.model.CategoriaEquipo
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.model.EstadoEquipo
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.model.SolicitudPrestamo

interface PrestamoRepository {
    fun obtenerEquipos(): List<Equipo>
    fun obtenerEquipo(id: Int): Equipo?
    fun obtenerSolicitudes(): List<SolicitudPrestamo>
    fun obtenerSolicitud(id: Int): SolicitudPrestamo?
    fun crearSolicitud(solicitud: SolicitudPrestamo): Boolean
    fun cancelarSolicitud(id: Int): Boolean
}

class InMemoryPrestamoRepository : PrestamoRepository {
    private val _equipos = mutableListOf(
        Equipo(1, "Osciloscopio Digital", CategoriaEquipo.ELECTRONICA, EstadoEquipo.DISPONIBLE),
        Equipo(2, "Multímetro Fluke", CategoriaEquipo.ELECTRONICA, EstadoEquipo.DISPONIBLE),
        Equipo(3, "Taladro Percutor", CategoriaEquipo.HERRAMIENTAS, EstadoEquipo.PRESTADO),
        Equipo(4, "Laptop Dell G15", CategoriaEquipo.COMPUTO, EstadoEquipo.DISPONIBLE),
        Equipo(5, "Kit de Herramientas Red", CategoriaEquipo.HERRAMIENTAS, EstadoEquipo.RESERVADO)
    )

    private val _solicitudes = mutableListOf<SolicitudPrestamo>()
    private var nextSolicitudId = 1

    override fun obtenerEquipos(): List<Equipo> = _equipos.toList()

    override fun obtenerEquipo(id: Int): Equipo? = _equipos.find { it.id == id }

    override fun obtenerSolicitudes(): List<SolicitudPrestamo> = _solicitudes.toList()

    override fun obtenerSolicitud(id: Int): SolicitudPrestamo? = _solicitudes.find { it.id == id }

    override fun crearSolicitud(solicitud: SolicitudPrestamo): Boolean {
        val equipo = obtenerEquipo(solicitud.equipoId)
        if (equipo != null && equipo.estado == EstadoEquipo.DISPONIBLE) {
            val nuevaSolicitud = solicitud.copy(id = nextSolicitudId++, estado = EstadoSolicitud.SOLICITADA)
            _solicitudes.add(nuevaSolicitud)
            
            // RN-06: Al crearSolicitud, el equipo asociado debe cambiar su estado a RESERVADO
            actualizarEstadoEquipo(solicitud.equipoId, EstadoEquipo.RESERVADO)
            return true
        }
        return false
    }

    override fun cancelarSolicitud(id: Int): Boolean {
        val solicitud = obtenerSolicitud(id)
        // RN-07: Solo debe permitirse si está en estado SOLICITADA
        if (solicitud != null && solicitud.estado == EstadoSolicitud.SOLICITADA) {
            val index = _solicitudes.indexOf(solicitud)
            _solicitudes[index] = solicitud.copy(estado = EstadoSolicitud.CANCELADA)
            
            // RN-07: El equipo debe volver a DISPONIBLE
            actualizarEstadoEquipo(solicitud.equipoId, EstadoEquipo.DISPONIBLE)
            return true
        }
        return false
    }

    private fun actualizarEstadoEquipo(equipoId: Int, nuevoEstado: EstadoEquipo) {
        val index = _equipos.indexOfFirst { it.id == equipoId }
        if (index != -1) {
            _equipos[index] = _equipos[index].copy(estado = nuevoEstado)
        }
    }
}
