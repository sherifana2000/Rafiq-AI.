package com.example.domain.parser

import com.example.model.Action
import com.example.model.ActionType
import com.example.model.ExecutionStatus
import com.example.model.RepeatFrequency
import com.example.model.ScheduleRule
import com.example.model.UserCommand
import java.util.UUID

/**
 * محلل تجريبي مؤقت ومنظم يحاكي استخراج النوايا (Intent) والكيانات (Entities)
 * من نصوص الأوامر العربية المحددة في المتطلبات، ليتم استبداله مستقبلاً بمحرك الذكاء الاصطناعي.
 */
class MockCommandParser : CommandParser {

    override suspend fun parse(rawText: String): UserCommand {
        val trimmed = rawText.trim()
        val normalized = stripDiacritics(trimmed)
        val id = UUID.randomUUID().toString()

        val isReplan = normalized.contains("اعد ترتيب") || normalized.contains("تاخرت") ||
                normalized.contains("رتب يومي") || normalized.contains("رتب جدول") ||
                trimmed.contains("أعد ترتيب") || trimmed.contains("تأخرت")

        val isDaily = normalized.contains("كل يوم") || normalized.contains("يوميا") ||
                trimmed.contains("يومياً")

        val isTomorrow = normalized.contains("بكرة") || normalized.contains("غدا") ||
                trimmed.contains("غداً") || trimmed.contains("غدًا")

        val isQuran = normalized.contains("القران") || normalized.contains("المصحف") ||
                normalized.contains("سورة") || normalized.contains("اية") || normalized.contains("ال عمران") ||
                trimmed.contains("القرآن") || trimmed.contains("آية") || trimmed.contains("آل عمران")

        val isFocus = normalized.contains("تركيز") || normalized.contains("جلسة تركيز") ||
                trimmed.contains("focus", ignoreCase = true)

        val isReminder = normalized.contains("ذكرني") || normalized.contains("تنبيه") ||
                normalized.contains("فكرني") || trimmed.contains("ذكّرني")

        val isYouTube = normalized.contains("يوتيوب") || trimmed.contains("youtube", ignoreCase = true)
        val isOpenApp = normalized.contains("افتح") || normalized.contains("شغل")
        val isSearch = normalized.contains("ابحث") || normalized.contains("بحث")

        // 1. إعادة ترتيب اليوم (REPLAN_DAY)
        if (isReplan) {
            val hours = extractNumber(trimmed) ?: 2
            val action = Action(
                type = ActionType.REPLAN_DAY,
                title = "إعادة ترتيب اليوم",
                payload = mapOf("delay_hours" to hours.toString()),
                description = "إزاحة مواعيد المهام المتبقية بمقدار $hours ساعة"
            )
            return UserCommand(
                id = id,
                rawText = trimmed,
                intent = ActionType.REPLAN_DAY,
                title = "إعادة ترتيب جدول باقي اليوم",
                scheduleRule = ScheduleRule(
                    frequency = RepeatFrequency.NONE,
                    targetDateArabic = "اليوم",
                    humanReadableArabic = "فوراً - تأخير $hours ساعة"
                ),
                confidence = 0.98f,
                status = ExecutionStatus.PENDING,
                action = action,
                responseFeedbackArabic = "سأعيد ترتيب المهام المتبقية بناءً على التأخير."
            )
        }

        // 2. أوامر القرآن (OPEN_QURAN)
        if (isQuran && (isOpenApp || trimmed.contains("على سورة") || trimmed.contains("على آل عمران") || trimmed.contains("افتح القرآن"))) {
            val surahAndAyah = extractSurahAndAyah(trimmed)
            val time = extractTime(trimmed) ?: "10:00 صباحاً"
            val repeat = if (isDaily) RepeatFrequency.DAILY else RepeatFrequency.NONE
            val action = Action(
                type = ActionType.OPEN_QURAN,
                title = if (surahAndAyah.isNotBlank()) "تلاوة $surahAndAyah" else "تطبيق القرآن الكريم",
                target = surahAndAyah.ifBlank { "القرآن الكريم" },
                description = "فتح المصحف عند التوقيت المحدد"
            )
            return UserCommand(
                id = id,
                rawText = trimmed,
                intent = ActionType.OPEN_QURAN,
                title = action.title,
                scheduleRule = ScheduleRule(
                    frequency = repeat,
                    targetDateArabic = if (isDaily) "كل يوم" else if (isTomorrow) "غداً" else "اليوم",
                    targetTimeArabic = time,
                    humanReadableArabic = if (isDaily) "يومياً الساعة $time" else "الساعة $time"
                ),
                confidence = 0.96f,
                status = ExecutionStatus.PENDING,
                action = action,
                responseFeedbackArabic = "تم تحليل الأمر: فتح المصحف الشريف (${action.target}) الساعة $time."
            )
        }

        // 3. جلسات التركيز (START_FOCUS_SESSION)
        if (isFocus) {
            val durationMinutes = extractNumber(trimmed) ?: 50
            val targetTask = if (trimmed.contains("مراجعة") || trimmed.contains("نحو")) "مراجعة النحو" else "جلسة إنجاز وتركيز"
            val action = Action(
                type = ActionType.START_FOCUS_SESSION,
                title = "جلسة تركيز ($durationMinutes دقيقة)",
                payload = mapOf(
                    "duration_minutes" to durationMinutes.toString(),
                    "task_name" to targetTask
                ),
                description = "كتم المشتتات والتركيز على: $targetTask"
            )
            return UserCommand(
                id = id,
                rawText = trimmed,
                intent = ActionType.START_FOCUS_SESSION,
                title = "جلسة تركيز",
                scheduleRule = ScheduleRule(
                    frequency = RepeatFrequency.NONE,
                    targetDateArabic = "اليوم",
                    humanReadableArabic = "المدة: $durationMinutes دقيقة"
                ),
                confidence = 0.94f,
                status = ExecutionStatus.PENDING,
                action = action,
                responseFeedbackArabic = "جاهز لبدء جلسة تركيز لمدة $durationMinutes دقيقة."
            )
        }

        // 4. التذكيرات المرتبطة بالصلاة أو المواعيد (CREATE_REMINDER)
        if (isReminder || trimmed.contains("بعد صلاة") || trimmed.contains("بعد صلاه")) {
            val prayer = extractPrayer(trimmed)
            val time = extractTime(trimmed) ?: if (isTomorrow) "08:00 صباحاً" else "09:00 صباحاً"
            val cleanTitle = extractReminderTitle(trimmed)
            val repeat = if (isDaily) RepeatFrequency.DAILY else RepeatFrequency.NONE

            val schedule = ScheduleRule(
                frequency = repeat,
                targetDateArabic = if (isTomorrow) "غداً" else if (isDaily) "كل يوم" else "اليوم",
                targetTimeArabic = if (prayer != null) null else time,
                prayerReference = prayer,
                humanReadableArabic = if (prayer != null) "بعد $prayer" else if (isTomorrow) "غداً الساعة $time" else "الساعة $time"
            )

            val futureIsoTime = if (isTomorrow) {
                java.time.LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString()
            } else {
                java.time.LocalDateTime.now().plusHours(2).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString()
            }

            val action = Action(
                type = ActionType.CREATE_REMINDER,
                title = cleanTitle,
                scheduledTime = futureIsoTime,
                repeat = if (isDaily) "DAILY" else "NONE",
                payload = mapOf("scheduledTime" to futureIsoTime),
                description = "تذكير مجدول: $cleanTitle"
            )

            return UserCommand(
                id = id,
                rawText = trimmed,
                intent = ActionType.CREATE_REMINDER,
                title = cleanTitle,
                scheduleRule = schedule,
                confidence = 0.95f,
                status = ExecutionStatus.PENDING,
                action = action,
                responseFeedbackArabic = "تم ضبط تذكير '$cleanTitle' (${schedule.humanReadableArabic})."
            )
        }

        // 5. يوتيوب (SEARCH_YOUTUBE)
        if (isYouTube) {
            val query = trimmed.replace("ابحث في يوتيوب عن", "")
                .replace("ابحث في يوتيوب", "")
                .replace("شغل في يوتيوب", "")
                .replace("يوتيوب", "")
                .trim()
                .ifBlank { "محتوى مقترح" }

            val action = Action(
                type = ActionType.SEARCH_YOUTUBE,
                title = "بحث يوتيوب: $query",
                target = query,
                description = "البحث وتشغيل فيديو يوتيوب"
            )

            return UserCommand(
                id = id,
                rawText = trimmed,
                intent = ActionType.SEARCH_YOUTUBE,
                title = "بحث يوتيوب",
                scheduleRule = ScheduleRule(
                    frequency = RepeatFrequency.NONE,
                    targetDateArabic = "اليوم",
                    humanReadableArabic = "فوراً"
                ),
                confidence = 0.93f,
                status = ExecutionStatus.PENDING,
                action = action,
                responseFeedbackArabic = "تم تجهيز البحث في يوتيوب عن: $query."
            )
        }

        // 6. فتح تطبيق (OPEN_APP)
        if (isOpenApp) {
            val appName = extractAppName(trimmed)
            val time = extractTime(trimmed) ?: "08:00 مساءً"

            val action = Action(
                type = ActionType.OPEN_APP,
                title = "فتح $appName",
                target = appName,
                description = "تشغيل تطبيق $appName"
            )

            return UserCommand(
                id = id,
                rawText = trimmed,
                intent = ActionType.OPEN_APP,
                title = "فتح $appName",
                scheduleRule = ScheduleRule(
                    frequency = if (isDaily) RepeatFrequency.DAILY else RepeatFrequency.NONE,
                    targetDateArabic = if (isTomorrow) "غداً" else "اليوم",
                    targetTimeArabic = time,
                    humanReadableArabic = "الساعة $time"
                ),
                confidence = 0.93f,
                status = ExecutionStatus.PENDING,
                action = action,
                responseFeedbackArabic = "تمت الجدولة لفتح $appName في تمام $time."
            )
        }

        // 7. بحث الويب (SEARCH_WEB)
        if (isSearch) {
            val query = trimmed.replace("ابحث عن", "")
                .replace("ابحث في الويب عن", "")
                .replace("ابحث", "")
                .trim()
            val action = Action(
                type = ActionType.SEARCH_WEB,
                title = "بحث عن: $query",
                target = query,
                description = "استعلام بحث"
            )
            return UserCommand(
                id = id,
                rawText = trimmed,
                intent = ActionType.SEARCH_WEB,
                title = "بحث في الويب",
                confidence = 0.90f,
                status = ExecutionStatus.PENDING,
                action = action,
                responseFeedbackArabic = "تم تسجيل طلب البحث: $query."
            )
        }

        // 8. افتراضي: إنشاء مهمة عادية (CREATE_TASK)
        val defaultTime = extractTime(trimmed)
        val action = Action(
            type = ActionType.CREATE_TASK,
            title = trimmed,
            description = "مهمة مضافة عبر الأوامر"
        )
        return UserCommand(
            id = id,
            rawText = trimmed,
            intent = ActionType.CREATE_TASK,
            title = trimmed,
            scheduleRule = ScheduleRule(
                frequency = if (isDaily) RepeatFrequency.DAILY else RepeatFrequency.NONE,
                targetDateArabic = if (isTomorrow) "غداً" else "اليوم",
                targetTimeArabic = defaultTime ?: "خلال اليوم",
                humanReadableArabic = if (defaultTime != null) "الساعة $defaultTime" else "خلال اليوم"
            ),
            confidence = 0.85f,
            status = ExecutionStatus.PENDING,
            action = action,
            responseFeedbackArabic = "تم إدراج المهمة الجديدة في جدولك بنجاح."
        )
    }

