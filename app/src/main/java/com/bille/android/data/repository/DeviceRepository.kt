package com.bille.android.data.repository

import android.os.Build
import com.bille.android.crypto.KeystoreManager
import com.bille.android.data.local.pref.UserPreferencesRepository
import com.bille.android.data.remote.api.BilleApiService
import com.bille.android.data.remote.api.DeviceRegistrationRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val billeApiService: BilleApiService,
    private val keystoreManager: KeystoreManager,
    private val preferencesRepository: UserPreferencesRepository
) {
    suspend fun registerDevice(): Result<String> {
        return try {
            val deviceId = keystoreManager.getDeviceId()
            val publicKeyPem = keystoreManager.getPublicKeyPem()
            val deviceName = "${Build.MANUFACTURER.capitalize()} ${Build.MODEL}"

            val request = DeviceRegistrationRequest(
                deviceId = deviceId,
                deviceName = deviceName,
                publicKeyPem = publicKeyPem
            )

            val response = billeApiService.registerDevice(request)
            if (response.status == "registered" || response.status == "already_registered") {
                preferencesRepository.setRegistered(true)
                Result.success(response.deviceId)
            } else {
                Result.failure(Exception("Registration failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
