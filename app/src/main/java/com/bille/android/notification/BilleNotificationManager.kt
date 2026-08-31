package com.bille.android.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bille.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BilleNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "bille_actionable_alerts"
        const val CHANNEL_NAME = "bill-e Actionable Alerts"

        const val ACTION_DONE = "com.bille.android.ACTION_DONE"
        const val ACTION_SNOOZE = "com.bille.android.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.bille.android.ACTION_DISMISS"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_RULE_NAME = "extra_rule_name"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for bill-e automated household rules"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showActionableNotification(
        notificationId: Int,
        taskId: String,
        ruleName: String,
        title: String,
        message: String,
        actions: List<String> = listOf("Done", "Snooze", "Dismiss")
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if ("Done" in actions) {
            val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_DONE
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_RULE_NAME, ruleName)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            val donePendingIntent = PendingIntent.getBroadcast(
                context, notificationId * 10 + 1, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_save, "Done", donePendingIntent)
        }

        if ("Snooze" in actions) {
            val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_RULE_NAME, ruleName)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context, notificationId * 10 + 2, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_recent_history, "Snooze", snoozePendingIntent)
        }

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_RULE_NAME, ruleName)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, notificationId * 10 + 3, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}
