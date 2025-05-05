package com.example.dandolalata.data.database

import androidx.room.*
import com.example.dandolalata.data.entities.Marca
import kotlinx.coroutines.flow.Flow

@Dao
interface MarcaDao {

    @Query("SELECT * FROM marcas ORDER BY nombre")
    suspend fun obtenerTodasPorNombre(): List<Marca>

    @Query("SELECT marcas.id, marcas.nombre || ' (' ||  COUNT(latas.id) || ')' as nombre " +
            "FROM marcas " +
            "LEFT JOIN latas ON latas.marcaId = marcas.id " +
            "GROUP BY marcas.id, marcas.nombre " +
            "ORDER BY nombre")
    fun obtenerTodasFlowPorNombreConTotales(): Flow<List<Marca>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertar(marca: Marca): Long

    @Delete
    fun eliminar(marca: Marca)

    @Query("SELECT * FROM marcas ORDER BY nombre")
    fun obtenerTodasFlowPornombre(): Flow<List<Marca>>
}
