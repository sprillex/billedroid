package com.bille.android.data.remote.api

import com.bille.android.domain.model.BilleRule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerStatusResponse(
    val status: String,
    val service: String? = null,
    val version: String? = null
)

@Serializable
data class DeviceRegistrationRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("public_key_pem") val publicKeyPem: String
)

@Serializable
data class DeviceRegistrationResponse(
    val status: String,
    @SerialName("device_id") val deviceId: String
)

@Serializable
data class RegisteredDeviceItem(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("registered_at") val registeredAt: String? = null
)

@Serializable
data class DeviceListResponse(
    val status: String,
    val count: Int? = null,
    val devices: List<RegisteredDeviceItem> = emptyList()
)

@Serializable
data class RuleListResponse(
    val status: String,
    val count: Int? = null,
    val rules: List<BilleRule> = emptyList()
)

@Serializable
data class RuleInstallResponse(
    val status: String,
    @SerialName("task_id") val taskId: String,
    @SerialName("file_path") val filePath: String? = null
)

@Serializable
data class RuleDeleteResponse(
    val status: String,
    @SerialName("task_id") val taskId: String
)
