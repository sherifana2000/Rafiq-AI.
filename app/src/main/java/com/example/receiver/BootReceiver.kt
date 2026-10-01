package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.reminder.AndroidReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_TIME_CHANGED
        ) {
            return
        }

        Log.d("BootReceiver", "Device boot or time changed ($action). Rescheduling active reminders...")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val db = AppDatabase.getInstance(context)
                val activeReminders = db.reminderDao().getActivePendingReminders(now)
                val scheduler = AndroidReminderScheduler(context)

                Log.d("BootReceiver", "Found ${activeReminders.size} active pending reminders to reschedule.")

                for (reminder in activeReminders) {
                    scheduler.schedule(
                        reminderId = reminder.id,
                        title = reminder.title,
                        target = reminder.target,
                        triggerAtMillis = reminder.scheduledEpochMillis,
                        repeatInterval = reminder.repeatInterval
                    )
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to reschedule reminders after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
