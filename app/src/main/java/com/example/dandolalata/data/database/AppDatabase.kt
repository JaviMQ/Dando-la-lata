package com.example.dandolalata.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dandolalata.data.entities.Lata
import com.example.dandolalata.data.entities.Marca

@Database(entities = [Marca::class, Lata::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun marcaDao(): MarcaDao
    abstract fun lataDao(): LataDao

    // Instancia singleton de la BD
    companion object {
        // Variable para guardar la instancia de la BD
        @Volatile private var instancia: AppDatabase? = null


        fun obtenerInstancia(context: Context): AppDatabase {
            return instancia ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "db_latas")
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Aquí puedes agregar cualquier lógica que necesites cuando la base de datos se crea
                            // Ejemplo: insertar datos iniciales
                        }
                    })
                    .build()
                    .also { instancia = it }
            }
        }
    }
}
