package com.example.model

/**
 * مصدر إنشاء المهمة أو الإجراء
 */
enum class TaskSource(val displayNameArabic: String) {
    USER_COMMAND("أمر كتابي من المستخدم"),
    USER_VOICE("أمر صوتي"),
    SCHEDULED_RECURRING("جدول دوري مكرر"),
    REPLAN_ENGINE("محرك إعادة الترتيب التلقائي"),
    SYSTEM("النظام الداخلي")
}
