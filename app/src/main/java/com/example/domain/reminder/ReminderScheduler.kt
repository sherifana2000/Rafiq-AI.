package com.example.domain.reminder

sealed class ScheduleResult {
    data class Success(val reminderId: Long, val triggerAtMillis: Long) : ScheduleResult()
    data class Failure(val reason: String, val canRetry: Boolean = false) : ScheduleResult()
}

interface ReminderScheduler {
    fun schedule(
        reminderId: Long,
        title: String,
        target: String?,
        triggerAtMillis: Long,
        repeatInterval: String? = null
    ): ScheduleResult

    fun cancel(reminderId: Long)
}
