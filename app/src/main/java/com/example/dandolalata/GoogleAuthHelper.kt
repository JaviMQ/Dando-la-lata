package com.example.dandolalata

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID
import android.util.Base64
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


object DriveScopes {
    //const val DRIVE = "https://www.googleapis.com/auth/drive"
    const val DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
    //const val DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    //const val DRIVE_READONLY = "https://www.googleapis.com/auth/drive.readonly"
}


class GoogleAuthHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(activity: Activity): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(R.string.web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(true)
                // .associateLinkedAccounts("drive", listOf(DriveScopes.DRIVE_FILE))
                .setNonce(UUID.randomUUID().toString()) // Añade seguridad
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .setPreferImmediatelyAvailableCredentials(false)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = activity
            )

            processResponse(response)

        } catch (e: GetCredentialException) {
            Log.e("AUTH_ERROR", "Error: ${e.type} - ${e.message}")
            null
        }
    }

    private fun processResponse(response: GetCredentialResponse): Pair<String, String>? {
        return try {
            // Extraer credencial de Google ID
            val credential = response.credential

            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(credential.data)

                // No se puede verificar 100%. Verificacion manual. De momento paso.
                // En .iss hay https://accounts.google.com
/*
                val payloadJson = decodeIdTokenPayload(googleIdTokenCredential.idToken)
                val email = payloadJson.getString("email")
                Log.e("JAVI", payloadJson.toString())
 */



                val driveScope = Scope(DriveScopes.DRIVE_FILE)
                val authorizationRequest = AuthorizationRequest.Builder()
                    .setRequestedScopes(listOf(driveScope))
                    .build()

                val authorizationClient = Identity.getAuthorizationClient(context)
                authorizationClient.authorize(authorizationRequest)
                    .addOnSuccessListener { authorizationResult ->
                        // Maneja el resultado exitoso aquí
                        Log.e("JAVI", "TENGO AUTORIZACION OK")
                        val token = authorizationResult.accessToken
                        if (token != null) {
                            subirArchivoADrive(token)
                        }

                    }
                    .addOnFailureListener { exception ->
                        // Maneja el error aquí
                        Log.e("JAVI", "Error: ${exception.message}")
                    }



                return Pair(googleIdTokenCredential.id, googleIdTokenCredential.idToken)
            }
            throw Exception();


        } catch (e: Exception) {
            null
        }
    }

    fun decodeIdTokenPayload(idToken: String): JSONObject {
        val parts = idToken.split(".")
        val payload = parts[1]
        val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val decodedPayload = String(decodedBytes, Charsets.UTF_8)
        return JSONObject(decodedPayload)
    }

    suspend fun signOut() {
        // No hay necesidad de PlayServicesAuthProvider
        // La limpieza de credenciales se maneja automáticamente
    }

    fun subirArchivoADrive(accessToken: String) {
        val dbFile = File(context.getDatabasePath("nombre_de_tu_db.db").path)
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