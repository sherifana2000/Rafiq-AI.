package com.example.domain.repository

import com.example.model.ScheduledTask
import com.example.model.UserCommand
import kotlinx.coroutines.flow.Flow

/**
 * واجهة مستودع المهام والأوامر المنفذة في النظام.
 */
interface TaskRepository {
    fun getTodayTasks(): Flow<List<ScheduledTask>>
    fun getCurrentTask(): Flow<ScheduledTask?>
    fun getRecentCommands(): Flow<List<UserCommand>>
    suspend fun addScheduledTask(task: ScheduledTask)
    suspend fun addUserCommand(command: UserCommand)
    suspend fun replanDay(delayHours: Int)
    suspend fun toggleTaskCompletion(taskId: String)
    suspend fun postponeTask(taskId: String, delayMinutes: Int = 30)
    suspend fun deleteTask(taskId: String)
    suspend fun clearTasks()
    suspend fun resetDefaultTasks()
}
