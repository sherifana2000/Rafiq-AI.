package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "rafiq_reminders_channel"
        const val CHANNEL_NAME = "تذكيرات رفيق"
        const val CHANNEL_DESC = "إشعارات التذكيرات المجدولة عبر المساعد الذكي رفيق"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * التحقق الآمن مما إذا كانت الإشعارات مسموحة في النظام
     */
    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * إظهار الإشعار بشكل آمن دون التسبب في Crash حتى لو لم تمنح الصلاحية
     */
    fun showReminderNotification(
        reminderId: Long,
        title: String,
        target: String?
    ): Boolean {
        if (!areNotificationsEnabled()) {
            return false
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_REMINDER_ID", reminderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (!target.isNullOrBlank() && target != title) {
            target
        } else {
            "حان موعد التذكير المحدد في جدولك"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("تذكير من رفيق: $title")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(reminderId.toInt(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
