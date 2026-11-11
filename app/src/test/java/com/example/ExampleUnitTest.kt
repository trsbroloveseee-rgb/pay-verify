package com.example

import com.example.domain.HmacSigner
import com.example.domain.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun senderIdFilter_allowsOfficialAndCustomTestNumber() {
        val customTestPhone = "01711223344"

        assertTrue(SmsParser.isAllowedSender("bKash", customTestPhone))
        assertTrue(SmsParser.isAllowedSender("NAGAD", customTestPhone))
        assertTrue(SmsParser.isAllowedSender("16216", customTestPhone))
        assertTrue(SmsParser.isAllowedSender("+8801711223344", customTestPhone))
        assertTrue(SmsParser.isAllowedSender("01711223344", customTestPhone))
        assertFalse(SmsParser.isAllowedSender("GP-INFO", customTestPhone))
        assertFalse(SmsParser.isAllowedSender("01999887766", customTestPhone))
    }

    @Test
    fun regexParser_extractsFieldsFromSampleBkashSms() {
        val sms =
            "You have received Tk 500.00 from 01712345678. Ref tbuddy1. Fee Tk 0.00. Balance Tk 1500.00. TrxID BJK4P9X2T1 at 10/11/2026 22:30"
        val parsed = SmsParser.parsePaymentSms("bKash", sms)

        assertNotNull(parsed)
        assertEquals("bKash", parsed!!.platform)
        assertEquals(500.00, parsed.amount, 0.001)
        assertEquals("01712345678", parsed.senderNumber)
        assertEquals("5678", parsed.senderLast4)
        assertEquals("BJK4P9X2T1", parsed.trxId)
        assertEquals("tbuddy1", parsed.reference)
    }

    @Test
    fun regexParser_supportsMaskedSenderNumberAndMissingReference() {
        val sms =
            "Money Received. Amount: Tk 1,250.50 Sender: 017****1234 TxnID: ab98k2m1p at 10/11/2026 22:30"
        val parsed = SmsParser.parsePaymentSms("NAGAD", sms)

        assertNotNull(parsed)
        assertEquals("Nagad", parsed!!.platform)
        assertEquals(1250.50, parsed.amount, 0.001)
        assertEquals("017****1234", parsed.senderNumber)
        assertEquals("1234", parsed.senderLast4)
        assertEquals("AB98K2M1P", parsed.trxId)
        assertEquals("", parsed.reference)
    }

    @Test
    fun hmacSigner_generates64CharSha256HexSignature() {
        val payload = HmacSigner.buildWebhookJsonPayload(
            platform = "bKash",
            amount = 500.00,
            senderNumber = "01712345678",
            senderLast4 = "5678",
            trxId = "BJK4P9X2T1",
            reference = "tbuddy1",
            receivedAt = 1794330600000L
        )
        val sig = HmacSigner.generateHmacSha256(payload, "my_secret_key")
        assertEquals(64, sig.length)
        assertTrue(sig.all { it.isDigit() || it in 'a'..'f' })
    }
}
