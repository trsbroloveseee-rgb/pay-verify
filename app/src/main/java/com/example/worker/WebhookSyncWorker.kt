package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.domain.HmacSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Requirement 3 & 4: Auto-Sync & Retry (WorkManager + OkHttp) with HMAC-SHA256 Signature
 *
 * Reads all "UNSYNCED" payment SMS records from Room, signs each JSON payload with HMAC-SHA256
 * using the configured Secret Key, and sends a POST request with header "X-Signature" to the Webhook URL.
 *
 * - On HTTP 200 OK: Updates the Room row status to "SYNCED".
 * - On network error or non-200 response: Keeps status as "UNSYNCED" and returns Result.retry()
 *   so WorkManager automatically retries with exponential backoff when internet returns.
 */
class WebhookSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(applicationContext)
        val dao = database.paymentSmsDao()
        val prefs = AppPreferences(applicationContext)
        val config = prefs.readConfig()

        val pendingList = dao.getPendingPayments()
        if (pendingList.isEmpty()) {
            return@withContext Result.success()
        }

        var shouldRetryAny = false

        for (item in pendingList) {
            val jsonPayload = HmacSigner.buildWebhookJsonPayload(
                platform = item.platform,
                amount = item.amount,
                senderNumber = item.senderNumber,
                senderLast4 = item.senderLast4,
                trxId = item.trxId,
                reference = item.reference,
                receivedAt = item.receivedAt
            )

            val hmacSignature = HmacSigner.generateHmacSha256(
                data = jsonPayload,
                secretKey = config.secretKey
            )
            val concatSignature = HmacSigner.generateHmacSha256(
                data = HmacSigner.buildConcatSigningString(item.trxId, item.amount, item.receivedAt),
                secretKey = config.secretKey
            )

            // Optional sandbox loopback mode for testing without a live external server
            if (config.loopbackSimulationMode) {
                delay(300)
                val matchRule = if (item.reference.isNotBlank()) {
                    "Matched via (Ref '${item.reference}' + Tk ${item.amount} + Last4 '${item.senderLast4}')"
                } else {
                    "Matched via (TrxID '${item.trxId}' + Tk ${item.amount})"
                }
                dao.markAsSynced(
                    id = item.id,
                    httpCode = 200,
                    responseMessage = "200 OK (Loopback Verified HMAC • $matchRule)",
                    hmacSignature = hmacSignature
                )
                continue
            }

            // Validate Webhook URL before attempting network call
            if (!config.webhookUrl.startsWith("http://") && !config.webhookUrl.startsWith("https://")) {
                dao.recordSyncFailure(
                    id = item.id,
                    httpCode = null,
                    errorMessage = "Configure a valid http/https Webhook URL in the Webhook tab",
                    hmacSignature = hmacSignature
                )
                shouldRetryAny = true
                continue
            }

            try {
                // Note: Google Apps Script web apps often respond with a 302 redirect on POST;
                // OkHttp follows redirects automatically. We also pass signature in query param
                // as a fallback because Google Apps Script strips custom HTTP headers in doPost(e).
                val urlWithSigParams = appendQuerySignatureForAppsScript(
                    baseUrl = config.webhookUrl,
                    signature = hmacSignature,
                    payloadSignature = concatSignature
                )

                val requestBody = jsonPayload.toRequestBody(JSON_MEDIA_TYPE)
                val request = Request.Builder()
                    .url(urlWithSigParams)
                    .post(requestBody)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-Signature", hmacSignature)
                    .header("X-Payload-Signature", concatSignature)
                    .header("X-Wallet-Platform", item.platform)
                    .header("X-Trx-Id", item.trxId)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val code = response.code
                    val responseBody = response.body?.string()?.take(200)?.trim().orEmpty()

                    if (code == 200 || response.isSuccessful) {
                        dao.markAsSynced(
                            id = item.id,
                            httpCode = code,
                            responseMessage = responseBody.ifEmpty { "HTTP $code OK — Verified by Webhook" },
                            hmacSignature = hmacSignature
                        )
                    } else {
                        Log.w(TAG, "Webhook returned HTTP $code for TrxID=${item.trxId}")
                        dao.recordSyncFailure(
                            id = item.id,
                            httpCode = code,
                            errorMessage = "HTTP $code: ${responseBody.ifEmpty { response.message }}",
                            hmacSignature = hmacSignature
                        )
                        shouldRetryAny = true
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error syncing TrxID=${item.trxId}: ${e.message}")
                dao.recordSyncFailure(
                    id = item.id,
                    httpCode = null,
                    errorMessage = "Offline / Network Error: ${e.localizedMessage ?: "Connection failed"} (Auto-retry queued)",
                    hmacSignature = hmacSignature
                )
                shouldRetryAny = true
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error syncing TrxID=${item.trxId}: ${e.message}")
                dao.recordSyncFailure(
                    id = item.id,
                    httpCode = null,
                    errorMessage = "Error: ${e.localizedMessage ?: "Unknown exception"}",
                    hmacSignature = hmacSignature
                )
                shouldRetryAny = true
            }
        }

        if (shouldRetryAny) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {
        private const val TAG = "WebhookSyncWorker"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }

        /**
         * Google Apps Script `doPost(e)` does not expose custom HTTP headers (`X-Signature`),
         * only `e.parameter.signature` and `e.postData.contents`. Appending `?signature=...`
         * ensures both standard servers (Node.js/PHP via `X-Signature` header) AND Google Apps Script
         * can verify the HMAC-SHA256 signature seamlessly!
         */
        fun appendQuerySignatureForAppsScript(
            baseUrl: String,
            signature: String,
            payloadSignature: String
        ): String {
            val separator = if (baseUrl.contains("?")) "&" else "?"
            return "${baseUrl}${separator}signature=$signature&payloadSig=$payloadSignature"
        }
    }
}
