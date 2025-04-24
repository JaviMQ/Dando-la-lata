package com.example.dandolalata

import android.app.Activity
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID


class GoogleAuthHelper(private val activity: ComponentActivity) {

    private val credentialManager = CredentialManager.create(activity)
    private var driveTokenDeferred: CompletableDeferred<String?>? = null

    private val authLauncher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Usuario aceptó acceso a Drive
                driveTokenDeferred?.complete(lastAuthorizationResult?.accessToken)
            } else {
                driveTokenDeferred?.complete(null)
            }
        }

    private var lastAuthorizationResult: AuthorizationResult? = null

    suspend fun signIn(): String? = withContext(Dispatchers.Main) {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.WEB_GOOGLE_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(true)
                .setNonce(UUID.randomUUID().toString())
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .setPreferImmediatelyAvailableCredentials(false)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val result = processResponse(response)
            result

        } catch (e: GetCredentialException) {
            Log.e("AUTH_ERROR", "Cred error: ${e.type} - ${e.message}")
            null
        } catch (e: Exception) {
            Log.e("AUTH_ERROR", "Unexpected error: ${e.message}")
            null
        }
    }

    private suspend fun processResponse(response: GetCredentialResponse): String? {
        val credential = response.credential

        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            Log.e("AUTH", "Invalid credential type")
            return null
        }

        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val tokenJson = tokenAJson(googleIdTokenCredential.idToken)
        if(tokenJson.getString("email") != "javier.mquetglas@gmail.com"){
            Toast.makeText(activity.applicationContext, "Email no permitido", Toast.LENGTH_SHORT).show()
            return null
        }
        val driveScope = Scope(activity.applicationContext.getString(R.string.drive_scope_file))

        val authRequest = AuthorizationRequest.Builder()
            .setRequestedScopes(listOf(driveScope))
            .build()

        val authClient = Identity.getAuthorizationClient(activity)

        return autorizacionSuspendCancellable(authClient, authRequest)
    }

    private suspend fun autorizacionSuspendCancellable(
        authClient: com.google.android.gms.auth.api.identity.AuthorizationClient,
        authRequest: AuthorizationRequest
    ): String? = withContext(Dispatchers.Main) {
        driveTokenDeferred = CompletableDeferred()

        authClient.authorize(authRequest) // Aqui Google ya sabe el usuario autenticado previamente
            .addOnSuccessListener { authResult ->
                lastAuthorizationResult = authResult
                if (authResult.hasResolution()) {
                    val intentSender = authResult.pendingIntent?.intentSender
                    if (intentSender != null) {
                        val request = IntentSenderRequest.Builder(intentSender).build()
                        authLauncher.launch(request)
                    } else {
                        driveTokenDeferred?.complete(null)
                    }
                } else {
                    // Acceso directo sin pedir confirmación, ya se habia dado permiso
                    driveTokenDeferred?.complete(authResult.accessToken)
                }
            }
            .addOnFailureListener { e ->
                Log.e("AUTH_ERROR", "Auth failed: ${e.message}")
                driveTokenDeferred?.complete(null)
            }

        driveTokenDeferred?.await()
    }

    private fun tokenAJson(idToken: String): JSONObject {
        val parts = idToken.split(".")
        val payload = parts[1]
        val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val decodedPayload = String(decodedBytes, Charsets.UTF_8)
        return JSONObject(decodedPayload)
    }
}