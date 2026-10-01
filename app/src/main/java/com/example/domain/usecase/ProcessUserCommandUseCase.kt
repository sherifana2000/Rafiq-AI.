package com.example.domain.usecase

import com.example.domain.action.ActionEngine
import com.example.domain.parser.CommandParser
import com.example.domain.repository.TaskRepository
import com.example.model.Action
import com.example.model.ActionType
import com.example.model.ExecutionStatus
import com.example.model.ScheduledTask
import com.example.model.TaskPriority
import com.example.model.TaskSource
import com.example.model.UserCommand
import java.util.UUID

/**
 * حالة الإرجاع عند معالجة أمر المستخدم
 */
data class ProcessCommandResult(
    val command: UserCommand,
    val isSuccess: Boolean,
    val feedbackMessageArabic: String
)

/**
 * UseCase مركزي لمعالجة أمر المستخدم من خلال:
 * التحليل (Command Parser) -> التنفيذ الأولي (Action Engine) -> الحفظ (Repository)
 */
class ProcessUserCommandUseCase(
    private val commandParser: CommandParser,
    private val actionEngine: ActionEngine,
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(rawInput: String): ProcessCommandResult {
        if (rawInput.isBlank()) {
            val emptyCmd = UserCommand(
                id = UUID.randomUUID().toString(),
                rawText = "",
                intent = ActionType.CREATE_TASK,
                title = "أمر فارغ",
                status = ExecutionStatus.FAILED,
                responseFeedbackArabic = "يرجى كتابة ما تريده قبل الإرسال."
            )
            return ProcessCommandResult(
                command = emptyCmd,
                isSuccess = false,
                feedbackMessageArabic = "يرجى إدخال نص الأمر."
            )
        }

        // 1. تحليل الأمر عبر Command Parser
        val parsedCommand = commandParser.parse(rawInput)

        // 2. التنفيذ والحفظ
        return executeAndRecord(parsedCommand)
    }

    /**
     * تنفيذ أمر تم تحليله مسبقاً بعد تأكيد المستخدم (Confirmation Flow)
     * يتم التنفيذ مباشرة دون إعادة استدعاء Command Parser أو Gemini مرة أخرى.
     */
    suspend fun executeConfirmedCommand(command: UserCommand): ProcessCommandResult {
        return executeAndRecord(command)
    }

    private suspend fun executeAndRecord(parsedCommand: UserCommand): ProcessCommandResult {
        // 2. تمرير الأمر إلى Action Engine
        val action = parsedCommand.action ?: Action(
            type = parsedCommand.intent,
            title = parsedCommand.title
        )
        val executionResult = actionEngine.execute(action)

        // 3. تحديث حالة الأمر
        val finalCommand = parsedCommand.copy(
            status = executionResult.status,
            responseFeedbackArabic = executionResult.feedbackMessageArabic
        )

        // 4. حفظ الأمر في المستودع
        taskRepository.addUserCommand(finalCommand)

        // 5. إذا كان الأمر يتطلب إضافة مهمة للجدول أو إعادة الترتيب
        when (finalCommand.intent) {
            ActionType.REPLAN_DAY -> {
                val delayHours = action.payload["delay_hours"]?.toIntOrNull() ?: 2
                taskRepository.replanDay(delayHours)
            }
            ActionType.CREATE_TASK,
            ActionType.CREATE_REMINDER,
            ActionType.OPEN_APP,
            ActionType.OPEN_QURAN,
            ActionType.START_FOCUS_SESSION,
            ActionType.OPEN_URL,
            ActionType.OPEN_FILE,
            ActionType.PLAY_AUDIO,
            ActionType.SEARCH_WEB,
            ActionType.SEARCH_YOUTUBE,
            ActionType.SUMMARIZE_DOCUMENT,
            ActionType.CREATE_FLASHCARDS -> {
                val priority = when (finalCommand.intent) {
                    ActionType.START_FOCUS_SESSION -> TaskPriority.HIGH
                    ActionType.CREATE_REMINDER -> TaskPriority.HIGH
                    else -> TaskPriority.NORMAL
                }
                val newTask = ScheduledTask(
                    id = "task-${System.currentTimeMillis()}",
                    title = finalCommand.title,
                    description = action.description.ifBlank { action.type.descriptionArabic },
                    action = action,
                    scheduleRule = finalCommand.scheduleRule,
                    priority = priority,
                    source = TaskSource.USER_COMMAND,
                    status = executionResult.status,
                    timeLabelArabic = finalCommand.scheduleRule.targetTimeArabic ?: "محدد لاحقاً",
                    dateLabelArabic = finalCommand.scheduleRule.targetDateArabic ?: "اليوم",
                    isCurrentNow = false
                )
                taskRepository.addScheduledTask(newTask)
            }
            ActionType.UNKNOWN -> {
                // لا ننشئ مهمة مجدولة للأوامر غير المعروفة
            }
        }

        return ProcessCommandResult(
            command = finalCommand,
            isSuccess = executionResult.isSuccess,
            feedbackMessageArabic = executionResult.feedbackMessageArabic
        )
    }
}
