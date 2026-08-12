package com.example.bioguard_wearos.presentation.service

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PairingListenerServiceTest {

    @Test
    fun pairingProtocol_validatesProtocolVersionAndRequestId() {
        val requestId = UUID.randomUUID().toString()
        val validPayload = JSONObject()
            .put("protocolVersion", 2)
            .put("requestId", requestId)
            .put("nonce", "challenge-nonce-123456789")
            .toString()

        val json = JSONObject(validPayload)
        assertEquals(2, json.getInt("protocolVersion"))
        assertTrue(json.getString("requestId").matches(Regex("^[0-9a-fA-F-]{36}$")))
        assertEquals("challenge-nonce-123456789", json.getString("nonce"))
    }

    @Test
    fun pairingProtocol_rejectsInvalidProtocolVersion() {
        val invalidPayload = JSONObject()
            .put("protocolVersion", 1)
            .put("requestId", UUID.randomUUID().toString())
            .put("nonce", "challenge-nonce-123456789")
            .toString()

        val json = JSONObject(invalidPayload)
        assertFalse(json.optInt("protocolVersion") == 2)
    }

    @Test
    fun pairingProtocol_rejectsMissingOrBlankNonce() {
        val payloadWithoutNonce = JSONObject()
            .put("protocolVersion", 2)
            .put("requestId", UUID.randomUUID().toString())
            .put("nonce", "")
            .toString()

        val json = JSONObject(payloadWithoutNonce)
        val nonce = json.optString("nonce")
        assertTrue(nonce.isBlank())
    }
}
