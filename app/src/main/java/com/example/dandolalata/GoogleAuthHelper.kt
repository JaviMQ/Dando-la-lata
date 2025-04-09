package com.example.dandolalata

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
//import com.google.android.gms.drive.DriveScopes


object DriveScopes {
    const val DRIVE = "https://www.googleapis.com/auth/drive"
    const val DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
    const val DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    const val DRIVE_READONLY = "https://www.googleapis.com/auth/drive.readonly"
}


class GoogleAuthHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(activity: Activity): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                // .setServerClientId(context.getString(R.string.web_client_id))
                .setServerClientId("404713282672-phdvlpf9emfgs2efhu1gdpfdqtto79lq.apps.googleusercontent.com")
                .setFilterByAuthorizedAccounts(false)
                .associateLinkedAccounts("drive", listOf(DriveScopes.DRIVE_FILE))
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

                Pair(googleIdTokenCredential.id, googleIdTokenCredential.idToken)
            }
            throw Exception();


        } catch (e: Exception) {
            null
        }
    }



    suspend fun signOut() {
        // No hay necesidad de PlayServicesAuthProvider
        // La limpieza de credenciales se maneja automáticamente
    }


}