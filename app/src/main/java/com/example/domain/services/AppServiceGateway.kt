package com.example.domain.services

/**
 * بوابة الربط مع خدمات أندرويد (Android Services Gateway).
 * تفصل محرك الأوامر عن خدمات النظام المباشرة مثل AlarmManager و NotificationManager و AccessibilityService.
 */
interface AppServiceGateway {
    fun logActionIntent(actionName: String, target: String)
    fun isAppInstalled(packageName: String): Boolean
    fun canScheduleExactAlarms(): Boolean
}
