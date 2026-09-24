package com.example.prestamolabctma.data

import com.example.prestamolabctma.data.local.EquipoDao
import com.example.prestamolabctma.data.local.PrestamoDao
import com.example.prestamolabctma.data.mapper.toDomain
import com.example.prestamolabctma.data.mapper.toEntity
import com.example.prestamolabctma.data.remote.ApiException
import com.example.prestamolabctma.data.remote.NetworkException
import com.example.prestamolabctma.data.remote.PrestamoRemoteDataSource
import com.example.prestamolabctma.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class OfflineFirstPrestamoRepository(
    private val remoteDataSource: PrestamoRemoteDataSource,
    private val localDataSource: PrestamoDao,
    private val equipoDao: EquipoDao,
    private val internalFilesDir: File
) : PrestamoRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val usuariosIniciales = listOf(
        Usuario("123456", "Juan Perez", "juan@misena.edu.co", Role.APRENDIZ, "2558662"),
        Usuario("654321", "Maria Lopez", "maria@misena.edu.co", Role.INSTRUCTOR),
        Usuario("admin", "Admin Lab", "admin@ctma.edu.co", Role.ADMIN),
        Usuario("cuentadante", "Cuentadante 1", "c1@ctma.edu.co", Role.CUENTADANTE)
    )

    private val equiposIniciales = listOf(
        Equipo(1, "PL-001", "Osciloscopio Digital", CategoriaEquipo.ELECTRONICA, EstadoEquipo.DISPONIBLE, "Laboratorio 1", "Calibrado 2024"),
        Equipo(2, "PL-002", "Multímetro Fluke", CategoriaEquipo.ELECTRONICA, EstadoEquipo.DISPONIBLE, "Laboratorio 1"),
        Equipo(3, "PL-003", "Taladro Percutor", CategoriaEquipo.HERRAMIENTAS, EstadoEquipo.DISPONIBLE, "Taller Mecánica"),
        Equipo(4, "PL-004", "Laptop Dell Precision", CategoriaEquipo.COMPUTO, EstadoEquipo.MANTENIMIENTO, "Almacén"),
        Equipo(5, "PL-005", "Cámara Sony Alpha", CategoriaEquipo.AUDIO_VISUAL, EstadoEquipo.PRESTADO, "Audiovisuales")
    )

    private val novedades = mutableListOf<Novedad>()

    init {
        // Inicializar catálogo de equipos en Room si está vacío
        scope.launch {
            val existentes = equipoDao.getAllEquipos().first()
            if (existentes.isEmpty()) {
                equipoDao.insertEquipos(equiposIniciales.map { it.toEntity() })
            }
        }
    }

    override fun obtenerEquiposFlow(): Flow<List<Equipo>> {
        return equipoDao.getAllEquipos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun obtenerEquipos(): List<Equipo> {
        return withContext(Dispatchers.IO) {
            equipoDao.getAllEquipos().first().map { it.toDomain() }
        }
    }

    override suspend fun obtenerEquipo(id: Int): Equipo? {
        return withContext(Dispatchers.IO) {
            equipoDao.getEquipoById(id)?.toDomain()
        }
    }

    override fun actualizarEstadoEquipo(id: Int, nuevoEstado: EstadoEquipo) {
        scope.launch {
            equipoDao.updateEstadoEquipo(id, nuevoEstado)
        }
    }

    override fun obtenerSolicitudesFlow(): Flow<List<SolicitudPrestamo>> {
        return localDataSource.getAllPrestamos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun obtenerSolicitudes(): List<SolicitudPrestamo> {
        return withContext(Dispatchers.IO) {
            localDataSource.getAllPrestamos().first().map { it.toDomain() }
        }
    }

    override suspend fun obtenerSolicitud(id: Int): SolicitudPrestamo? {
        return withContext(Dispatchers.IO) {
            localDataSource.getPrestamoById(id)?.toDomain()
        }
    }

    override fun crearSolicitud(solicitud: SolicitudPrestamo) {
        scope.launch {
            localDataSource.insertPrestamo(solicitud.toEntity())
            if (solicitud.estado == EstadoSolicitud.APROBADA || solicitud.estado == EstadoSolicitud.SOLICITADA) {
                equipoDao.updateEstadoEquipo(solicitud.equipoId, EstadoEquipo.RESERVADO)
            }
        }
    }

    override fun actualizarEstadoSolicitud(id: Int, nuevoEstado: EstadoSolicitud, motivo: String?) {
        scope.launch {
            val entity = localDataSource.getPrestamoById(id) ?: return@launch
            val updated = entity.copy(estado = nuevoEstado, motivoRechazo = motivo)
            localDataSource.insertPrestamo(updated)

            when (nuevoEstado) {
                EstadoSolicitud.ENTREGADA -> equipoDao.updateEstadoEquipo(entity.equipoId, EstadoEquipo.PRESTADO)
                EstadoSolicitud.DEVUELTA, EstadoSolicitud.CANCELADA, EstadoSolicitud.RECHAZADA -> 
                    equipoDao.updateEstadoEquipo(entity.equipoId, EstadoEquipo.DISPONIBLE)
                EstadoSolicitud.EN_REVISION -> equipoDao.updateEstadoEquipo(entity.equipoId, EstadoEquipo.REPARACION)
                else -> {}
            }
        }
    }

    override fun validarUsuario(identificador: String, contrasena: String): Usuario? {
        return usuariosIniciales.find { it.id == identificador || it.correo == identificador }
    }

    override fun obtenerUsuario(id: String): Usuario? = usuariosIniciales.find { it.id == id }

    override suspend fun refreshPrestamos(): Result<Unit> {
        return try {
            val result = remoteDataSource.fetchPrestamos()
            result.fold(
                onSuccess = { dtos ->
                    localDataSource.replaceAll(dtos.map { it.toEntity() })
                    Result.success(Unit)
                },
                onFailure = { Result.failure(mapError(it)) }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun registrarNovedad(novedad: Novedad) {
        novedades.add(novedad)
        if (novedad.esGrave) {
            actualizarEstadoEquipo(novedad.equipoId, EstadoEquipo.MANTENIMIENTO)
        }
    }

    override fun obtenerNovedadesPorEquipo(equipoId: Int): List<Novedad> =
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

    override suspend fun vincularEvidenciaASolicitud(solicitudId: Int, uri: String, mimeType: String, tamano: Long) {
        val entity = localDataSource.getPrestamoById(solicitudId) ?: return
        val updated = entity.copy(
            evidenciaUri = uri,
            evidenciaMimeType = mimeType,
            evidenciaTamano = tamano,
            evidenciaSyncEstado = EvidenciaSyncEstado.LOCAL
        )
        localDataSource.insertPrestamo(updated)
    }

    override suspend fun subirEvidenciaAlServidor(solicitudId: Int): Result<Unit> {
        val entity = localDataSource.getPrestamoById(solicitudId) 
            ?: return Result.failure(Exception("Solicitud no encontrada"))
        
        val filePath = entity.evidenciaUri ?: return Result.failure(Exception("No hay archivo local"))
        val file = File(filePath)
        if (!file.exists()) return Result.failure(Exception("Archivo no encontrado en almacenamiento"))

        localDataSource.insertPrestamo(entity.copy(evidenciaSyncEstado = EvidenciaSyncEstado.SUBIENDO))

        val requestFile = file.asRequestBody(entity.evidenciaMimeType?.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("evidencia", file.name, requestFile)

        return try {
            val result = remoteDataSource.uploadEvidencia(solicitudId, body)
            result.fold(
                onSuccess = {
                    localDataSource.insertPrestamo(entity.copy(evidenciaSyncEstado = EvidenciaSyncEstado.SINCRONIZADA))
                    Result.success(Unit)
                },
                onFailure = {
                    localDataSource.insertPrestamo(entity.copy(evidenciaSyncEstado = EvidenciaSyncEstado.FALLIDA))
                    Result.failure(mapError(it))
                }
            )
        } catch (e: Exception) {
            localDataSource.insertPrestamo(entity.copy(evidenciaSyncEstado = EvidenciaSyncEstado.FALLIDA))
            Result.failure(e)
        }
    }

    private fun mapError(e: Throwable): Throwable {
        return when (e) {
            is ApiException -> {
                when (e.code) {
                    401 -> Exception("Sesión vencida.")
                    404 -> Exception("No encontrado.")
                    in 500..599 -> Exception("Error en servidor.")
                    else -> e
                }
            }
            is NetworkException -> Exception("Sin conexión.")
            else -> e
        }
    }
}
