package com.bille.android.data.local.pref

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "bille_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val IS_REGISTERED_KEY = booleanPreferencesKey("is_registered")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SERVER_URL_KEY] ?: "http://127.0.0.1:8080"
    }

    val geminiApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GEMINI_API_KEY] ?: ""
    }

    val isRegistered: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_REGISTERED_KEY] ?: false
    }

    suspend fun updateServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_URL_KEY] = url
        }
    }

    suspend fun updateGeminiApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[GEMINI_API_KEY] = key
        }
    }

    suspend fun setRegistered(registered: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_REGISTERED_KEY] = registered
        }
    }
}
