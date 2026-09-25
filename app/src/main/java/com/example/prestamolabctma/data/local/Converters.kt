package com.example.prestamolabctma.data.local

import androidx.room.TypeConverter
import com.example.prestamolabctma.model.CategoriaEquipo
import com.example.prestamolabctma.model.EstadoEquipo
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.model.EvidenciaSyncEstado

class Converters {

    @TypeConverter
    fun fromEstadoSolicitud(value: EstadoSolicitud?): String? = value?.name

    @TypeConverter
    fun toEstadoSolicitud(value: String?): EstadoSolicitud? =
        value?.let { runCatching { EstadoSolicitud.valueOf(it) }.getOrDefault(EstadoSolicitud.SOLICITADA) }

    @TypeConverter
    fun fromEvidenciaSyncEstado(value: EvidenciaSyncEstado?): String? = value?.name

    @TypeConverter
    fun toEvidenciaSyncEstado(value: String?): EvidenciaSyncEstado? =
        value?.let { runCatching { EvidenciaSyncEstado.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromCategoriaEquipo(value: CategoriaEquipo?): String? = value?.name

    @TypeConverter
    fun toCategoriaEquipo(value: String?): CategoriaEquipo? =
        value?.let { runCatching { CategoriaEquipo.valueOf(it) }.getOrDefault(CategoriaEquipo.ELECTRONICA) }

    @TypeConverter
    fun fromEstadoEquipo(value: EstadoEquipo?): String? = value?.name

    @TypeConverter
    fun toEstadoEquipo(value: String?): EstadoEquipo? =
        value?.let { runCatching { EstadoEquipo.valueOf(it) }.getOrDefault(EstadoEquipo.DISPONIBLE) }
}
