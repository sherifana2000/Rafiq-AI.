package com.example.data.local.reminder

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE scheduledEpochMillis > :currentTimeMillis AND status = 'SCHEDULED' ORDER BY scheduledEpochMillis ASC")
    suspend fun getActivePendingReminders(currentTimeMillis: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders ORDER BY scheduledEpochMillis DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("UPDATE reminders SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)
}
