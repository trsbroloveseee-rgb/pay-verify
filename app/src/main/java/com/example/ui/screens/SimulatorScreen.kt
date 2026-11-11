package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.HmacSigner
import com.example.domain.SmsParser
import com.example.ui.theme.Amber400
import com.example.ui.theme.BkashPink
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald900
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.RocketPurple
import com.example.ui.theme.Rose400
import com.example.ui.theme.Rose900
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import java.util.Locale

@Composable
fun SimulatorScreen(
    senderInput: String,
    smsBodyInput: String,
    customTestPhone: String,
    secretKey: String,
    onSenderChange: (String) -> Unit,
    onSmsBodyChange: (String) -> Unit,
    onSimulateIncomingSms: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val isSenderAllowed = remember(senderInput, customTestPhone) {
        SmsParser.isAllowedSender(senderInput, customTestPhone)
    }

    val parsedResult = remember(senderInput, smsBodyInput) {
        SmsParser.parsePaymentSms(senderInput, smsBodyInput)
    }

    val previewJson = remember(parsedResult) {
        parsedResult?.let {
            HmacSigner.buildWebhookJsonPayload(
                platform = it.platform,
                amount = it.amount,
                senderNumber = it.senderNumber,
                senderLast4 = it.senderLast4,
                trxId = it.trxId,
                reference = it.reference,
                receivedAt = System.currentTimeMillis()
            )
        } ?: ""
    }

    val previewHmac = remember(previewJson, secretKey) {
        if (previewJson.isNotEmpty()) {
            HmacSigner.generateHmacSha256(previewJson, secretKey)
        } else {
            ""
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Slate850,
        unfocusedContainerColor = Slate850,
        focusedBorderColor = Emerald400,
        unfocusedBorderColor = Slate800,
        focusedLabelColor = Emerald400,
        unfocusedLabelColor = Slate400
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Slate900)
                    .border(1.dp, Slate800, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Emerald900)
                                .border(1.dp, EmeraldBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "SMS Simulator",
                                tint = Emerald400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SMS Regex & Pipeline Simulator",
                                style = MaterialTheme.typography.titleLarge,
                                color = Slate50
                            )
                            Text(
                                text = "Select a wallet template or type a custom SMS to test extraction & Room sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }

                    // Clean 1-Tap Template Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SimulatorPresetPill(
                            label = "bKash • Ref tbuddy1",
                            dotColor = BkashPink,
                            onClick = {
                                onSenderChange("bKash")
                                onSmsBodyChange(SmsParser.SAMPLE_BKASH_SMS)
                            }
                        )
                        SimulatorPresetPill(
                            label = "Nagad • Masked",
                            dotColor = NagadOrange,
                            onClick = {
                                onSenderChange("NAGAD")
                                onSmsBodyChange(SmsParser.SAMPLE_NAGAD_SMS)
                            }
                        )
                        SimulatorPresetPill(
                            label = "Rocket • 16216",
                            dotColor = RocketPurple,
                            onClick = {
                                onSenderChange("16216")
                                onSmsBodyChange(SmsParser.SAMPLE_ROCKET_SMS)
                            }
                        )
                        SimulatorPresetPill(
                            label = "Test Phone + New TrxID",
                            dotColor = Emerald400,
                            onClick = {
                                onSenderChange(customTestPhone.ifBlank { "01700000000" })
                                val randomSuffix = (100..999).random()
                                onSmsBodyChange(
                                    "You have received Tk 500.00 from 01712345678. Ref tbuddy1. Fee Tk 0.00. Balance Tk 1500.00. TrxID BJK4P9X$randomSuffix at 10/11/2026 22:30"
                                )
                            }
                        )
                        SimulatorPresetPill(
                            label = "No-Ref (Agent Cash-In)",
                            dotColor = Cyan400,
                            onClick = {
                                onSenderChange("bKash")
                                onSmsBodyChange(SmsParser.SAMPLE_NO_REF_SMS)
                            }
                        )
                    }

                    OutlinedTextField(
                        value = senderInput,
                        onValueChange = onSenderChange,
                        label = { Text("Originating Sender ID") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = "Sender ID",
                                tint = Slate400
                            )
                        },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sim_sender_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = smsBodyInput,
                        onValueChange = onSmsBodyChange,
                        label = { Text("Incoming SMS Body") },
                        minLines = 3,
                        maxLines = 5,
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sim_body_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Button(
                        onClick = onSimulateIncomingSms,
                        enabled = isSenderAllowed && parsedResult != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("simulate_sms_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald500,
                            contentColor = Slate950
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Inject SMS",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Inject SMS into Room Queue & Sync",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Live Extraction Breakdown Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Slate900)
                    .border(1.dp, Slate800, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE REGEX EXTRACTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                            letterSpacing = 1.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSenderAllowed) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = "Sender status",
                                tint = if (isSenderAllowed) Emerald400 else Rose400,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isSenderAllowed) "Sender Allowed" else "Sender Blocked",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSenderAllowed) Emerald400 else Rose400
                            )
                        }
                    }

                    if (parsedResult != null) {
                        ParsedKeyValueRow("Platform", parsedResult.platform, Emerald400)
                        ParsedKeyValueRow(
                            "Amount",
                            "৳ ${String.format(Locale.US, "%.2f", parsedResult.amount)}",
                            Slate50
                        )
                        ParsedKeyValueRow(
                            "TrxID (Uppercase)",
                            parsedResult.trxId,
                            Cyan400
                        )
                        ParsedKeyValueRow(
                            "Order Reference",
                            parsedResult.reference.ifEmpty { "None (Matches by TrxID)" },
                            if (parsedResult.reference.isNotEmpty()) Emerald400 else Amber400
                        )
                        ParsedKeyValueRow(
                            "Sender • Last 4",
                            "${parsedResult.senderNumber}  (Last 4: ${parsedResult.senderLast4})",
                            Slate200
                        )

                        HorizontalDivider(color = Slate800, thickness = 1.dp)

                        Text(
                            text = "X-SIGNATURE (HMAC-SHA256)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Slate950)
                                .border(1.dp, Slate800, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = previewHmac,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = Emerald400
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Rose900.copy(alpha = 0.5f))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Regex could not extract Amount, Sender Number, and TrxID from this SMS text.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Rose400
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimulatorPresetPill(
    label: String,
    dotColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Slate850)
            .border(1.dp, Slate700.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Slate200
        )
    }
}

@Composable
private fun ParsedKeyValueRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Slate850.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Slate400
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = JetBrainsMonoFontFamily,
            color = valueColor
        )
    }
}
