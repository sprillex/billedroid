package com.bille.android.data.repository

import com.bille.android.data.local.dao.RuleDao
import com.bille.android.data.local.entity.RuleEntity
import com.bille.android.data.remote.api.BilleApiService
import com.bille.android.domain.model.BilleRule
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepository @Inject constructor(
    private val billeApiService: BilleApiService,
    private val ruleDao: RuleDao,
    private val json: Json
) {
    val installedRules: Flow<List<RuleEntity>> = ruleDao.getAllRules()

    suspend fun installRule(rule: BilleRule): Result<String> {
        return try {
            val response = billeApiService.installRule(rule)
            if (response.status == "installed") {
                val entity = RuleEntity(
                    taskId = rule.taskId,
                    name = rule.name,
                    cooldownHours = rule.cooldownHours,
                    rawJsonPayload = json.encodeToString(rule),
                    isInstalled = true,
                    installedAtTimestamp = System.currentTimeMillis()
                )
                ruleDao.insertRule(entity)
                Result.success(response.taskId)
            } else {
                Result.failure(Exception("Installation returned status: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
