package com.example.dandolalata

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.dandolalata.data.database.AppDatabase
import com.example.dandolalata.data.database.DatabaseConfig
import com.example.dandolalata.utils.AppPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicInteger


class GoogleDriveHelper (private val context: Context, private val token : String) {
    private val nombreCarpetaDrive : String = "FotosLatas"

    // Revisado OK
    suspend fun exportarADrive(onProgress: ((Int) -> Unit)? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke(0)

            AppDatabase.cerrarBaseDeDatos()

            val pathDb = context.getDatabasePath(DatabaseConfig.DATABASE_NAME).path
            val dbFile = File(pathDb)
            val nombreDb = dbFile.name

            // 1. Buscar si ya existe el archivo en Drive y eliminarlo
            val fileIdExistente = buscarEnDrive(token, nombreDb, false)
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
            val subidaImagenesExitosa = subirImagenesADrive(token, 20, onProgress)

            subidaImagenesExitosa
        } catch (e: Exception) {
            Toast.makeText(context, "Error en exportarADrive: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("DRIVE", "Error en exportarADrive: ${e.message}")
            false
        }
    }



    suspend fun importarDesdeDrive(onProgress: ((Int) -> Unit)? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Descargar base de datos
            onProgress?.invoke(0)
            val nombreBd = DatabaseConfig.DATABASE_NAME
            val dbFileId = buscarEnDrive(token, nombreBd, false) ?: return@withContext false
            val dbDestino = File(context.getDatabasePath(DatabaseConfig.DATABASE_NAME).path)

            // Cerrar base de datos si está abierta
            AppDatabase.cerrarBaseDeDatos()
            onProgress?.invoke(10)

            if (!descargarArchivoDesdeDrive(token, dbFileId, dbDestino))
                throw Exception("Error descargando .DB desde Drive")
            onProgress?.invoke(20)

            // 2. Descargar imágenes
            val carpetaDriveId = buscarEnDrive(token, nombreCarpetaDrive, true) ?: return@withContext false
            val archivos = listarArchivosEnCarpeta(token, carpetaDriveId)
            for ((id, nombre) in archivos) {
                val destino = File(AppPaths.IMAGENES_DIR, nombre)
                val ok = descargarArchivoDesdeDrive(token, id, destino)
                if (ok) {
                    Log.d("DRIVE", "Descargado: $nombre")
                }
            }

            onProgress?.invoke(100)

            true
        } catch (e: Exception) {
            Toast.makeText(context, "Error en importarDesdeDrive: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("DRIVE", "Error en importarDesdeDrive: ${e.message}")
            false
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

    private suspend fun subirArchivoADrive(
        accessToken: String,
        archivo: File,
        mimeType: String,
        folderId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
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

            val metadata = if (folderId != null) {
                """
            {
              "name": "${archivo.name}",
              "parents": ["$folderId"]
            }
            """.trimIndent()
            } else {
                """
            {
              "name": "${archivo.name}"
            }
            """.trimIndent()
            }

            writer.write(metadata)
            writer.write("\r\n")

            // Parte 2: archivo ZIP (streamed)
            writer.write("--$boundary\r\n")
            writer.write("Content-Type: $mimeType\r\n\r\n")
            writer.flush()

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

    private suspend fun subirImagenesADrive(
        accessToken: String,
        porcentajeBaseBarra: Int,
        onProgress: ((Int) -> Unit)? = null
    ): Boolean = coroutineScope {
        var idCarpetaDrive = buscarEnDrive(accessToken, nombreCarpetaDrive, true)
        if (idCarpetaDrive != null) {
            eliminarContenidoCarpeta(accessToken, idCarpetaDrive)
        } else {
            idCarpetaDrive = crearCarpetaEnDrive(nombreCarpetaDrive, accessToken)
        }

        val carpetaImagenes = File(context.filesDir, AppPaths.IMAGENES_DIR)
        if (!carpetaImagenes.exists() || !carpetaImagenes.isDirectory) return@coroutineScope false
        val db = AppDatabase.obtenerInstancia(context)
        val latas = db.lataDao().obtenerTodas()

        val cantidadTotal = latas.size
        if (cantidadTotal == 0) return@coroutineScope true

        val incrementoBarraBase = (100 - porcentajeBaseBarra) / cantidadTotal
        val progresoActual = AtomicInteger(0) // Para progreso seguro entre hilos

        // Subir en paralelo usando async
        val resultados =  latas.map { lata ->
            async(Dispatchers.IO) {
                val nombreArchivo = Uri.parse(lata.foto).lastPathSegment ?: return@async false
                val archivo = File(File(context.filesDir, AppPaths.IMAGENES_DIR), nombreArchivo)


                val ok = subirArchivoADrive(accessToken, archivo, "image/jpeg", idCarpetaDrive) ||
                        subirArchivoADrive(accessToken, archivo, "image/jpeg", idCarpetaDrive) // reintento

                val progreso = porcentajeBaseBarra + progresoActual.addAndGet(incrementoBarraBase)
                onProgress?.invoke(progreso)
                Log.e("JAVI", "Nuevo progreso: ${progreso}")
                if(!ok){
                    Toast.makeText(context, "Error subiendo foto ${archivo.name}", Toast.LENGTH_SHORT).show()
                }
                ok
            }
        }

        // Esperar todas las subidas
        val resultadosFinales = resultados.awaitAll()
        return@coroutineScope resultadosFinales.all { it }
    }


    private suspend fun crearCarpetaEnDrive(nombreCarpeta: String, accessToken: String): String? = withContext(Dispatchers.IO) {
        val url = URL("https://www.googleapis.com/drive/v3/files")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

        try {
            val metadata = """
            {
              "name": "$nombreCarpeta",
              "mimeType": "application/vnd.google-apps.folder"
            }
        """.trimIndent()

            connection.outputStream.use { it.write(metadata.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().readText()
                val id = JSONObject(response).getString("id")
                return@withContext id
            } else {
                Log.e("DRIVE", "Error al crear carpeta: $responseCode - ${connection.errorStream?.bufferedReader()?.readText()}")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("DRIVE", "Excepción creando carpeta: ${e.message}", e)
            return@withContext null
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun buscarEnDrive(
        accessToken: String,
        nombre: String,
        esCarpeta: Boolean
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 1. Buscar archivo o carpeta
            val tipoMime = if (esCarpeta) "application/vnd.google-apps.folder" else "*"
            val query =
                "name = '$nombre' and trashed = false and mimeType ${if (esCarpeta) "=" else "!="} '$tipoMime'"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://www.googleapis.com/drive/v3/files?q=$encodedQuery&fields=files(id,name)")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $accessToken")

            val respuesta = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(respuesta)
            val archivos = json.getJSONArray("files")

            if (archivos.length() == 0) return@withContext null

            val id = archivos.getJSONObject(0).getString("id")

            return@withContext id
        } catch (e: Exception) {
            Log.e("DRIVE", "Error en búsqueda y limpieza: ${e.message}", e)
            return@withContext null
        }
    }

    private suspend fun eliminarContenidoCarpeta(accessToken: String, carpetaId: String) = withContext(Dispatchers.IO) {
        try {
            val query = "'$carpetaId' in parents and trashed = false"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://www.googleapis.com/drive/v3/files?q=$encodedQuery&fields=files(id)")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $accessToken")

            val response = conn.inputStream.bufferedReader().readText()
            val archivos = JSONObject(response).getJSONArray("files")

            for (i in 0 until archivos.length()) {
                val fileId = archivos.getJSONObject(i).getString("id")
                eliminarArchivoDeDrive(accessToken, fileId)
            }

        } catch (e: Exception) {
            Log.e("DRIVE", "Error al limpiar carpeta: ${e.message}", e)
        }
    }

    private suspend fun listarArchivosEnCarpeta(accessToken: String, folderId: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val query = "parents in '$folderId' and trashed=false"
        val url = URL("https://www.googleapis.com/drive/v3/files?q=${URLEncoder.encode(query, "UTF-8")}&fields=files(id,name)")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $accessToken")

        return@withContext try {
            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val filesArray = json.getJSONArray("files")
            val result = mutableListOf<Pair<String, String>>() // Pair(id, name)
            for (i in 0 until filesArray.length()) {
                val fileObj = filesArray.getJSONObject(i)
                result.add(Pair(fileObj.getString("id"), fileObj.getString("name")))
            }
            result
        } catch (e: Exception) {
            Log.e("DRIVE", "Error al listar archivos: ${e.message}", e)
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

}