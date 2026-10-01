package com.example.domain.repository

import com.example.model.Action
import com.example.model.ActionType
import com.example.model.ExecutionStatus
import com.example.model.RepeatFrequency
import com.example.model.ScheduleRule
import com.example.model.ScheduledTask
import com.example.model.TaskPriority
import com.example.model.TaskSource
import com.example.model.UserCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * تنفيذ الذاكرة لمستودع المهام، مهيأ مسبقاً ببيانات نموذجية واقعية تعكس يوماً منظماً
 * مطابقة للمتطلبات الدقيقة للنسخة التجريبية (Prototype).
 */
class InMemoryTaskRepository : TaskRepository {

    private fun generateDefaultTasks(): List<ScheduledTask> = listOf(
        ScheduledTask(
            id = "task-fajr",
            title = "صلاة الفجر",
            description = "في وقتها بالمسجد أو جماعة",
            action = Action(
                type = ActionType.CREATE_REMINDER,
                title = "صلاة الفجر",
                description = "موعد الصلاة"
            ),
            scheduleRule = ScheduleRule(
                frequency = RepeatFrequency.DAILY,
                targetTimeArabic = "05:30 ص",
                humanReadableArabic = "يومياً 05:30 ص"
            ),
            priority = TaskPriority.URGENT,
            source = TaskSource.SYSTEM,
            status = ExecutionStatus.COMPLETED,
            timeLabelArabic = "05:30 ص",
            dateLabelArabic = "اليوم",
            isCurrentNow = false,
            durationLabelArabic = null
        ),
        ScheduledTask(
            id = "task-quran-morning",
            title = "قراءة القرآن",
            description = "ورد الصباح المبارك مع التدبر",
            action = Action(
                type = ActionType.OPEN_QURAN,
                title = "قراءة القرآن",
                target = "سورة البقرة",
                description = "مصحف المدينة"
            ),
            scheduleRule = ScheduleRule(
                frequency = RepeatFrequency.DAILY,
                targetTimeArabic = "06:00 ص",
                humanReadableArabic = "يومياً 06:00 ص"
            ),
            priority = TaskPriority.HIGH,
            source = TaskSource.USER_COMMAND,
            status = ExecutionStatus.COMPLETED,
            timeLabelArabic = "06:00 ص",
            dateLabelArabic = "اليوم",
            isCurrentNow = false,
            durationLabelArabic = "30 دقيقة"
        ),
        ScheduledTask(
            id = "task-nahw-current",
            title = "مراجعة درس النحو",
            description = "حل تدريبات النحو ومراجعة باب الفاعل والمفعول",
            action = Action(
                type = ActionType.START_FOCUS_SESSION,
                title = "مراجعة درس النحو",
                description = "جلسة تركيز دراسي"
            ),
            scheduleRule = ScheduleRule(
                frequency = RepeatFrequency.NONE,
                targetTimeArabic = "08:00 ص",
                humanReadableArabic = "اليوم 08:00 — 10:00 ص"
            ),
            priority = TaskPriority.HIGH,
            source = TaskSource.USER_COMMAND,
            status = ExecutionStatus.IN_PROGRESS,
            timeLabelArabic = "08:00 ص",
            dateLabelArabic = "اليوم",
            isCurrentNow = true,
            durationLabelArabic = "50 دقيقة"
        ),
        ScheduledTask(
            id = "task-break-1",
            title = "استراحة",
            description = "شرب قهوة وتمدد خفيف بدون شاشات",
            action = Action(
                type = ActionType.CREATE_TASK,
                title = "استراحة",
                description = "فترة نقاهة"
            ),
            scheduleRule = ScheduleRule(
                frequency = RepeatFrequency.NONE,
                targetTimeArabic = "09:00 ص",
                humanReadableArabic = "اليوم 09:00 ص"
            ),
            priority = TaskPriority.LOW,
            source = TaskSource.SCHEDULED_RECURRING,
            status = ExecutionStatus.SCHEDULED,
            timeLabelArabic = "09:00 ص",
            dateLabelArabic = "اليوم",
            isCurrentNow = false,
            durationLabelArabic = "15 دقيقة"
        ),
        ScheduledTask(
            id = "task-study-2",
            title = "مذاكرة",
            description = "استكمال حل التمارين وتدوين الملاحظات",
            action = Action(
                type = ActionType.START_FOCUS_SESSION,
                title = "مذاكرة",
                description = "جلسة إنجاز"
            ),
            scheduleRule = ScheduleRule(
                frequency = RepeatFrequency.NONE,
                targetTimeArabic = "09:15 ص",
                humanReadableArabic = "اليوم 09:15 ص"
            ),
            priority = TaskPriority.NORMAL,
            source = TaskSource.USER_COMMAND,
            status = ExecutionStatus.SCHEDULED,
            timeLabelArabic = "09:15 ص",
            dateLabelArabic = "اليوم",
            isCurrentNow = false,
            durationLabelArabic = "90 دقيقة"
        ),
        ScheduledTask(
            id = "task-rest-noon",
            title = "راحة",
            description = "فترة استرخاء وغداء قبل جدول المساء",
            action = Action(
                type = ActionType.CREATE_TASK,
                title = "راحة",
                description = "استرخاء"
            ),
            scheduleRule = ScheduleRule(
                frequency = RepeatFrequency.DAILY,
                targetTimeArabic = "11:00 ص",
                humanReadableArabic = "اليوم 11:00 ص"
            ),
            priority = TaskPriority.LOW,
            source = TaskSource.SYSTEM,
            status = ExecutionStatus.SCHEDULED,
            timeLabelArabic = "11:00 ص",
            dateLabelArabic = "اليوم",
            isCurrentNow = false,
            durationLabelArabic = "60 دقيقة"
        )
    )

