package com.bille.android.crypto

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeystoreManager @Inject constructor(
    private val context: Context
) {
    companion object {
        const val KEY_ALIAS = "bille_device_signing_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val SIGNING_ALGORITHM = "SHA256withECDSA"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    /**
     * Retrieves existing KeyPair or generates a new EC secp256r1 keypair in Android Keystore.
     * Tries StrongBox backing first; falls back gracefully to standard TEE.
     */
    fun getOrCreateKeyPair(): KeyStore.PrivateKeyEntry {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateEcKeyPair()
        }
        return keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
    }

    private fun generateEcKeyPair() {
        try {
            // Attempt StrongBox hardware security module first
            createKeyPair(isStrongBox = true)
        } catch (e: Exception) {
            // Fallback to standard TEE if StrongBox is unavailable
            createKeyPair(isStrongBox = false)
        }
    }

    private fun createKeyPair(isStrongBox: Boolean) {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN
        ).apply {
            setDigests(KeyProperties.DIGEST_SHA256)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isStrongBox) {
                setIsStrongBoxBacked(true)
            }
        }
        keyPairGenerator.initialize(builder.build())
        keyPairGenerator.generateKeyPair()
    }

    /**
     * Calculates the device ID as the lowercase Hex SHA-256 fingerprint of the X.509 encoded public key.
     */
    fun getDeviceId(): String {
        val publicKey = getOrCreateKeyPair().certificate.publicKey
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKey.encoded)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Exports the Public Key in standard PEM format.
     */
    fun getPublicKeyPem(): String {
        val publicKey = getOrCreateKeyPair().certificate.publicKey
        val base64Key = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        return "-----BEGIN PUBLIC KEY-----\n$base64Key\n-----END PUBLIC KEY-----"
    }

    /**
     * Signs payload using HMAC-SHA256 over: (nonce + timestamp).toByteArray(UTF_8) + rawBodyBytes
     * using the device's public key string (public_key_pem) as the secret key.
     * Returns Base64-encoded signature string.
     */
    fun signPayload(nonce: String, timestamp: String, rawBodyBytes: ByteArray): String {
        val canonicalHeaderStr = nonce + timestamp
        val canonicalHeaderBytes = canonicalHeaderStr.toByteArray(Charsets.UTF_8)
        val payloadBytes = canonicalHeaderBytes + rawBodyBytes

        val publicKeyPem = getPublicKeyPem()
        val secretKey = SecretKeySpec(publicKeyPem.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(payloadBytes)

        return Base64.encodeToString(hmacBytes, Base64.NO_WRAP)
    }

    /**
     * Signs string payload using HMAC-SHA256 with device public_key_pem as secret key.
     * Returns Base64-encoded signature string.
     */
    fun signPayload(canonicalPayload: String): String {
        val publicKeyPem = getPublicKeyPem()
        val secretKey = SecretKeySpec(publicKeyPem.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(canonicalPayload.toByteArray(Charsets.UTF_8))

        return Base64.encodeToString(hmacBytes, Base64.NO_WRAP)
    }
}
