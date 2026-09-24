package com.example.prestamolabctma.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.prestamolabctma.model.CategoriaEquipo
import com.example.prestamolabctma.model.EstadoEquipo

@Entity(tableName = "equipos")
data class EquipoEntity(
    @PrimaryKey val id: Int,
    val placa: String,
    val nombre: String,
    val categoria: CategoriaEquipo,
    val estado: EstadoEquipo,
    val ubicacion: String,
    val observaciones: String,
    val imagenUrl: String? = null
)
