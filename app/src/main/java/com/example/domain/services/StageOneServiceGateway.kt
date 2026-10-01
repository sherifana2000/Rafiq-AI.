package com.example.domain.services

import android.content.Context
import android.util.Log

/**
 * تنفيذ أولي آمن لـ AppServiceGateway للمرحلة الأولى،
 * لا يقوم بتشغيل تطبيقات أو طلب أذونات معقدة حتى تكتمل مرحلة البناء المعماري.
 */
class StageOneServiceGateway(
    @Suppress("unused") private val context: Context
) : AppServiceGateway {

    override fun logActionIntent(actionName: String, target: String) {
        Log.d("RafiqAI", "Stage 1 Action Registered: $actionName -> $target")
    }

    override fun isAppInstalled(packageName: String): Boolean {
        return true
    }

    override fun canScheduleExactAlarms(): Boolean {
        return true
    }
}
