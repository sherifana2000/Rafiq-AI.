package com.example.domain.usecase

import com.example.domain.repository.TaskRepository
import com.example.model.ScheduledTask
import com.example.model.UserCommand
import kotlinx.coroutines.flow.Flow

class GetTodayScheduleUseCase(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(): Flow<List<ScheduledTask>> = taskRepository.getTodayTasks()
}

class GetCurrentTaskUseCase(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(): Flow<ScheduledTask?> = taskRepository.getCurrentTask()
}

class GetRecentCommandsUseCase(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(): Flow<List<UserCommand>> = taskRepository.getRecentCommands()
}

class ToggleTaskCompletionUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String) = taskRepository.toggleTaskCompletion(taskId)
}

class PostponeTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String, delayMinutes: Int = 30) =
        taskRepository.postponeTask(taskId, delayMinutes)
}

class AddNewTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(task: ScheduledTask) = taskRepository.addScheduledTask(task)
}

class ClearTasksUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke() = taskRepository.clearTasks()
}

class ResetTasksUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke() = taskRepository.resetDefaultTasks()
}
