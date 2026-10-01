package com.example.model

/**
 * حالة تنفيذ الأمر أو المهمة
 */
enum class ExecutionStatus(val displayNameArabic: String) {
    PENDING("قيد المعالجة"),
    SCHEDULED("مجدول"),
    IN_PROGRESS("جارية الآن"),
    COMPLETED("مكتملة"),
    CANCELLED("ملغاة"),
    FAILED("تعذر التنفيذ")
}
