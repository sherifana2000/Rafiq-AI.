package com.example.brain.service

import com.example.domain.parser.CommandParser
import com.example.model.ActionType
import com.example.model.ExecutionStatus
import com.example.model.UserCommand
import java.util.UUID

/**
 * محول يربط RafiqBrain بواجهة CommandParser الحالية لضمان توافق النواة القديمة
 * مع محرك Gemini دون كسر أي من عقود الـ Domain أو UseCases.
 */
class BrainCommandParser(
    private val brain: RafiqBrain
) : CommandParser {

    override suspend fun parse(rawText: String): UserCommand {
        return when (val result = brain.processCommand(rawText)) {
            is BrainResult.Success -> result.command
            is BrainResult.NeedsClarification -> {
                UserCommand(
                    id = UUID.randomUUID().toString(),
                    rawText = rawText,
                    intent = ActionType.UNKNOWN,
                    title = "يحتاج توضيح",
                    confidence = result.response.confidence,
                    status = ExecutionStatus.PENDING,
                    responseFeedbackArabic = result.question
                )
            }
            is BrainResult.ValidationError -> {
                UserCommand(
                    id = UUID.randomUUID().toString(),
                    rawText = rawText,
                    intent = ActionType.UNKNOWN,
                    title = "أمر غير صالح",
                    confidence = 0f,
                    status = ExecutionStatus.FAILED,
                    responseFeedbackArabic = result.reason
                )
            }
            is BrainResult.NetworkError -> {
                UserCommand(
                    id = UUID.randomUUID().toString(),
                    rawText = rawText,
                    intent = ActionType.UNKNOWN,
                    title = "خطأ في الشبكة",
                    confidence = 0f,
                    status = ExecutionStatus.FAILED,
                    responseFeedbackArabic = result.message
                )
            }
            is BrainResult.ServerError -> {
                UserCommand(
                    id = UUID.randomUUID().toString(),
                    rawText = rawText,
                    intent = ActionType.UNKNOWN,
                    title = "خطأ في الخادم",
                    confidence = 0f,
                    status = ExecutionStatus.FAILED,
                    responseFeedbackArabic = result.message
                )
            }
            is BrainResult.AIUnavailable -> {
                UserCommand(
                    id = UUID.randomUUID().toString(),
                    rawText = rawText,
                    intent = ActionType.UNKNOWN,
                    title = "الذكاء غير متاح",
                    confidence = 0f,
                    status = ExecutionStatus.FAILED,
                    responseFeedbackArabic = result.message
                )
            }
        }
    }
}
