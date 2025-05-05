package com.example.dandolalata

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.example.dandolalata.data.database.AppDatabase
import com.example.dandolalata.data.database.DatabaseConfig
import com.example.dandolalata.utils.ZipHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL



class GoogleDriveHelper (private val context: Context, private val token : String) {
    private val nombreZip : String = "imagenes.zip"

    // Revisado OK
    suspend fun exportarADrive(onProgress: ((Int) -> Unit)? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke(0)
            AppDatabase.cerrarBaseDeDatos()

            val pathDb = context.getDatabasePath(DatabaseConfig.DATABASE_NAME).path
            val dbFile = File(pathDb)
            val nombreDb = dbFile.name

            // 1. Buscar si ya existe el archivo en Drive y eliminarlo
            val fileIdExistente = buscarArchivoEnDrive(token, nombreDb)
            if (fileIdExistente != null) {
                eliminarArchivoDeDrive(token, fileIdExistente)
            }

            // 2. Subir base de datos
            onProgress?.invoke(10)
            val subidaDbExitosa = subirArchivoADrive(token, dbFile, "application/octet-stream")
            if(!subidaDbExitosa)
                throw Exception("Error exportando .db")
            onProgress?.invoke(20)

            // 3. Subir imágenes
            val zip = ZipHelper.comprimirImagenes(context, nombreZip)
            onProgress?.invoke(30)
            /*
            val tamañoEnBytes = zip?.length()
            val tamañoEnKB = tamañoEnBytes?.div(1024)
            val tamañoEnMB = tamañoEnKB?.div(1024)

            Toast.makeText(context, "Creado zip $tamañoEnMB MB", Toast.LENGTH_SHORT).show()
               */
            var subidaImagenesExitosa = false
            if (zip != null) {
                subidaImagenesExitosa = subirArchivoADrive(token, zip, "application/zip")
            }


            subidaImagenesExitosa
        } catch (e: Exception) {
            Toast.makeText(context, "Error en exportarADrive: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("DRIVE", "Error en exportarADrive: ${e.message}")
            false
        }
    }



    suspend fun importarDesdeDrive(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Descargar base de datos
            val nombreBd = DatabaseConfig.DATABASE_NAME
            val dbFileId = buscarArchivoEnDrive(token, nombreBd) ?: return@withContext false
            val dbDestino = File(context.getDatabasePath(DatabaseConfig.DATABASE_NAME).path)

            // Cerrar base de datos si está abierta
            AppDatabase.cerrarBaseDeDatos()

            if (!descargarArchivoDesdeDrive(token, dbFileId, dbDestino))
                throw Exception("Error descargando .DB desde Drive")


                // 2. Descargar imágenes
            val zipFileId = buscarArchivoEnDrive(token, nombreZip) ?: return@withContext false
            val zipFile = File(context.cacheDir, nombreZip)
            if(!descargarArchivoDesdeDrive(token, zipFileId, zipFile))
                throw Exception("Error descargando fotos desde Drive")
            ZipHelper.descomprimirImagenes(context, zipFile)


            true
        } catch (e: Exception) {
            Toast.makeText(context, "Error en importarDesdeDrive: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("DRIVE", "Error en importarDesdeDrive: ${e.message}")
            false
        }
    }



    private suspend fun buscarArchivoEnDrive(accessToken: String, nombreArchivo: String): String? = withContext(Dispatchers.IO) {
        val url = URL("https://www.googleapis.com/drive/v3/files?q=name='$nombreArchivo' and trashed=false&fields=files(id,name)")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")

        return@withContext try {
            val respuesta = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(respuesta)
            val archivos = json.getJSONArray("files")
            if (archivos.length() > 0) archivos.getJSONObject(0).getString("id") else null
        } catch (e: Exception) {
            Log.e("DRIVE", "Error al buscar archivo: ${e.message}")
            null
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun eliminarArchivoDeDrive(accessToken: String, fileId: String): Boolean = withContext(Dispatchers.IO) {
        val url = URL("https://www.googleapis.com/drive/v3/files/$fileId")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "DELETE"
        conn.setRequestProperty("Authorization", "Bearer $accessToken")

        return@withContext try {
            conn.responseCode == 204
        } catch (e: Exception) {
            Log.e("DRIVE", "Error al eliminar archivo: ${e.message}")
            false
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun subirArchivoADrive(accessToken: String, archivo: File, mimeType: String): Boolean = withContext(Dispatchers.IO) {
        val boundary = "-------${System.currentTimeMillis()}"
        val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")

        try {
            val output = BufferedOutputStream(connection.outputStream)
            val writer = OutputStreamWriter(output)

            // Parte 1: metadatos
            writer.write("--$boundary\r\n")
            writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            writer.write("""
            {
              "name": "${archivo.name}"
            }
        """.trimIndent())
            writer.write("\r\n")

            // Parte 2: archivo ZIP (streamed)
            writer.write("--$boundary\r\n")
            writer.write("Content-Type: $mimeType\r\n\r\n")
            writer.flush() // importante antes de copiar el archivo

            // Aquí evitamos cargar el archivo completo en memoria
            archivo.inputStream().use { it.copyTo(output) }

            // Parte final
            writer.write("\r\n--$boundary--\r\n")
            writer.flush()
            output.flush()

            val success = connection.responseCode in 200..299
            if (!success) {
                Log.e("DRIVE", "Respuesta HTTP: ${connection.responseCode}, ${connection.errorStream?.bufferedReader()?.readText()}")
            }
            return@withContext success
        } catch (e: Exception) {
            Log.e("DRIVE", "Error al subir archivo: ${e.message}", e)
            return@withContext false
        } finally {
            connection.disconnect()
        }
    }


    private suspend fun descargarArchivoDesdeDrive(accessToken: String, fileId: String, destino: File): Boolean = withContext(Dispatchers.IO) {
        val url = URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")

        return@withContext try {
            destino.outputStream().use { output ->
                conn.inputStream.copyTo(output)
            }
            true
        } catch (e: Exception) {
            Log.e("DRIVE", "Error al descargar archivo: ${e.message}")
            false
        } finally {
            conn.disconnect()
        }
    }



}