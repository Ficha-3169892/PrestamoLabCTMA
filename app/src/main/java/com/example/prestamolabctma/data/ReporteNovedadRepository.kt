package com.example.prestamolabctma.data

import android.util.Log
import com.example.prestamolabctma.data.remote.ReporteFotoDto
import com.example.prestamolabctma.data.remote.ReporteNovedadDto
import com.example.prestamolabctma.data.remote.SupabaseReporteDataSource
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.model.ReporteNovedad
import com.example.prestamolabctma.model.SolicitudPrestamo

interface ReporteNovedadRepository {
    suspend fun crearReporte(
        prestamo: SolicitudPrestamo,
        equipo: Equipo,
        descripcion: String,
        fotos: List<Pair<String, ByteArray>>
    ): Result<Int>

    suspend fun obtenerReportesAprendiz(aprendizId: String): List<ReporteNovedad>
    suspend fun obtenerReportesInstructor(instructorId: String): List<ReporteNovedad>
    suspend fun resolverReporte(reporteId: String): Result<Unit>
    suspend fun solicitarDevolucion(reporteId: String): Result<Unit>
    suspend fun marcarRecibido(reporteId: String, prestamoId: String?, equipoId: String?): Result<Unit>
    suspend fun resolverReporteSiExiste(equipoId: String): Result<Unit>
    suspend fun tieneNovedadActiva(equipoId: String): Boolean
}

class ReporteNovedadRepositoryImpl(
    private val dataSource: SupabaseReporteDataSource
) : ReporteNovedadRepository {

    override suspend fun crearReporte(
        prestamo: SolicitudPrestamo,
        equipo: Equipo,
        descripcion: String,
        fotos: List<Pair<String, ByteArray>>
    ): Result<Int> {
        return try {
            val dto = ReporteNovedadDto(
                equipoId = equipo.id,
                equipoNombre = equipo.nombre,
                equipoPlaca = equipo.placa,
                prestamoId = prestamo.id,
                aprendizId = prestamo.aprendizId,
                instructorId = prestamo.instructorId,
                descripcion = descripcion.trim(),
                estado = "activo"
            )

            Log.d("EquipoDebug", "ReporteNovedadRepository.crearReporte: creando reporte para equipo ${equipo.id}")
            val createResult = dataSource.crearReporte(dto)
            if (createResult.isFailure) {
                return Result.failure(createResult.exceptionOrNull() ?: Exception("Error al crear reporte"))
            }

            val reporteId = createResult.getOrThrow()
            Log.d("EquipoDebug", "ReporteNovedadRepository: reporte creado id=$reporteId con ${fotos.size} fotos")

            var uploadedCount = 0
            for ((fileName, bytes) in fotos) {
                val uploadResult = dataSource.subirFoto(reporteId, bytes, fileName)
                if (uploadResult.isSuccess) {
                    val path = uploadResult.getOrThrow()
                    dataSource.insertarFotoRecord(
                        ReporteFotoDto(
                            reporteId = reporteId,
                            storagePath = path
                        )
                    )
                    uploadedCount++
                    Log.d("EquipoDebug", "ReporteNovedadRepository: foto $fileName subida con éxito")
                } else {
                    Log.e("EquipoDebug", "ReporteNovedadRepository: fallo al subir foto $fileName")
                }
            }

            Log.d("EquipoDebug", "ReporteNovedadRepository: subidas $uploadedCount de ${fotos.size} fotos")
            Result.success(uploadedCount)
        } catch (e: Exception) {
            Log.e("EquipoDebug", "ReporteNovedadRepository.crearReporte error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun obtenerReportesAprendiz(aprendizId: String): List<ReporteNovedad> {
        return dataSource.getReportesByAprendiz(aprendizId)
    }

    override suspend fun obtenerReportesInstructor(instructorId: String): List<ReporteNovedad> {
        return dataSource.getReportesByInstructor(instructorId)
    }

    override suspend fun resolverReporte(reporteId: String): Result<Unit> {
        Log.d("EquipoDebug", "ReporteNovedadRepository.resolverReporte id=$reporteId")
        return dataSource.resolverReporte(reporteId)
    }

    override suspend fun solicitarDevolucion(reporteId: String): Result<Unit> {
        Log.d("EquipoDebug", "ReporteNovedadRepository.solicitarDevolucion id=$reporteId")
        return dataSource.solicitarDevolucion(reporteId)
    }

    override suspend fun marcarRecibido(reporteId: String, prestamoId: String?, equipoId: String?): Result<Unit> {
        Log.d("EquipoDebug", "ReporteNovedadRepository.marcarRecibido id=$reporteId")
        return dataSource.marcarRecibido(reporteId, prestamoId, equipoId)
    }

    override suspend fun resolverReporteSiExiste(equipoId: String): Result<Unit> {
        Log.d("EquipoDebug", "ReporteNovedadRepository.resolverReporteSiExiste equipoId=$equipoId")
        return dataSource.resolverReporteSiExiste(equipoId)
    }

    override suspend fun tieneNovedadActiva(equipoId: String): Boolean {
        val activo = dataSource.tieneNovedadActiva(equipoId)
        Log.d("EquipoDebug", "ReporteNovedadRepository.tieneNovedadActiva equipoId=$equipoId -> $activo")
        return activo
    }
}
