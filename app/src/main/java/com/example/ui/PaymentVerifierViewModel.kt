package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.data.local.PaymentSmsEntity
import com.example.data.local.VerifierConfig
import com.example.data.repository.PaymentRepository
import com.example.domain.HmacSigner
import com.example.domain.ParsedPaymentSms
import com.example.domain.SmsParser
import com.example.service.PaymentVerifierService
import com.example.worker.WebhookSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

enum class QueueFilter {
    ALL, PENDING, SYNCED
}

class PaymentVerifierViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = PaymentRepository(application, database.paymentSmsDao())
    private val preferences = AppPreferences(application)

    val configState: StateFlow<VerifierConfig> = preferences.configFlow

    private val _selectedFilter = MutableStateFlow(QueueFilter.ALL)
    val selectedFilter: StateFlow<QueueFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _isTestingWebhook = MutableStateFlow(false)
    val isTestingWebhook: StateFlow<Boolean> = _isTestingWebhook.asStateFlow()

    // Simulator state
    private val _simSender = MutableStateFlow("bKash")
    val simSender: StateFlow<String> = _simSender.asStateFlow()

    private val _simBody = MutableStateFlow(SmsParser.SAMPLE_BKASH_SMS)
    val simBody: StateFlow<String> = _simBody.asStateFlow()

    val allPayments: StateFlow<List<PaymentSmsEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPayments: StateFlow<List<PaymentSmsEntity>> = combine(
        allPayments,
        _selectedFilter,
        _searchQuery
    ) { list, filter, query ->
        list.filter { item ->
            val matchesFilter = when (filter) {
                QueueFilter.ALL -> true
                QueueFilter.PENDING -> item.status != PaymentSmsEntity.STATUS_SYNCED
                QueueFilter.SYNCED -> item.status == PaymentSmsEntity.STATUS_SYNCED
            }
            val trimmedQuery = query.trim()
            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                item.trxId.contains(trimmedQuery, ignoreCase = true) ||
                    item.reference.contains(trimmedQuery, ignoreCase = true) ||
                    item.senderNumber.contains(trimmedQuery, ignoreCase = true) ||
                    item.platform.contains(trimmedQuery, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = repository.pendingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncedCount: StateFlow<Int> = repository.syncedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalSyncedVolume: StateFlow<Double> = repository.totalSyncedVolume
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setFilter(filter: QueueFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSimSender(sender: String) {
        _simSender.value = sender
    }

    fun updateSimBody(body: String) {
        _simBody.value = body
    }

    fun dismissBanner() {
        _statusBannerMessage.value = null
    }

    fun saveWebhookConfiguration(
        webhookUrl: String,
        secretKey: String,
        testPhoneNumber: String,
        loopbackMode: Boolean
    ) {
        preferences.updateWebhookConfig(
            webhookUrl = webhookUrl,
            secretKey = secretKey,
            testPhoneNumber = testPhoneNumber,
            loopbackSimulationMode = loopbackMode
        )
        _statusBannerMessage.value = "Saved Webhook URL, HMAC Secret Key & Test Phone Number."
    }

    /**
     * Sends a live signed test ping to the configured Webhook URL to verify connectivity and HMAC validation.
     */
    fun testWebhookConnection(
        webhookUrl: String,
        secretKey: String,
        loopbackMode: Boolean
    ) {
        if (loopbackMode) {
            _statusBannerMessage.value =
                "Loopback Sandbox Mode is ON: Local HTTP 200 OK verification succeeded!"
            return
        }
        val cleanUrl = webhookUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            _statusBannerMessage.value = "Please enter a valid http:// or https:// Webhook URL first."
            return
        }

        _isTestingWebhook.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val jsonPayload = HmacSigner.buildWebhookJsonPayload(
                    platform = "bKash",
                    amount = 1.00,
                    senderNumber = "01700000000",
                    senderLast4 = "0000",
                    trxId = "PING${now.toString().takeLast(6)}",
                    reference = "ping_test",
                    receivedAt = now
                )
                val sig = HmacSigner.generateHmacSha256(jsonPayload, secretKey)
                val concatSig = HmacSigner.generateHmacSha256(
                    HmacSigner.buildConcatSigningString(
                        "PING${now.toString().takeLast(6)}",
                        1.00,
                        now
                    ),
                    secretKey
                )
                val fullUrl = WebhookSyncWorker.appendQuerySignatureForAppsScript(
                    baseUrl = cleanUrl,
                    signature = sig,
                    payloadSignature = concatSig
                )

                val request = Request.Builder()
                    .url(fullUrl)
                    .post(jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .header("Content-Type", "application/json")
                    .header("X-Signature", sig)
                    .header("X-Payload-Signature", concatSig)
                    .build()

                WebhookSyncWorker.httpClient.newCall(request).execute().use { response ->
                    val bodySnippet = response.body?.string()?.take(120)?.trim().orEmpty()
                    if (response.isSuccessful) {
                        _statusBannerMessage.value =
                            "Webhook Ping Success (HTTP ${response.code}): ${bodySnippet.ifEmpty { "OK" }}"
                    } else {
                        _statusBannerMessage.value =
                            "Webhook returned HTTP ${response.code}: ${bodySnippet.ifEmpty { response.message }}"
                    }
                }
            } catch (e: Exception) {
                _statusBannerMessage.value =
                    "Webhook Ping Failed: ${e.localizedMessage ?: "Connection error"}"
            } finally {
                _isTestingWebhook.value = false
            }
        }
    }

    fun toggleForegroundService(enable: Boolean) {
        val appContext = getApplication<Application>()
        if (enable) {
            PaymentVerifierService.startService(appContext)
            preferences.setServiceEnabled(true)
            _statusBannerMessage.value = "24/7 Foreground Service ('Payment Listener Active') started."
        } else {
            PaymentVerifierService.stopService(appContext)
            preferences.setServiceEnabled(false)
            _statusBannerMessage.value = "24/7 Foreground Service stopped."
        }
    }

    fun simulateIncomingSms(senderAddress: String, smsBody: String, onSuccessNavigateToQueue: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val cfg = preferences.readConfig()
            val allowed = SmsParser.isAllowedSender(senderAddress, cfg.testPhoneNumber)
            if (!allowed) {
                _statusBannerMessage.value =
                    "Blocked by Sender ID Filter: '$senderAddress' is not bKash, NAGAD, 16216, or Test Phone (${cfg.testPhoneNumber})."
                return@launch
            }

            val parsed: ParsedPaymentSms? = SmsParser.parsePaymentSms(senderAddress, smsBody)
            if (parsed == null) {
                _statusBannerMessage.value =
                    "Regex Parser could not extract all required fields (Amount, Sender, TrxID)."
                return@launch
            }

            val rowId = repository.enqueuePaymentSms(
                parsed = parsed,
                rawSenderAddress = senderAddress,
                rawSmsBody = smsBody,
                secretKey = cfg.secretKey
            )
            if (rowId == -1L) {
                _statusBannerMessage.value =
                    "Duplicate TrxID '${parsed.trxId}' ignored (already stored in Room database)."
            } else {
                _statusBannerMessage.value =
                    "Captured ${parsed.platform} Tk ${parsed.amount} (TrxID: ${parsed.trxId}) -> Saved as UNSYNCED & WorkManager triggered!"
                launch(Dispatchers.Main) {
                    onSuccessNavigateToQueue()
                }
            }
        }
    }

    fun syncAllPendingNow() {
        repository.triggerSyncNow()
        _statusBannerMessage.value = "WorkManager task triggered to POST unsynced payments with X-Signature."
    }

    fun retrySinglePayment(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.retryPaymentSync(id)
            _statusBannerMessage.value = "Payment queued for immediate WorkManager retry."
        }
    }

    fun deletePayment(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePayment(id)
        }
    }

    fun clearSyncedHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearSyncedPayments()
            _statusBannerMessage.value = "Cleared all SYNCED transactions from local Room database."
        }
    }
}
