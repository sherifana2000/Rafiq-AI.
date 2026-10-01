package com.example.domain.usecase

import com.example.domain.action.StageOneActionEngine
import com.example.domain.parser.MockCommandParser
import com.example.domain.repository.InMemoryTaskRepository
import com.example.domain.services.AppServiceGateway
import com.example.model.ActionType
import com.example.model.ExecutionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProcessUserCommandUseCaseTest {

    private lateinit var useCase: ProcessUserCommandUseCase
    private lateinit var repository: InMemoryTaskRepository

    @Before
    fun setUp() {
        val dummyGateway = object : AppServiceGateway {
            override fun logActionIntent(actionName: String, target: String) {}
            override fun isAppInstalled(packageName: String): Boolean = true
            override fun canScheduleExactAlarms(): Boolean = true
        }
        val actionEngine = StageOneActionEngine(dummyGateway)
        val commandParser = MockCommandParser()
        repository = InMemoryTaskRepository()

        useCase = ProcessUserCommandUseCase(commandParser, actionEngine, repository)
    }

    @Test
    fun invoke_withBlankInput_returnsFailure() = runTest {
        val result = useCase("")
        assertFalse(result.isSuccess)
    }

    @Test
    fun invoke_withReminderCommand_addsTaskAndCommandToRepository() = runTest {
        val input = "بكرة الساعة 8 ذكرني أذاكر الرياضيات"
        val result = useCase(input)

        assertTrue(result.isSuccess)
        assertEquals(ActionType.CREATE_REMINDER, result.command.intent)
        assertEquals(ExecutionStatus.SCHEDULED, result.command.status)

        val tasks = repository.getTodayTasks().first()
        assertTrue(tasks.any { it.action.type == ActionType.CREATE_REMINDER })

        val commands = repository.getRecentCommands().first()
        assertTrue(commands.any { it.rawText == input })
    }

    @Test
    fun invoke_withReplanCommand_modifiesTasksSchedule() = runTest {
        val input = "أعد ترتيب باقي اليوم لأنني تأخرت ساعتين"
        val result = useCase(input)

        assertTrue(result.isSuccess)
        assertEquals(ActionType.REPLAN_DAY, result.command.intent)

        val tasks = repository.getTodayTasks().first()
        assertTrue(tasks.any { it.timeLabelArabic.contains("مؤجلة") })
    }
}
