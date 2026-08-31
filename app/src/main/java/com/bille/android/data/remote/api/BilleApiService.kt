package com.bille.android.data.remote.api

import com.bille.android.domain.model.BilleRule
import retrofit2.http.Body
import retrofit2.http.POST

interface BilleApiService {

    @POST("api/v1/devices/register")
    suspend fun registerDevice(
        @Body request: DeviceRegistrationRequest
    ): DeviceRegistrationResponse

    @POST("api/rules/install")
    suspend fun installRule(
        @Body rule: BilleRule
    ): RuleInstallResponse
}
