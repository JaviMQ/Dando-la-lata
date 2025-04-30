package com.example.dandolalata.data.database

import androidx.room.*
import com.example.dandolalata.data.entities.Marca
import kotlinx.coroutines.flow.Flow

@Dao
interface MarcaDao {
    @Query("SELECT * FROM marcas")
    suspend fun obtenerTodas(): List<Marca>

    @Query("SELECT * FROM marcas ORDER BY nombre")
    suspend fun obtenerTodasPorNombre(): List<Marca>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertar(marca: Marca): Long

    @Delete
    fun eliminar(marca: Marca)

    @Query("SELECT * FROM marcas ORDER BY nombre")
    fun obtenerTodasFlowPornombre(): Flow<List<Marca>>
}