    private fun stripDiacritics(str: String): String =
        str.replace(Regex("[\u064B-\u065F\u0670]"), "")

    private fun extractTime(text: String): String? {
        val regex = Regex("""الساعة\s*(\d{1,2})(?::(\d{2}))?\s*(صباحاً|مساءً|ص|م)?""")
        val match = regex.find(text)
        if (match != null) {
            val hour = match.groupValues[1]
            val minutes = match.groupValues[2].ifBlank { "00" }
            val period = when (match.groupValues[3]) {
                "مساءً", "م" -> "مساءً"
                "صباحاً", "ص" -> "صباحاً"
                else -> if (text.contains("مساءً") || text.contains("مساء")) "مساءً" else "صباحاً"
            }
            val paddedHour = if (hour.length == 1) "0$hour" else hour
            return "$paddedHour:$minutes $period"
        }
        return null
    }

    private fun extractNumber(text: String): Int? {
        val digits = Regex("""\d+""").find(text)?.value?.toIntOrNull()
        if (digits != null) return digits
        if (text.contains("ساعتين")) return 2
        if (text.contains("ساعة")) return 1
        if (text.contains("نصف ساعة")) return 30
        return null
    }

    private fun extractPrayer(text: String): String? {
        val prayers = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
        for (prayer in prayers) {
            if (text.contains(prayer)) return "صلاة $prayer"
        }
        return null
    }

