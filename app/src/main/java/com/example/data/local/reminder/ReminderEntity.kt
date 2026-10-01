package com.example.data.local.reminder

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * يمثل كيان التذكير المحفوظ محلياً في قاعدة بيانات Room لضمان عدم ضياعه
 * بعد إغلاق التطبيق أو إعادة تشغيل الجهاز.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val target: String? = null,
    val scheduledEpochMillis: Long,
    val scheduledTimeIso: String,
    val repeatInterval: String = "NONE", // NONE, DAILY, WEEKLY
    val status: String = "SCHEDULED", // SCHEDULED, FIRED, CANCELLED
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
