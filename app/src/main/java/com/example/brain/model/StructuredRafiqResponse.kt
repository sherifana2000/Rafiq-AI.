package com.example.brain.model

import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * نموذج الاستجابة المنظمة العائدة من محرك رفيق الذكي (Structured Gemini Response)
 */
@JsonClass(generateAdapter = true)
data class StructuredRafiqResponse(
    val understood: Boolean,
    val intent: String,
    val confidence: Float,
    val summary: String,
    val actions: List<StructuredActionItem> = emptyList(),
    val requiresConfirmation: Boolean = true,
    val clarificationQuestion: String? = null
)

/**
 * تفاصيل كل إجراء تنفيذي داخل الاستجابة المنظمة
 */
@JsonClass(generateAdapter = true)
data class StructuredActionItem(
    val type: String,
    val title: String,
    val target: String? = null,
    val scheduledTime: String? = null,
    val durationMinutes: Int? = null,
    val repeat: String? = "NONE",
    val payload: Map<String, String> = emptyMap()
)

/**
 * السياق الزمني والجغرافي الموجه للذكاء الاصطناعي لضمان فهم التواريخ بدقة دون تخمين
 */
@JsonClass(generateAdapter = true)
data class RafiqUserContext(
    val currentDate: String,
    val currentTime: String,
    val timeZone: String,
    val locale: String
) {
    companion object {
        fun current(): RafiqUserContext {
            val tz = TimeZone.getDefault()
            val locale = Locale.getDefault()
            val now = Date()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tz }
            val timeFormat = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = tz }

            return RafiqUserContext(
                currentDate = dateFormat.format(now),
                currentTime = timeFormat.format(now),
                timeZone = tz.id,
                locale = locale.toLanguageTag()
            )
        }
    }
}
