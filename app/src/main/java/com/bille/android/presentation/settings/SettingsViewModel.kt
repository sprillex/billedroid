package com.bille.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bille.android.crypto.KeystoreManager
import com.bille.android.data.local.pref.UserPreferencesRepository
import com.bille.android.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val deviceRepository: DeviceRepository,
    private val keystoreManager: KeystoreManager
) : ViewModel() {

    val serverUrl: StateFlow<String> = preferencesRepository.serverUrl.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "http://127.0.0.1:8080"
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

    fun getDeviceId(): String = keystoreManager.getDeviceId()

    fun getPublicKeyPem(): String = keystoreManager.getPublicKeyPem()

    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            preferencesRepository.updateServerUrl(url)
        }
    }

    fun updateGeminiApiKey(key: String) {
        viewModelScope.launch {
            preferencesRepository.updateGeminiApiKey(key)
        }
    }

    fun registerDevice() {
        viewModelScope.launch {
            deviceRepository.registerDevice()
        }
    }
}
