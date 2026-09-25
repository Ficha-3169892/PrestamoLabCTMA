package com.example.prestamolabctma.data

import android.util.Log
import com.example.prestamolabctma.data.local.EquipoDao
import com.example.prestamolabctma.data.local.PrestamoDao
import com.example.prestamolabctma.data.mapper.toDomain
import com.example.prestamolabctma.data.mapper.toEntity
import com.example.prestamolabctma.data.remote.PrestamoDto
import com.example.prestamolabctma.data.remote.PrestamoRemoteDataSource
import com.example.prestamolabctma.data.remote.SupabaseEquipoDataSource
import com.example.prestamolabctma.data.remote.SupabasePrestamoDataSource
import com.example.prestamolabctma.data.remote.SupabaseReporteDataSource
import com.example.prestamolabctma.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class OfflineFirstPrestamoRepository(
    private val remoteDataSource: PrestamoRemoteDataSource,
    private val localDataSource: PrestamoDao,
    private val equipoDao: EquipoDao,
    private val internalFilesDir: File,
    private val supabaseEquipoDataSource: SupabaseEquipoDataSource? = null,
    private val supabasePrestamoDataSource: SupabasePrestamoDataSource? = null,
    private val supabaseReporteDataSource: SupabaseReporteDataSource? = null,
    private val supabaseClient: SupabaseClient? = null
) : PrestamoRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val usuariosIniciales = listOf(
        Usuario("123456", "Juan Perez", "juan@misena.edu.co", Role.APRENDIZ, "2558662"),
        Usuario("654321", "Maria Lopez", "maria@misena.edu.co", Role.INSTRUCTOR),
        Usuario("admin", "Admin Lab", "admin@ctma.edu.co", Role.ADMIN),
        Usuario("cuentadante", "Cuentadante 1", "c1@ctma.edu.co", Role.CUENTADANTE)
    )

    private val novedades = mutableListOf<Novedad>()

    init {
        // Inicialización limpia sin llamadas de red desatendidas sin sesión autenticada
    }

    override fun obtenerEquiposFlow(): Flow<List<Equipo>> {
        return equipoDao.getAllEquipos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun obtenerEquipos(): List<Equipo> {
        return withContext(Dispatchers.IO) {
            try {
                if (supabaseEquipoDataSource != null) {
                    val equiposSupabase = supabaseEquipoDataSource.getAllEquipos()
                    Log.d("EquipoDebug", "OfflineFirstPrestamoRepository.obtenerEquipos: Supabase devolvió ${equiposSupabase.size} equipos")
                    if (equiposSupabase.isNotEmpty()) {
                        equipoDao.replaceAll(equiposSupabase.map { it.toEntity() })
                    }
                }
            } catch (e: Exception) {
                // fallback to local cache
            }
            val localCount = equipoDao.getAllEquipos().first().size
            Log.d("EquipoDebug", "OfflineFirstPrestamoRepository.obtenerEquipos: Room tiene $localCount equipos")
            equipoDao.getAllEquipos().first().map { it.toDomain() }
        }
    }

    override suspend fun obtenerEquipo(id: String): Equipo? {
        return withContext(Dispatchers.IO) {
            equipoDao.getEquipoById(id)?.toDomain()
        }
    }

    override fun actualizarEstadoEquipo(id: String, nuevoEstado: EstadoEquipo) {
        scope.launch {
            equipoDao.updateEstadoEquipo(id, nuevoEstado)
        }
    }

    override suspend fun obtenerEquiposDisponibles(): List<Equipo> {
        return withContext(Dispatchers.IO) {
            try {
                if (supabaseEquipoDataSource != null) {
                    supabaseEquipoDataSource.getAvailableEquipos()
                } else {
                    equipoDao.getAllEquipos().first().map { it.toDomain() }.filter { it.estado == EstadoEquipo.DISPONIBLE }
                }
            } catch (e: Exception) {
                equipoDao.getAllEquipos().first().map { it.toDomain() }.filter { it.estado == EstadoEquipo.DISPONIBLE }
            }
        }
    }

    override suspend fun obtenerEquiposInstructor(instructorId: String): List<Equipo> {
        return withContext(Dispatchers.IO) {
            try {
                if (supabaseEquipoDataSource != null) {
                    supabaseEquipoDataSource.getEquiposByInstructor(instructorId)
                } else {
                    equipoDao.getAllEquipos().first().map { it.toDomain() }.filter { it.instructorId == instructorId }
                }
            } catch (e: Exception) {
                equipoDao.getAllEquipos().first().map { it.toDomain() }.filter { it.instructorId == instructorId }
            }
        }
    }

    override suspend fun guardarEquipo(equipo: Equipo, instructorId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val result = if (supabaseEquipoDataSource != null) {
                supabaseEquipoDataSource.insertEquipo(equipo, instructorId)
            } else {
                equipoDao.insertEquipo(equipo.copy(instructorId = instructorId).toEntity())
                Result.success(Unit)
            }
            if (result.isSuccess) {
                try {
                    if (supabaseEquipoDataSource != null) {
                        val updated = supabaseEquipoDataSource.getAllEquipos()
                        equipoDao.replaceAll(updated.map { it.toEntity() })
                    }
                } catch (e: Exception) {}
            }
            result
        }
    }

    override suspend fun actualizarEquipo(equipo: Equipo, instructorId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            if (equipo.estado == EstadoEquipo.DISPONIBLE && supabaseReporteDataSource != null) {
                try {
                    supabaseReporteDataSource.resolverReporteSiExiste(equipo.id)
                } catch (e: Exception) {
                    Log.e("EquipoDebug", "actualizarEquipo resolverReporteSiExiste error: ${e.message}", e)
                }
            }

            val result = if (supabaseEquipoDataSource != null) {
                supabaseEquipoDataSource.updateEquipo(equipo, instructorId)
            } else {
                equipoDao.insertEquipo(equipo.copy(instructorId = instructorId).toEntity())
                Result.success(Unit)
            }
            if (result.isSuccess) {
                try {
                    if (supabaseEquipoDataSource != null) {
                        val updated = supabaseEquipoDataSource.getAllEquipos()
                        equipoDao.replaceAll(updated.map { it.toEntity() })
                    }
                } catch (e: Exception) {}
            }
            result
        }
    }

    override suspend fun eliminarEquipo(equipoId: String, instructorId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val result = if (supabaseEquipoDataSource != null) {
                supabaseEquipoDataSource.deleteEquipo(equipoId, instructorId)
            } else {
                Result.success(Unit)
            }
            if (result.isSuccess) {
                equipoDao.deleteEquipoById(equipoId)
            }
            result
        }
    }

    override fun obtenerSolicitudesFlow(): Flow<List<SolicitudPrestamo>> {
        return localDataSource.getAllPrestamos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun obtenerSolicitudes(): List<SolicitudPrestamo> {
        return withContext(Dispatchers.IO) {
            try {
                if (supabasePrestamoDataSource != null) {
                    val prestamosSupabase = supabasePrestamoDataSource.getPrestamos()
                    Log.d("EquipoDebug", "OfflineFirstPrestamoRepository.obtenerSolicitudes: Supabase devolvió ${prestamosSupabase.size} registros")
                    if (prestamosSupabase.isNotEmpty()) {
                        localDataSource.replaceAll(prestamosSupabase.map { it.toEntity() })
                    }
                }
            } catch (e: Exception) {
                Log.e("EquipoDebug", "OfflineFirstPrestamoRepository.obtenerSolicitudes error fetching supabase: ${e.message}")
            }
            val localList = localDataSource.getAllPrestamos().first()
            Log.d("EquipoDebug", "OfflineFirstPrestamoRepository.obtenerSolicitudes: Room tiene ${localList.size} registros")
            localList.map { it.toDomain() }
        }
    }

    override suspend fun obtenerSolicitud(id: Int): SolicitudPrestamo? {
        return null
    }

    override fun crearSolicitud(solicitud: SolicitudPrestamo) {
        scope.launch {
            localDataSource.insertPrestamo(solicitud.toEntity())
        }
    }

    override fun actualizarEstadoSolicitud(id: Int, nuevoEstado: EstadoSolicitud, motivo: String?) {}

    override suspend fun solicitarPrestamo(equipo: Equipo, aprendizId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            if (equipo.estado != EstadoEquipo.DISPONIBLE) {
                return@withContext Result.failure(Exception("El equipo no está disponible para préstamo."))
            }
            val instructorId = equipo.instructorId ?: run {
                Log.e("EquipoDebug", "solicitarPrestamo: equipo ${equipo.id} no tiene instructor_id")
                return@withContext Result.failure(Exception("El equipo no tiene un instructor asignado."))
            }

            val dto = PrestamoDto(
                equipoId = equipo.id,
                equipoNombre = equipo.nombre,
                equipoPlaca = equipo.placa,
                equipoCategoria = equipo.categoria.name,
                instructorId = instructorId,
                aprendizId = aprendizId,
                estado = "solicitado"
            )

            val result = if (supabasePrestamoDataSource != null) {
                supabasePrestamoDataSource.createPrestamo(dto)
            } else {
                Result.success(Unit)
            }

            if (result.isSuccess) {
                refreshPrestamos()
            }
            result
        }
    }

    override suspend fun aprobarPrestamo(prestamoId: String, equipoId: String?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            Log.d("EquipoDebug", "Préstamo $prestamoId - Estado anterior: solicitado -> Nuevo estado: aprobado")
            val result = if (supabasePrestamoDataSource != null) {
                supabasePrestamoDataSource.updatePrestamoEstado(
                    prestamoId = prestamoId,
                    nuevoEstado = "aprobado",
                    timestampField = "fecha_aprobacion",
                    equipoIdToUpdateEstado = equipoId,
                    nuevoEstadoEquipo = "prestado"
                )
            } else {
                Result.success(Unit)
            }

            if (result.isSuccess) {
                refreshPrestamos()
                if (equipoId != null) {
                    equipoDao.updateEstadoEquipo(equipoId, EstadoEquipo.PRESTADO)
                }
            }
            result
        }
    }

    override suspend fun rechazarPrestamo(prestamoId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            Log.d("EquipoDebug", "Préstamo $prestamoId - Estado anterior: solicitado -> Nuevo estado: rechazado")
            val result = if (supabasePrestamoDataSource != null) {
                supabasePrestamoDataSource.updatePrestamoEstado(
                    prestamoId = prestamoId,
                    nuevoEstado = "rechazado",
                    timestampField = null,
                    equipoIdToUpdateEstado = null,
                    nuevoEstadoEquipo = null
                )
            } else {
                Result.success(Unit)
            }

            if (result.isSuccess) refreshPrestamos()
            result
        }
    }

    override suspend fun entregarPrestamo(prestamoId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            Log.d("EquipoDebug", "Préstamo $prestamoId - Estado anterior: aprobado -> Nuevo estado: entregado")
            val result = if (supabasePrestamoDataSource != null) {
                supabasePrestamoDataSource.updatePrestamoEstado(
                    prestamoId = prestamoId,
                    nuevoEstado = "entregado",
                    timestampField = "fecha_entrega",
                    equipoIdToUpdateEstado = null,
                    nuevoEstadoEquipo = null
                )
            } else {
                Result.success(Unit)
            }

            if (result.isSuccess) refreshPrestamos()
            result
        }
    }

    override suspend fun devolverPrestamo(prestamoId: String, equipoId: String?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            Log.d("EquipoDebug", "Préstamo $prestamoId - Estado anterior: entregado -> Nuevo estado: devuelto")
            val result = if (supabasePrestamoDataSource != null) {
                supabasePrestamoDataSource.updatePrestamoEstado(
                    prestamoId = prestamoId,
                    nuevoEstado = "devuelto",
                    timestampField = "fecha_devolucion",
                    equipoIdToUpdateEstado = equipoId,
                    nuevoEstadoEquipo = "disponible"
                )
            } else {
                Result.success(Unit)
            }

            if (result.isSuccess) {
                refreshPrestamos()
                if (equipoId != null) {
                    equipoDao.updateEstadoEquipo(equipoId, EstadoEquipo.DISPONIBLE)
                }
            }
            result
        }
    }

    override fun validarUsuario(identificador: String, contrasena: String): Usuario? {
        return usuariosIniciales.find { id -> id.id == identificador || id.correo == identificador }
    }

    override fun obtenerUsuario(id: String): Usuario? = usuariosIniciales.find { it.id == id }

    @Serializable
    data class PerfilNameDto(
        @SerialName("id") val id: String,
        @SerialName("nombre") val nombre: String
    )

    override suspend fun obtenerNombreUsuario(userId: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val localUser = usuariosIniciales.find { it.id == userId }
                if (localUser != null) {
                    Log.d("EquipoDebug", "obtenerNombreUsuario: encontrado en usuariosIniciales para userId=$userId -> ${localUser.nombre}")
                    return@withContext localUser.nombre
                }

                Log.d("EquipoDebug", "obtenerNombreUsuario: consultando perfil en Supabase para userId=$userId")
                if (supabaseClient != null) {
                    val perfiles = supabaseClient.postgrest.from("perfiles")
                        .select {
                            filter { eq("id", userId) }
                        }
                        .decodeList<PerfilNameDto>()
                    Log.d("EquipoDebug", "obtenerNombreUsuario: perfiles encontrados en Supabase=${perfiles.size}")
                    if (perfiles.isNotEmpty()) {
                        return@withContext perfiles.first().nombre
                    }
                }
            } catch (e: Exception) {
                Log.e("EquipoDebug", "obtenerNombreUsuario error for userId=$userId: ${e.message}", e)
            }
            Log.d("EquipoDebug", "obtenerNombreUsuario: fallback a take(8) para userId=$userId")
            userId.take(8)
        }
    }

    override suspend fun refreshPrestamos(): Result<Unit> {
        return try {
            if (supabaseEquipoDataSource != null) {
                val equiposSupabase = supabaseEquipoDataSource.getAllEquipos()
                if (equiposSupabase.isNotEmpty()) {
                    equipoDao.replaceAll(equiposSupabase.map { it.toEntity() })
                }
            }
            if (supabasePrestamoDataSource != null) {
                val prestamosSupabase = supabasePrestamoDataSource.getPrestamos()
                if (prestamosSupabase.isNotEmpty()) {
                    localDataSource.replaceAll(prestamosSupabase.map { it.toEntity() })
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("EquipoDebug", "refreshPrestamo error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun registrarNovedad(novedad: Novedad) {
        novedades.add(novedad)
        if (novedad.esGrave) {
            actualizarEstadoEquipo(novedad.equipoId, EstadoEquipo.MANTENIMIENTO)
        }
    }

    override fun obtenerNovedadesPorEquipo(equipoId: String): List<Novedad> =
        novedades.filter { it.equipoId == equipoId }

    // Semana 9: Evidencia fotográfica
    override suspend fun guardarEvidenciaLocal(inputStream: InputStream, fileName: String): Result<String> {
        return try {
            val evidenceDir = File(internalFilesDir, "evidencias").apply { if (!exists()) mkdirs() }
            val targetFile = File(evidenceDir, "${System.currentTimeMillis()}_$fileName")
            
            FileOutputStream(targetFile).use { output ->
                inputStream.copyTo(output)
            }
            Result.success(targetFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun vincularEvidenciaASolicitud(solicitudId: Int, uri: String, mimeType: String, tamano: Long) {}

    override suspend fun subirEvidenciaAlServidor(solicitudId: Int): Result<Unit> {
        return Result.success(Unit)
    }
}
