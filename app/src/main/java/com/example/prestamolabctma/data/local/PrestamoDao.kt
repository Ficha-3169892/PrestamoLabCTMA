package com.example.prestamolabctma.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PrestamoDao {
    @Query("SELECT * FROM prestamos ORDER BY id DESC")
    fun getAllPrestamos(): Flow<List<PrestamoEntity>>

    @Query("SELECT * FROM prestamos WHERE id = :id")
    suspend fun getPrestamoById(id: Int): PrestamoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrestamos(prestamos: List<PrestamoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrestamo(prestamo: PrestamoEntity)

    @Query("DELETE FROM prestamos")
    suspend fun deleteAllPrestamos()

    @Transaction
    suspend fun replaceAll(prestamos: List<PrestamoEntity>) {
        deleteAllPrestamos()
        insertPrestamos(prestamos)
    }
}