    private fun generateDefaultCommands(): List<UserCommand> = listOf(
        UserCommand(
            id = "cmd-1",
            rawText = "ذكّرني بمراجعة النحو",
            intent = ActionType.CREATE_REMINDER,
            title = "مراجعة درس النحو",
            scheduleRule = ScheduleRule(
                frequency = RepeatFrequency.NONE,
                targetDateArabic = "اليوم",
                targetTimeArabic = "08:00 صباحاً",
                humanReadableArabic = "الساعة 08:00 صباحاً"
            ),
            confidence = 0.98f,
            status = ExecutionStatus.COMPLETED,
            responseFeedbackArabic = "تم فهم الأمر"
        ),
        UserCommand(
            id = "cmd-2",
            rawText = "افتح القرآن على آل عمران",
            intent = ActionType.OPEN_QURAN,
            title = "سورة آل عمران",
            scheduleRule = ScheduleRule(
                frequency = RepeatFrequency.NONE,
                targetDateArabic = "اليوم",
                targetTimeArabic = "10:00 صباحاً",
                humanReadableArabic = "الساعة 10:00 صباحاً"
            ),
            confidence = 0.97f,
            status = ExecutionStatus.SCHEDULED,
            responseFeedbackArabic = "تم تحليل الأمر"
        ),
        UserCommand(
            id = "cmd-3",
            rawText = "أعد ترتيب يومي",
            intent = ActionType.REPLAN_DAY,
            title = "إعادة ترتيب يومي",
            scheduleRule = ScheduleRule(
                frequency = RepeatFrequency.NONE,
                targetDateArabic = "اليوم",
                humanReadableArabic = "فوراً"
            ),
            confidence = 0.99f,
            status = ExecutionStatus.COMPLETED,
            responseFeedbackArabic = "تم إعداد خطة جديدة"
        )
    )

    private val tasksFlow = MutableStateFlow<List<ScheduledTask>>(generateDefaultTasks())
    private val commandsFlow = MutableStateFlow<List<UserCommand>>(generateDefaultCommands())

    override fun getTodayTasks(): Flow<List<ScheduledTask>> = tasksFlow.asStateFlow()

    override fun getCurrentTask(): Flow<ScheduledTask?> {
        return tasksFlow.map { list ->
            list.firstOrNull { it.isCurrentNow } ?: list.firstOrNull { it.status == ExecutionStatus.IN_PROGRESS }
        }
    }

    override fun getRecentCommands(): Flow<List<UserCommand>> = commandsFlow.asStateFlow()

    override suspend fun addScheduledTask(task: ScheduledTask) {
        tasksFlow.value = listOf(task) + tasksFlow.value
    }

    override suspend fun addUserCommand(command: UserCommand) {
        commandsFlow.value = listOf(command) + commandsFlow.value
    }

    override suspend fun replanDay(delayHours: Int) {
        val updated = tasksFlow.value.map { task ->
            if (task.status == ExecutionStatus.SCHEDULED && !task.isCurrentNow) {
                task.copy(
                    description = "${task.description} (تم تأخير الموعد $delayHours ساعة)",
                    timeLabelArabic = "مؤجلة +$delayHours س"
                )
            } else {
                task
            }
        }
        tasksFlow.value = updated
    }

    override suspend fun toggleTaskCompletion(taskId: String) {
        tasksFlow.value = tasksFlow.value.map { task ->
            if (task.id == taskId) {
                val newStatus = if (task.status == ExecutionStatus.COMPLETED) ExecutionStatus.SCHEDULED else ExecutionStatus.COMPLETED
                task.copy(
                    status = newStatus,
                    isCurrentNow = if (newStatus == ExecutionStatus.COMPLETED) false else task.isCurrentNow
                )
            } else task
        }
    }

    override suspend fun postponeTask(taskId: String, delayMinutes: Int) {
        tasksFlow.value = tasksFlow.value.map { task ->
            if (task.id == taskId) {
                task.copy(
                    timeLabelArabic = "مؤجلة +$delayMinutes د",
                    isCurrentNow = false,
                    status = ExecutionStatus.SCHEDULED
                )
            } else task
        }
    }

    override suspend fun deleteTask(taskId: String) {
        tasksFlow.value = tasksFlow.value.filterNot { it.id == taskId }
    }

    override suspend fun clearTasks() {
        tasksFlow.value = emptyList()
    }

    override suspend fun resetDefaultTasks() {
        tasksFlow.value = generateDefaultTasks()
    }
}
