package com.bille.android.data.remote.api

import com.bille.android.domain.model.BilleRule
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BilleApiService {

    @GET("api/v1/status")
    suspend fun getStatus(): ServerStatusResponse

    @POST("api/v1/devices/register")
    suspend fun registerDevice(
        @Body request: DeviceRegistrationRequest
    ): DeviceRegistrationResponse

    @GET("api/v1/devices")
    suspend fun getDevices(): DeviceListResponse

    @GET("api/rules")
    suspend fun getRules(): RuleListResponse

    @POST("api/rules/install")
    suspend fun installRule(
        @Body rule: BilleRule
    ): RuleInstallResponse

    @DELETE("api/rules/{task_id}")
    suspend fun deleteRule(
        @Path("task_id") taskId: String
    ): RuleDeleteResponse
}
