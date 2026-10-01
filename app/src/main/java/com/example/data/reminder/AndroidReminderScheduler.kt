package com.example.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.domain.reminder.ReminderScheduler
import com.example.domain.reminder.ScheduleResult
import com.example.receiver.ReminderBroadcastReceiver

class AndroidReminderScheduler(
    private val context: Context
) : ReminderScheduler {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun schedule(
        reminderId: Long,
        title: String,
        target: String?,
        triggerAtMillis: Long,
        repeatInterval: String?
    ): ScheduleResult {
        if (alarmManager == null) {
            return ScheduleResult.Failure("خدمة المنبهات (AlarmManager) غير متوفرة على هذا الجهاز.")
        }

        if (triggerAtMillis <= System.currentTimeMillis()) {
            return ScheduleResult.Failure("لا يمكن جدولة تذكير في وقت مضى.")
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_REMINDER_TRIGGER
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, title)
            putExtra(ReminderBroadcastReceiver.EXTRA_TARGET, target)
            putExtra(ReminderBroadcastReceiver.EXTRA_REPEAT, repeatInterval ?: "NONE")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (canScheduleExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                // تراجع آمن في حال عدم وجود إذن المنبه الدقيق
                Log.w("ReminderScheduler", "canScheduleExactAlarms is false. Falling back to setAndAllowWhileIdle.")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            }
            ScheduleResult.Success(reminderId, triggerAtMillis)
        } catch (e: SecurityException) {
            Log.e("ReminderScheduler", "SecurityException scheduling exact alarm", e)
            ScheduleResult.Failure("تعذر ضبط المنبه الدقيق بسبب أذونات النظام: ${e.message}")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Unexpected error scheduling alarm", e)
            ScheduleResult.Failure("حدث خطأ أثناء جدولة المنبه: ${e.message}")
        }
    }

    override fun cancel(reminderId: Long) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_REMINDER_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
