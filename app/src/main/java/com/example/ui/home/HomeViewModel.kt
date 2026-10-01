package com.example.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.brain.client.DefaultRafiqBrainClient
import com.example.brain.model.RafiqUserContext
import com.example.brain.service.BrainCommandParser
import com.example.brain.service.BrainResult
import com.example.brain.service.DefaultRafiqBrain
import com.example.brain.service.RafiqBrain
import com.example.brain.validator.RafiqActionValidator
import com.example.domain.action.StageOneActionEngine
import com.example.domain.parser.CommandParser
import com.example.domain.repository.InMemoryTaskRepository
import com.example.domain.repository.TaskRepository
import com.example.domain.services.StageOneServiceGateway
import com.example.domain.usecase.AddNewTaskUseCase
import com.example.domain.usecase.ClearTasksUseCase
import com.example.domain.usecase.GetCurrentTaskUseCase
import com.example.domain.usecase.GetRecentCommandsUseCase
import com.example.domain.usecase.GetTodayScheduleUseCase
import com.example.domain.usecase.PostponeTaskUseCase
import com.example.domain.usecase.ProcessUserCommandUseCase
import com.example.domain.usecase.ResetTasksUseCase
import com.example.domain.usecase.ToggleTaskCompletionUseCase
import com.example.model.Action
import com.example.model.ActionType
import com.example.model.ExecutionStatus
import com.example.model.ScheduleRule
import com.example.model.ScheduledTask
import com.example.model.TaskPriority
import com.example.model.TaskSource
import com.example.ui.character.CharacterState
import com.example.ui.navigation.AppThemeMode
import com.example.ui.navigation.DrawerDestination
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeViewModel(
    private val processUserCommandUseCase: ProcessUserCommandUseCase,
    private val brain: RafiqBrain,
    private val getTodayScheduleUseCase: GetTodayScheduleUseCase,
    private val getCurrentTaskUseCase: GetCurrentTaskUseCase,
    private val getRecentCommandsUseCase: GetRecentCommandsUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val postponeTaskUseCase: PostponeTaskUseCase,
    private val addNewTaskUseCase: AddNewTaskUseCase,
    private val clearTasksUseCase: ClearTasksUseCase,
    private val resetTasksUseCase: ResetTasksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var focusTimerJob: Job? = null
    private var voiceMockJob: Job? = null

    init {
        updateDateAndGreeting()
        observeData()
    }

    private fun updateDateAndGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when (hour) {
            in 4..11 -> "صباح الخير 👋"
            in 12..16 -> "يوم سعيد 👋"
            in 17..21 -> "مساء الخير 👋"
            else -> "مساء هادئ 👋"
        }

        val arabicDateFormat = SimpleDateFormat("EEEE، d MMMM", Locale.forLanguageTag("ar"))
        val formattedDate = arabicDateFormat.format(Date())

        _uiState.update {
            it.copy(
                greetingText = greeting,
                greetingSubText = "أنا رفيقك الشخصي.",
                currentDateArabic = formattedDate
            )
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            getTodayScheduleUseCase().collect { tasks ->
                val remaining = tasks.count { !it.isCompleted }
                val summary = if (tasks.isEmpty()) {
                    "يومك ما زال مفتوحًا ✨"
                } else {
                    "حالة اليوم: متبقي $remaining مهام في جدولك"
                }
                _uiState.update {
                    it.copy(
                        todaySchedule = tasks,
                        dayStatusSummary = summary
                    )
                }
            }
        }

        viewModelScope.launch {
            getCurrentTaskUseCase().collect { current ->
                _uiState.update { it.copy(currentTask = current) }
            }
        }

        viewModelScope.launch {
            getRecentCommandsUseCase().collect { commands ->
                _uiState.update { it.copy(recentCommands = commands) }
            }
        }
    }

    fun selectDestination(destination: DrawerDestination) {
        _uiState.update { it.copy(currentDestination = destination) }
    }

    fun onCommandInputChanged(newValue: String) {
        _uiState.update { it.copy(commandInput = newValue) }
    }

    fun onSuggestionSelected(suggestion: String) {
        _uiState.update { it.copy(commandInput = suggestion) }
        submitCommand()
    }

    /**
     * التفاعل الصوتي مع رفيق عبر الميكروفون
     * مسار الحالات: LISTENING -> THINKING -> RafiqBrain -> SUCCESS / NeedsClarification / Error
     */
    fun onMicPressed() {
        voiceMockJob?.cancel()
        voiceMockJob = viewModelScope.launch {
            if (_uiState.value.characterState == CharacterState.LISTENING) {
                _uiState.update {
                    it.copy(
                        characterState = CharacterState.IDLE,
                        characterSpeechText = "ماذا تريد أن نفعل اليوم؟"
                    )
                }
                return@launch
            }

            // 1. حالة الاستماع
            _uiState.update {
                it.copy(
                    characterState = CharacterState.LISTENING,
                    characterSpeechText = "أنا أسمعك..."
                )
            }

            delay(1700)

            // 2. حالة التفكير
            _uiState.update {
                it.copy(
                    characterState = CharacterState.THINKING,
                    characterSpeechText = "لحظة، أفكر في أفضل طريقة..."
                )
            }

            // 3. إرسال الأمر الصوتي إلى RafiqBrain
            val mockVoiceQuery = "بكرة الساعة 8 ذكرني أذاكر النحو"
            handleBrainCommand(mockVoiceQuery)
        }
    }

    /**
     * إرسال الأمر الكتابي إلى RafiqBrain
     */
    fun submitCommand() {
        val input = _uiState.value.commandInput.trim()
        if (input.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    characterState = CharacterState.THINKING,
                    characterSpeechText = "لحظة، أفكر في أفضل طريقة..."
                )
            }

            handleBrainCommand(input)
        }
    }

    /**
     * معالجة نتائج RafiqBrain بدقة حسب الحالات (Success, NeedsClarification, Errors)
     */
    private suspend fun handleBrainCommand(input: String) {
        val userContext = RafiqUserContext.current()

        when (val result = brain.processCommand(input, userContext)) {
            is BrainResult.Success -> {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        commandInput = "",
                        characterState = CharacterState.SUCCESS,
                        characterSpeechText = "فهمت طلبك.",
                        pendingParsedCommand = result.command,
                        showResponseSheet = true
                    )
                }
            }

            is BrainResult.NeedsClarification -> {
                // حالة عدم وضوح الأمر أو انخفاض نسبة الثقة عن 0.70 (Requirement 9)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        characterState = CharacterState.SPEAKING,
                        characterSpeechText = result.question
                    )
                }
            }

            is BrainResult.ValidationError -> {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        characterState = CharacterState.ERROR,
                        characterSpeechText = result.reason
                    )
                }
            }

            is BrainResult.NetworkError -> {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        characterState = CharacterState.ERROR,
                        characterSpeechText = result.message
                    )
                }
            }

            is BrainResult.ServerError -> {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        characterState = CharacterState.ERROR,
                        characterSpeechText = result.message
                    )
                }
            }

            is BrainResult.AIUnavailable -> {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        characterState = CharacterState.ERROR,
                        characterSpeechText = result.message
                    )
                }
            }
        }
    }

    /**
     * تأكيد الأمر بعد مراجعة الرد في الـ Bottom Sheet
     */
    fun confirmPendingCommand() {
        val pending = _uiState.value.pendingParsedCommand ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(showResponseSheet = false, isProcessing = true) }
            val result = processUserCommandUseCase.executeConfirmedCommand(pending)

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    pendingParsedCommand = null,
                    characterState = if (result.isSuccess) CharacterState.HAPPY else CharacterState.ERROR,
                    characterSpeechText = result.feedbackMessageArabic.ifBlank {
                        if (result.isSuccess) "تم ترتيب كل شيء بنجاح ✨" else "تعذر تنفيذ الأمر."
                    }
                )
            }

            delay(3000)
            _uiState.update {
                it.copy(
                    characterState = CharacterState.IDLE,
                    characterSpeechText = "ماذا تريد أن نفعل اليوم؟"
                )
            }
        }
    }

    fun dismissResponseSheet() {
        _uiState.update {
            it.copy(
                showResponseSheet = false,
                characterState = CharacterState.IDLE,
                characterSpeechText = "ماذا تريد أن نفعل اليوم؟"
            )
        }
    }

    // إدارة المهمة الحالية (Current Action)
    fun startCurrentTaskFocus() {
        val current = _uiState.value.currentTask
        val title = current?.title ?: "مراجعة درس النحو"
        startFocusSession(title, 50)
    }

    fun postponeCurrentTask() {
        val current = _uiState.value.currentTask ?: return
        viewModelScope.launch {
            postponeTaskUseCase(current.id, 30)
            _uiState.update {
                it.copy(
                    characterState = CharacterState.SPEAKING,
                    characterSpeechText = "تم تأجيل المهمة نصف ساعة."
                )
            }
            delay(2000)
            _uiState.update {
                it.copy(
                    characterState = CharacterState.IDLE,
                    characterSpeechText = "ماذا تريد أن نفعل اليوم؟"
                )
            }
        }
    }

    // تفاعلات المهام
    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            toggleTaskCompletionUseCase(taskId)
        }
    }

    fun postponeTask(taskId: String) {
        viewModelScope.launch {
            postponeTaskUseCase(taskId, 30)
        }
    }

    // جلسات التركيز (Focus Session)
    fun startFocusSession(title: String, durationMinutes: Int = 50) {
        val totalSecs = durationMinutes * 60
        _uiState.update {
            it.copy(
                activeFocusTimer = FocusSessionState(
                    title = title,
                    totalSeconds = totalSecs,
                    remainingSeconds = totalSecs,
                    isRunning = true
                ),
                showFocusDialog = true,
                characterState = CharacterState.IDLE
            )
        }
        runFocusTimer()
    }

    fun toggleFocusTimerRunning() {
        val timer = _uiState.value.activeFocusTimer ?: return
        if (timer.isRunning) {
            focusTimerJob?.cancel()
            _uiState.update { it.copy(activeFocusTimer = timer.copy(isRunning = false)) }
        } else {
            _uiState.update { it.copy(activeFocusTimer = timer.copy(isRunning = true)) }
            runFocusTimer()
        }
    }

    fun resetFocusTimer() {
        focusTimerJob?.cancel()
        val timer = _uiState.value.activeFocusTimer ?: return
        _uiState.update {
            it.copy(
                activeFocusTimer = timer.copy(
                    remainingSeconds = timer.totalSeconds,
                    isRunning = false,
                    isFinished = false
                )
            )
        }
    }

    fun dismissFocusDialog() {
        _uiState.update { it.copy(showFocusDialog = false) }
    }

    private fun runFocusTimer() {
        focusTimerJob?.cancel()
        focusTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val currentTimer = _uiState.value.activeFocusTimer
                if (currentTimer != null && currentTimer.isRunning && currentTimer.remainingSeconds > 0) {
                    val nextSec = currentTimer.remainingSeconds - 1
                    _uiState.update {
                        it.copy(
                            activeFocusTimer = currentTimer.copy(
                                remainingSeconds = nextSec,
                                isFinished = nextSec == 0,
                                isRunning = nextSec > 0
                            )
                        )
                    }
                } else {
                    break
                }
            }
        }
    }

    // إضافة مهمة سريعة
    fun showAddTaskDialog(show: Boolean) {
        _uiState.update { it.copy(showAddTaskDialog = show) }
    }

    fun addNewManualTask(title: String, time: String, priority: TaskPriority) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = ScheduledTask(
                id = "manual-${System.currentTimeMillis()}",
                title = title,
                description = "مهمة مضافة",
                action = Action(
                    type = ActionType.CREATE_TASK,
                    title = title
                ),
                scheduleRule = ScheduleRule(
                    targetTimeArabic = time.ifBlank { "طوال اليوم" }
                ),
                priority = priority,
                source = TaskSource.USER_COMMAND,
                status = ExecutionStatus.SCHEDULED,
                timeLabelArabic = time.ifBlank { "طوال اليوم" },
                dateLabelArabic = "اليوم",
                isCurrentNow = false
            )
            addNewTaskUseCase(task)
            _uiState.update { it.copy(showAddTaskDialog = false) }
        }
    }

    // إفراغ أو إعادة ملء المهام
    fun clearAllTasks() {
        viewModelScope.launch {
            clearTasksUseCase()
        }
    }

    fun resetDefaultTasks() {
        viewModelScope.launch {
            resetTasksUseCase()
        }
    }

    // المظهر والتفضيلات
    fun setThemeMode(mode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun toggleVoiceFeedback() {
        _uiState.update { it.copy(voiceFeedbackReminderEnabled = !it.voiceFeedbackReminderEnabled) }
    }

    fun toggleCalmSounds() {
        _uiState.update { it.copy(calmSoundsEnabled = !it.calmSoundsEnabled) }
    }

    override fun onCleared() {
        super.onCleared()
        focusTimerJob?.cancel()
        voiceMockJob?.cancel()
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val repository: TaskRepository = InMemoryTaskRepository()
                    val gateway = StageOneServiceGateway(appContext)
                    val appDatabase = com.example.data.local.AppDatabase.getInstance(appContext)
                    val reminderRepository = com.example.data.repository.RoomReminderRepository(appDatabase.reminderDao())
                    val reminderScheduler = com.example.data.reminder.AndroidReminderScheduler(appContext)
                    val actionEngine = StageOneActionEngine(gateway, reminderRepository, reminderScheduler)

                    // إعداد طبقة العقل الذكي RafiqBrain والعميل الآمن
                    val brainClient = DefaultRafiqBrainClient()
                    val validator = RafiqActionValidator()
                    val brain: RafiqBrain = DefaultRafiqBrain(brainClient, validator)
                    val commandParser: CommandParser = BrainCommandParser(brain)

                    val processCommandUseCase = ProcessUserCommandUseCase(commandParser, actionEngine, repository)
                    val getTodayScheduleUseCase = GetTodayScheduleUseCase(repository)
                    val getCurrentTaskUseCase = GetCurrentTaskUseCase(repository)
                    val getRecentCommandsUseCase = GetRecentCommandsUseCase(repository)
                    val toggleTaskCompletionUseCase = ToggleTaskCompletionUseCase(repository)
                    val postponeTaskUseCase = PostponeTaskUseCase(repository)
                    val addNewTaskUseCase = AddNewTaskUseCase(repository)
                    val clearTasksUseCase = ClearTasksUseCase(repository)
                    val resetTasksUseCase = ResetTasksUseCase(repository)

                    return HomeViewModel(
                        processCommandUseCase,
                        brain,
                        getTodayScheduleUseCase,
                        getCurrentTaskUseCase,
                        getRecentCommandsUseCase,
                        toggleTaskCompletionUseCase,
                        postponeTaskUseCase,
                        addNewTaskUseCase,
                        clearTasksUseCase,
                        resetTasksUseCase
                    ) as T
                }
            }
    }
}
