package com.example.prestamolabctma.data

import com.example.prestamolabctma.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.InputStream

interface PrestamoRepository {
    // Equipos
    suspend fun obtenerEquipos(): List<Equipo>
    fun obtenerEquiposFlow(): Flow<List<Equipo>>
    suspend fun obtenerEquipo(id: String): Equipo?
    fun actualizarEstadoEquipo(id: String, nuevoEstado: EstadoEquipo)
    suspend fun obtenerEquiposDisponibles(): List<Equipo>
    suspend fun obtenerEquiposInstructor(instructorId: String): List<Equipo>
    suspend fun guardarEquipo(equipo: Equipo, instructorId: String): Result<Unit>
    suspend fun actualizarEquipo(equipo: Equipo, instructorId: String): Result<Unit>
    suspend fun eliminarEquipo(equipoId: String, instructorId: String): Result<Unit>
    
    // Usuarios & Auth
    fun validarUsuario(identificador: String, contrasena: String): Usuario?
    fun obtenerUsuario(id: String): Usuario?
    suspend fun obtenerNombreUsuario(userId: String): String

    // Solicitudes & Préstamos
    suspend fun obtenerSolicitudes(): List<SolicitudPrestamo>
    fun obtenerSolicitudesFlow(): Flow<List<SolicitudPrestamo>>
    suspend fun obtenerSolicitud(id: Int): SolicitudPrestamo?
    fun crearSolicitud(solicitud: SolicitudPrestamo)
    fun actualizarEstadoSolicitud(id: Int, nuevoEstado: EstadoSolicitud, motivo: String? = null)

    suspend fun solicitarPrestamo(equipo: Equipo, aprendizId: String): Result<Unit>
    suspend fun aprobarPrestamo(prestamoId: String, equipoId: String?): Result<Unit>
    suspend fun rechazarPrestamo(prestamoId: String): Result<Unit>
    suspend fun entregarPrestamo(prestamoId: String): Result<Unit>
    suspend fun devolverPrestamo(prestamoId: String, equipoId: String?): Result<Unit>
    
    // Remote
    suspend fun refreshPrestamos(): Result<Unit>
    
