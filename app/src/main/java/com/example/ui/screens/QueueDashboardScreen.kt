package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PaymentSmsEntity
import com.example.data.local.VerifierConfig
import com.example.domain.HmacSigner
import com.example.ui.QueueFilter
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber900
import com.example.ui.theme.AmberBorder
import com.example.ui.theme.BkashPink
import com.example.ui.theme.BkashPinkSoft
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald900
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.NagadOrangeSoft
import com.example.ui.theme.RocketPurple
import com.example.ui.theme.RocketPurpleSoft
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QueueDashboardScreen(
    payments: List<PaymentSmsEntity>,
    pendingCount: Int,
    syncedCount: Int,
    totalSyncedVolume: Double,
    selectedFilter: QueueFilter,
    searchQuery: String,
    config: VerifierConfig,
    hasSmsPermission: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestIgnoreBatteryOptimizations: () -> Unit,
    onToggleService: (Boolean) -> Unit,
    onFilterChange: (QueueFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSyncAllNow: () -> Unit,
    onRetryPayment: (Long) -> Unit,
    onDeletePayment: (Long) -> Unit,
    onClearSynced: () -> Unit,
    onOpenSimulator: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedItemForInspect by remember { mutableStateOf<PaymentSmsEntity?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sleek Bento Telemetry & Listener Control Card
        item {
            FintechCommandHeroCard(
                config = config,
                totalSyncedVolume = totalSyncedVolume,
                syncedCount = syncedCount,
                pendingCount = pendingCount,
                hasSmsPermission = hasSmsPermission,
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                onRequestPermissions = onRequestPermissions,
                onRequestIgnoreBatteryOptimizations = onRequestIgnoreBatteryOptimizations,
                onToggleService = onToggleService,
                onSyncAllNow = onSyncAllNow
            )
        }

        // 2. Whitelisted Senders & Search / Segmented Filter Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WhitelistedSendersBar(testPhoneNumber = config.testPhoneNumber)

                SleekSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SegmentedQueueFilterBar(
                        selectedFilter = selectedFilter,
                        allCount = syncedCount + pendingCount,
                        unsyncedCount = pendingCount,
                        syncedCount = syncedCount,
                        onFilterSelect = onFilterChange,
                        modifier = Modifier.weight(1f)
                    )

                    if (syncedCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate400,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onClearSynced() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 3. Transaction Queue List or Clean Empty State
        if (payments.isEmpty()) {
            item {
                RefinedEmptyQueueState(onOpenSimulator = onOpenSimulator)
            }
        } else {
            items(payments, key = { it.id }) { payment ->
                RefinedPaymentCard(
                    payment = payment,
                    onClickInspect = { selectedItemForInspect = payment },
                    onRetry = { onRetryPayment(payment.id) },
                    onDelete = { onDeletePayment(payment.id) }
                )
            }
        }
    }

    selectedItemForInspect?.let { item ->
        PaymentPayloadInspectorDialog(
            payment = item,
            secretKey = config.secretKey,
            onDismiss = { selectedItemForInspect = null },
            onRetryNow = {
                onRetryPayment(item.id)
                selectedItemForInspect = null
            }
        )
    }
}

@Composable
private fun FintechCommandHeroCard(
    config: VerifierConfig,
    totalSyncedVolume: Double,
    syncedCount: Int,
    pendingCount: Int,
    hasSmsPermission: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestIgnoreBatteryOptimizations: () -> Unit,
    onToggleService: (Boolean) -> Unit,
    onSyncAllNow: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )

    val statusColor = if (config.isServiceEnabled) Emerald400 else Amber400
    val borderColor = if (config.isServiceEnabled) EmeraldBorder.copy(alpha = 0.55f) else Slate800

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Slate900)
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .drawBehind {
                // Subtle ambient top-right radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.15f),
                        radius = size.width * 0.65f
                    )
                )
            }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            // Top Row: Pulsing Live Status Badge + 1-Tap Listener Power Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(18.dp)) {
                        if (config.isServiceEnabled) {
                            Box(
                                modifier = Modifier
                                    .size((8 * pulseScale).dp)
                                    .clip(CircleShape)
                                    .background(Emerald400.copy(alpha = pulseAlpha))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                    }

                    Column {
                        Text(
                            text = if (config.isServiceEnabled) "LISTENER ACTIVE • 24/7" else "LISTENER STANDBY",
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (config.loopbackSimulationMode) {
                                "Mode: Local Sandbox (200 OK)"
                            } else {
                                "Webhook: ${config.webhookUrl.removePrefix("https://").removePrefix("http://").take(28)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Clean Pill Toggle Button for 24/7 Service
                Surface(
                    onClick = { onToggleService(!config.isServiceEnabled) },
                    color = if (config.isServiceEnabled) Emerald900 else Slate850,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (config.isServiceEnabled) EmeraldBorder else Slate700,
                            RoundedCornerShape(50)
                        )
                        .testTag("toggle_service_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Toggle 24/7 Listener",
                            tint = if (config.isServiceEnabled) Emerald400 else Slate200,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (config.isServiceEnabled) "Active" else "Start 24/7",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (config.isServiceEnabled) Emerald400 else Slate50
                        )
                    }
                }
            }

            // Middle Row: Editorial BDT Volume + Bento Mini Counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "VERIFIED VOLUME",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "৳",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Emerald400,
                            modifier = Modifier.padding(bottom = 3.dp, end = 3.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%,.2f", totalSyncedVolume),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                            color = Slate50
                        )
                    }
                }

                // Sync Queue Action Pill
                Surface(
                    onClick = onSyncAllNow,
                    color = Slate850,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                        .testTag("sync_now_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Queue",
                            tint = Cyan400,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Sync Queue",
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate200
                        )
                    }
                }
            }

            // Bento Stat Strip (Synced vs Unsynced Queue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoCounterTile(
                    label = "SYNCED (HTTP 200)",
                    count = syncedCount,
                    dotColor = Emerald400,
                    modifier = Modifier.weight(1f)
                )
                BentoCounterTile(
                    label = "UNSYNCED QUEUE",
                    count = pendingCount,
                    dotColor = if (pendingCount > 0) Amber400 else Slate500,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = Slate800.copy(alpha = 0.7f), thickness = 1.dp)

            // Bottom System Readiness Strip (Permissions & Battery Unthrottling)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SystemHealthPill(
                    icon = if (hasSmsPermission) Icons.Default.VerifiedUser else Icons.Default.Sms,
                    label = if (hasSmsPermission) "SMS Access Ready" else "Grant SMS Access",
                    isHealthy = hasSmsPermission,
                    onClick = onRequestPermissions,
                    modifier = Modifier.weight(1f)
                )
                SystemHealthPill(
                    icon = Icons.Default.BatteryChargingFull,
                    label = if (isIgnoringBatteryOptimizations) "Battery Unrestricted" else "Unrestrict Battery",
                    isHealthy = isIgnoringBatteryOptimizations,
                    onClick = onRequestIgnoreBatteryOptimizations,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BentoCounterTile(
    label: String,
    count: Int,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Slate850.copy(alpha = 0.7f))
            .border(1.dp, Slate800.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Slate400
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = Slate50
            )
        }
    }
}

