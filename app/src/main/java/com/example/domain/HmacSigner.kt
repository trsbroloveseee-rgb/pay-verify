package com.example.domain

import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Requirement 4: Security (HMAC-SHA256 Signature)
 * Generates the canonical, deterministic JSON payload for a payment SMS and signs it with HMAC-SHA256
 * using the app's Secret Key so the website/Google Sheet backend can verify "X-Signature".
 */
object HmacSigner {

    private const val HMAC_ALGORITHM = "HmacSHA256"

    /**
     * Builds a deterministic JSON string with fixed key order sent to the Webhook URL.
     */
    fun buildWebhookJsonPayload(
        platform: String,
        amount: Double,
        senderNumber: String,
        senderLast4: String,
        trxId: String,
        reference: String,
        receivedAt: Long
    ): String {
        val amountFormatted = String.format(Locale.US, "%.2f", amount)
        val normalizedTrx = trxId.trim().uppercase(Locale.ROOT)
        val trimmedRef = reference.trim()

        return buildString {
            append("{")
            append("\"platform\":\"").append(escapeJson(platform)).append("\",")
            append("\"amount\":").append(amountFormatted).append(",")
            append("\"amountFormatted\":\"").append(amountFormatted).append("\",")
            append("\"senderNumber\":\"").append(escapeJson(senderNumber)).append("\",")
            append("\"senderLast4\":\"").append(escapeJson(senderLast4)).append("\",")
            append("\"trxId\":\"").append(escapeJson(normalizedTrx)).append("\",")
            append("\"reference\":\"").append(escapeJson(trimmedRef)).append("\",")
            append("\"timestamp\":").append(receivedAt)
            append("}")
        }
    }

    /**
     * Builds the canonical concatenated signing string: "trxId + amountFormatted + timestamp"
     * in case the server prefers verifying key fields directly.
     */
    fun buildConcatSigningString(
        trxId: String,
        amount: Double,
        receivedAt: Long
    ): String {
        val normalizedTrx = trxId.trim().uppercase(Locale.ROOT)
        val amountFormatted = String.format(Locale.US, "%.2f", amount)
        return "$normalizedTrx$amountFormatted$receivedAt"
    }

    private fun escapeJson(value: String): String {
        return buildString(value.length) {
            for (ch in value) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
    }

    /**
     * Computes the lowercase hexadecimal HMAC-SHA256 digest of [data] using [secretKey].
     * Sent in the HTTP request header as:
     * X-Signature: <64-char hex digest of raw JSON body>
     * X-Payload-Signature: <64-char hex digest of trxId + amountFormatted + timestamp>
     */
    fun generateHmacSha256(data: String, secretKey: String): String {
        val safeKey = secretKey.ifBlank { "default_secret_key_change_me" }
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        val secretKeySpec = SecretKeySpec(
            safeKey.toByteArray(StandardCharsets.UTF_8),
            HMAC_ALGORITHM
        )
        mac.init(secretKeySpec)
        val hmacBytes = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return hmacBytes.joinToString("") { byte -> "%02x".format(byte) }
    }
}
