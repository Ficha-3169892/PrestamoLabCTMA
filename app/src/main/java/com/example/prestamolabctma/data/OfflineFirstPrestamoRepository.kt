package com.example.prestamolabctma.data

import com.example.prestamolabctma.data.local.PrestamoDao
import com.example.prestamolabctma.data.mapper.toDomain
import com.example.prestamolabctma.data.mapper.toEntity
import com.example.prestamolabctma.data.remote.ApiException
import com.example.prestamolabctma.data.remote.NetworkException
import com.example.prestamolabctma.data.remote.PrestamoDto
import com.example.prestamolabctma.data.remote.PrestamoRemoteDataSource
import com.example.prestamolabctma.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class OfflineFirstPrestamoRepository(
    private val remoteDataSource: PrestamoRemoteDataSource,
    private val localDataSource: PrestamoDao,
    private val internalFilesDir: File // Directorio interno de la app (context.filesDir)
) : PrestamoRepository {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun obtenerSolicitudesFlow(): Flow<List<SolicitudPrestamo>> {
        return localDataSource.getAllPrestamos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

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

    override fun obtenerSolicitudes(): List<SolicitudPrestamo> = emptyList()
    override fun obtenerSolicitud(id: Int): SolicitudPrestamo? = null
    override fun crearSolicitud(solicitud: SolicitudPrestamo) {}
    override fun obtenerEquipos(): List<Equipo> = emptyList()
    override fun obtenerEquipo(id: Int): Equipo? = null
    override fun actualizarEstadoEquipo(id: Int, nuevoEstado: EstadoEquipo) {}
    override fun validarUsuario(identificador: String, contrasena: String): Usuario? = null
    override fun obtenerUsuario(id: String): Usuario? = null
    override fun actualizarEstadoSolicitud(id: Int, nuevoEstado: EstadoSolicitud, motivo: String?) {}
    override fun registrarNovedad(novedad: Novedad) {}
    override fun obtenerNovedadesPorEquipo(equipoId: Int): List<Novedad> = emptyList()

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
