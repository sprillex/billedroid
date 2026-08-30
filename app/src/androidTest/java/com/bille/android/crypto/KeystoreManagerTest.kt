package com.bille.android.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreManagerTest {

    private lateinit var keystoreManager: KeystoreManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        keystoreManager = KeystoreManager(context)
    }

    @Test
    fun testKeyPairGenerationAndPemExport() {
        val pem = keystoreManager.getPublicKeyPem()
        assertNotNull(pem)
        assertTrue(pem.startsWith("-----BEGIN PUBLIC KEY-----"))
        assertTrue(pem.endsWith("-----END PUBLIC KEY-----"))
    }

    @Test
    fun testDeviceIdGeneration() {
        val deviceId = keystoreManager.getDeviceId()
        assertNotNull(deviceId)
        assertEquals(64, deviceId.length) // SHA-256 hex fingerprint length
    }

    @Test
    fun testPayloadSigning() {
        val payload = "2026-08-30T21:15:00Z\n4d2b2f6e-7128-4f10-9b34-8c83e29f8f2e\n{\"test\":true}"
        val signature = keystoreManager.signPayload(payload)
        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())
    }
}
