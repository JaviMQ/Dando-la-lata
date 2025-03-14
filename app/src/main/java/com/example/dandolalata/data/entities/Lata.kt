package com.example.dandolalata.data.entities
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "latas",
    foreignKeys = [
        ForeignKey(
            entity = Marca::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("marcaId"),
            onDelete = ForeignKey.RESTRICT  // Esto elimina las latas cuando se elimina una marca
        )
    ],
    indices = [Index(value = ["marcaId"])]
)
data class Lata(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val marcaId: Int, // Clave foránea que referencia a la tabla Marcas
    val procedencia: String,
    val nombre: String,
    val foto: String
)

