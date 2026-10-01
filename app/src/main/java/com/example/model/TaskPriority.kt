package com.example.model

/**
 * أولوية المهمة أو التذكير
 */
enum class TaskPriority(val displayNameArabic: String) {
    LOW("منخفضة"),
    NORMAL("عادية"),
    HIGH("مهمة"),
    URGENT("عاجلة")
}