    private fun extractSurahAndAyah(text: String): String {
        val index = text.indexOf("سورة")
        if (index != -1) {
            return text.substring(index).trim()
        }
        if (text.contains("آل عمران")) {
            val ayahPart = if (text.contains("آية") || text.contains("اية")) {
                " آية " + (Regex("""\d+""").find(text)?.value ?: "15")
            } else ""
            return "سورة آل عمران$ayahPart"
        }
        if (text.contains("صفحتين")) return "قراءة صفحتين من القرآن"
        return "القرآن الكريم"
    }

    private fun extractReminderTitle(text: String): String {
        var clean = text
        val removals = listOf(
            "بكرة", "غداً", "غدا", "غدًا", "اليوم", "كل يوم", "يومياً", "يوميا",
            "الساعة 8 مساءً", "الساعة 9 صباحاً", "الساعة 10 صباحاً",
            "الساعة 8", "الساعة 9", "الساعة 10",
            "بعد صلاة الفجر", "بعد صلاة الظهر", "بعد صلاة العصر", "بعد صلاة المغرب", "بعد صلاة العشاء",
            "ذكرني", "ذكّرني", "فكرني", "تنبيه"
        )
        for (token in removals) {
            clean = clean.replace(token, "")
        }
        clean = clean.replace(Regex("""الساعة\s*\d+(\s*(صباحاً|مساءً))?"""), "")
        clean = clean.trim()
        if (clean.startsWith("أن ") || clean.startsWith("ان ")) clean = clean.substring(3).trim()
        if (clean.startsWith("بـ") || clean.startsWith("ب")) clean = clean.substring(1).trim()
        if (clean.isBlank()) return "مهمة تذكير"
        return clean
    }

    private fun extractAppName(text: String): String {
        val targets = listOf("Facebook", "WhatsApp", "YouTube", "فيسبوك", "واتساب", "يوتيوب", "تويتر", "إنستغرام", "القرآن")
        for (target in targets) {
            if (text.contains(target, ignoreCase = true)) return target
        }
        val afterOpen = text.substringAfter("افتح", "").trim()
        if (afterOpen.isNotBlank()) {
            return afterOpen.split(" ").firstOrNull() ?: "التطبيق"
        }
        return "التطبيق"
    }
}
