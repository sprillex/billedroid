package com.bille.android.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bille.android.data.local.dao.TriggerHistoryDao
import com.bille.android.data.local.entity.RuleEntity
import com.bille.android.data.local.entity.TriggerHistoryEntity
import com.bille.android.data.remote.api.DaemonConnectionState
import com.bille.android.data.remote.api.DaemonStateUpdateEvent
import com.bille.android.data.remote.api.DaemonStatusEvent
import com.bille.android.data.repository.DaemonSyncRepository
import com.bille.android.data.repository.RuleRepository
import com.bille.android.notification.BilleNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val triggerHistoryDao: TriggerHistoryDao,
    private val daemonSyncRepository: DaemonSyncRepository,
    private val notificationManager: BilleNotificationManager
) : ViewModel() {

    val connectionState: StateFlow<DaemonConnectionState> = daemonSyncRepository.connectionState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DaemonConnectionState.OFFLINE
    )

    val daemonStatus: StateFlow<DaemonStatusEvent?> = daemonSyncRepository.statusEvent.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val stateUpdate: StateFlow<DaemonStateUpdateEvent?> = daemonSyncRepository.stateUpdateEvent.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val rules: StateFlow<List<RuleEntity>> = ruleRepository.installedRules.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val triggerHistory: StateFlow<List<TriggerHistoryEntity>> = triggerHistoryDao.getAllHistory().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun testNotification(rule: RuleEntity) {
        notificationManager.showActionableNotification(
            notificationId = (System.currentTimeMillis() % 10000).toInt(),
            taskId = rule.taskId,
            ruleName = rule.name,
            title = "Test Notification: ${rule.name}",
            message = "This is a local trigger test for the ${rule.taskId} rule condition.",
            actions = listOf("Done", "Snooze", "Dismiss")
        )
    }
}
