package com.bille.android.data.repository

import android.content.Context
import com.bille.android.data.local.dao.TriggerHistoryDao
import com.bille.android.data.local.entity.TriggerHistoryEntity
import com.bille.android.data.local.pref.UserPreferencesRepository
import com.bille.android.data.remote.api.DaemonConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class DaemonSyncRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val okHttpClient = OkHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val insertedHistory = mutableListOf<TriggerHistoryEntity>()

    private val fakeTriggerHistoryDao = object : TriggerHistoryDao {
        override fun getAllHistory() = flowOf(insertedHistory)
        override suspend fun insertHistory(history: TriggerHistoryEntity) {
            insertedHistory.add(history)
        }
    }

    private val mockUserPreferencesRepository: UserPreferencesRepository = mock(UserPreferencesRepository::class.java)

    private lateinit var repository: DaemonSyncRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = DaemonSyncRepository(
            okHttpClient = okHttpClient,
            userPreferencesRepository = mockUserPreferencesRepository,
            triggerHistoryDao = fakeTriggerHistoryDao,
            json = json,
            scope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun handleSseEvent_statusEvent_updatesStatusStateAndConnectionOnline() = runTest {
        val payload = """{"status":"ONLINE","uptime_seconds":3600,"active_rules":5}"""

        repository.handleSseEvent("status", payload)

        val status = repository.statusEvent.value
        assertNotNull(status)
        assertEquals("ONLINE", status?.status)
        assertEquals(3600L, status?.uptimeSeconds)
        assertEquals(5, status?.activeRules)
        assertEquals(DaemonConnectionState.ONLINE, repository.connectionState.value)
    }

    @Test
    fun handleSseEvent_stateUpdateEvent_updatesTelemetryState() = runTest {
        val payload = """{"indoor_temp":22.5,"outdoor_temp":14.0,"kp_index":3.2,"hvac_mode":"COOL"}"""

        repository.handleSseEvent("state_update", payload)

        val state = repository.stateUpdateEvent.value
        assertNotNull(state)
        assertEquals(22.5f, state?.indoorTemp)
        assertEquals(14.0f, state?.outdoorTemp)
        assertEquals(3.2f, state?.kpIndex)
        assertEquals("COOL", state?.hvacMode)
    }

    @Test
    fun handleSseEvent_triggerEvent_insertsHistoryIntoDao() = runTest {
        val payload = """{"task_id":"temp_alert","rule_name":"High Temp Warning","action_taken":"DISPATCH_NOTIF","timestamp":1700000000000}"""

        repository.handleSseEvent("trigger_event", payload)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, insertedHistory.size)
        val entity = insertedHistory.first()
        assertEquals("temp_alert", entity.taskId)
        assertEquals("High Temp Warning", entity.ruleName)
        assertEquals(1700000000000L, entity.triggeredAtTimestamp)
        assertEquals("DISPATCH_NOTIF", entity.actionTaken)
    }
}
