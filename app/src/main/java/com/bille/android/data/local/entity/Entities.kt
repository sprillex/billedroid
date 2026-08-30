package com.bille.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey val taskId: String,
    val name: String,
    val cooldownHours: Int,
    val rawJsonPayload: String,
    val isInstalled: Boolean,
    val installedAtTimestamp: Long
)

@Entity(tableName = "trigger_history")
data class TriggerHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String,
    val ruleName: String,
    val triggeredAtTimestamp: Long,
    val actionTaken: String
)
