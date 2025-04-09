package com.example.dandolalata

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.drive.Drive
import com.google.android.gms.drive.MetadataChangeSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveHelper(private val context: Context) {
    suspend fun uploadToDrive(
        accountEmail: String,
        idToken: String,
        fileContent: ByteArray,
        fileName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Configurar GoogleSignInOptions con el scope manual
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .setAccountName(accountEmail)
                .requestIdToken(idToken)
                .requestScopes(Scope(DriveScopes.DRIVE_FILE)) // Scope manual
                .build()

            // 2. Obtener el cliente de GoogleSignIn
            val googleSignInAccount = GoogleSignIn.getClient(context, gso)
                .silentSignIn()
                .await()

            // 3. Crear cliente de Drive
            val driveClient = Drive.getDriveClient(context, googleSignInAccount)
            val driveResourceClient = Drive.getDriveResourceClient(context, googleSignInAccount)

            // 4. Subir el archivo
            val metadata = MetadataChangeSet.Builder()
                .setTitle(fileName)
                .setMimeType("image/jpeg")
                .build()

            val uploadTask = driveResourceClient.rootFolder
                .continueWithTask { task ->
                    val parentFolder = task.result
                    driveResourceClient.createFile(
                        parentFolder,
                        metadata,
                        null,
                        fileContent.inputStream()
                    )
                }
                .await()

            uploadTask.driveId != null
        } catch (e: Exception) {
            Log.e("GoogleDriveHelper", "Error al subir a Drive", e)
            false
        }
    }
}