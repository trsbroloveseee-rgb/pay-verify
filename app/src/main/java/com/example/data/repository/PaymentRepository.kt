package com.example.data.repository

import android.content.Context
import com.example.data.local.PaymentSmsDao
import com.example.data.local.PaymentSmsEntity
import com.example.domain.HmacSigner
import com.example.domain.ParsedPaymentSms
import com.example.worker.WebhookSyncScheduler
import kotlinx.coroutines.flow.Flow

/**
 * Repository abstracting Room Database operations and WorkManager scheduling.
 */
class PaymentRepository(
    private val context: Context,
    private val dao: PaymentSmsDao
) {
    val allPayments: Flow<List<PaymentSmsEntity>> = dao.observeAllPayments()
    val pendingCount: Flow<Int> = dao.observePendingCount()
    val syncedCount: Flow<Int> = dao.observeSyncedCount()
    val totalSyncedVolume: Flow<Double> = dao.observeTotalSyncedVolume()

    /**
     * Saves an extracted payment SMS to Room with status = "PENDING"
     * and immediately triggers WorkManager to sync with the Webhook URL.
     */
    suspend fun enqueuePaymentSms(
        parsed: ParsedPaymentSms,
        rawSenderAddress: String,
        rawSmsBody: String,
        secretKey: String
    ): Long {
        val now = System.currentTimeMillis()
        val payload = HmacSigner.buildWebhookJsonPayload(
            platform = parsed.platform,
            amount = parsed.amount,
            senderNumber = parsed.senderNumber,
            senderLast4 = parsed.senderLast4,
            trxId = parsed.trxId,
            reference = parsed.reference,
            receivedAt = now
        )
        val signature = HmacSigner.generateHmacSha256(payload, secretKey)

        val entity = PaymentSmsEntity(
            platform = parsed.platform,
            amount = parsed.amount,
            senderNumber = parsed.senderNumber,
            senderLast4 = parsed.senderLast4,
            trxId = parsed.trxId,
            reference = parsed.reference,
            rawSenderAddress = rawSenderAddress,
            rawSmsBody = rawSmsBody,
            status = PaymentSmsEntity.STATUS_PENDING,
            hmacSignature = signature,
            receivedAt = now
        )
        val rowId = dao.insertPaymentSms(entity)
        if (rowId != -1L) {
            WebhookSyncScheduler.enqueueImmediateSync(context)
        }
        return rowId
    }

    suspend fun retryPaymentSync(id: Long) {
        dao.resetToPending(id)
        WebhookSyncScheduler.enqueueImmediateSync(context)
    }

    fun triggerSyncNow() {
        WebhookSyncScheduler.enqueueImmediateSync(context)
    }

    suspend fun deletePayment(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearSyncedPayments() {
        dao.deleteSyncedPayments()
    }
}
