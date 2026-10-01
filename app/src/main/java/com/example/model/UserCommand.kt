package com.example.model

/**
 * يمثل أمر المستخدم المدخل سواء نصياً أو صوتياً، بعد مرحلة التحليل
 */
data class UserCommand(
    val id: String,
    val rawText: String,
    val intent: ActionType,
    val title: String,
    val scheduleRule: ScheduleRule = ScheduleRule(),
    val confidence: Float = 1.0f,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val status: ExecutionStatus = ExecutionStatus.PENDING,
    val action: Action? = null,
    val responseFeedbackArabic: String = ""
)
