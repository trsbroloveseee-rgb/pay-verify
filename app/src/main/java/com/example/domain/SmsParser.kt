package com.example.domain

import java.util.Locale

/**
 * Holds the extracted fields from a mobile wallet confirmation SMS (bKash, Nagad, Rocket).
 */
data class ParsedPaymentSms(
    val platform: String,       // "bKash", "Nagad", or "Rocket"
    val amount: Double,         // e.g., 500.00
    val trxId: String,          // Trimmed & UPPERCASE alphanumeric code (e.g., "BJK4P9X2T1")
    val reference: String,      // Optional Reference (e.g., "tbuddy1"), or "" if missing/agent cash-in
    val senderNumber: String,   // Full number (e.g., "01712345678") or masked (e.g., "017****1234")
    val senderLast4: String     // Always the last 4 digits (e.g., "5678" or "1234")
)

/**
 * Sender ID Filter and Regex Parser for Bangladesh Mobile Wallets:
 * Supports bKash, Nagad, Rocket (16216), and a configurable Test Phone Number.
 */
object SmsParser {

    val OFFICIAL_SENDERS = setOf("BKASH", "NAGAD", "16216")

    /**
     * Requirement 1: Sender ID Filter
     * Checks if the SMS originating address matches official providers ("bKash", "NAGAD", "16216")
     * or matches the configurable Test Phone Number (ignoring +88 / 88 country prefix differences).
     */
    fun isAllowedSender(incomingAddress: String?, customTestNumber: String): Boolean {
        if (incomingAddress.isNullOrBlank()) return false
        val trimmedIncoming = incomingAddress.trim()
        val upperIncoming = trimmedIncoming.uppercase(Locale.ROOT)

        // 1. Check official sender IDs ("bKash", "NAGAD", "16216")
        if (OFFICIAL_SENDERS.any { official -> upperIncoming == official || upperIncoming.contains(official) }) {
            return true
        }

        // 2. Check configurable Test Phone Number
        val normalizedTest = normalizeBdPhoneNumber(customTestNumber)
        val normalizedIncoming = normalizeBdPhoneNumber(trimmedIncoming)
        if (normalizedTest.isNotEmpty() && normalizedIncoming.isNotEmpty()) {
            if (normalizedIncoming == normalizedTest ||
                (normalizedTest.length >= 10 && normalizedIncoming.endsWith(normalizedTest.takeLast(10)))
            ) {
                return true
            }
        }

        return false
    }

    /**
     * Normalizes Bangladesh phone numbers so "+8801712345678", "8801712345678", and "01712345678"
     * all normalize consistently to "01712345678".
     */
    fun normalizeBdPhoneNumber(phone: String): String {
        val digitsOnly = phone.filter { it.isDigit() }
        return when {
            digitsOnly.startsWith("880") && digitsOnly.length == 13 -> digitsOnly.removePrefix("88")
            digitsOnly.startsWith("01") && digitsOnly.length == 11 -> digitsOnly
            else -> digitsOnly
        }
    }

