package com.example.brain.validator

import com.example.brain.model.StructuredActionItem
import com.example.brain.model.StructuredRafiqResponse
import com.example.model.ActionType
import java.net.URI

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

/**
 * مدقق مستقل للاستجابة المنظمة العائدة من الذكاء الاصطناعي (RafiqActionValidator)
 * يضمن خلو الأوامر من أي بيانات غير صالحة أو مشبوهة أو مفقودة قبل اعتمادها.
 */
class RafiqActionValidator {

    private val validRepeatOptions = setOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "CUSTOM")

    fun validate(response: StructuredRafiqResponse): ValidationResult {
        // إذا كان هناك سؤال استيضاحي والأمر غير مفهوم، فهو صالح للمرحلة
        if (!response.understood || response.clarificationQuestion != null) {
            return ValidationResult.Valid
        }

        // 1. التحقق من صحة الـ Intent العام
        val mainIntent = parseActionType(response.intent)
        if (mainIntent == null || mainIntent == ActionType.UNKNOWN) {
            return ValidationResult.Invalid("نوع الأمر العام غير معروف أو غير مدعوم: ${response.intent}")
        }

        // 2. التحقق من وجود أفعال إذا كان الأمر مفهوماً
        if (response.actions.isEmpty()) {
            return ValidationResult.Invalid("الاستجابة لا تحتوي على أي أفعال تنفيذية.")
        }

        // 3. فحص كل Action على حدة
        for ((index, action) in response.actions.withIndex()) {
            val actionResult = validateActionItem(action, index)
            if (actionResult is ValidationResult.Invalid) {
                return actionResult
            }
        }

        return ValidationResult.Valid
    }

    private fun validateActionItem(action: StructuredActionItem, index: Int): ValidationResult {
        // أ. التحقق من أن النوع معروف
        val actionType = parseActionType(action.type)
            ?: return ValidationResult.Invalid("الفعل رقم ${index + 1} يحتوي على نوع غير معروف: ${action.type}")

        // ب. التحقق من الحقول الإجبارية (العنوان)
        if (action.title.trim().isBlank()) {
            return ValidationResult.Invalid("الفعل رقم ${index + 1} يفتقر إلى عنوان أو موضوع محدد.")
        }

        // ج. التحقق من أن المدة الزمنية غير سالبة
        if (action.durationMinutes != null && action.durationMinutes < 0) {
            return ValidationResult.Invalid("الفعل '${action.title}' يحتوي على مدة زمنية سالبة غير صالحة.")
        }

        // د. التحقق من صلاحية خيار التكرار
        val repeat = action.repeat?.uppercase() ?: "NONE"
        if (repeat !in validRepeatOptions) {
            return ValidationResult.Invalid("قاعدة التكرار غير صالحة: $repeat في فعل '${action.title}'.")
        }

        // هـ. التحقق من صحة الرابط إذا كان الفعل OPEN_URL
        if (actionType == ActionType.OPEN_URL) {
            val targetUrl = action.target?.trim()
            if (targetUrl.isNullOrBlank()) {
                return ValidationResult.Invalid("فعل فتح الرابط يفتقر إلى عنوان URL.")
            }
            if (!isValidUrl(targetUrl)) {
                return ValidationResult.Invalid("عنوان الرابط غير صالح: $targetUrl")
            }
        }

        // و. فحص أمان البيانات المرفقة (Payload)
        for ((key, value) in action.payload) {
            if (key.length > 100 || value.length > 500) {
                return ValidationResult.Invalid("البيانات المرفقة تتجاوز الحجم المسموح به في فعل '${action.title}'.")
            }
            if (value.contains("<script>", ignoreCase = true) || value.contains("javascript:", ignoreCase = true)) {
                return ValidationResult.Invalid("تم اكتشاف محتوى غير آمن في معطيات الفعل.")
            }
        }

        return ValidationResult.Valid
    }

    private fun parseActionType(name: String): ActionType? {
        return try {
            ActionType.valueOf(name.trim().uppercase())
        } catch (_: Exception) {
            null
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            uri.scheme != null && (uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true))
        } catch (_: Exception) {
            false
        }
    }
}
