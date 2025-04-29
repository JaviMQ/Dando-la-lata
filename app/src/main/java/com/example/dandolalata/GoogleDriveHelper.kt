package com.example.dandolalata

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.dandolalata.data.database.AppDatabase
import com.example.dandolalata.data.database.DatabaseConfig
import com.example.dandolalata.utils.ZipHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL



class GoogleDriveHelper (private val context: Context, private val token : String) {
    private val nombreZip : String = "imagenes.zip"

    // Revisado OK
    suspend fun exportarADrive(): Boolean = withContext(Dispatchers.Main) {
        try {
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
            val subidaDbExitosa = subirArchivoADrive(token, dbFile, "application/octet-stream")
            if(!subidaDbExitosa)
                throw Exception("Error exportando .db")


            // 3. Subir imágenes
            val zip = ZipHelper.comprimirImagenes(context, nombreZip)
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

    suspend fun importarDesdeDrive(): Boolean = withContext(Dispatchers.Main) {
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

    private suspend fun subirArchivoADrive(accessToken: String, archivo: File, mimeType : String): Boolean = withContext(Dispatchers.IO) {
        val boundary = "-------${System.currentTimeMillis()}"
        val metadata = """
        {
            "name": "${archivo.name}"
        }
    """.trimIndent()

        val bodyStream = ByteArrayOutputStream().apply {
            val writer = OutputStreamWriter(this)
            writer.write("--$boundary\r\n")
            writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            writer.write(metadata)
            writer.write("\r\n--$boundary\r\n")
            writer.write("Content-Type: $mimeType\r\n\r\n")
            writer.flush()
            write(archivo.readBytes())
            writer.write("\r\n--$boundary--\r\n")
            writer.flush()
        }

        val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")

        return@withContext try {
            conn.outputStream.write(bodyStream.toByteArray())
            conn.outputStream.flush()
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.e("DRIVE", "Error al subir archivo: ${e.message}")
            false
        } finally {
            conn.disconnect()
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