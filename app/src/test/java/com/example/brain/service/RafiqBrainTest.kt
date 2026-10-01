package com.example.brain.service

import com.example.brain.client.BrainApiResponse
import com.example.brain.client.RafiqBrainClient
import com.example.brain.client.RafiqBrainRequest
import com.example.brain.model.RafiqUserContext
import com.example.brain.model.StructuredActionItem
import com.example.brain.model.StructuredRafiqResponse
import com.example.brain.validator.RafiqActionValidator
import com.example.model.ActionType
import com.example.model.ExecutionStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RafiqBrainTest {

    private val testContext = RafiqUserContext(
        currentDate = "2026-09-28",
        currentTime = "14:00",
        timeZone = "UTC",
        locale = "ar"
    )

    @Test
    fun `processCommand with valid response returns Success and maps to UserCommand`() = runTest {
        val fakeClient = object : RafiqBrainClient {
            override suspend fun analyzeCommand(request: RafiqBrainRequest): BrainApiResponse {
                return BrainApiResponse.Success(
                    StructuredRafiqResponse(
                        understood = true,
                        intent = "CREATE_REMINDER",
                        confidence = 0.95f,
                        summary = "تذكير غداً الساعة 8 صباحاً بمراجعة النحو",
                        actions = listOf(
                            StructuredActionItem(
                                type = "CREATE_REMINDER",
                                title = "مراجعة النحو",
                                scheduledTime = "08:00:00"
                            )
                        )
                    )
                )
            }
        }

        val brain = DefaultRafiqBrain(fakeClient, RafiqActionValidator())
        val result = brain.processCommand("بكرة الساعة 8 ذكرني أذاكر النحو", testContext)

        assertTrue(result is BrainResult.Success)
        val success = result as BrainResult.Success
        assertEquals(ActionType.CREATE_REMINDER, success.command.intent)
        assertEquals("مراجعة النحو", success.command.title)
        assertEquals(ExecutionStatus.PENDING, success.command.status)
    }

    @Test
    fun `processCommand with low confidence returns NeedsClarification without executing`() = runTest {
        val fakeClient = object : RafiqBrainClient {
            override suspend fun analyzeCommand(request: RafiqBrainRequest): BrainApiResponse {
                return BrainApiResponse.Success(
                    StructuredRafiqResponse(
                        understood = true,
                        intent = "CREATE_REMINDER",
                        confidence = 0.60f, // Low confidence (< 0.70)
                        summary = "أمر غير مؤكد",
                        clarificationQuestion = "تحب أذكرك الساعة كام؟",
                        actions = emptyList()
                    )
                )
            }
        }

        val brain = DefaultRafiqBrain(fakeClient, RafiqActionValidator())
        val result = brain.processCommand("ذكرني أذاكر", testContext)

        assertTrue(result is BrainResult.NeedsClarification)
        val clarification = result as BrainResult.NeedsClarification
        assertEquals("تحب أذكرك الساعة كام؟", clarification.question)
    }

    @Test
    fun `processCommand with blank input returns ValidationError immediately`() = runTest {
        val fakeClient = object : RafiqBrainClient {
            override suspend fun analyzeCommand(request: RafiqBrainRequest): BrainApiResponse {
                throw AssertionError("Should not be called for blank input")
            }
        }

        val brain = DefaultRafiqBrain(fakeClient, RafiqActionValidator())
        val result = brain.processCommand("   ", testContext)

        assertTrue(result is BrainResult.ValidationError)
    }

    @Test
    fun `processCommand with network error returns NetworkError`() = runTest {
        val fakeClient = object : RafiqBrainClient {
            override suspend fun analyzeCommand(request: RafiqBrainRequest): BrainApiResponse {
                return BrainApiResponse.NetworkError("لا يوجد اتصال بالإنترنت")
            }
        }

        val brain = DefaultRafiqBrain(fakeClient, RafiqActionValidator())
        val result = brain.processCommand("أمر ما", testContext)

        assertTrue(result is BrainResult.NetworkError)
        assertEquals("لا يوجد اتصال بالإنترنت", (result as BrainResult.NetworkError).message)
    }
}
