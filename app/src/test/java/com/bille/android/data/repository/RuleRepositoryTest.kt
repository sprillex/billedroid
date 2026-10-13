package com.bille.android.data.repository

import com.bille.android.data.local.dao.RuleDao
import com.bille.android.data.remote.api.BilleApiService
import com.bille.android.data.remote.api.RuleDeleteResponse
import com.bille.android.data.remote.api.RuleListResponse
import com.bille.android.domain.model.BilleRule
import com.bille.android.domain.model.RuleAction
import com.bille.android.domain.model.RuleConditions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class RuleRepositoryTest {

    private val mockApiService: BilleApiService = mock(BilleApiService::class.java)
    private val mockRuleDao: RuleDao = mock(RuleDao::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var repository: RuleRepository

    @Before
    fun setUp() {
        repository = RuleRepository(
            billeApiService = mockApiService,
            ruleDao = mockRuleDao,
            json = json
        )
    }

    @Test
    fun deleteRule_success_deletesFromDaoAndReturnsSuccess() = runTest {
        val taskId = "rule_123"
        `when`(mockApiService.deleteRule(taskId)).thenReturn(RuleDeleteResponse("deleted", taskId))

        val result = repository.deleteRule(taskId)

        assertTrue(result.isSuccess)
        verify(mockRuleDao).deleteRule(taskId)
    }

    @Test
    fun fetchInstalledRulesFromDaemon_success_returnsRules() = runTest {
        val rule = BilleRule(
            taskId = "test_rule",
            name = "Test Rule",
            cooldownHours = 1,
            conditions = RuleConditions(),
            action = RuleAction("Title", "Message")
        )
        `when`(mockApiService.getRules()).thenReturn(RuleListResponse("success", 1, listOf(rule)))

        val result = repository.fetchInstalledRulesFromDaemon()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("test_rule", result.getOrNull()?.first()?.taskId)
    }
}
