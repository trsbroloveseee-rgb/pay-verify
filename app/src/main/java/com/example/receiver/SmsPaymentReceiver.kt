package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.data.local.PaymentSmsEntity
import com.example.domain.HmacSigner
import com.example.domain.SmsParser
import com.example.worker.WebhookSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Requirement 1, 2, 3, 4:
 * BroadcastReceiver (RECEIVE_SMS) that:
 * 1. Filters incoming SMS messages so ONLY official wallet sender IDs ("bKash", "NAGAD", "16216")
 *    or the customizable Test Phone Number are processed.
 * 2. Parses the 5 payment fields via Regex (platform, amount, senderNumber + last4, trxId, reference).
 * 3. Saves the extracted record in the local Room database with status = "PENDING".
 * 4. Immediately triggers WorkManager to POST the HMAC-SHA256 signed JSON payload to the Webhook URL.
 */
class SmsPaymentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Group multi-part SMS segments by originating sender address
        val groupedBySender = mutableMapOf<String, StringBuilder>()
        for (sms in messages) {
            val sender = sms?.displayOriginatingAddress ?: sms?.originatingAddress ?: continue
            val bodyPart = sms.displayMessageBody ?: sms.messageBody ?: continue
            groupedBySender.getOrPut(sender) { StringBuilder() }.append(bodyPart)
        }

        if (groupedBySender.isEmpty()) return

        val appContext = context.applicationContext
        val prefs = AppPreferences(appContext)
        val config = prefs.readConfig()

        // Use goAsync() so the BroadcastReceiver safely completes Room DB insert before terminating
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(appContext).paymentSmsDao()
                var anyInserted = false

                for ((senderAddress, fullBodyBuilder) in groupedBySender) {
                    val fullBody = fullBodyBuilder.toString().trim()

                    // Step 1: Sender ID Filter ("bKash", "NAGAD", "16216", or Test Phone Number)
                    if (!SmsParser.isAllowedSender(senderAddress, config.testPhoneNumber)) {
                        Log.d(TAG, "Ignored SMS from non-whitelisted sender: $senderAddress")
                        continue
                    }

                    // Step 2: Regex Parser (extract 5 fields)
                    val parsed = SmsParser.parsePaymentSms(senderAddress, fullBody)
                    if (parsed == null) {
                        Log.d(TAG, "Sender $senderAddress matched filter, but SMS did not match payment regex.")
                        continue
                    }

                    // Step 3: Compute HMAC-SHA256 preview and save in Room with status = "PENDING"
                    val now = System.currentTimeMillis()
                    val jsonPayload = HmacSigner.buildWebhookJsonPayload(
                        platform = parsed.platform,
                        amount = parsed.amount,
                        senderNumber = parsed.senderNumber,
                        senderLast4 = parsed.senderLast4,
                        trxId = parsed.trxId,
                        reference = parsed.reference,
                        receivedAt = now
                    )
                    val signature = HmacSigner.generateHmacSha256(jsonPayload, config.secretKey)

                    val entity = PaymentSmsEntity(
                        platform = parsed.platform,
                        amount = parsed.amount,
                        senderNumber = parsed.senderNumber,
                        senderLast4 = parsed.senderLast4,
                        trxId = parsed.trxId,
                        reference = parsed.reference,
                        rawSenderAddress = senderAddress,
                        rawSmsBody = fullBody,
                        status = PaymentSmsEntity.STATUS_PENDING,
                        hmacSignature = signature,
                        receivedAt = now
                    )

                    val rowId = dao.insertPaymentSms(entity)
                    if (rowId != -1L) {
                        anyInserted = true
                        Log.i(TAG, "Saved ${parsed.platform} payment TrxID=${parsed.trxId} (Tk ${parsed.amount}) as PENDING")
                    }
                }

                // Step 4: Trigger WorkManager immediate webhook sync
                if (anyInserted) {
                    WebhookSyncScheduler.enqueueImmediateSync(appContext)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming payment SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsPaymentReceiver"
    }
}
