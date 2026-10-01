package com.example.brain.service

import com.example.brain.client.BrainApiResponse
import com.example.brain.client.RafiqBrainClient
import com.example.brain.client.RafiqBrainRequest
import com.example.brain.logging.RafiqLogger
import com.example.brain.model.RafiqUserContext
import com.example.brain.model.StructuredRafiqResponse
import com.example.brain.validator.RafiqActionValidator
import com.example.brain.validator.ValidationResult
import com.example.model.Action
import com.example.model.ActionType
import com.example.model.ExecutionStatus
import com.example.model.RepeatFrequency
import com.example.model.ScheduleRule
import com.example.model.UserCommand
import java.util.UUID

sealed class BrainResult {
    data class Success(val response: StructuredRafiqResponse, val command: UserCommand) : BrainResult()
    data class NeedsClarification(val question: String, val response: StructuredRafiqResponse) : BrainResult()
    data class ValidationError(val reason: String) : BrainResult()
    data class NetworkError(val message: String) : BrainResult()
    data class ServerError(val message: String) : BrainResult()
    data class AIUnavailable(val message: String) : BrainResult()
}

/**
 * واجهة عقل رفيق الذكي (RafiqBrain).
 * المسؤولة عن تنسيق تدفق الأوامر بين العميل والتحقق والمنطق التوجيهي.
 */
interface RafiqBrain {
    suspend fun processCommand(rawText: String, context: RafiqUserContext = RafiqUserContext.current()): BrainResult
}

/**
 * التطبيق الأساسي لـ RafiqBrain
 */
class DefaultRafiqBrain(
    private val client: RafiqBrainClient,
    private val validator: RafiqActionValidator = RafiqActionValidator()
) : RafiqBrain {

    override suspend fun processCommand(rawText: String, context: RafiqUserContext): BrainResult {
        val trimmed = rawText.trim()
        val startTime = System.currentTimeMillis()

        if (trimmed.isBlank()) {
            return BrainResult.ValidationError("يرجى إدخال نص الأمر قبل الإرسال.")
        }

        val request = RafiqBrainRequest(userCommand = trimmed, context = context)

        // استدعاء Backend API / Gemini Client
        val apiResponse = client.analyzeCommand(request)
        val latencyMs = System.currentTimeMillis() - startTime

        return when (apiResponse) {
            is BrainApiResponse.Success -> {
                val structured = apiResponse.response

                // 1. التحقق من الثقة وحالات الغموض (Requirement 9)
                // إذا كانت الثقة أقل من 0.70 أو يحتوي على سؤال استيضاحي، لا ننفذ ولا نخمن
                if (structured.confidence < 0.70f || !structured.clarificationQuestion.isNullOrBlank()) {
                    val question = structured.clarificationQuestion
                        ?: "أكيد، تحب توضح لي أكثر الوقت أو التفاصيل المحددة؟"
                    RafiqLogger.logRequest(structured.intent, latencyMs, structured.confidence, isSuccess = true)
                    BrainResult.NeedsClarification(question, structured)
                } else {
                    // 2. التحقق من صحة الاستجابة والأفعال عبر Validator (Requirement 12)
                    when (val valResult = validator.validate(structured)) {
                        is ValidationResult.Invalid -> {
                            RafiqLogger.logError("VALIDATION_FAILED", latencyMs, valResult.reason)
                            BrainResult.ValidationError(valResult.reason)
                        }
                        is ValidationResult.Valid -> {
                            RafiqLogger.logRequest(structured.intent, latencyMs, structured.confidence, isSuccess = true)
                            val userCommand = mapToDomainCommand(trimmed, structured)
                            BrainResult.Success(structured, userCommand)
                        }
                    }
                }
            }

            is BrainApiResponse.NeedsClarification -> {
                RafiqLogger.logRequest(apiResponse.response.intent, latencyMs, apiResponse.response.confidence, isSuccess = true)
                BrainResult.NeedsClarification(apiResponse.question, apiResponse.response)
            }

            is BrainApiResponse.NetworkError -> {
                RafiqLogger.logError("NETWORK_ERROR", latencyMs, apiResponse.message)
                BrainResult.NetworkError(apiResponse.message)
            }

            is BrainApiResponse.ServerError -> {
                RafiqLogger.logError("SERVER_ERROR_${apiResponse.statusCode}", latencyMs, apiResponse.message)
                BrainResult.ServerError("حدث خطأ في معالجة الطلب على الخادم (${apiResponse.statusCode}).")
            }

            is BrainApiResponse.AIUnavailable -> {
                RafiqLogger.logError("AI_UNAVAILABLE", latencyMs, apiResponse.message)
                BrainResult.AIUnavailable(apiResponse.message)
            }

            is BrainApiResponse.AuthenticationRequired -> {
                RafiqLogger.logError("AUTH_REQUIRED", latencyMs, apiResponse.message)
                BrainResult.AIUnavailable(apiResponse.message)
            }

            is BrainApiResponse.MalformedResponse -> {
                RafiqLogger.logError("MALFORMED_RESPONSE", latencyMs, apiResponse.reason)
                BrainResult.ValidationError("استجابة الذكاء الاصطناعي غير صالحة.")
            }
        }
    }

    /**
     * تحويل الرد المنظم إلى كائن Domain Model (UserCommand) القابل للعرض والجدولة
     */
    private fun mapToDomainCommand(rawText: String, response: StructuredRafiqResponse): UserCommand {
        val primaryActionItem = response.actions.firstOrNull()

        val intentType = try {
            ActionType.valueOf(response.intent.uppercase())
        } catch (_: Exception) {
            ActionType.UNKNOWN
        }

        val primaryActionType = if (primaryActionItem != null) {
            try {
                ActionType.valueOf(primaryActionItem.type.uppercase())
            } catch (_: Exception) {
                intentType
            }
        } else intentType

        val actionPayload = mutableMapOf<String, String>().apply {
            putAll(primaryActionItem?.payload ?: emptyMap())
            primaryActionItem?.scheduledTime?.let { put("scheduledTime", it) }
            primaryActionItem?.repeat?.let { put("repeat", it) }
        }

        val action = Action(
            type = primaryActionType,
            title = primaryActionItem?.title ?: response.summary,
            target = primaryActionItem?.target,
            scheduledTime = primaryActionItem?.scheduledTime,
            repeat = primaryActionItem?.repeat,
            payload = actionPayload,
            description = response.summary
        )

        val frequency = when (primaryActionItem?.repeat?.uppercase()) {
            "DAILY" -> RepeatFrequency.DAILY
            "WEEKLY" -> RepeatFrequency.WEEKLY
            else -> RepeatFrequency.NONE
        }

        val scheduleRule = ScheduleRule(
            frequency = frequency,
            targetTimeArabic = primaryActionItem?.scheduledTime ?: "محدد لاحقاً",
            humanReadableArabic = response.summary
        )

        return UserCommand(
            id = UUID.randomUUID().toString(),
            rawText = rawText,
            intent = intentType,
            title = primaryActionItem?.title ?: response.summary,
            scheduleRule = scheduleRule,
            confidence = response.confidence,
            status = ExecutionStatus.PENDING,
            action = action,
            responseFeedbackArabic = response.summary
        )
    }
}
