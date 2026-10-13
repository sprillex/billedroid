package com.bille.android.data.remote

import com.bille.android.crypto.KeystoreManager
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SigningInterceptor @Inject constructor(
    private val keystoreManager: KeystoreManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Only sign requests that mutate server state or require intent authentication (e.g. POST /api/rules/install)
        if (originalRequest.method != "POST" && originalRequest.method != "PUT" && originalRequest.method != "DELETE") {
            return chain.proceed(originalRequest)
        }

        val rawJsonBodyBytes = originalRequest.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readByteArray()
        } ?: ByteArray(0)

        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        val nonce = UUID.randomUUID().toString()
        val deviceId = keystoreManager.getDeviceId()

        val signature = keystoreManager.signPayload(nonce, timestamp, rawJsonBodyBytes)

        val signedRequest = originalRequest.newBuilder()
            .header("X-Device-ID", deviceId)
            .header("X-Signature", signature)
            .header("X-Nonce", nonce)
            .header("X-Timestamp", timestamp)
            .build()

        return chain.proceed(signedRequest)
    }
}
