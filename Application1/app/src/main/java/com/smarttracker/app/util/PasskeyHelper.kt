@file:Suppress("KotlinConstantConditions")

package com.smarttracker.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import com.google.gson.Gson
import java.security.SecureRandom

object PasskeyHelper {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // ✅ Generate secure random challenge
    private fun generateChallenge(): String {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        return Base64.encodeToString(randomBytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    // ✅ Used in registration
    fun createPasskeyOptions(email: String): String {
        val challengeBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val userIdBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }

        val challenge = Base64.encodeToString(challengeBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val userId = Base64.encodeToString(userIdBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

        val options = mapOf(
            "rp" to mapOf(
                "name" to "Smart Tracker",
                "id" to "smarttracker.online" // ✅ Use your new package name here!
        ),
            "user" to mapOf(
                "id" to userId,
                "name" to email,
                "displayName" to email
            ),
            "challenge" to challenge,
            "pubKeyCredParams" to listOf(
                mapOf("type" to "public-key", "alg" to -7)
            ),
            "excludeCredentials" to emptyList<Map<String, String>>(), // ✅ Add this
            "timeout" to 60000,
            "attestation" to "none",
            "authenticatorSelection" to mapOf(
                "userVerification" to "required",
                "authenticatorAttachment" to "platform",
                "residentKey" to "required",
                "requireResidentKey" to "true",
            )
        )


        Log.d("PASSKEY", "Register JSON: ${Gson().toJson(options)}")
        return Gson().toJson(options)
    }


    @SuppressLint("PrivateApi", "PublicKeyCredential")
    fun registerPasskey(
        activity: ComponentActivity,
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val credentialManager = CredentialManager.create(activity)
        val executor = ContextCompat.getMainExecutor(activity)

        val requestJson = createPasskeyOptions(email)
        val request = CreatePublicKeyCredentialRequest(requestJson)

        credentialManager.createCredentialAsync(
            request = request,
            context = activity,
            cancellationSignal = null,
            executor = executor,
            callback = object : CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException> {
                override fun onResult(result: CreateCredentialResponse) {
                    try {
                        val credentialJson = result.data.getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON")
                        val parsed = Gson().fromJson<Map<String, Any>>(credentialJson, Map::class.java)
                        val credentialId = parsed["id"] as? String

                        if (credentialId != null) {
                            Log.d("PASSKEY", "✅ Registration success. Credential ID: $credentialId")
                            onSuccess(credentialId)
                        } else {
                            onError("Credential ID missing in response")
                        }
                    } catch (e: Exception) {
                        Log.e("PASSKEY", "❌ Failed to parse registration response", e)
                        onError("Failed to parse response: ${e.message}")
                    }
                }

                override fun onError(e: CreateCredentialException) {
                    Log.e("PASSKEY", "❌ Passkey registration failed", e)
                    onError(((e.errorMessage ?: e.toString()).toString()))
                }
            }
        )
    }

    @SuppressLint("PrivateApi", "PublicKeyCredential")
    fun loginWithPasskey(
        activity: ComponentActivity,
        onSuccess: (email: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val credentialManager = CredentialManager.create(activity)
        val executor = ContextCompat.getMainExecutor(activity)

        // ⚠️ Required: "allowCredentials" to match registered passkey ID
        // For now, we simulate with a wildcard — real apps fetch from server
        val loginOptionsJson = """
        {
            "challenge": "${Base64.encodeToString("simpleLoginChallenge123".toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)}",
            "timeout": 60000,
            "rpId": "android.com",
            "userVerification": "required",
            "allowCredentials": []
        }
    """.trimIndent()

        val request = GetCredentialRequest(
            listOf(
                GetPublicKeyCredentialOption(loginOptionsJson)
            )
        )

        credentialManager.getCredentialAsync(
            request = request,
            context = activity,
            cancellationSignal = null,
            executor = executor,
            callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    try {
                        val credential = result.credential
                        val credentialJson = credential.data.getString("androidx.credentials.BUNDLE_KEY_CREDENTIAL_JSON")

                        if (credentialJson == null) {
                            onError("Missing credential JSON")
                            return
                        }

                        val parsed = Gson().fromJson(credentialJson, Map::class.java)
                        val email = parsed["id"] as? String

                        if (email != null) {
                            Log.d("PASSKEY", "✅ Login success. Email: $email")
                            onSuccess(email)
                        } else {
                            onError("Email missing from credential")
                        }
                    } catch (e: Exception) {
                        Log.e("PASSKEY", "Failed to parse login response", e)
                        onError("Parsing error: ${e.message}")
                    }
                }

                override fun onError(e: GetCredentialException) {
                    Log.e("PASSKEY", "❌ Passkey login failed", e)
                    onError((e.errorMessage ?: "Unknown error").toString())
                }
            }
        )
    }

}
