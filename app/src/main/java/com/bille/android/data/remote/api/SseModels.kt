package com.bille.android.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class DaemonConnectionState {
    ONLINE,
    RECONNECTING,
    OFFLINE
}

@Serializable
data class DaemonStatusEvent(
    @SerialName("status") val status: String = "ONLINE",
    @SerialName("uptime_seconds") val uptimeSeconds: Long = 0,
    @SerialName("active_rules") val activeRules: Int = 0
)

@Serializable
data class DaemonStateUpdateEvent(
    @SerialName("indoor_temp") val indoorTemp: Float? = null,
    @SerialName("outdoor_temp") val outdoorTemp: Float? = null,
    @SerialName("kp_index") val kpIndex: Float? = null,
    @SerialName("hvac_mode") val hvacMode: String? = null
)

@Serializable
data class DaemonTriggerEvent(
    @SerialName("task_id") val taskId: String,
    @SerialName("rule_name") val ruleName: String,
    @SerialName("title") val title: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("action_taken") val actionTaken: String = "EXECUTED",
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)
