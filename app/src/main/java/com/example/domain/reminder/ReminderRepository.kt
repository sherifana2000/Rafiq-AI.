package com.example.domain.reminder

import com.example.data.local.reminder.ReminderEntity
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    suspend fun saveReminder(reminder: ReminderEntity): Long
    suspend fun updateReminder(reminder: ReminderEntity)
    suspend fun getReminderById(id: Long): ReminderEntity?
    suspend fun getActivePendingReminders(currentTimeMillis: Long): List<ReminderEntity>
    fun observeAllReminders(): Flow<List<ReminderEntity>>
    suspend fun updateStatus(id: Long, status: String)
    suspend fun deleteReminder(id: Long)
}