    // Novedades
    fun registrarNovedad(novedad: Novedad)
    fun obtenerNovedadesPorEquipo(equipoId: String): List<Novedad>

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
        Equipo("1", "PL-001", "Osciloscopio Digital", CategoriaEquipo.ELECTRONICA, EstadoEquipo.DISPONIBLE, "Laboratorio 1", "Calibrado 2024"),
        Equipo("2", "PL-002", "Multímetro Fluke", CategoriaEquipo.ELECTRONICA, EstadoEquipo.DISPONIBLE, "Laboratorio 1"),
        Equipo("3", "PL-003", "Taladro Percutor", CategoriaEquipo.HERRAMIENTAS, EstadoEquipo.DISPONIBLE, "Taller Mecánica"),
        Equipo("4", "PL-004", "Laptop Dell Precision", CategoriaEquipo.COMPUTO, EstadoEquipo.MANTENIMIENTO, "Almacén"),
        Equipo("5", "PL-005", "Cámara Sony Alpha", CategoriaEquipo.AUDIO_VISUAL, EstadoEquipo.PRESTADO, "Audiovisuales")
    )

    private val equiposFlow = MutableStateFlow<List<Equipo>>(equipos.toList())
    private val solicitudes = mutableListOf<SolicitudPrestamo>()
    private val solicitudesFlow = MutableStateFlow<List<SolicitudPrestamo>>(emptyList())
    private val novedades = mutableListOf<Novedad>()

    override suspend fun obtenerEquipos(): List<Equipo> = equipos.toList()

    override fun obtenerEquiposFlow(): Flow<List<Equipo>> = equiposFlow

    override suspend fun obtenerEquipo(id: String): Equipo? = equipos.find { it.id == id }

    override fun actualizarEstadoEquipo(id: String, nuevoEstado: EstadoEquipo) {
        val index = equipos.indexOfFirst { it.id == id }
        if (index != -1) {
            equipos[index] = equipos[index].copy(estado = nuevoEstado)
            equiposFlow.value = equipos.toList()
        }
    }

    override suspend fun obtenerEquiposDisponibles(): List<Equipo> = equipos.filter { it.estado == EstadoEquipo.DISPONIBLE }

    override suspend fun obtenerEquiposInstructor(instructorId: String): List<Equipo> = equipos.filter { it.instructorId == instructorId }

    override suspend fun guardarEquipo(equipo: Equipo, instructorId: String): Result<Unit> {
        equipos.add(equipo)
        equiposFlow.value = equipos.toList()
        return Result.success(Unit)
    }

    override suspend fun actualizarEquipo(equipo: Equipo, instructorId: String): Result<Unit> {
        val index = equipos.indexOfFirst { it.id == equipo.id }
        if (index != -1) {
            equipos[index] = equipo
            equiposFlow.value = equipos.toList()
            return Result.success(Unit)
        }
        return Result.failure(Exception("Equipo no encontrado"))
    }

    override suspend fun eliminarEquipo(equipoId: String, instructorId: String): Result<Unit> {
        equipos.removeAll { it.id == equipoId }
        equiposFlow.value = equipos.toList()
        return Result.success(Unit)
    }

    override fun validarUsuario(identificador: String, contrasena: String): Usuario? {
        return usuarios.find { it.id == identificador || it.correo == identificador }
    }

    override fun obtenerUsuario(id: String): Usuario? = usuarios.find { it.id == id }

    override suspend fun obtenerNombreUsuario(userId: String): String {
        return usuarios.find { it.id == userId }?.nombre ?: userId.take(8)
    }

    override suspend fun obtenerSolicitudes(): List<SolicitudPrestamo> = solicitudes.toList()

    override fun obtenerSolicitudesFlow(): Flow<List<SolicitudPrestamo>> = solicitudesFlow

    override suspend fun obtenerSolicitud(id: Int): SolicitudPrestamo? = null

    override fun crearSolicitud(solicitud: SolicitudPrestamo) {
        solicitudes.add(solicitud)
        solicitudesFlow.value = solicitudes.toList()
    }

    override fun actualizarEstadoSolicitud(id: Int, nuevoEstado: EstadoSolicitud, motivo: String?) {}

    override suspend fun solicitarPrestamo(equipo: Equipo, aprendizId: String): Result<Unit> {
        val sol = SolicitudPrestamo(
            id = (0..10000).random().toString(),
            equipoId = equipo.id,
            equipoNombre = equipo.nombre,
            equipoPlaca = equipo.placa,
            equipoCategoria = equipo.categoria.name,
            instructorId = equipo.instructorId ?: "instructor_demo",
            aprendizId = aprendizId,
            estado = EstadoSolicitud.SOLICITADA
        )
        crearSolicitud(sol)
        return Result.success(Unit)
    }

    override suspend fun aprobarPrestamo(prestamoId: String, equipoId: String?): Result<Unit> = Result.success(Unit)
    override suspend fun rechazarPrestamo(prestamoId: String): Result<Unit> = Result.success(Unit)
    override suspend fun entregarPrestamo(prestamoId: String): Result<Unit> = Result.success(Unit)
    override suspend fun devolverPrestamo(prestamoId: String, equipoId: String?): Result<Unit> = Result.success(Unit)

    override suspend fun refreshPrestamos(): Result<Unit> {
        return Result.success(Unit)
    }

    override fun registrarNovedad(novedad: Novedad) {
        novedades.add(novedad)
    }

    override fun obtenerNovedadesPorEquipo(equipoId: String): List<Novedad> = 
        novedades.filter { it.equipoId == equipoId }

    override suspend fun guardarEvidenciaLocal(inputStream: InputStream, fileName: String): Result<String> {
        return Result.success("internal_storage/$fileName")
    }

    override suspend fun vincularEvidenciaASolicitud(solicitudId: Int, uri: String, mimeType: String, tamano: Long) {}

    override suspend fun subirEvidenciaAlServidor(solicitudId: Int): Result<Unit> {
        return Result.success(Unit)
    }
}
