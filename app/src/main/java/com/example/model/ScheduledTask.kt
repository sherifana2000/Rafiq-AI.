package com.example.model

/**
 * يمثل المهمة المجدولة أو التذكير في جدول المستخدم
 */
data class ScheduledTask(
    val id: String,
    val title: String,
    val description: String = "",
    val action: Action,
    val scheduleRule: ScheduleRule,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val source: TaskSource = TaskSource.USER_COMMAND,
    val status: ExecutionStatus = ExecutionStatus.SCHEDULED,
    val timeLabelArabic: String = "", // e.g. "08:00 ص", "10:30 م"
    val dateLabelArabic: String = "اليوم", // e.g. "اليوم", "غداً"
    val isCurrentNow: Boolean = false,
    val durationLabelArabic: String? = null, // e.g. "30 دقيقة", "50 دقيقة"
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    val isCompleted: Boolean
        get() = status == ExecutionStatus.COMPLETED
}
