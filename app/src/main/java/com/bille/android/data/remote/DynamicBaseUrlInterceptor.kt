package com.bille.android.data.remote

import com.bille.android.data.local.pref.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Skip Gemini requests
        if (request.url.host.contains("googleapis.com")) {
            return chain.proceed(request)
        }

        // Get the current server URL synchronously blocking on the OkHttp background thread
        val serverUrlString = runBlocking {
            preferencesRepository.serverUrl.first()
        }

        val newBaseUrl = serverUrlString.toHttpUrlOrNull()

        if (newBaseUrl != null) {
            val newUrl = request.url.newBuilder()
                .scheme(newBaseUrl.scheme)
                .host(newBaseUrl.host)
                .port(newBaseUrl.port)
                .build()

            val newRequest = request.newBuilder()
                .url(newUrl)
                .build()

            return chain.proceed(newRequest)
        }

        return chain.proceed(request)
    }
}
