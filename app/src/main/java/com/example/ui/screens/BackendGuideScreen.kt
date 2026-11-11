package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald900
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun BackendGuideScreen(
    secretKey: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val clipboard = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val appsScriptSnippet = remember(secretKey) {
        """
// ============================================================================
// GOOGLE APPS SCRIPT (Google Sheets Webhook Backend)
// Sheets required:
// 1. "SMS_Logs" -> [Timestamp, Platform, TrxID, Amount, Reference, SenderNumber, SenderLast4, MatchStatus]
// 2. "Orders"   -> [OrderID, CustomerRef, ExpectedAmount, SubmittedTrxID, SenderLast4, OrderStatus, VerifiedAt]
// ============================================================================

const SECRET_KEY = "$secretKey";

function toHexHmacSha256(message, key) {
  const rawSig = Utilities.computeHmacSha256Signature(message, key);
  return rawSig.map(b => ('0' + (b & 0xFF).toString(16)).slice(-2)).join('');
}

function doPost(e) {
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const rawBody = e.postData.contents;
    const data = JSON.parse(rawBody);

    // 1. Verify HMAC-SHA256 Signature
    const incomingSig = (e.parameter.signature || "").toLowerCase();
    const incomingPayloadSig = (e.parameter.payloadSig || "").toLowerCase();

    const expectedJsonSig = toHexHmacSha256(rawBody, SECRET_KEY);
    const concatData = String(data.trxId).trim().toUpperCase() +
                       Number(data.amount).toFixed(2) +
                       String(data.timestamp);
    const expectedPayloadSig = toHexHmacSha256(concatData, SECRET_KEY);

    if (incomingSig !== expectedJsonSig && incomingPayloadSig !== expectedPayloadSig) {
      return ContentService.createTextOutput(JSON.stringify({ error: "Invalid HMAC Signature" }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    const ss = SpreadsheetApp.getActiveSpreadsheet();
    const smsSheet = ss.getSheetByName("SMS_Logs") || ss.insertSheet("SMS_Logs");
    const ordersSheet = ss.getSheetByName("Orders") || ss.insertSheet("Orders");

    const trxId = String(data.trxId || "").trim().toUpperCase();
    const amount = Number(data.amount || 0);
    const reference = String(data.reference || "").trim().toLowerCase();
    const senderLast4 = String(data.senderLast4 || "").trim();

    // Prevent duplicate TrxID replay
    const existingLogs = smsSheet.getDataRange().getValues();
    for (let i = 1; i < existingLogs.length; i++) {
      if (String(existingLogs[i][2]).toUpperCase() === trxId) {
        return ContentService.createTextOutput(JSON.stringify({ status: "SYNCED", note: "Duplicate TrxID already logged" }))
          .setMimeType(ContentService.MimeType.JSON);
      }
    }

    // 2. Two-Way Order Matching against "Orders" sheet (by Reference OR by TrxID)
    let matchedOrderId = "";
    const orders = ordersSheet.getDataRange().getValues();
    for (let r = 1; r < orders.length; r++) {
      const [orderId, orderRef, expectedAmount, submittedTrxId, orderLast4, orderStatus] = orders[r];
      if (String(orderStatus).toUpperCase() === "PAID") continue;
      if (Number(expectedAmount) !== amount) continue;

      const refMatches = reference !== "" &&
        String(orderRef).trim().toLowerCase() === reference &&
        (String(orderLast4).trim() === "" || String(orderLast4).trim() === senderLast4);

      const trxMatches = String(submittedTrxId).trim().toUpperCase() === trxId;

      if (refMatches || trxMatches) {
        matchedOrderId = String(orderId);
        ordersSheet.getRange(r + 1, 4).setValue(trxId);
        ordersSheet.getRange(r + 1, 6).setValue("PAID");
        ordersSheet.getRange(r + 1, 7).setValue(new Date().toISOString());
        break;
      }
    }

    // 3. Log incoming SMS in "SMS_Logs"
    smsSheet.appendRow([
      new Date(Number(data.timestamp || Date.now())).toISOString(),
      data.platform,
      trxId,
      amount,
      data.reference || "",
      data.senderNumber,
      senderLast4,
      matchedOrderId ? "MATCHED_ORDER_" + matchedOrderId : "LOGGED_UNMATCHED"
    ]);

    return ContentService.createTextOutput(JSON.stringify({
      status: "SYNCED",
      trxId: trxId,
      matchedOrder: matchedOrderId || null
    })).setMimeType(ContentService.MimeType.JSON);
  } finally {
    lock.releaseLock();
  }
}
        """.trimIndent()
    }

    val nodeExpressSnippet = remember(secretKey) {
        """
// ============================================================================
// NODE.JS / EXPRESS WEBHOOK (Logs to SMS_Logs & Two-Way Matches Orders)
// ============================================================================
const express = require('express');
const crypto = require('crypto');
const app = express();

const SECRET_KEY = '$secretKey';

app.use(express.json({
  verify: (req, res, buf) => { req.rawBody = buf.toString('utf8'); }
}));

app.post('/api/webhook/payment-verify', async (req, res) => {
  const signatureHeader = (req.headers['x-signature'] || '').toLowerCase();
  const payloadSigHeader = (req.headers['x-payload-signature'] || '').toLowerCase();

  const expectedJsonHmac = crypto
    .createHmac('sha256', SECRET_KEY)
    .update(req.rawBody || '')
    .digest('hex');

  const { platform, amount, senderNumber, senderLast4, trxId, reference, timestamp } = req.body;
  const normalizedTrx = String(trxId || '').trim().toUpperCase();
  const concatString = `${'$'}{normalizedTrx}${'$'}{Number(amount).toFixed(2)}${'$'}{timestamp}`;
  const expectedPayloadHmac = crypto
    .createHmac('sha256', SECRET_KEY)
    .update(concatString)
    .digest('hex');

  if (signatureHeader !== expectedJsonHmac && payloadSigHeader !== expectedPayloadHmac) {
    return res.status(401).json({ error: 'Invalid X-Signature HMAC-SHA256' });
  }

  // 1. Insert into SMS_Logs table (ignore if TrxID already exists)
  // 2. Two-Way Match against Pending Orders table:
  //    WHERE status = 'PENDING' AND amount = :amount
  //      AND ((reference = :reference AND :reference != '') OR submitted_trx_id = :normalizedTrx)
  // 3. Mark Order as PAID and release product!

  return res.status(200).json({ status: 'SYNCED', trxId: normalizedTrx });
});
        """.trimIndent()
    }

    val activeCode = if (selectedTab == 0) appsScriptSnippet else nodeExpressSnippet

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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Backend Reference",
                                    tint = Emerald400,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Server Webhook Scripts",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Slate50
                                )
                                Text(
                                    text = "Pre-configured with your HMAC Secret Key",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                        }

                        Surface(
                            onClick = { clipboard.setText(AnnotatedString(activeCode)) },
                            color = Emerald900,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.border(1.dp, EmeraldBorder, RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = Emerald400,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Copy",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Emerald400
                                )
                            }
                        }
                    }

                    // Segmented Script Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate850)
                            .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CodeTabPill(
                            label = "Google Apps Script",
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        CodeTabPill(
                            label = "Node.js / Express",
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Slate950)
                            .border(1.dp, Slate800, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = activeCode,
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

@Composable
private fun CodeTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) Slate900 else Color.Transparent,
        label = "codeTabBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Slate50 else Slate400,
        label = "codeTabFg"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg
        )
    }
}
