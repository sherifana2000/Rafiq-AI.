package com.example.domain.reminder

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.reminder.ReminderEntity
import com.example.data.reminder.AndroidReminderScheduler
import com.example.data.repository.RoomReminderRepository
import com.example.domain.action.StageOneActionEngine
import com.example.domain.parser.CommandParser
import com.example.domain.repository.InMemoryTaskRepository
import com.example.domain.services.AppServiceGateway
import com.example.domain.usecase.ProcessUserCommandUseCase
import com.example.model.Action
import com.example.model.ActionType
import com.example.model.ExecutionStatus
import com.example.model.ScheduleRule
import com.example.model.UserCommand
import com.example.notification.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderExecutionTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var reminderRepository: RoomReminderRepository
    private lateinit var fakeScheduler: FakeReminderScheduler
    private lateinit var dummyGateway: AppServiceGateway
    private lateinit var actionEngine: StageOneActionEngine

    class FakeReminderScheduler : ReminderScheduler {
        val scheduledList = mutableListOf<Triple<Long, String, Long>>()
        val cancelledList = mutableListOf<Long>()

        override fun schedule(
            reminderId: Long,
            title: String,
            target: String?,
            triggerAtMillis: Long,
            repeatInterval: String?
        ): ScheduleResult {
            if (triggerAtMillis <= System.currentTimeMillis()) {
                return ScheduleResult.Failure("لا يمكن جدولة تذكير في وقت مضى.")
            }
            scheduledList.add(Triple(reminderId, title, triggerAtMillis))
            return ScheduleResult.Success(reminderId, triggerAtMillis)
        }

        override fun cancel(reminderId: Long) {
            cancelledList.add(reminderId)
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reminderRepository = RoomReminderRepository(database.reminderDao())
        fakeScheduler = FakeReminderScheduler()
        dummyGateway = object : AppServiceGateway {
            override fun logActionIntent(actionName: String, target: String) {}
            override fun isAppInstalled(packageName: String): Boolean = true
            override fun canScheduleExactAlarms(): Boolean = true
        }

        actionEngine = StageOneActionEngine(
            serviceGateway = dummyGateway,
            reminderRepository = reminderRepository,
            reminderScheduler = fakeScheduler
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun test1_futureReminder_isSavedInRoomAndScheduled() = runTest {
        val futureTime = LocalDateTime.now()
            .plusDays(1)
            .withHour(10)
            .withMinute(0)
            .withSecond(0)
            .truncatedTo(ChronoUnit.SECONDS)
        val isoString = futureTime.toString()

        val action = Action(
            type = ActionType.CREATE_REMINDER,
            title = "الاتصال بأحمد",
            target = "أحمد",
            scheduledTime = isoString,
            repeat = "NONE"
        )

        val result = actionEngine.execute(action)

        assertTrue(result.isSuccess)
        assertEquals(ExecutionStatus.SCHEDULED, result.status)
        assertTrue(result.feedbackMessageArabic.contains("الاتصال بأحمد"))

        // تحقق من الحفظ في قاعدة بيانات Room
        val savedReminders = reminderRepository.observeAllReminders().first()
        assertEquals(1, savedReminders.size)
        assertEquals("الاتصال بأحمد", savedReminders[0].title)
        assertEquals("SCHEDULED", savedReminders[0].status)

        // تحقق من الجدولة في ReminderScheduler
        assertEquals(1, fakeScheduler.scheduledList.size)
        assertEquals("الاتصال بأحمد", fakeScheduler.scheduledList[0].second)
    }

    @Test
    fun test2_pastReminder_isRejected() = runTest {
        val pastIsoString = "2020-01-01T10:00:00"

        val action = Action(
            type = ActionType.CREATE_REMINDER,
            title = "أمر قديم",
            scheduledTime = pastIsoString
        )

        val result = actionEngine.execute(action)

        assertFalse(result.isSuccess)
        assertEquals(ExecutionStatus.FAILED, result.status)
        assertTrue(result.feedbackMessageArabic.contains("لا يمكن جدولة تذكير في وقت مضى"))

        // تأكيد عدم الحفظ أو الجدولة
        val saved = reminderRepository.observeAllReminders().first()
        assertTrue(saved.isEmpty())
        assertTrue(fakeScheduler.scheduledList.isEmpty())
    }

    @Test
    fun test3_confirmationFlow_executesParsedActionDirectlyWithoutCallingParser() = runTest {
        var parserCallCount = 0
        val mockParser = object : CommandParser {
            override suspend fun parse(rawText: String): UserCommand {
                parserCallCount++
                throw AssertionError("يجب ألا يتم استدعاء CommandParser عند تأكيد أمر محلل مسبقاً!")
            }
        }

        val taskRepo = InMemoryTaskRepository()
        val useCase = ProcessUserCommandUseCase(mockParser, actionEngine, taskRepo)

        val futureIso = LocalDateTime.now().plusDays(1).withHour(9).truncatedTo(ChronoUnit.SECONDS).toString()
        val preParsedCommand = UserCommand(
            id = UUID.randomUUID().toString(),
            rawText = "ذكّرني غداً بموعد الطبيب",
            intent = ActionType.CREATE_REMINDER,
            title = "موعد الطبيب",
            scheduleRule = ScheduleRule(targetTimeArabic = futureIso),
            action = Action(
                type = ActionType.CREATE_REMINDER,
                title = "موعد الطبيب",
                scheduledTime = futureIso
            )
        )

        // تنفيذ الأمر المؤكد
        val confirmedResult = useCase.executeConfirmedCommand(preParsedCommand)

        // تأكيد عدم استدعاء الـ Parser / Gemini
        assertEquals(0, parserCallCount)
        assertTrue(confirmedResult.isSuccess)
        assertEquals(ExecutionStatus.SCHEDULED, confirmedResult.command.status)

        // تأكيد حفظ المهمة في المستودع
        val todayTasks = taskRepo.getTodayTasks().first()
        assertTrue(todayTasks.any { it.title == "موعد الطبيب" })
    }

    @Test
    fun test4_notificationPermission_safeHandling() {
        val notificationHelper = NotificationHelper(context)
        // عند استدعاء إظهار الإشعار، يجب ألا يحدث Crash حتى لو كانت الصلاحيات غير مفعلة
        val result = notificationHelper.showReminderNotification(
            reminderId = 999L,
            title = "تنبيه اختباري",
            target = "هدف اختباري"
        )
        // النتيجة إما true أو false لكن العملية آمنة تماماً بدون استثناءات
        assertNotNull(result)
    }

    @Test
    fun test5_cancelReminder_cancelsInSchedulerAndStatus() = runTest {
        val futureTime = LocalDateTime.now().plusDays(1).withHour(11).truncatedTo(ChronoUnit.SECONDS)
        val entity = ReminderEntity(
            id = 55L,
            title = "تذكير للإلغاء",
            scheduledEpochMillis = futureTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            scheduledTimeIso = futureTime.toString(),
            status = "SCHEDULED"
        )
        reminderRepository.saveReminder(entity)
        fakeScheduler.schedule(entity.id, entity.title, null, entity.scheduledEpochMillis)

        // إلغاء التذكير
        fakeScheduler.cancel(entity.id)
        reminderRepository.updateStatus(entity.id, "CANCELLED")

        assertTrue(fakeScheduler.cancelledList.contains(55L))
        val updated = reminderRepository.getReminderById(55L)
        assertNotNull(updated)
        assertEquals("CANCELLED", updated?.status)
    }

    @Test
    fun test6_bootReceiverQuery_fetchesOnlyPendingFutureReminders() = runTest {
        val now = System.currentTimeMillis()
        val future1 = now + 3600_000L
        val future2 = now + 7200_000L
        val past = now - 3600_000L

        // 1. تذكير مستقبلي SCHEDULED (يجب إعادة جدولته)
        reminderRepository.saveReminder(ReminderEntity(
            id = 101L,
            title = "مستقبلي 1",
            scheduledEpochMillis = future1,
            scheduledTimeIso = "2026-10-02T10:00:00",
            status = "SCHEDULED"
        ))

        // 2. تذكير مستقبلي لكنه FIRED (يجب تجاهله)
        reminderRepository.saveReminder(ReminderEntity(
            id = 102L,
            title = "منتهي سابقاً",
            scheduledEpochMillis = future2,
            scheduledTimeIso = "2026-10-02T11:00:00",
            status = "FIRED"
        ))

        // 3. تذكير ماضٍ (يجب تجاهله)
        reminderRepository.saveReminder(ReminderEntity(
            id = 103L,
            title = "قديم",
            scheduledEpochMillis = past,
            scheduledTimeIso = "2026-09-01T10:00:00",
            status = "SCHEDULED"
        ))

        // 4. تذكير ملغى (يجب تجاهله)
        reminderRepository.saveReminder(ReminderEntity(
            id = 104L,
            title = "ملغى",
            scheduledEpochMillis = future1,
            scheduledTimeIso = "2026-10-02T10:00:00",
            status = "CANCELLED"
        ))

        val activeReminders = database.reminderDao().getActivePendingReminders(now)

        assertEquals(1, activeReminders.size)
        assertEquals(101L, activeReminders[0].id)
        assertEquals("مستقبلي 1", activeReminders[0].title)
    }
}