@Composable
private fun SystemHealthPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isHealthy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (isHealthy) Emerald400 else Amber400
    val bg = if (isHealthy) Emerald900.copy(alpha = 0.35f) else Amber900.copy(alpha = 0.45f)
    val border = if (isHealthy) EmeraldBorder.copy(alpha = 0.4f) else AmberBorder.copy(alpha = 0.6f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = if (isHealthy) Slate200 else Amber400,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WhitelistedSendersBar(testPhoneNumber: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "LISTENING TO",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Slate500,
            letterSpacing = 0.8.sp
        )
        SenderPill(name = "bKash", dotColor = BkashPink)
        SenderPill(name = "NAGAD", dotColor = NagadOrange)
        SenderPill(name = "16216 (Rocket)", dotColor = RocketPurple)
        if (testPhoneNumber.isNotBlank()) {
            SenderPill(name = "Test: $testPhoneNumber", dotColor = Emerald400)
        }
    }
}

@Composable
private fun SenderPill(name: String, dotColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Slate900)
            .border(1.dp, Slate800, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = Slate200
        )
    }
}

@Composable
private fun SleekSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Slate900)
            .border(1.dp, Slate800, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search transactions",
            tint = Slate500,
            modifier = Modifier.size(18.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Filter by TrxID, Reference (tbuddy1), or Sender...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Slate50),
                cursorBrush = SolidColor(Emerald400),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_queue_input")
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear search",
                tint = Slate400,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onQueryChange("") }
            )
        }
    }
}

