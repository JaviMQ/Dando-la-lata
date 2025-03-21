package com.example.dandolalata.data.database

import androidx.room.*
import com.example.dandolalata.data.entities.Marca

@Dao
interface MarcaDao {
    @Query("SELECT * FROM marcas")
    suspend fun obtenerTodas(): List<Marca>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertar(marca: Marca): Long

    @Delete
    fun eliminar(marca: Marca)
}
