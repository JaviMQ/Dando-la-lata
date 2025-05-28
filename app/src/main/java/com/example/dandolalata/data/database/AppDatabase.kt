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
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DatabaseConfig.DATABASE_NAME)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Aquí puedes agregar cualquier lógica que necesites cuando la base de datos se crea
                            // Ejemplo: insertar datos iniciales
                        }
/*
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)

                            // Vaciar la base de datos cuando se abra
                            CoroutineScope(Dispatchers.IO).launch {
                                instancia?.clearAllTables()
                            }
                        }

 */
                    })
                    .setJournalMode(JournalMode.TRUNCATE) // Con esto la BD se guarda en un unico fichero
                    .build()
                    .also { instancia = it }
            }
        }

        fun cerrarBaseDeDatos() {
            instancia?.close()
            instancia = null
        }
    }
}
