package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.reminder.AndroidReminderScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER_TRIGGER = "com.example.action.REMINDER_TRIGGER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TARGET = "extra_target"
        const val EXTRA_REPEAT = "extra_repeat"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "تذكير رفيق"
        val target = intent.getStringExtra(EXTRA_TARGET)
        val repeatInterval = intent.getStringExtra(EXTRA_REPEAT) ?: "NONE"

        Log.d("ReminderReceiver", "Reminder triggered: id=$reminderId, title=$title, repeat=$repeatInterval")

        // 1. إظهار الإشعار للمستخدم عبر NotificationHelper
        val notificationHelper = NotificationHelper(context)
        val shown = notificationHelper.showReminderNotification(reminderId, title, target)
        Log.d("ReminderReceiver", "Notification shown result: $shown")

        if (reminderId <= 0) return

        // 2. تحديث حالة التذكير أو إعادة جدولته في حال كان متكرراً
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val dao = db.reminderDao()
                val current = dao.getReminderById(reminderId)

                if (current != null) {
                    when (repeatInterval.uppercase()) {
                        "DAILY" -> {
                            val nextTrigger = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
                            val updated = current.copy(scheduledEpochMillis = nextTrigger)
                            dao.updateReminder(updated)
                            AndroidReminderScheduler(context).schedule(
                                reminderId = current.id,
                                title = current.title,
                                target = current.target,
                                triggerAtMillis = nextTrigger,
                                repeatInterval = "DAILY"
                            )
                        }
                        "WEEKLY" -> {
                            val nextTrigger = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
                            val updated = current.copy(scheduledEpochMillis = nextTrigger)
                            dao.updateReminder(updated)
                            AndroidReminderScheduler(context).schedule(
                                reminderId = current.id,
                                title = current.title,
                                target = current.target,
                                triggerAtMillis = nextTrigger,
                                repeatInterval = "WEEKLY"
                            )
                        }
                        else -> {
                            dao.updateStatus(reminderId, "FIRED")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error updating reminder status in Room", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
