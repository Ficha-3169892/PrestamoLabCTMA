package com.example.prestamolabctma.data.remote

import android.util.Log
import com.example.prestamolabctma.model.ReporteFoto
import com.example.prestamolabctma.model.ReporteNovedad
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.hours

class SupabaseReporteDataSource(
    private val supabaseClient: SupabaseClient
) {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    private fun parseDateTimeSafely(dateStr: String?): LocalDateTime? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(dateStr).toLocalDateTime()
        } catch (e1: Exception) {
            try {
                ZonedDateTime.parse(dateStr).toLocalDateTime()
            } catch (e2: Exception) {
                try {
                    LocalDateTime.parse(dateStr, formatter)
                } catch (e3: Exception) {
                    try {
                        LocalDateTime.parse(dateStr)
                    } catch (e4: Exception) {
                        null
                    }
                }
            }
        }
    }

    suspend fun crearReporte(dto: ReporteNovedadDto): Result<String> {
        return try {
            Log.d("EquipoDebug", "SupabaseReporteDataSource.crearReporte: creando reporte dto=$dto")
            val response = supabaseClient.postgrest.from("reportes_novedad")
                .insert(dto) {
                    select()
                }
                .decodeSingle<ReporteNovedadDto>()
            
            val reporteId = response.id ?: throw Exception("No se generó ID para el reporte")
            Log.d("EquipoDebug", "SupabaseReporteDataSource.crearReporte: creado con id=$reporteId")
            Result.success(reporteId)
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseReporteDataSource.crearReporte error: ${e.message}", e)
            Result.failure(Exception("Error al crear reporte de novedad: ${e.localizedMessage}", e))
        }
    }

    suspend fun subirFoto(reporteId: String, bytes: ByteArray, fileName: String): Result<String> {
        val path = "$reporteId/$fileName"
        return try {
            Log.d("EquipoDebug", "SupabaseReporteDataSource.subirFoto: subiendo a bucket 'reportes-fotos' path=$path")
            supabaseClient.storage.from("reportes-fotos").upload(path, bytes) {
                upsert = true
            }
            Log.d("EquipoDebug", "SupabaseReporteDataSource.subirFoto: subida exitosa path=$path")
            Result.success(path)
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseReporteDataSource.subirFoto error path=$path: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun insertarFotoRecord(dto: ReporteFotoDto): Result<Unit> {
        return try {
            supabaseClient.postgrest.from("reporte_fotos").insert(dto)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseReporteDataSource.insertarFotoRecord error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getSignedUrl(storagePath: String): String? {
        return try {
            val url = supabaseClient.storage.from("reportes-fotos").createSignedUrl(storagePath, 1.hours)
            Log.d("EquipoDebug", "SupabaseReporteDataSource.getSignedUrl: generado para path=$storagePath")
            url
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseReporteDataSource.getSignedUrl error for path=$storagePath: ${e.message}", e)
            null
        }
    }

    suspend fun getReportesByAprendiz(aprendizId: String): List<ReporteNovedad> {
        return getReportes { eq("aprendiz_id", aprendizId) }
    }

    suspend fun getReportesByInstructor(instructorId: String): List<ReporteNovedad> {
        val all = getReportes { eq("instructor_id", instructorId) }
        val filtered = all.filter { it.estado == "activo" && it.fechaRecepcion == null }
        Log.d("EquipoDebug", "SupabaseReporteDataSource.getReportesByInstructor: total=${all.size}, activos sin recepcion=${filtered.size}")
        return filtered
    }

    suspend fun tieneNovedadActiva(equipoId: String): Boolean {
        return try {
            val list = supabaseClient.postgrest.from("reportes_novedad")
                .select {
                    filter {
                        eq("equipo_id", equipoId)
                        eq("estado", "activo")
                    }
                }
                .decodeList<ReporteNovedadDto>()
            val hasActive = list.isNotEmpty()
            Log.d("EquipoDebug", "SupabaseReporteDataSource.tieneNovedadActiva equipoId=$equipoId -> $hasActive")
            hasActive
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseReporteDataSource.tieneNovedadActiva error: ${e.message}", e)
            false
        }
    }

    suspend fun solicitarDevolucion(reporteId: String): Result<Unit> {
        return try {
            val nowStr = LocalDateTime.now().format(formatter)
            Log.d("EquipoDebug", "SupabaseReporteDataSource.solicitarDevolucion id=$reporteId, time=$nowStr")
            supabaseClient.postgrest.from("reportes_novedad").update(
                buildJsonObject {
                    put("devolucion_solicitada", true)
                    put("devolucion_solicitada_en", nowStr)
                }
            ) {
                filter { eq("id", reporteId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseReporteDataSource.solicitarDevolucion error: ${e.message}", e)
            Result.failure(Exception("Error al solicitar devolución: ${e.localizedMessage}", e))
        }
    }

    suspend fun marcarRecibido(reporteId: String, prestamoId: String?, equipoId: String?): Result<Unit> {
        return try {
            val nowStr = LocalDateTime.now().format(formatter)
            Log.d("EquipoDebug", "SupabaseReporteDataSource.marcarRecibido reporteId=$reporteId, prestamoId=$prestamoId, equipoId=$equipoId, time=$nowStr")
            
            if (!prestamoId.isNullOrBlank()) {
                supabaseClient.postgrest.from("prestamos").update(
                    buildJsonObject {
                        put("estado", "devuelto")
                        put("fecha_devolucion", nowStr)
                    }
                ) {
                    filter { eq("id", prestamoId) }
                }
            }

            if (!equipoId.isNullOrBlank()) {
                supabaseClient.postgrest.from("equipos").update(
                    buildJsonObject {
                        put("estado", "mantenimiento")
                    }
                ) {
                    filter { eq("id", equipoId) }
                }
            }

            supabaseClient.postgrest.from("reportes_novedad").update(
                buildJsonObject {
                    put("fecha_recepcion", nowStr)
                }
            ) {
                filter { eq("id", reporteId) }
            }

            Log.d("EquipoDebug", "SupabaseReporteDataSource.marcarRecibido: éxito, fecha_recepcion actualizada a $nowStr")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseReporteDataSource.marcarRecibido error: ${e.message}", e)
            Result.failure(Exception("Error al marcar recibido: ${e.localizedMessage}", e))
        }
    }

    suspend fun resolverReporteSiExiste(equipoId: String): Result<Unit> {
        Log.d("EquipoDebug", "SupabaseReporteDataSource.resolverReporteSiExiste ENTER: equipoId=$equipoId")
        return try {
            val nowStr = LocalDateTime.now().format(formatter)
            val list = supabaseClient.postgrest.from("reportes_novedad")
                .select {
                    filter {
                        eq("equipo_id", equipoId)
                        eq("estado", "activo")
                    }
                }
                .decodeList<ReporteNovedadDto>()

            Log.d("EquipoDebug", "SupabaseReporteDataSource.resolverReporteSiExiste: encontrados ${list.size} reportes activos para equipoId=$equipoId")
            val activeWithReception = list.find { !it.fechaRecepcion.isNullOrBlank() }
            if (activeWithReception != null && activeWithReception.id != null) {
                Log.d("EquipoDebug", "SupabaseReporteDataSource.resolverReporteSiExiste: cerrando reporte id=${activeWithReception.id}")
                supabaseClient.postgrest.from("reportes_novedad").update(
                    buildJsonObject {
                        put("estado", "resuelto")
                        put("fecha_resolucion", nowStr)
                    }
                ) {
                    filter { eq("id", activeWithReception.id) }
                }
                Log.d("EquipoDebug", "SupabaseReporteDataSource.resolverReporteSiExiste: UPDATE exitoso para reporte id=${activeWithReception.id}")
            } else {
                Log.d("EquipoDebug", "SupabaseReporteDataSource.resolverReporteSiExiste: ningún reporte activo con fecha_recepcion no nula para equipoId=$equipoId")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseReporteDataSource.resolverReporteSiExiste error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun resolverReporte(reporteId: String): Result<Unit> {
        return try {
            val nowStr = LocalDateTime.now().format(formatter)
            Log.d("EquipoDebug", "SupabaseReporteDataSource.resolverReporte id=$reporteId")
            supabaseClient.postgrest.from("reportes_novedad").update(
                buildJsonObject {
                    put("estado", "resuelto")
                    put("fecha_resolucion", nowStr)
                }
            ) {
                filter {
                    eq("id", reporteId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseReporteDataSource.resolverReporte error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun getReportes(filterBuilder: PostgrestFilterBuilder.() -> Unit): List<ReporteNovedad> {
        return try {
            val reportesDto = supabaseClient.postgrest.from("reportes_novedad")
                .select { filter(filterBuilder) }
                .decodeList<ReporteNovedadDto>()

            reportesDto.map<ReporteNovedadDto, ReporteNovedad> { rDto ->
                val rId = rDto.id ?: ""
                val fotosDto = try {
                    supabaseClient.postgrest.from("reporte_fotos")
                        .select { filter { eq("reporte_id", rId) } }
                        .decodeList<ReporteFotoDto>()
                } catch (e: Exception) {
                    emptyList<ReporteFotoDto>()
                }

                val fotos = fotosDto.map<ReporteFotoDto, ReporteFoto> { fDto ->
                    val signed = getSignedUrl(fDto.storagePath)
                    ReporteFoto(
                        id = fDto.id ?: "",
                        reporteId = rId,
                        storagePath = fDto.storagePath,
                        signedUrl = signed
                    )
                }

                ReporteNovedad(
                    id = rId,
                    equipoId = rDto.equipoId,
                    equipoNombre = rDto.equipoNombre,
                    equipoPlaca = rDto.equipoPlaca,
                    prestamoId = rDto.prestamoId,
                    aprendizId = rDto.aprendizId,
                    instructorId = rDto.instructorId,
                    descripcion = rDto.descripcion,
                    estado = rDto.estado,
                    devolucionSolicitada = rDto.devolucionSolicitada,
                    devolucionSolicitadaEn = parseDateTimeSafely(rDto.devolucionSolicitadaEn),
                    fechaRecepcion = parseDateTimeSafely(rDto.fechaRecepcion),
                    fechaReporte = parseDateTimeSafely(rDto.fechaReporte),
                    fechaResolucion = parseDateTimeSafely(rDto.fechaResolucion),
                    fotos = fotos
                )
            }
        } catch (e: Exception) {
            Log.e("EquipoDebug", "SupabaseReporteDataSource.getReportes error: ${e.message}", e)
            emptyList<ReporteNovedad>()
        }
    }
}
