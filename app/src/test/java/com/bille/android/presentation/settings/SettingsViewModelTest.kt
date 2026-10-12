package com.bille.android.presentation.settings

import com.bille.android.crypto.KeystoreManager
import com.bille.android.data.local.pref.UserPreferencesRepository
import com.bille.android.data.repository.DeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockUserPreferencesRepository: UserPreferencesRepository = mock(UserPreferencesRepository::class.java)
    private val mockDeviceRepository: DeviceRepository = mock(DeviceRepository::class.java)
    private val mockKeystoreManager: KeystoreManager = mock(KeystoreManager::class.java)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        `when`(mockUserPreferencesRepository.serverUrl).thenReturn(flowOf("http://127.0.0.1:8080"))
        `when`(mockUserPreferencesRepository.geminiApiKey).thenReturn(flowOf("test-key"))
        `when`(mockUserPreferencesRepository.isRegistered).thenReturn(flowOf(false))

        viewModel = SettingsViewModel(
            preferencesRepository = mockUserPreferencesRepository,
            deviceRepository = mockDeviceRepository,
            keystoreManager = mockKeystoreManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun isValidUrl_validUrls_returnsTrue() {
        assertTrue(SettingsViewModel.isValidUrl("http://127.0.0.1:8080"))
        assertTrue(SettingsViewModel.isValidUrl("https://example.com"))
        assertTrue(SettingsViewModel.isValidUrl("http://localhost:3000"))
    }

    @Test
    fun isValidUrl_invalidUrls_returnsFalse() {
        assertFalse(SettingsViewModel.isValidUrl("http://127.0.0.1:80801"))
        assertFalse(SettingsViewModel.isValidUrl("invalid-url"))
        assertFalse(SettingsViewModel.isValidUrl(""))
        assertFalse(SettingsViewModel.isValidUrl("ftp://127.0.0.1"))
    }

    @Test
    fun updateServerUrl_validUrl_updatesPreferencesRepository() = runTest {
        backgroundScope.launch { viewModel.serverUrl.collect {} }
        backgroundScope.launch { viewModel.isServerUrlError.collect {} }

        viewModel.updateServerUrl("http://192.168.1.100:8080")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("http://192.168.1.100:8080", viewModel.serverUrl.value)
        assertFalse(viewModel.isServerUrlError.value)
        verify(mockUserPreferencesRepository).updateServerUrl("http://192.168.1.100:8080")
    }

    @Test
    fun updateServerUrl_invalidUrl_setsErrorAndDoesNotUpdatePreferencesRepository() = runTest {
        backgroundScope.launch { viewModel.serverUrl.collect {} }
        backgroundScope.launch { viewModel.isServerUrlError.collect {} }

        viewModel.updateServerUrl("http://127.0.0.1:80801")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("http://127.0.0.1:80801", viewModel.serverUrl.value)
        assertTrue(viewModel.isServerUrlError.value)
        verify(mockUserPreferencesRepository, never()).updateServerUrl("http://127.0.0.1:80801")
    }
}
