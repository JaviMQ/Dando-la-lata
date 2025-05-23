package com.example.dandolalata.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.dandolalata.data.entities.Lata
import kotlinx.coroutines.flow.Flow

@Dao
interface LataDao {
    @Query("SELECT * FROM latas")
    fun obtenerTodas(): List<Lata>

    @Query("SELECT latas.* " +
            "FROM latas " +
            "INNER JOIN marcas ON marcas.id = latas.marcaId " +
            "ORDER BY marcas.nombre")
    fun obtenerTodasFlow(): Flow<List<Lata>>

    @Query("SELECT * FROM latas WHERE id = :id")
    fun obtenerPorId(id: Int): LiveData<Lata>

    @Query("SELECT * FROM latas WHERE id = :id")
    suspend fun obtenerPorIdDirecto(id: Int): Lata

    @Query("SELECT * FROM latas WHERE marcaId = :id")
    fun obtenerPorMarcaId(id: Int): List<Lata>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(lata: Lata)

    @Delete
    fun eliminar(lata: Lata)

    @Update
    suspend fun actualizar(lata: Lata)

}
