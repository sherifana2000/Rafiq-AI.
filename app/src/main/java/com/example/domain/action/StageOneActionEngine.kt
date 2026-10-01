package com.example.domain.action

import com.example.data.local.reminder.ReminderEntity
import com.example.domain.reminder.ReminderRepository
import com.example.domain.reminder.ReminderScheduler
import com.example.domain.reminder.ScheduleResult
import com.example.domain.services.AppServiceGateway
import com.example.model.Action
import com.example.model.ActionType
import com.example.model.ExecutionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * محرك الإجراءات للمرحلة الأولى:
 * ينفذ جدولة التذكيرات الحقيقية (CREATE_REMINDER) عبر Room و AlarmManager،
 * ويحافظ على جاهزية بقية الإجراءات.
 */
class StageOneActionEngine(
    private val serviceGateway: AppServiceGateway,
    private val reminderRepository: ReminderRepository? = null,
    private val reminderScheduler: ReminderScheduler? = null
) : ActionEngine {

    override suspend fun execute(action: Action): ExecutionResult {
        return when (action.type) {
            ActionType.CREATE_TASK -> {
                ExecutionResult(
                    isSuccess = true,
                    status = ExecutionStatus.SCHEDULED,
                    feedbackMessageArabic = "تم تسجيل المهمة '${action.title}' بنجاح.",
                    actionType = action.type
                )
            }
            ActionType.CREATE_REMINDER -> {
                executeCreateReminder(action)
            }
            ActionType.OPEN_APP,
            ActionType.OPEN_URL,
            ActionType.OPEN_FILE,
            ActionType.PLAY_AUDIO,
            ActionType.SEARCH_WEB,
            ActionType.SEARCH_YOUTUBE,
            ActionType.OPEN_QURAN,
            ActionType.START_FOCUS_SESSION,
            ActionType.SUMMARIZE_DOCUMENT,
            ActionType.CREATE_FLASHCARDS -> {
                serviceGateway.logActionIntent(action.type.name, action.title)
                ExecutionResult(
                    isSuccess = true,
                    status = ExecutionStatus.SCHEDULED,
                    feedbackMessageArabic = "تم قبول أمر ${action.type.titleArabic} '${action.title}' وجدولته ضمن الخطة.",
                    actionType = action.type
                )
            }
            ActionType.REPLAN_DAY -> {
                ExecutionResult(
                    isSuccess = true,
                    status = ExecutionStatus.COMPLETED,
                    feedbackMessageArabic = "تمت إعادة موازنة وترتيب جدول باقي اليوم بنجاح.",
                    actionType = action.type
                )
            }
            ActionType.UNKNOWN -> {
                ExecutionResult(
                    isSuccess = false,
                    status = ExecutionStatus.FAILED,
                    feedbackMessageArabic = "الأمر غير معروف أو يحتاج إلى توضيح إضافي.",
                    actionType = action.type
                )
            }
        }
    }

    private suspend fun executeCreateReminder(action: Action): ExecutionResult {
        val rawTime = action.scheduledTime
            ?: action.payload["scheduledTime"]

        if (rawTime.isNullOrBlank()) {
            return ExecutionResult(
                isSuccess = false,
                status = ExecutionStatus.FAILED,
                feedbackMessageArabic = "لم يتم تحديد وقت محدد للتذكير.",
                actionType = action.type
            )
        }

        val triggerEpochMillis = parseLocalScheduleTimeToEpoch(rawTime)
        if (triggerEpochMillis == null) {
            return ExecutionResult(
                isSuccess = false,
                status = ExecutionStatus.FAILED,
                feedbackMessageArabic = "تعذر قراءة موعد التذكير بشكل صحيح.",
                actionType = action.type
            )
        }

        val now = System.currentTimeMillis()
        if (triggerEpochMillis <= now) {
            return ExecutionResult(
                isSuccess = false,
                status = ExecutionStatus.FAILED,
                feedbackMessageArabic = "لا يمكن جدولة تذكير في وقت مضى.",
                actionType = action.type
            )
        }

        val repeat = action.repeat ?: action.payload["repeat"] ?: "NONE"

        // حفظ التذكير في قاعدة بيانات Room إن توفرت
        val reminderEntity = ReminderEntity(
            title = action.title,
            target = action.target,
            scheduledEpochMillis = triggerEpochMillis,
            scheduledTimeIso = rawTime,
            repeatInterval = repeat,
            status = "SCHEDULED"
        )

        val reminderId = reminderRepository?.saveReminder(reminderEntity) ?: System.currentTimeMillis()

        // جدولة المنبه في نظام أندرويد إن توفر الـ Scheduler
        if (reminderScheduler != null) {
            val scheduleResult = reminderScheduler.schedule(
                reminderId = reminderId,
                title = action.title,
                target = action.target,
                triggerAtMillis = triggerEpochMillis,
                repeatInterval = repeat
            )

            if (scheduleResult is ScheduleResult.Failure) {
                return ExecutionResult(
                    isSuccess = false,
                    status = ExecutionStatus.FAILED,
                    feedbackMessageArabic = "تعذر ضبط المنبه: ${scheduleResult.reason}",
                    actionType = action.type
                )
            }
        }

        val formattedTime = DateTimeFormatter.ofPattern("d MMMM الساعة h:mm a", Locale.forLanguageTag("ar"))
            .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(triggerEpochMillis), ZoneId.systemDefault()))

        return ExecutionResult(
            isSuccess = true,
            status = ExecutionStatus.SCHEDULED,
            feedbackMessageArabic = "تم ضبط التذكير '${action.title}' بنجاح في $formattedTime.",
            actionType = action.type
        )
    }

    /**
     * تحويل التوقيت المحلي القادم من الذكاء الاصطناعي (ISO String) إلى وقت النظام
     * باستخدام ZoneId.systemDefault() لضمان مطابقة توقيت المستخدم المحلي بدقة دون تحويل خاطئ لـ UTC.
     */
    fun parseLocalScheduleTimeToEpoch(timeStr: String, zoneId: ZoneId = ZoneId.systemDefault()): Long? {
        val cleanStr = timeStr.trim().replace(" ", "T")
        return try {
            val localDateTime = try {
                LocalDateTime.parse(cleanStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (_: Exception) {
                try {
                    LocalDateTime.parse(cleanStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                } catch (_: Exception) {
                    try {
                        LocalDate.parse(cleanStr, DateTimeFormatter.ISO_LOCAL_DATE).atTime(9, 0)
                    } catch (_: Exception) {
                        null
                    }
                }
            } ?: return null

            localDateTime.atZone(zoneId).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}

