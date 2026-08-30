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
     * Signs canonical UTF-8 bytes payload: "${X-Timestamp}\n${X-Nonce}\n${RAW_JSON_BODY}"
     * Returns Base64-encoded signature string.
     */
    fun signPayload(canonicalPayload: String): String {
        val privateKeyEntry = getOrCreateKeyPair()
        val signature = Signature.getInstance(SIGNING_ALGORITHM).apply {
            initSign(privateKeyEntry.privateKey)
            update(canonicalPayload.toByteArray(Charsets.UTF_8))
        }
        val signatureBytes = signature.sign()
        return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
    }
}
