package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.VerifierConfig
import com.example.domain.HmacSigner
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald900
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun WebhookConfigScreen(
    config: VerifierConfig,
    isTestingWebhook: Boolean,
    onSaveConfig: (webhookUrl: String, secretKey: String, testPhone: String, loopbackMode: Boolean) -> Unit,
    onTestWebhook: (webhookUrl: String, secretKey: String, loopbackMode: Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    var webhookUrl by remember(config.webhookUrl) { mutableStateOf(config.webhookUrl) }
    var secretKey by remember(config.secretKey) { mutableStateOf(config.secretKey) }
    var testPhone by remember(config.testPhoneNumber) { mutableStateOf(config.testPhoneNumber) }
    var loopbackMode by remember(config.loopbackSimulationMode) {
        mutableStateOf(config.loopbackSimulationMode)
    }

    val sampleJson = remember {
        HmacSigner.buildWebhookJsonPayload(
            platform = "bKash",
            amount = 500.00,
            senderNumber = "01712345678",
            senderLast4 = "5678",
            trxId = "BJK4P9X2T1",
            reference = "tbuddy1",
            receivedAt = 1794330600000L
        )
    }
    val sampleSignature = remember(sampleJson, secretKey) {
        HmacSigner.generateHmacSha256(sampleJson, secretKey)
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
                                imageVector = Icons.Default.Security,
                                contentDescription = "Webhook Config",
                                tint = Emerald400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Endpoint & Security Configuration",
                                style = MaterialTheme.typography.titleLarge,
                                color = Slate50
                            )
                            Text(
                                text = "Stored locally in SharedPreferences for background Worker sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }

                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { webhookUrl = it },
                        label = { Text("Server Webhook URL (POST)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Link, contentDescription = "Webhook URL", tint = Slate400)
                        },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("webhook_url_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = secretKey,
                        onValueChange = { secretKey = it },
                        label = { Text("HMAC-SHA256 Secret Key (X-Signature)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Key, contentDescription = "Secret Key", tint = Slate400)
                        },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("secret_key_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = testPhone,
                        onValueChange = { testPhone = it },
                        label = { Text("Test Sender Phone Number") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = "Test Phone", tint = Slate400)
                        },
                        supportingText = {
                            Text(
                                text = "Whitelists your second phone number for live SMS testing",
                                color = Slate500
                            )
                        },
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_phone_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Slate850)
                            .border(1.dp, Slate800, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Sandbox Loopback Mode (HTTP 200)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Slate50
                            )
                            Text(
                                text = "Simulates HTTP 200 OK locally without an external server",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                        Switch(
                            checked = loopbackMode,
                            onCheckedChange = { loopbackMode = it },
                            modifier = Modifier.testTag("loopback_mode_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveConfig(webhookUrl, secretKey, testPhone, loopbackMode)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_webhook_config_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald500,
                                contentColor = Slate950
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save configuration",
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Config", style = MaterialTheme.typography.labelLarge)
                        }

                        FilledTonalButton(
                            onClick = {
                                onSaveConfig(webhookUrl, secretKey, testPhone, loopbackMode)
                                onTestWebhook(webhookUrl, secretKey, loopbackMode)
                            },
                            enabled = !isTestingWebhook,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("test_webhook_ping_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = "Ping Webhook",
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTestingWebhook) "Pinging..." else "Ping Webhook",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        // Contract Preview
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Slate900)
                    .border(1.dp, Slate800, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "WEBHOOK PAYLOAD & SIGNATURE CONTRACT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate950)
                            .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "X-Signature: $sampleSignature",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = Emerald400
                            )
                            Text(
                                text = sampleJson,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = Cyan400
                            )
                        }
                    }
                }
            }
        }
    }
}
