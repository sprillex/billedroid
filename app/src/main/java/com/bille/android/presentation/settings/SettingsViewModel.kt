package com.bille.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bille.android.crypto.KeystoreManager
import com.bille.android.data.local.pref.UserPreferencesRepository
import com.bille.android.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val deviceRepository: DeviceRepository,
    private val keystoreManager: KeystoreManager
) : ViewModel() {

    private val _serverUrlInput = MutableStateFlow<String?>(null)

    val serverUrl: StateFlow<String> = _serverUrlInput
        .combine(preferencesRepository.serverUrl) { input, saved ->
            input ?: saved
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            "http://127.0.0.1:8080"
        )

    val isServerUrlError: StateFlow<Boolean> = serverUrl
        .map { !isValidUrl(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    val geminiApiKey: StateFlow<String> = preferencesRepository.geminiApiKey.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    val isRegistered: StateFlow<Boolean> = preferencesRepository.isRegistered.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    private val _registrationMessage = MutableStateFlow<String?>(null)
    val registrationMessage: StateFlow<String?> = _registrationMessage.asStateFlow()

    fun getDeviceId(): String = keystoreManager.getDeviceId()

    fun getPublicKeyPem(): String = keystoreManager.getPublicKeyPem()

    fun updateServerUrl(url: String) {
        _serverUrlInput.value = url
        if (isValidUrl(url)) {
            viewModelScope.launch {
                preferencesRepository.updateServerUrl(url)
            }
        }
    }

    fun updateGeminiApiKey(key: String) {
        viewModelScope.launch {
            preferencesRepository.updateGeminiApiKey(key)
        }
    }

    fun registerDevice() {
        viewModelScope.launch {
            _registrationMessage.value = "Registering device..."
            val result = deviceRepository.registerDevice()
            result.fold(
                onSuccess = { deviceId ->
                    _registrationMessage.value = "Device registered successfully! (ID: $deviceId)"
                },
                onFailure = { error ->
                    _registrationMessage.value = "Registration failed: ${error.localizedMessage ?: "Unknown error"}"
                }
            )
        }
    }

    companion object {
        fun isValidUrl(url: String): Boolean {
            if (url.isBlank()) return false
            val parsed = try {
                url.toHttpUrlOrNull()
            } catch (_: Exception) {
                null
            } ?: return false
            return (parsed.scheme == "http" || parsed.scheme == "https") && parsed.host.isNotEmpty()
        }
    }
}
