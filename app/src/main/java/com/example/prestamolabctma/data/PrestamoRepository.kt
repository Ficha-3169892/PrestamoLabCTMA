package com.example.prestamolabctma.data

import com.example.prestamolabctma.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.InputStream

interface PrestamoRepository {
    // Equipos
    suspend fun obtenerEquipos(): List<Equipo>
    fun obtenerEquiposFlow(): Flow<List<Equipo>>
    suspend fun obtenerEquipo(id: Int): Equipo?
    fun actualizarEstadoEquipo(id: Int, nuevoEstado: EstadoEquipo)
    
    // Usuarios & Auth
    fun validarUsuario(identificador: String, contrasena: String): Usuario?
    fun obtenerUsuario(id: String): Usuario?

    // Solicitudes
    suspend fun obtenerSolicitudes(): List<SolicitudPrestamo>
    fun obtenerSolicitudesFlow(): Flow<List<SolicitudPrestamo>>
    suspend fun obtenerSolicitud(id: Int): SolicitudPrestamo?
    fun crearSolicitud(solicitud: SolicitudPrestamo)
    fun actualizarEstadoSolicitud(id: Int, nuevoEstado: EstadoSolicitud, motivo: String? = null)
    
    // Remote
    suspend fun refreshPrestamos(): Result<Unit>
    
    // Novedades
    fun registrarNovedad(novedad: Novedad)
    fun obtenerNovedadesPorEquipo(equipoId: Int): List<Novedad>

    // Semana 9: Evidencia fotográfica
    suspend fun guardarEvidenciaLocal(inputStream: InputStream, fileName: String): Result<String>
    suspend fun vincularEvidenciaASolicitud(solicitudId: Int, uri: String, mimeType: String, tamano: Long)
    suspend fun subirEvidenciaAlServidor(solicitudId: Int): Result<Unit>
}

class InMemoryPrestamoRepository : PrestamoRepository {
    private val usuarios = mutableListOf(
        Usuario("123456", "Juan Perez", "juan@misena.edu.co", Role.APRENDIZ, "2558662"),
        Usuario("654321", "Maria Lopez", "maria@misena.edu.co", Role.INSTRUCTOR),
        Usuario("admin", "Admin Lab", "admin@ctma.edu.co", Role.ADMIN),
        Usuario("cuentadante", "Cuentadante 1", "c1@ctma.edu.co", Role.CUENTADANTE)
    )

    private val equipos = mutableListOf(
        Equipo(1, "PL-001", "Osciloscopio Digital", CategoriaEquipo.ELECTRONICA, EstadoEquipo.DISPONIBLE, "Laboratorio 1", "Calibrado 2024"),
        Equipo(2, "PL-002", "Multímetro Fluke", CategoriaEquipo.ELECTRONICA, EstadoEquipo.DISPONIBLE, "Laboratorio 1"),
        Equipo(3, "PL-003", "Taladro Percutor", CategoriaEquipo.HERRAMIENTAS, EstadoEquipo.DISPONIBLE, "Taller Mecánica"),
        Equipo(4, "PL-004", "Laptop Dell Precision", CategoriaEquipo.COMPUTO, EstadoEquipo.MANTENIMIENTO, "Almacén"),
        Equipo(5, "PL-005", "Cámara Sony Alpha", CategoriaEquipo.AUDIO_VISUAL, EstadoEquipo.PRESTADO, "Audiovisuales")
    )

    private val equiposFlow = MutableStateFlow<List<Equipo>>(equipos.toList())
    private val solicitudes = mutableListOf<SolicitudPrestamo>()
    private val solicitudesFlow = MutableStateFlow<List<SolicitudPrestamo>>(emptyList())
    private val novedades = mutableListOf<Novedad>()

    override suspend fun obtenerEquipos(): List<Equipo> = equipos.toList()

    override fun obtenerEquiposFlow(): Flow<List<Equipo>> = equiposFlow

    override suspend fun obtenerEquipo(id: Int): Equipo? = equipos.find { it.id == id }

