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


class GoogleAuthHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(activity: Activity): Pair<String, String>? = withContext(Dispatchers.Main) {
        try {

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.WEB_GOOGLE_CLIENT_ID) // Debe ser el web, no el android!
                .setFilterByAuthorizedAccounts(false)
                // .setAutoSelectEnabled(true)
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



//                val driveScope = Scope( context.getString(R.string.drive_scope_file))
                val driveScope = Scope("https://www.googleapis.com/auth/drive.file")


                val authorizationRequest = AuthorizationRequest.Builder()
                    .setRequestedScopes(listOf(driveScope))
                    .build()

                val authorizationClient = Identity.getAuthorizationClient(context)
                authorizationClient.authorize(authorizationRequest)
                    .addOnSuccessListener { authorizationResult ->
                        // Maneja el resultado exitoso aquí
                        Log.e("JAVI", "TENGO AUTORIZACION OK")
                        Log.e("JAVI 2", authorizationResult.grantedScopes.toString())

                        val token = authorizationResult.accessToken
                        if (token != null) {
                            val driveHelper = GoogleDriveHelper(context)
                            driveHelper.subirArchivoADrive(token)
                        }

                    }
                    .addOnFailureListener { exception ->
                        // Maneja el error aquí
                        Log.e("JAVI", "Error: ${exception.message}")
                    }



                return Pair(googleIdTokenCredential.id, googleIdTokenCredential.idToken)
            }
            throw Exception("Credential type is not valid")


        } catch (e: Exception) {
            Log.e("AUTH_ERROR", "Error al procesar la respuesta: ${e.message}")
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

}