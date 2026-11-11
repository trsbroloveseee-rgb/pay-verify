package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Requirement 3: Offline Queue (Room Database)
 * Saves every extracted payment SMS in a local SQLite Room database with
 * status = "PENDING" or "SYNCED" so no payment is lost if Wi-Fi or mobile data disconnects.
 */
@Entity(
    tableName = "payment_sms_queue",
    indices = [Index(value = ["trxId"], unique = true)]
)
data class PaymentSmsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val platform: String,               // "bKash", "Nagad", "Rocket"
    val amount: Double,                 // e.g., 500.00
    val senderNumber: String,           // Full sender number, e.g., "01712345678"
    val senderLast4: String,            // Last 4 digits, e.g., "5678"
    val trxId: String,                  // Uppercase 8-10 char TrxID, e.g., "BJK4P9X2T1"
    val reference: String,              // Optional reference after "Ref", e.g., "tbuddy1", or ""
    val rawSenderAddress: String,       // Original SMS sender ID, e.g., "bKash", "NAGAD", "16216", or Test Phone
    val rawSmsBody: String,             // Full SMS body text for audit/debugging
    val status: String = STATUS_UNSYNCED,// "UNSYNCED" or "SYNCED"
    val syncAttempts: Int = 0,          // Number of webhook POST attempts
    val lastHttpCode: Int? = null,      // Last HTTP status code returned by webhook (e.g., 200)
    val lastResponseMessage: String? = null, // Server response snippet or error details
    val hmacSignature: String = "",     // Last computed X-Signature HMAC-SHA256 hash
    val receivedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
) {
    companion object {
        const val STATUS_UNSYNCED = "UNSYNCED"
        const val STATUS_PENDING = "UNSYNCED"
        const val STATUS_SYNCED = "SYNCED"
    }
}
