package com.example.prestamolabctma.data.local

import androidx.room.*
import com.example.prestamolabctma.model.EstadoEquipo
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipoDao {
    @Query("SELECT * FROM equipos ORDER BY nombre ASC")
    fun getAllEquipos(): Flow<List<EquipoEntity>>

    @Query("SELECT * FROM equipos WHERE id = :id")
    suspend fun getEquipoById(id: String): EquipoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipos(equipos: List<EquipoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipo(equipo: EquipoEntity)

    @Query("UPDATE equipos SET estado = :nuevoEstado WHERE id = :id")
    suspend fun updateEstadoEquipo(id: String, nuevoEstado: EstadoEquipo)

    @Query("DELETE FROM equipos WHERE id = :id")
    suspend fun deleteEquipoById(id: String)

    @Query("DELETE FROM equipos")
    suspend fun deleteAllEquipos()

    @Transaction
    suspend fun replaceAll(equipos: List<EquipoEntity>) {
        deleteAllEquipos()
        insertEquipos(equipos)
    }
}
