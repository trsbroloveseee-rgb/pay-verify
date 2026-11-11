package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppPreferences
import com.example.service.PaymentVerifierService
import com.example.worker.WebhookSyncScheduler

/**
 * Production Boot Receiver:
 * Automatically restarts the 24/7 Payment Listener ForegroundService and triggers
 * WorkManager sync for any UNSYNCED payments whenever the Android phone reboots.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val appContext = context.applicationContext
            val config = AppPreferences(appContext).readConfig()
            if (config.isServiceEnabled) {
                Log.i(TAG, "Device booted: restarting 24/7 PaymentVerifierService")
                PaymentVerifierService.startService(appContext)
            }
            // Also trigger WorkManager to flush any UNSYNCED payments queued before reboot
            WebhookSyncScheduler.enqueueImmediateSync(appContext)
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