    override fun actualizarEstadoEquipo(id: Int, nuevoEstado: EstadoEquipo) {
        val index = equipos.indexOfFirst { it.id == id }
        if (index != -1) {
            equipos[index] = equipos[index].copy(estado = nuevoEstado)
            equiposFlow.value = equipos.toList()
        }
    }

    override fun validarUsuario(identificador: String, contrasena: String): Usuario? {
        return usuarios.find { it.id == identificador || it.correo == identificador }
    }

    override fun obtenerUsuario(id: String): Usuario? = usuarios.find { it.id == id }

    override suspend fun obtenerSolicitudes(): List<SolicitudPrestamo> = solicitudes.toList()

    override fun obtenerSolicitudesFlow(): Flow<List<SolicitudPrestamo>> = solicitudesFlow

    override suspend fun obtenerSolicitud(id: Int): SolicitudPrestamo? = solicitudes.find { it.id == id }

    override fun crearSolicitud(solicitud: SolicitudPrestamo) {
        solicitudes.add(solicitud)
        solicitudesFlow.value = solicitudes.toList()
        if (solicitud.estado == EstadoSolicitud.APROBADA || solicitud.estado == EstadoSolicitud.SOLICITADA) {
            actualizarEstadoEquipo(solicitud.equipoId, EstadoEquipo.RESERVADO)
        }
    }

    override fun actualizarEstadoSolicitud(id: Int, nuevoEstado: EstadoSolicitud, motivo: String?) {
        val index = solicitudes.indexOfFirst { it.id == id }
        if (index != -1) {
            val oldSol = solicitudes[index]
            solicitudes[index] = oldSol.copy(estado = nuevoEstado, motivoRechazo = motivo)
            solicitudesFlow.value = solicitudes.toList()
            
            when (nuevoEstado) {
                EstadoSolicitud.ENTREGADA -> actualizarEstadoEquipo(oldSol.equipoId, EstadoEquipo.PRESTADO)
                EstadoSolicitud.DEVUELTA, EstadoSolicitud.CANCELADA, EstadoSolicitud.RECHAZADA -> 
                    actualizarEstadoEquipo(oldSol.equipoId, EstadoEquipo.DISPONIBLE)
                EstadoSolicitud.EN_REVISION -> actualizarEstadoEquipo(oldSol.equipoId, EstadoEquipo.REPARACION)
                else -> {}
            }
        }
    }

    override suspend fun refreshPrestamos(): Result<Unit> {
        return Result.success(Unit)
    }

    override fun registrarNovedad(novedad: Novedad) {
        novedades.add(novedad)
        if (novedad.esGrave) {
            actualizarEstadoEquipo(novedad.equipoId, EstadoEquipo.MANTENIMIENTO)
        }
    }

    override fun obtenerNovedadesPorEquipo(equipoId: Int): List<Novedad> = 
        novedades.filter { it.equipoId == equipoId }

    override suspend fun guardarEvidenciaLocal(inputStream: InputStream, fileName: String): Result<String> {
        return Result.success("internal_storage/$fileName")
    }

    override suspend fun vincularEvidenciaASolicitud(solicitudId: Int, uri: String, mimeType: String, tamano: Long) {
        val index = solicitudes.indexOfFirst { it.id == solicitudId }
        if (index != -1) {
            solicitudes[index] = solicitudes[index].copy(
                evidenciaUri = uri,
                evidenciaMimeType = mimeType,
                evidenciaTamano = tamano,
                evidenciaSyncEstado = EvidenciaSyncEstado.LOCAL
            )
            solicitudesFlow.value = solicitudes.toList()
        }
    }

    override suspend fun subirEvidenciaAlServidor(solicitudId: Int): Result<Unit> {
        val index = solicitudes.indexOfFirst { it.id == solicitudId }
        if (index != -1) {
            solicitudes[index] = solicitudes[index].copy(evidenciaSyncEstado = EvidenciaSyncEstado.SUBIENDO)
            solicitudesFlow.value = solicitudes.toList()

            delay(1000)
            
            solicitudes[index] = solicitudes[index].copy(evidenciaSyncEstado = EvidenciaSyncEstado.SINCRONIZADA)
            solicitudesFlow.value = solicitudes.toList()
        }
        return Result.success(Unit)
    }
}