@Composable
private fun SegmentedQueueFilterBar(
    selectedFilter: QueueFilter,
    allCount: Int,
    unsyncedCount: Int,
    syncedCount: Int,
    onFilterSelect: (QueueFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Slate900)
            .border(1.dp, Slate800, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SegmentedTabItem(
            label = "All ($allCount)",
            selected = selectedFilter == QueueFilter.ALL,
            onClick = { onFilterSelect(QueueFilter.ALL) },
            modifier = Modifier.weight(1f)
        )
        SegmentedTabItem(
            label = "Unsynced ($unsyncedCount)",
            selected = selectedFilter == QueueFilter.PENDING,
            onClick = { onFilterSelect(QueueFilter.PENDING) },
            modifier = Modifier.weight(1f)
        )
        SegmentedTabItem(
            label = "Synced ($syncedCount)",
            selected = selectedFilter == QueueFilter.SYNCED,
            onClick = { onFilterSelect(QueueFilter.SYNCED) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentedTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) Slate800 else Color.Transparent,
        label = "segBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Slate50 else Slate400,
        label = "segText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun RefinedPaymentCard(
    payment: PaymentSmsEntity,
    onClickInspect: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val isSynced = payment.status == PaymentSmsEntity.STATUS_SYNCED
    val (monogram, brandColor, brandBg) = when (payment.platform.lowercase(Locale.ROOT)) {
        "bkash" -> Triple("bK", BkashPink, BkashPinkSoft)
        "nagad" -> Triple("Ng", NagadOrange, NagadOrangeSoft)
        else -> Triple("Rk", RocketPurple, RocketPurpleSoft)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Slate900)
            .border(1.dp, Slate800, RoundedCornerShape(18.dp))
            .clickable { onClickInspect() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Row 1: Brand Squircle + Platform & TrxID + Right-aligned Amount & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Clean Brand Monogram Squircle
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(brandBg)
                            .border(1.dp, brandColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = monogram,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = brandColor
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = payment.platform,
                                style = MaterialTheme.typography.titleMedium,
                                color = Slate50
                            )
                            Surface(
                                color = Slate850,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.border(1.dp, Slate800, RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = payment.trxId,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = Cyan400,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "From ${payment.senderNumber} • Last 4: ${payment.senderLast4}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "+৳${String.format(Locale.US, "%,.2f", payment.amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = Slate50
                    )

                    // Status Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSynced) Emerald900 else Amber900)
                            .border(
                                1.dp,
                                if (isSynced) EmeraldBorder else AmberBorder,
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isSynced) Icons.Default.CheckCircle else Icons.Default.Schedule,
                            contentDescription = payment.status,
                            tint = if (isSynced) Emerald400 else Amber400,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = payment.status,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isSynced) Emerald400 else Amber400
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate800.copy(alpha = 0.6f), thickness = 1.dp)

            // Row 2: Order Reference Chip + Timestamp + Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = Slate850,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (payment.reference.isNotBlank()) {
                                "Ref: ${payment.reference}"
                            } else {
                                "Ref: None (TrxID Match)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (payment.reference.isNotBlank()) Emerald400 else Slate400,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = formatTimestamp(payment.receivedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isSynced) {
                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry webhook sync",
                                tint = Amber400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete record",
                            tint = Slate500,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RefinedEmptyQueueState(onOpenSimulator: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Slate900)
            .border(1.dp, Slate800, RoundedCornerShape(20.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Emerald900.copy(alpha = 0.5f))
                    .border(1.dp, EmeraldBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = "Waiting for SMS",
                    tint = Emerald400,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Waiting for Incoming Wallet SMS",
                style = MaterialTheme.typography.titleMedium,
                color = Slate50
            )

            Text(
                text = "Incoming payment confirmations from bKash, NAGAD, 16216, or your Test Phone will automatically appear here and sync to your webhook.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400
            )

            Spacer(modifier = Modifier.height(2.dp))

            Button(
                onClick = onOpenSimulator,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald500,
                    contentColor = Slate950
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Launch SMS Simulator",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.NorthEast,
                    contentDescription = "Open Simulator",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PaymentPayloadInspectorDialog(
    payment: PaymentSmsEntity,
    secretKey: String,
    onDismiss: () -> Unit,
    onRetryNow: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val jsonPayload = remember(payment) {
        HmacSigner.buildWebhookJsonPayload(
            platform = payment.platform,
            amount = payment.amount,
            senderNumber = payment.senderNumber,
            senderLast4 = payment.senderLast4,
            trxId = payment.trxId,
            reference = payment.reference,
            receivedAt = payment.receivedAt
        )
    }
    val computedSignature = remember(jsonPayload, secretKey) {
        HmacSigner.generateHmacSha256(jsonPayload, secretKey)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Webhook Payload Inspector",
                    tint = Emerald400
                )
                Text(
                    text = "${payment.platform} • ${payment.trxId}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Slate50
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InspectorSection(
                    label = "RAW SMS (${payment.rawSenderAddress})",
                    content = payment.rawSmsBody,
                    contentColor = Slate200,
                    onCopy = { clipboardManager.setText(AnnotatedString(payment.rawSmsBody)) }
                )

                InspectorSection(
                    label = "WEBHOOK JSON PAYLOAD",
                    content = jsonPayload,
                    contentColor = Cyan400,
                    isMono = true,
                    onCopy = { clipboardManager.setText(AnnotatedString(jsonPayload)) }
                )

                InspectorSection(
                    label = "X-SIGNATURE (HMAC-SHA256)",
                    content = computedSignature,
                    contentColor = Emerald400,
                    isMono = true,
                    onCopy = { clipboardManager.setText(AnnotatedString(computedSignature)) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRetryNow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald500,
                    contentColor = Slate950
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Re-Send Webhook POST")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Slate400)
            }
        }
    )
}

@Composable
private fun InspectorSection(
    label: String,
    content: String,
    contentColor: Color,
    isMono: Boolean = false,
    onCopy: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy $label",
                tint = Slate400,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onCopy() }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Slate950)
                .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Text(
                text = content,
                style = if (isMono) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                color = contentColor
            )
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
    return sdf.format(Date(epochMillis))
}
