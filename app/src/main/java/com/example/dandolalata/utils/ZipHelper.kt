package com.example.dandolalata.utils

import android.content.Context
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipHelper {

    fun comprimirImagenes(context: Context, nombreZip: String): File? {
        val carpetaImagenes = File(context.filesDir, AppPaths.IMAGENES_DIR)
        if (!carpetaImagenes.exists() || !carpetaImagenes.isDirectory) return null

        val archivoZip = File(context.cacheDir, nombreZip)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(archivoZip))).use { zos ->
            carpetaImagenes.listFiles()?.forEach { archivo ->
                if (archivo.isFile) {
                    FileInputStream(archivo).use { fis ->
                        val entrada = ZipEntry(archivo.name)
                        zos.putNextEntry(entrada)

                        fis.copyTo(zos)
                        zos.closeEntry()
                    }
                }
            }
        }

        return archivoZip
    }

    fun descomprimirImagenes(context: Context, archivoZip: File): Boolean {
        val carpetaDestino = File(context.filesDir, AppPaths.IMAGENES_DIR)
        if (!carpetaDestino.exists()) carpetaDestino.mkdirs()

        return try {
            ZipInputStream(BufferedInputStream(FileInputStream(archivoZip))).use { zis ->
                var entrada: ZipEntry?
                while (zis.nextEntry.also { entrada = it } != null) {
                    entrada?.let {
                        val archivoDestino = File(carpetaDestino, it.name)
                        FileOutputStream(archivoDestino).use { fos ->
                            zis.copyTo(fos)
                        }
                        zis.closeEntry()
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}