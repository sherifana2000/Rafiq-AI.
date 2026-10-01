package com.example.data.repository

import com.example.data.local.reminder.ReminderDao
import com.example.data.local.reminder.ReminderEntity
import com.example.domain.reminder.ReminderRepository
import kotlinx.coroutines.flow.Flow

class RoomReminderRepository(
    private val reminderDao: ReminderDao
) : ReminderRepository {

    override suspend fun saveReminder(reminder: ReminderEntity): Long {
        return reminderDao.insertReminder(reminder)
    }

    override suspend fun updateReminder(reminder: ReminderEntity) {
        reminderDao.updateReminder(reminder)
    }

    override suspend fun getReminderById(id: Long): ReminderEntity? {
        return reminderDao.getReminderById(id)
    }

    override suspend fun getActivePendingReminders(currentTimeMillis: Long): List<ReminderEntity> {
        return reminderDao.getActivePendingReminders(currentTimeMillis)
    }

    override fun observeAllReminders(): Flow<List<ReminderEntity>> {
        return reminderDao.getAllReminders()
    }

    override suspend fun updateStatus(id: Long, status: String) {
        reminderDao.updateStatus(id, status)
    }

    override suspend fun deleteReminder(id: Long) {
        reminderDao.deleteReminderById(id)
    }
}
