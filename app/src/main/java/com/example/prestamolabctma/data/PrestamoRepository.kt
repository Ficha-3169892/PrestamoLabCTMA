package com.example.prestamolabctma.data

import com.example.prestamolabctma.model.*

interface PrestamoRepository {
    fun obtenerEquipos(): List<Equipo>
    fun obtenerEquipo(id: Int): Equipo?
    fun obtenerSolicitudes(): List<SolicitudPrestamo>
    fun crearSolicitud(solicitud: SolicitudPrestamo)
    fun cancelarSolicitud(id: Int)
}

class InMemoryPrestamoRepository : PrestamoRepository {
    private val equipos = mutableListOf(
        Equipo(1, "Osciloscopio", CategoriaEquipo.ELECTRONICA, EstadoEquipo.DISPONIBLE),
        Equipo(2, "Multímetro", CategoriaEquipo.ELECTRONICA, EstadoEquipo.DISPONIBLE),
        Equipo(3, "Taladro", CategoriaEquipo.HERRAMIENTAS, EstadoEquipo.DISPONIBLE),
        Equipo(4, "Laptop Dell", CategoriaEquipo.COMPUTO, EstadoEquipo.RESERVADO),
        Equipo(5, "Proyector HDMI", CategoriaEquipo.AUDIO_VISUAL, EstadoEquipo.PRESTADO)
    )

    private val solicitudes = mutableListOf<SolicitudPrestamo>()

    override fun obtenerEquipos(): List<Equipo> = equipos.toList()

    override fun obtenerEquipo(id: Int): Equipo? = equipos.find { it.id == id }

    override fun obtenerSolicitudes(): List<SolicitudPrestamo> = solicitudes.toList()

    override fun crearSolicitud(solicitud: SolicitudPrestamo) {
        val index = equipos.indexOfFirst { it.id == solicitud.equipoId }
        if (index != -1 && equipos[index].estado == EstadoEquipo.DISPONIBLE) {
            equipos[index] = equipos[index].copy(estado = EstadoEquipo.RESERVADO)
            solicitudes.add(solicitud)
        }
    }

    override fun cancelarSolicitud(id: Int) {
        val solIndex = solicitudes.indexOfFirst { it.id == id }
        if (solIndex != -1 && solicitudes[solIndex].estado == EstadoSolicitud.SOLICITADA) {
            val solicitud = solicitudes[solIndex]
            solicitudes[solIndex] = solicitud.copy(estado = EstadoSolicitud.CANCELADA)
            
            val eqIndex = equipos.indexOfFirst { it.id == solicitud.equipoId }
            if (eqIndex != -1) {
                equipos[eqIndex] = equipos[eqIndex].copy(estado = EstadoEquipo.DISPONIBLE)
            }
        }
    }
}
