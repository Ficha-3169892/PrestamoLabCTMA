package com.example.prestamolabctma.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PrestamoEntity::class, EquipoEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prestamoDao(): PrestamoDao
    abstract fun equipoDao(): EquipoDao
}
