package com.example.model

/**
 * قاعدة التكرار والتوقيت لجدولة المهام والتذكيرات
 */
enum class RepeatFrequency(val displayNameArabic: String) {
    NONE("مرة واحدة"),
    DAILY("يومياً"),
    WEEKLY("أسبوعياً"),
    MONTHLY("شهرياً"),
    PRAYER_BASED("مرتبط بأوقات الصلاة"),
    CUSTOM("مخصص")
}

data class ScheduleRule(
    val frequency: RepeatFrequency = RepeatFrequency.NONE,
    val targetDateArabic: String? = null, // e.g. "غداً", "اليوم", "2026-09-29"
    val targetTimeArabic: String? = null, // e.g. "08:00 صباحاً", "10:00 مساءً"
    val daysOfWeek: List<Int> = emptyList(), // 1 for Monday to 7 for Sunday
    val prayerReference: String? = null, // e.g. "صلاة الفجر"
    val offsetMinutes: Int = 0, // e.g. +15 or -30
    val humanReadableArabic: String = ""
)
