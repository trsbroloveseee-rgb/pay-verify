package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PaymentVerifierViewModel
import com.example.ui.screens.BackendGuideScreen
import com.example.ui.screens.QueueDashboardScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.screens.WebhookConfigScreen
import com.example.ui.theme.Amber400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald900
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.PayPulseTheme
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PayPulseTheme(darkTheme = true) {
                PayPulseApp()
            }
        }
    }
}

@Composable
fun PayPulseApp(
    viewModel: PaymentVerifierViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentTab by rememberSaveable { mutableIntStateOf(0) }

    val payments by viewModel.filteredPayments.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val syncedCount by viewModel.syncedCount.collectAsStateWithLifecycle()
    val totalSyncedVolume by viewModel.totalSyncedVolume.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val config by viewModel.configState.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val isTestingWebhook by viewModel.isTestingWebhook.collectAsStateWithLifecycle()
    val simSender by viewModel.simSender.collectAsStateWithLifecycle()
    val simBody by viewModel.simBody.collectAsStateWithLifecycle()

    var hasSmsPermission by remember {
        mutableStateOf(checkSmsPermission(context))
    }
    var isIgnoringBatteryOpt by remember {
        mutableStateOf(checkBatteryOptimizationIgnored(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSmsPermission = checkSmsPermission(context)
                isIgnoringBatteryOpt = checkBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasSmsPermission = checkSmsPermission(context)
    }

    val requestRuntimePermissions = {
        val permsToRequest = buildList {
            add(Manifest.permission.RECEIVE_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        permissionsLauncher.launch(permsToRequest)
    }

    val requestIgnoreBatteryOpt = {
        requestIgnoreBatteryOptimizations(context)
    }

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Emerald400,
        selectedTextColor = Slate50,
        indicatorColor = Emerald900,
        unselectedIconColor = Slate500,
        unselectedTextColor = Slate500
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950),
        containerColor = Slate950,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(Slate950)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Emerald900)
                                .border(1.dp, EmeraldBorder, RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "PayPulse BD",
                                tint = Emerald400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "PayPulse BD",
                                style = MaterialTheme.typography.titleLarge,
                                color = Slate50
                            )
                            Text(
                                text = "bKash • Nagad • Rocket Gateway",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }

                    // Compact Connection Pill in Header
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Slate900)
                            .border(1.dp, Slate800, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (config.isServiceEnabled) Emerald400 else Amber400)
                        )
                        Text(
                            text = if (config.isServiceEnabled) "24/7 ON" else "STANDBY",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate200
                        )
                    }
                }
                HorizontalDivider(color = Slate800.copy(alpha = 0.6f), thickness = 1.dp)
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = Slate800.copy(alpha = 0.7f), thickness = 1.dp)
                NavigationBar(
                    containerColor = Slate900,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Queue") },
                        label = {
                            Text(
                                text = if (pendingCount > 0) "Queue ($pendingCount)" else "Queue",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_tab_queue")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.Bolt, contentDescription = "Simulator") },
                        label = { Text("Simulator", style = MaterialTheme.typography.labelSmall) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_tab_simulator")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.Security, contentDescription = "Webhook") },
                        label = { Text("Webhook", style = MaterialTheme.typography.labelSmall) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_tab_webhook")
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Default.Code, contentDescription = "Backend") },
                        label = { Text("Backend", style = MaterialTheme.typography.labelSmall) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_tab_backend")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(visible = bannerMessage != null) {
                bannerMessage?.let { msg ->
                    Surface(
                        color = Emerald900,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .border(1.dp, EmeraldBorder, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = Emerald400,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.dismissBanner() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss notification",
                                    tint = Emerald400,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            when (currentTab) {
                0 -> QueueDashboardScreen(
                    payments = payments,
                    pendingCount = pendingCount,
                    syncedCount = syncedCount,
                    totalSyncedVolume = totalSyncedVolume,
                    selectedFilter = selectedFilter,
                    searchQuery = searchQuery,
                    config = config,
                    hasSmsPermission = hasSmsPermission,
                    isIgnoringBatteryOptimizations = isIgnoringBatteryOpt,
                    onRequestPermissions = requestRuntimePermissions,
                    onRequestIgnoreBatteryOptimizations = requestIgnoreBatteryOpt,
                    onToggleService = { enable ->
                        if (enable && !hasSmsPermission) {
                            requestRuntimePermissions()
                        }
                        viewModel.toggleForegroundService(enable)
                    },
                    onFilterChange = viewModel::setFilter,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    onSyncAllNow = viewModel::syncAllPendingNow,
                    onRetryPayment = viewModel::retrySinglePayment,
                    onDeletePayment = viewModel::deletePayment,
                    onClearSynced = viewModel::clearSyncedHistory,
                    onOpenSimulator = { currentTab = 1 },
                    modifier = Modifier.weight(1f)
                )

                1 -> SimulatorScreen(
                    senderInput = simSender,
                    smsBodyInput = simBody,
                    customTestPhone = config.testPhoneNumber,
                    secretKey = config.secretKey,
                    onSenderChange = viewModel::updateSimSender,
                    onSmsBodyChange = viewModel::updateSimBody,
                    onSimulateIncomingSms = {
                        viewModel.simulateIncomingSms(
                            senderAddress = simSender,
                            smsBody = simBody,
                            onSuccessNavigateToQueue = { currentTab = 0 }
                        )
                    },
                    onNavigateBack = { currentTab = 0 },
                    modifier = Modifier.weight(1f)
                )

                2 -> WebhookConfigScreen(
                    config = config,
                    isTestingWebhook = isTestingWebhook,
                    onSaveConfig = viewModel::saveWebhookConfiguration,
                    onTestWebhook = viewModel::testWebhookConnection,
                    onNavigateBack = { currentTab = 0 },
                    modifier = Modifier.weight(1f)
                )

                3 -> BackendGuideScreen(
                    secretKey = config.secretKey,
                    onNavigateBack = { currentTab = 0 },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun checkSmsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun checkBatteryOptimizationIgnored(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    try {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoring = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        val intent = if (!isIgnoring) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
        }
    }
}
