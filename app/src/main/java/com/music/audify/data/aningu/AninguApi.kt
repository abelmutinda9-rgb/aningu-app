package com.music.audify.data.aningu

import android.content.Context
import android.media.MediaDrm
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

object AninguApi {
    private const val BASE_URL = "https://aningu-music-family-pass.lovable.app"

    data class Entitlement(
        val status: String, // "trial", "premium", "expired"
        val entitled: Boolean,
        val trialSecondsRemaining: Long = 0,
        val trialEndsAt: String? = null,
        val planType: String? = null,
        val expiresAt: String? = null,
        val message: String? = null
    )

    data class StkResponse(
        val success: Boolean,
        val reference: String? = null,
        val message: String? = null,
        val checkoutRequestId: String? = null
    )

    data class PaymentStatus(
        val status: String, // "QUEUED", "PENDING", "SUCCESS", "FAILED"
        val isTerminal: Boolean,
        val success: Boolean,
        val planType: String? = null,
        val expiresAt: String? = null
    )

    fun getDeviceId(context: Context): String {
        val widevineUuid = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
        var rawDrmId = ""
        try {
            val drm = MediaDrm(widevineUuid)
            val bytes = drm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            drm.close()
            rawDrmId = bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Throwable) {}

        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (_: Throwable) { "" }

        val seed = "$rawDrmId:${Build.FINGERPRINT}:${Build.SERIAL}:$androidId"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(seed.toByteArray())
        return "aningu_" + hash.take(16).joinToString("") { "%02x".format(it) }
    }

    suspend fun deviceHandshake(context: Context, platform: String = "android"): Entitlement =
        withContext(Dispatchers.IO) {
            val deviceId = getDeviceId(context)
            try {
                val url = URL("$BASE_URL/api/public/device/handshake")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val body = JSONObject().apply {
                    put("device_id", deviceId)
                    put("platform", platform)
                }

                conn.outputStream.use { os ->
                    os.write(body.toString().toByteArray())
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream?.bufferedReader()?.use(BufferedReader::readText) ?: "{}"

                val json = JSONObject(responseText)
                val status = json.optString("status", "expired")
                val entitled = json.optBoolean("entitled", status == "trial" || status == "premium")
                val trialSec = json.optLong("trial_seconds_remaining", 0)
                val trialEnds = json.optString("trial_ends_at", null)
                val planObj = json.optJSONObject("plan")
                val planType = planObj?.optString("plan_type")
                val expiresAt = planObj?.optString("expires_at")

                Entitlement(
                    status = status,
                    entitled = entitled,
                    trialSecondsRemaining = trialSec,
                    trialEndsAt = trialEnds,
                    planType = planType,
                    expiresAt = expiresAt
                )
            } catch (e: Exception) {
                // Server-authoritative: network errors do NOT grant unlimited free access
                Entitlement(
                    status = "expired",
                    entitled = false,
                    message = "Could not verify trial status. Please check your internet connection."
                )
            }
        }

    suspend fun initiateStkPush(context: Context, phone: String, plan: String): StkResponse =
        withContext(Dispatchers.IO) {
            val deviceId = getDeviceId(context)
            try {
                val url = URL("$BASE_URL/api/public/payments/payhero/stk-push")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val body = JSONObject().apply {
                    put("device_id", deviceId)
                    put("phone", phone)
                    put("plan", plan)
                }

                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream?.bufferedReader()?.use(BufferedReader::readText) ?: "{}"
                val json = JSONObject(responseText)

                StkResponse(
                    success = json.optBoolean("success", code == 200),
                    reference = json.optString("reference", null),
                    message = json.optString("message", null),
                    checkoutRequestId = json.optString("checkout_request_id", null)
                )
            } catch (e: Exception) {
                StkResponse(success = false, message = e.localizedMessage)
            }
        }

    suspend fun checkPaymentStatus(reference: String): PaymentStatus =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/api/public/payments/check-status?reference=$reference")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream?.bufferedReader()?.use(BufferedReader::readText) ?: "{}"
                val json = JSONObject(responseText)

                PaymentStatus(
                    status = json.optString("status", "PENDING"),
                    isTerminal = json.optBoolean("is_terminal", false),
                    success = json.optBoolean("success", false),
                    planType = json.optString("plan_type", null),
                    expiresAt = json.optString("expires_at", null)
                )
            } catch (e: Exception) {
                PaymentStatus(status = "PENDING", isTerminal = false, success = false)
            }
        }
}
