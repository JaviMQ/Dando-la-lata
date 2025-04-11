package com.example.dandolalata

import android.content.Context
import android.util.Log
import com.example.dandolalata.data.database.DatabaseConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


class GoogleDriveHelper (private val context: Context) {
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
}