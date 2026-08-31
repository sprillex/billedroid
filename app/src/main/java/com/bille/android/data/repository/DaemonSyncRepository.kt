package com.bille.android.data.repository

import com.bille.android.data.local.dao.TriggerHistoryDao
import com.bille.android.data.local.entity.TriggerHistoryEntity
import com.bille.android.data.local.pref.UserPreferencesRepository
import com.bille.android.data.remote.api.DaemonConnectionState
import com.bille.android.data.remote.api.DaemonStateUpdateEvent
import com.bille.android.data.remote.api.DaemonStatusEvent
import com.bille.android.data.remote.api.DaemonTriggerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DaemonSyncRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val triggerHistoryDao: TriggerHistoryDao,
    private val json: Json
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow(DaemonConnectionState.OFFLINE)
    val connectionState: StateFlow<DaemonConnectionState> = _connectionState.asStateFlow()

    private val _statusEvent = MutableStateFlow<DaemonStatusEvent?>(null)
    val statusEvent: StateFlow<DaemonStatusEvent?> = _statusEvent.asStateFlow()

    private val _stateUpdateEvent = MutableStateFlow<DaemonStateUpdateEvent?>(null)
    val stateUpdateEvent: StateFlow<DaemonStateUpdateEvent?> = _stateUpdateEvent.asStateFlow()

    private var eventSource: EventSource? = null
    private var connectionJob: Job? = null
    private var backoffDelayMs = 1000L
    private val maxBackoffDelayMs = 30000L

    fun startSync() {
        if (connectionJob != null) return
        connectionJob = scope.launch {
            userPreferencesRepository.serverUrl.collectLatest { baseUrl ->
                connectSse(baseUrl)
            }
        }
    }

    fun stopSync() {
        eventSource?.cancel()
        eventSource = null
        connectionJob?.cancel()
        connectionJob = null
        _connectionState.value = DaemonConnectionState.OFFLINE
    }

    private suspend fun connectSse(baseUrl: String) {
        eventSource?.cancel()
        backoffDelayMs = 1000L

        while (scope.coroutineContext[Job]?.isActive == true) {
            _connectionState.value = if (backoffDelayMs == 1000L) DaemonConnectionState.RECONNECTING else DaemonConnectionState.RECONNECTING

            val cleanUrl = baseUrl.trimEnd('/')
            val sseUrl = "$cleanUrl/api/v1/sync/events"

            val request = Request.Builder()
                .url(sseUrl)
                .header("Accept", "text/event-stream")
                .build()

            val factory = EventSources.createFactory(okHttpClient)

            val connectedSignal = Job()

            val listener = object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    _connectionState.value = DaemonConnectionState.ONLINE
                    backoffDelayMs = 1000L
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    handleSseEvent(type, data)
                }

                override fun onClosed(eventSource: EventSource) {
                    _connectionState.value = DaemonConnectionState.OFFLINE
                    if (connectedSignal.isActive) connectedSignal.complete()
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    _connectionState.value = DaemonConnectionState.OFFLINE
                    if (connectedSignal.isActive) connectedSignal.complete()
                }
            }

            eventSource = factory.newEventSource(request, listener)

            connectedSignal.join()

            // Retry with exponential backoff if job is active
            if (scope.coroutineContext[Job]?.isActive == true) {
                delay(backoffDelayMs)
                backoffDelayMs = (backoffDelayMs * 2).coerceAtMost(maxBackoffDelayMs)
            } else {
                break
            }
        }
    }

    fun handleSseEvent(type: String?, data: String) {
        try {
            when (type) {
                "status" -> {
                    val status = json.decodeFromString<DaemonStatusEvent>(data)
                    _statusEvent.value = status
                    _connectionState.value = DaemonConnectionState.ONLINE
                }
                "state_update" -> {
                    val stateUpdate = json.decodeFromString<DaemonStateUpdateEvent>(data)
                    _stateUpdateEvent.value = stateUpdate
                }
                "trigger_event" -> {
                    val triggerEvent = json.decodeFromString<DaemonTriggerEvent>(data)
                    scope.launch {
                        triggerHistoryDao.insertHistory(
                            TriggerHistoryEntity(
                                taskId = triggerEvent.taskId,
                                ruleName = triggerEvent.ruleName,
                                triggeredAtTimestamp = triggerEvent.timestamp,
                                actionTaken = triggerEvent.actionTaken
                            )
                        )
                    }
                }
                else -> {
                    // Try parsing generic JSON payload if type is null/empty
                    if (data.contains("\"status\"")) {
                        val status = json.decodeFromString<DaemonStatusEvent>(data)
                        _statusEvent.value = status
                        _connectionState.value = DaemonConnectionState.ONLINE
                    } else if (data.contains("\"task_id\"")) {
                        val triggerEvent = json.decodeFromString<DaemonTriggerEvent>(data)
                        scope.launch {
                            triggerHistoryDao.insertHistory(
                                TriggerHistoryEntity(
                                    taskId = triggerEvent.taskId,
                                    ruleName = triggerEvent.ruleName,
                                    triggeredAtTimestamp = triggerEvent.timestamp,
                                    actionTaken = triggerEvent.actionTaken
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore parse errors on malformed frames
        }
    }
}
