package com.bille.android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bille.android.data.local.dao.TriggerHistoryDao
import com.bille.android.data.local.entity.TriggerHistoryEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var triggerHistoryDao: TriggerHistoryDao

    @Inject
    lateinit var notificationManager: BilleNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getStringExtra(BilleNotificationManager.EXTRA_TASK_ID) ?: "unknown_task"
        val ruleName = intent.getStringExtra(BilleNotificationManager.EXTRA_RULE_NAME) ?: "Unknown Rule"
        val notificationId = intent.getIntExtra(BilleNotificationManager.EXTRA_NOTIFICATION_ID, 0)

        val actionLabel = when (action) {
            BilleNotificationManager.ACTION_DONE -> "Done"
            BilleNotificationManager.ACTION_SNOOZE -> "Snooze"
            BilleNotificationManager.ACTION_DISMISS -> "Dismiss"
            else -> action
        }

        notificationManager.cancelNotification(notificationId)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entity = TriggerHistoryEntity(
                    taskId = taskId,
                    ruleName = ruleName,
                    triggeredAtTimestamp = System.currentTimeMillis(),
                    actionTaken = actionLabel
                )
                triggerHistoryDao.insertHistory(entity)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
