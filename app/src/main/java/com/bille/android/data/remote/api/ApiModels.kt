package com.bille.android.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
data class RuleInstallResponse(
    val status: String,
    @SerialName("task_id") val taskId: String,
    @SerialName("file_path") val filePath: String? = null
)
