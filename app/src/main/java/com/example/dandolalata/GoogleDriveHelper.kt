package com.example.dandolalata

import android.content.Context
import android.util.Log
import com.example.dandolalata.data.database.AppDatabase
import com.example.dandolalata.data.database.DatabaseConfig
import com.example.dandolalata.utils.AppPaths
import com.example.dandolalata.utils.ZipHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class GoogleDriveHelper (private val context: Context, private val token : String) {
    private val nombreZip : String = "imagenes.zip"

    // Revisado OK
    suspend fun exportarADrive(): Boolean = withContext(Dispatchers.Main) {
        try {
            val dbFile = File(context.getDatabasePath(DatabaseConfig.DATABASE_NAME).path)
            val nombreDb = dbFile.name

            // 1. Buscar si ya existe el archivo en Drive y eliminarlo
            val fileIdExistente = buscarArchivoEnDrive(token, nombreDb)
            if (fileIdExistente != null) {
                eliminarArchivoDeDrive(token, fileIdExistente)
            }

            // 2. Subir base de datos
            val subidaDbExitosa = subirArchivoADrive(token, dbFile, "application/octet-stream")


            // 3. Subir imágenes
            val zip = ZipHelper.comprimirImagenes(context, nombreZip)
            var subidaImagenesExitosa = false
            if (zip != null) {
                subidaImagenesExitosa = subirArchivoADrive(token, zip, "application/zip")
            }

            subidaDbExitosa && subidaImagenesExitosa
        } catch (e: Exception) {
            Log.e("DRIVE", "Error en exportarADrive: ${e.message}")
            false
        }
    }

    suspend fun importarDesdeDrive(): Boolean = withContext(Dispatchers.Main) {
        try {
            // 1. Descargar base de datos
            val dbFileId = buscarArchivoEnDrive(token, DatabaseConfig.DATABASE_NAME) ?: return@withContext false
            val dbDestino = File(context.getDatabasePath(DatabaseConfig.DATABASE_NAME).path)

            // Cerrar base de datos si está abierta
            AppDatabase.cerrarBaseDeDatos()

            if (!descargarArchivoDesdeDrive(token, dbFileId, dbDestino))
                return@withContext false

            // 2. Descargar imágenes
            val zipFileId = buscarArchivoEnDrive(token, nombreZip) ?: return@withContext false
            val zipFile = File(context.cacheDir, nombreZip)
            if(!descargarArchivoDesdeDrive(token, zipFileId, zipFile))
                return@withContext false

            ZipHelper.descomprimirImagenes(context, zipFile)

        } catch (e: Exception) {
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

    data class ArchivoDrive(val id: String, val name: String)

    private suspend fun listarArchivosDrive(accessToken: String): List<ArchivoDrive> = withContext(Dispatchers.IO) {
        val url = URL("https://www.googleapis.com/drive/v3/files?fields=files(id,name)&pageSize=1000")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")

        return@withContext try {
            val respuesta = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(respuesta)
            val archivos = json.getJSONArray("files")
            List(archivos.length()) {
                val obj = archivos.getJSONObject(it)
                ArchivoDrive(obj.getString("id"), obj.getString("name"))
            }
        } catch (e: Exception) {
            Log.e("DRIVE", "Error al listar archivos: ${e.message}")
            emptyList()
        } finally {
            conn.disconnect()
        }
    }


    /*
    fun subirArchivoADrive(accessToken: String) {

        val dbFile = File(context.getDatabasePath(DatabaseConfig.DATABASE_NAME).path)
        val mimeType = "application/octet-stream"

        val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
        val boundary = "-------${System.currentTimeMillis()}"

        val metadata = """
        {
          "name": "${dbFile.name}"
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
            write(dbFile.readBytes())
            writer.write("\r\n--$boundary--\r\n")
            writer.flush()
        }

        Thread {
            try {
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")

                    outputStream.write(bodyStream.toByteArray())
                    outputStream.flush()

                    val response = inputStream.bufferedReader().readText()
                    Log.d("DRIVE", "Subida exitosa: $response")
                }
            } catch (e: Exception) {
                Log.e("DRIVE", "Error al subir archivo: ${e.message}")
            }
        }.start()
    }

    fun restaurarBaseDeDatosDesdeDrive(accessToken: String, fileId: String) {
        Thread {
            try {
                // 1. URL de descarga del archivo
                val url = URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $accessToken")

                if (connection.responseCode == 200) {
                    // 2. Ruta del archivo de la base de datos local
                    val dbPath = context.getDatabasePath(DatabaseConfig.DATABASE_NAME)
                    val dbFile = File(dbPath.absolutePath)

                    // 3. Cerrar Room antes de sobrescribir
                    AppDatabase.cerrarBaseDeDatos()

                    // 4. Sobrescribir el archivo .db
                    val input = connection.inputStream
                    val output = FileOutputStream(dbFile)
                    input.copyTo(output)

                    input.close()
                    output.close()

                    Log.d("DRIVE", "Base de datos restaurada con éxito.")
                } else {
                    Log.e("DRIVE", "Error de descarga: código ${connection.responseCode}")
                }

            } catch (e: Exception) {
                Log.e("DRIVE", "Error al restaurar la base de datos: ${e.message}")
            }
        }.start()
    }

    fun manejarArchivoEnDrive(accessToken: String, nombreArchivo: String, dbFile: File, onResult: (Boolean) -> Unit) {
        Thread {
            try {
                val encodedQuery = URLEncoder.encode("name = '$nombreArchivo'", "UTF-8")
                val url = URL("https://www.googleapis.com/drive/v3/files?q=$encodedQuery&spaces=drive&fields=files(id,name)")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $accessToken")

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().readText()

                if (responseCode == 200) {
                    val json = JSONObject(response)
                    val files = json.getJSONArray("files")

                    if (files.length() > 0) {
                        // Si el archivo existe, obtenemos el fileId y lo actualizamos
                        val file = files.getJSONObject(0)
                        val fileId = file.getString("id")
                        actualizarArchivoEnDrive(accessToken, fileId, dbFile, onResult)
                    } else {
                        // Si el archivo no existe, lo subimos
                        crearArchivoEnDrive(accessToken, dbFile, onResult)
                    }
                } else {
                    Log.e("DRIVE", "Error al buscar archivo. Código: $responseCode")
                    onResult(false)
                }

            } catch (e: Exception) {
                Log.e("DRIVE", "Error al manejar archivo: ${e.message}")
                onResult(false)
            }
        }.start()
    }

     */
}