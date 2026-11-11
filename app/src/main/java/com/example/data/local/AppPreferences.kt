package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VerifierConfig(
    val webhookUrl: String,
    val secretKey: String,
    val testPhoneNumber: String,
    val loopbackSimulationMode: Boolean,
    val isServiceEnabled: Boolean
)

/**
 * Manages persistent configuration for Webhook URL, HMAC-SHA256 Secret Key,
 * Customizable Test Phone Number, and 24/7 Service state.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(readConfig())
    val configFlow: StateFlow<VerifierConfig> = _configFlow.asStateFlow()

    fun readConfig(): VerifierConfig {
        return VerifierConfig(
            webhookUrl = prefs.getString(KEY_WEBHOOK_URL, DEFAULT_WEBHOOK_URL) ?: DEFAULT_WEBHOOK_URL,
            secretKey = prefs.getString(KEY_SECRET_KEY, DEFAULT_SECRET_KEY) ?: DEFAULT_SECRET_KEY,
            testPhoneNumber = prefs.getString(KEY_TEST_PHONE, DEFAULT_TEST_PHONE) ?: DEFAULT_TEST_PHONE,
            loopbackSimulationMode = prefs.getBoolean(KEY_LOOPBACK_MODE, false),
            isServiceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        )
    }

    fun updateWebhookConfig(
        webhookUrl: String,
        secretKey: String,
        testPhoneNumber: String,
        loopbackSimulationMode: Boolean
    ) {
        prefs.edit()
            .putString(KEY_WEBHOOK_URL, webhookUrl.trim())
            .putString(KEY_SECRET_KEY, secretKey.trim())
            .putString(KEY_TEST_PHONE, testPhoneNumber.trim())
            .putBoolean(KEY_LOOPBACK_MODE, loopbackSimulationMode)
            .apply()
        _configFlow.value = readConfig()
    }

    fun setServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
        _configFlow.value = readConfig()
    }

    companion object {
        private const val PREFS_NAME = "paypulse_verifier_prefs"
        private const val KEY_WEBHOOK_URL = "webhook_url"
        private const val KEY_SECRET_KEY = "hmac_secret_key"
        private const val KEY_TEST_PHONE = "test_phone_number"
        private const val KEY_LOOPBACK_MODE = "loopback_simulation_mode"
        private const val KEY_SERVICE_ENABLED = "service_enabled"

        const val DEFAULT_WEBHOOK_URL = "https://your-domain.com/api/webhook/payment-verify"
        const val DEFAULT_SECRET_KEY = "whsec_bd_paypulse_9f8a7b6c5d4e3f2a1b"
        const val DEFAULT_TEST_PHONE = "01700000000"
    }
}
