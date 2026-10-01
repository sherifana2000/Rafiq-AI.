package com.example.ui.home

import com.example.model.ScheduledTask
import com.example.model.UserCommand
import com.example.ui.character.CharacterState
import com.example.ui.character.CharacterTheme
import com.example.ui.navigation.AppThemeMode
import com.example.ui.navigation.DrawerDestination

data class FocusSessionState(
    val title: String = "مراجعة النحو",
    val totalSeconds: Int = 50 * 60,
    val remainingSeconds: Int = 50 * 60,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
) {
    val formattedTime: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return "%02d:%02d".format(java.util.Locale.US, minutes, seconds)
        }
}

/**
 * حالة واجهة رفيق المساعد الشخصي والتفاعلي (AI Character Companion State)
 */
data class HomeUiState(
    val currentDestination: DrawerDestination = DrawerDestination.HOME,
    val characterState: CharacterState = CharacterState.IDLE,
    val characterSpeechText: String = "ماذا تريد أن نفعل اليوم؟",
    val activeCharacterTheme: CharacterTheme = CharacterTheme.DEFAULT,
    val commandInput: String = "",
    val isProcessing: Boolean = false,
    val currentTask: ScheduledTask? = null,
    val todaySchedule: List<ScheduledTask> = emptyList(),
    val recentCommands: List<UserCommand> = emptyList(),
    val userName: String = "شريف",
    val greetingText: String = "مساء الخير 👋",
    val greetingSubText: String = "أنا رفيقك الشخصي.",
    val currentDateArabic: String = "",
    val dayStatusSummary: String = "حالة اليوم: جدولك متوازن",
    val pendingParsedCommand: UserCommand? = null,
    val showResponseSheet: Boolean = false,
    val activeFocusTimer: FocusSessionState? = null,
    val showFocusDialog: Boolean = false,
    val showAddTaskDialog: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val voiceFeedbackReminderEnabled: Boolean = true,
    val calmSoundsEnabled: Boolean = true
) {
    val completedTasksCount: Int
        get() = todaySchedule.count { it.isCompleted }

    val totalTasksCount: Int
        get() = todaySchedule.size

    val completionPercentage: Float
        get() = if (totalTasksCount == 0) 0f else completedTasksCount.toFloat() / totalTasksCount.toFloat()
}