    /**
     * Requirement 2: Regex SMS Parser
     * Extracts:
     * a) amount (Double)
     * b) trxId (String - trimmed and converted to UPPERCASE)
     * c) reference (String - e.g. "tbuddy1", or "" if missing / agent cash-in)
     * d) senderNumber (String - full number like "01712345678" or masked like "017****1234", plus last 4 digits)
     */
    fun parsePaymentSms(senderAddress: String, messageBody: String): ParsedPaymentSms? {
        val cleanBody = messageBody.trim()
        if (cleanBody.isEmpty()) return null

        // Detect Wallet Platform (bKash / Nagad / Rocket)
        val platform = detectPlatform(senderAddress, cleanBody)

        // a) Extract Amount (Double)
        // Matches "received Tk 500.00", "Cash In Tk 1,250.00", "Amount: Tk 500.00", "BDT 500.00", etc.
        val amountRegex = Regex(
            pattern = """(?:received\s+(?:Tk|BDT)|Cash\s*[-]?\s*In\s+(?:Tk|BDT)|Amount\s*:?\s*(?:Tk|BDT)|Payment\s+(?:Tk|BDT)|(?:Tk|BDT))\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
            option = RegexOption.IGNORE_CASE
        )
        val amountMatch = amountRegex.find(cleanBody) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        // b) Extract TrxID (8 to 12 character alphanumeric code, trimmed and converted to UPPERCASE)
        // Matches "TrxID BJK4P9X2T1", "TxnID: 7NK49P2M", "TxID 9RKT8821X"
        val trxIdRegex = Regex(
            pattern = """(?:TrxID|TxnID|TxID|Trans(?:action)?\s*ID)\s*[:.-]?\s*([A-Za-z0-9]{6,12})\b""",
            option = RegexOption.IGNORE_CASE
        )
        val trxMatch = trxIdRegex.find(cleanBody) ?: return null
        val trxId = trxMatch.groupValues[1].trim().uppercase(Locale.ROOT)

        // c) Extract Reference (Optional word/code after "Ref", or empty if missing)
        val refRegex = Regex(
            pattern = """\bRef(?:erence)?\s*[:.-]?\s*([A-Za-z0-9_-]+)""",
            option = RegexOption.IGNORE_CASE
        )
        val rawRef = refRegex.find(cleanBody)?.groupValues?.get(1)?.trim() ?: ""
        val ignoredRefTokens = setOf("FEE", "BALANCE", "TRXID", "TXNID", "TXID", "NA", "N/A", "NONE", "NULL")
        val reference = if (rawRef.uppercase(Locale.ROOT) in ignoredRefTokens) "" else rawRef

        // d) Extract Sender Number (Supports full numbers like 01712345678 AND masked numbers like 017****1234 or 017XXXX1234)
        val contextualSenderRegex = Regex(
            pattern = """(?:from|Sender\s*:?|A/C\s*:?|by)\s*(\+?8801[3-9][0-9*Xx]{4,6}\d{4}|01[3-9][0-9*Xx]{4,6}\d{4}|\*{3,7}\d{4})""",
            option = RegexOption.IGNORE_CASE
        )
        val fallbackSenderRegex = Regex(
            pattern = """(\+?8801[3-9][0-9*Xx]{4,6}\d{4}|01[3-9][0-9*Xx]{4,6}\d{4})"""
        )

        val rawPhone = contextualSenderRegex.find(cleanBody)?.groupValues?.get(1)
            ?: fallbackSenderRegex.find(cleanBody)?.groupValues?.get(1)
            ?: return null

        val senderNumber = normalizeExtractedSender(rawPhone)
        val trailingDigits = senderNumber.takeLastWhile { it.isDigit() }
        if (trailingDigits.length < 4) return null
        val senderLast4 = trailingDigits.takeLast(4)

        return ParsedPaymentSms(
            platform = platform,
            amount = amount,
            trxId = trxId,
            reference = reference,
            senderNumber = senderNumber,
            senderLast4 = senderLast4
        )
    }

    private fun detectPlatform(senderAddress: String, body: String): String {
        val upperSender = senderAddress.trim().uppercase(Locale.ROOT)
        val upperBody = body.uppercase(Locale.ROOT)

        return when {
            upperSender.contains("BKASH") -> "bKash"
            upperSender.contains("NAGAD") -> "Nagad"
            upperSender.contains("16216") || upperSender.contains("ROCKET") -> "Rocket"
            upperBody.contains("NAGAD") || upperBody.contains("TXNID") -> "Nagad"
            upperBody.contains("ROCKET") || upperBody.contains("16216") || upperBody.contains("DBBL") -> "Rocket"
            else -> "bKash"
        }
    }

    private fun normalizeExtractedSender(rawPhone: String): String {
        val trimmed = rawPhone.trim()
        // If it includes asterisks or X masking (e.g., 017****1234), preserve the masked representation
        if (trimmed.any { it == '*' || it.uppercaseChar() == 'X' }) {
            return trimmed.removePrefix("+88").removePrefix("88")
        }
        val digits = trimmed.filter { it.isDigit() }
        return if (digits.startsWith("880") && digits.length >= 13) {
            digits.removePrefix("88")
        } else {
            digits
        }
    }

    val SAMPLE_BKASH_SMS =
        "You have received Tk 500.00 from 01712345678. Ref tbuddy1. Fee Tk 0.00. Balance Tk 1500.00. TrxID BJK4P9X2T1 at 10/11/2026 22:30"

    val SAMPLE_NAGAD_SMS =
        "Money Received. Amount: Tk 1250.00 Sender: 018****5432 Ref: order904 TxnID: 7NK49P2M at 10/11/2026 22:45. Balance: Tk 4800.00"

    val SAMPLE_ROCKET_SMS =
        "Cash-In Tk 850.00 from 01911223344. Ref vip2026. Fee Tk 0.00. Balance Tk 3200.00. TrxID 9RKT8821X at 10/11/2026 23:05"

    val SAMPLE_NO_REF_SMS =
        "You have received Tk 300.00 from 016****9988. Fee Tk 0.00. Balance Tk 1800.00. TrxID X9M2K7L4P0 at 10/11/2026 23:12"
}
