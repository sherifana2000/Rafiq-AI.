package com.example.brain.validator

import com.example.brain.model.StructuredActionItem
import com.example.brain.model.StructuredRafiqResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RafiqActionValidatorTest {

    private val validator = RafiqActionValidator()

    @Test
    fun `validate valid structured response returns Valid`() {
        val response = StructuredRafiqResponse(
            understood = true,
            intent = "CREATE_REMINDER",
            confidence = 0.95f,
            summary = "تذكير بمراجعة النحو",
            actions = listOf(
                StructuredActionItem(
                    type = "CREATE_REMINDER",
                    title = "مراجعة النحو",
                    scheduledTime = "08:00:00",
                    repeat = "NONE"
                )
            )
        )

        val result = validator.validate(response)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validate response with unknown intent returns Invalid`() {
        val response = StructuredRafiqResponse(
            understood = true,
            intent = "NON_EXISTENT_INTENT",
            confidence = 0.9f,
            summary = "أمر غير معروف",
            actions = listOf(
                StructuredActionItem(
                    type = "CREATE_TASK",
                    title = "مهمة جديدة"
                )
            )
        )

        val result = validator.validate(response)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).reason.contains("غير معروف"))
    }

    @Test
    fun `validate response with empty actions returns Invalid`() {
        val response = StructuredRafiqResponse(
            understood = true,
            intent = "CREATE_TASK",
            confidence = 0.9f,
            summary = "مهمة بدون أفعال",
            actions = emptyList()
        )

        val result = validator.validate(response)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validate action with blank title returns Invalid`() {
        val response = StructuredRafiqResponse(
            understood = true,
            intent = "CREATE_TASK",
            confidence = 0.9f,
            summary = "مهمة",
            actions = listOf(
                StructuredActionItem(
                    type = "CREATE_TASK",
                    title = "   "
                )
            )
        )

        val result = validator.validate(response)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validate action with negative duration returns Invalid`() {
        val response = StructuredRafiqResponse(
            understood = true,
            intent = "START_FOCUS_SESSION",
            confidence = 0.9f,
            summary = "جلسة تركيز",
            actions = listOf(
                StructuredActionItem(
                    type = "START_FOCUS_SESSION",
                    title = "جلسة تركيز",
                    durationMinutes = -15
                )
            )
        )

        val result = validator.validate(response)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validate action with invalid repeat rule returns Invalid`() {
        val response = StructuredRafiqResponse(
            understood = true,
            intent = "CREATE_REMINDER",
            confidence = 0.9f,
            summary = "تذكير",
            actions = listOf(
                StructuredActionItem(
                    type = "CREATE_REMINDER",
                    title = "تذكير يومي",
                    repeat = "HOURLY_RANDOM"
                )
            )
        )

        val result = validator.validate(response)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validate OPEN_URL with invalid url returns Invalid`() {
        val response = StructuredRafiqResponse(
            understood = true,
            intent = "OPEN_URL",
            confidence = 0.9f,
            summary = "فتح رابط",
            actions = listOf(
                StructuredActionItem(
                    type = "OPEN_URL",
                    title = "فتح موقع",
                    target = "not-a-valid-url"
                )
            )
        )

        val result = validator.validate(response)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validate script injection payload returns Invalid`() {
        val response = StructuredRafiqResponse(
            understood = true,
            intent = "CREATE_TASK",
            confidence = 0.9f,
            summary = "مهمة مشبوهة",
            actions = listOf(
                StructuredActionItem(
                    type = "CREATE_TASK",
                    title = "مهمة كود",
                    payload = mapOf("custom" to "<script>alert('xss')</script>")
                )
            )
        )

        val result = validator.validate(response)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validate clarification response returns Valid without checking actions`() {
        val response = StructuredRafiqResponse(
            understood = true,
            intent = "CREATE_REMINDER",
            confidence = 0.6f,
            summary = "توضيح",
            clarificationQuestion = "تحب أذكرك الساعة كام؟",
            actions = emptyList()
        )

        val result = validator.validate(response)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validate CREATE_TASK returns Valid`() {
        val response = StructuredRafiqResponse(
            understood = true,
            intent = "CREATE_TASK",
            confidence = 0.95f,
            summary = "إنشاء مهمة المذاكرة",
            actions = listOf(
                StructuredActionItem(
                    type = "CREATE_TASK",
                    title = "مذاكرة الفيزياء",
                    durationMinutes = 45
                )
            )
        )
        val result = validator.validate(response)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validate SEARCH_YOUTUBE returns Valid`() {
        val response = StructuredRafiqResponse(
            understood = true,
            intent = "SEARCH_YOUTUBE",
            confidence = 0.92f,
            summary = "بحث يوتيوب",
            actions = listOf(
                StructuredActionItem(
                    type = "SEARCH_YOUTUBE",
                    title = "بحث عن شرح المفعول المطلق",
                    target = "شرح المفعول المطلق"
                )
            )
        )
        val result = validator.validate(response)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validate OPEN_QURAN returns Valid`() {
        val response = StructuredRafiqResponse(
            understood = true,
            intent = "OPEN_QURAN",
            confidence = 0.96f,
            summary = "فتح سورة آل عمران",
            actions = listOf(
                StructuredActionItem(
                    type = "OPEN_QURAN",
                    title = "سورة آل عمران آية 15",
                    target = "سورة آل عمران آية 15"
                )
            )
        )
        val result = validator.validate(response)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validate REPLAN_DAY returns Valid`() {
        val response = StructuredRafiqResponse(
            understood = true,
            intent = "REPLAN_DAY",
            confidence = 0.95f,
            summary = "إعادة ترتيب اليوم",
            actions = listOf(
                StructuredActionItem(
                    type = "REPLAN_DAY",
                    title = "إعادة ترتيب اليوم",
                    payload = mapOf("delay_hours" to "2")
                )
            )
        )
        val result = validator.validate(response)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validate valid OPEN_URL returns Valid`() {
        val response = StructuredRafiqResponse(
            understood = true,
            intent = "OPEN_URL",
            confidence = 0.95f,
            summary = "فتح موقع موثوق",
            actions = listOf(
                StructuredActionItem(
                    type = "OPEN_URL",
                    title = "فتح محرك البحث",
                    target = "https://www.google.com"
                )
            )
        )
        val result = validator.validate(response)
        assertTrue(result is ValidationResult.Valid)
    }
}
