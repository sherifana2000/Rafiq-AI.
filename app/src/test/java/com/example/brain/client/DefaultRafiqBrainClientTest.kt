package com.example.brain.client

import com.example.auth.RafiqAccessTokenProvider
import com.example.brain.model.RafiqUserContext
import com.example.model.ActionType
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultRafiqBrainClientTest {

    private val fakeTokenProvider = object : RafiqAccessTokenProvider {
        override suspend fun getAccessToken(): String = "test-valid-jwt-token"
    }

    private val client = DefaultRafiqBrainClient(
        backendBaseUrl = null,
        enableLocalServerSimulation = true
    )

    private val testContext = RafiqUserContext(
        currentDate = "2026-09-28",
        currentTime = "14:00",
        timeZone = "UTC",
        locale = "ar"
    )

    @Test
    fun `analyzeCommand parses Arabic reminder accurately`() = runTest {
        val request = RafiqBrainRequest(
            userCommand = "بكرة الساعة 8 ذكرني أذاكر النحو",
            context = testContext
        )

        val response = client.analyzeCommand(request)
        assertTrue(response is BrainApiResponse.Success)

        val structured = (response as BrainApiResponse.Success).response
        assertTrue(structured.understood)
        assertEquals(ActionType.CREATE_REMINDER.name, structured.intent)
        assertTrue(structured.confidence >= 0.70f)
        assertTrue(structured.actions.isNotEmpty())
        assertEquals("مذاكرة النحو", structured.actions[0].title)
    }

    @Test
    fun `analyzeCommand flags ambiguous command for clarification`() = runTest {
        val request = RafiqBrainRequest(
            userCommand = "ذكرني أذاكر",
            context = testContext
        )

        val response = client.analyzeCommand(request)
        assertTrue(response is BrainApiResponse.NeedsClarification)

        val clarification = response as BrainApiResponse.NeedsClarification
        assertNotNull(clarification.question)
        assertTrue(clarification.response.confidence < 0.70f)
    }

    @Test
    fun `analyzeCommand handles replan day request`() = runTest {
        val request = RafiqBrainRequest(
            userCommand = "أنا اتأخرت ساعتين، رتب باقي اليوم",
            context = testContext
        )

        val response = client.analyzeCommand(request)
        assertTrue(response is BrainApiResponse.Success)

        val structured = (response as BrainApiResponse.Success).response
        assertEquals(ActionType.REPLAN_DAY.name, structured.intent)
        assertEquals("2", structured.actions[0].payload["delay_hours"])
    }

    @Test
    fun `analyzeCommand handles Quran recitation command`() = runTest {
        val request = RafiqBrainRequest(
            userCommand = "شغل سورة البقرة لمدة نصف ساعة",
            context = testContext
        )

        val response = client.analyzeCommand(request)
        assertTrue(response is BrainApiResponse.Success)

        val structured = (response as BrainApiResponse.Success).response
        assertEquals(ActionType.PLAY_AUDIO.name, structured.intent)
        assertEquals(30, structured.actions[0].durationMinutes)
        assertEquals("سورة البقرة", structured.actions[0].target)
    }

    @Test
    fun `analyzeCommand handles complex command with multiple actions`() = runTest {
        val request = RafiqBrainRequest(
            userCommand = "لخص ملف النحو وأنشئ 10 بطاقات استذكار",
            context = testContext
        )

        val response = client.analyzeCommand(request)
        assertTrue(response is BrainApiResponse.Success)

        val structured = (response as BrainApiResponse.Success).response
        assertTrue(structured.actions.size >= 2)
        assertTrue(structured.actions.any { it.type == ActionType.SUMMARIZE_DOCUMENT.name })
        assertTrue(structured.actions.any { it.type == ActionType.CREATE_FLASHCARDS.name })
    }

    @Test
    fun `analyzeCommand returns AIUnavailable when backend is not configured and simulation disabled`() = runTest {
        val unconfiguredClient = DefaultRafiqBrainClient(
            backendBaseUrl = null,
            enableLocalServerSimulation = false
        )
        val request = RafiqBrainRequest("أي أمر", testContext)
        val response = unconfiguredClient.analyzeCommand(request)
        assertTrue(response is BrainApiResponse.AIUnavailable)
    }

    @Test
    fun `analyzeCommand returns NetworkError when endpoint is not HTTPS`() = runTest {
        val insecureClient = DefaultRafiqBrainClient(
            backendBaseUrl = "http://insecure-backend.com/api",
            enableLocalServerSimulation = false
        )
        val request = RafiqBrainRequest("أي أمر", testContext)
        val response = insecureClient.analyzeCommand(request)
        assertTrue(response is BrainApiResponse.NetworkError)
    }

    @Test
    fun `analyzeCommand with valid HTTP 200 JSON returns Success`() = runTest {
        val validJson = """
            {
              "understood": true,
              "intent": "CREATE_REMINDER",
              "confidence": 0.95,
              "summary": "سأذكرك بمراجعة النحو",
              "actions": [
                {
                  "type": "CREATE_REMINDER",
                  "title": "مراجعة النحو",
                  "scheduledTime": "2026-09-29T08:00:00",
                  "repeat": "NONE"
                }
              ],
              "requiresConfirmation": true,
              "clarificationQuestion": null
            }
        """.trimIndent()

        val mockOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(validJson.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val httpClient = DefaultRafiqBrainClient(
            backendBaseUrl = "https://mock-backend.supabase.co/functions/v1/rafiq-brain",
            enableLocalServerSimulation = false,
            okHttpClient = mockOkHttpClient,
            tokenProvider = fakeTokenProvider
        )

        val response = httpClient.analyzeCommand(RafiqBrainRequest("ذكرني بالنحو", testContext))
        assertTrue(response is BrainApiResponse.Success)
        val structured = (response as BrainApiResponse.Success).response
        assertEquals("CREATE_REMINDER", structured.intent)
        assertEquals(0.95f, structured.confidence)
    }

    @Test
    fun `analyzeCommand with malformed JSON returns MalformedResponse`() = runTest {
        val malformedJson = "{ broken json: true "

        val mockOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(malformedJson.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val httpClient = DefaultRafiqBrainClient(
            backendBaseUrl = "https://mock-backend.supabase.co/functions/v1/rafiq-brain",
            enableLocalServerSimulation = false,
            okHttpClient = mockOkHttpClient,
            tokenProvider = fakeTokenProvider
        )

        val response = httpClient.analyzeCommand(RafiqBrainRequest("ذكرني بالنحو", testContext))
        assertTrue(response is BrainApiResponse.MalformedResponse)
    }

    @Test
    fun `analyzeCommand with HTTP 429 returns ServerError with rate limit message`() = runTest {
        val mockOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(429)
                    .message("Too Many Requests")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val httpClient = DefaultRafiqBrainClient(
            backendBaseUrl = "https://mock-backend.supabase.co/functions/v1/rafiq-brain",
            enableLocalServerSimulation = false,
            okHttpClient = mockOkHttpClient,
            tokenProvider = fakeTokenProvider
        )

        val response = httpClient.analyzeCommand(RafiqBrainRequest("ذكرني بالنحو", testContext))
        assertTrue(response is BrainApiResponse.ServerError)
        assertEquals(429, (response as BrainApiResponse.ServerError).statusCode)
    }

    @Test
    fun `analyzeCommand with HTTP 503 returns ServerError with unavailable message`() = runTest {
        val mockOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(503)
                    .message("Service Unavailable")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val httpClient = DefaultRafiqBrainClient(
            backendBaseUrl = "https://mock-backend.supabase.co/functions/v1/rafiq-brain",
            enableLocalServerSimulation = false,
            okHttpClient = mockOkHttpClient,
            tokenProvider = fakeTokenProvider
        )

        val response = httpClient.analyzeCommand(RafiqBrainRequest("ذكرني بالنحو", testContext))
        assertTrue(response is BrainApiResponse.ServerError)
        assertEquals(503, (response as BrainApiResponse.ServerError).statusCode)
    }

    @Test
    fun `analyzeCommand with network IOException returns NetworkError`() = runTest {
        val mockOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { _ ->
                throw java.io.IOException("No internet connectivity")
            }
            .build()

        val httpClient = DefaultRafiqBrainClient(
            backendBaseUrl = "https://mock-backend.supabase.co/functions/v1/rafiq-brain",
            enableLocalServerSimulation = false,
            okHttpClient = mockOkHttpClient,
            tokenProvider = fakeTokenProvider
        )

        val response = httpClient.analyzeCommand(RafiqBrainRequest("ذكرني بالنحو", testContext))
        assertTrue(response is BrainApiResponse.NetworkError)
    }

    @Test
    fun `analyzeCommand without access token returns AuthenticationRequired and does not call backend`() = runTest {
        var networkCalled = false
        val mockOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                networkCalled = true
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val emptyTokenProvider = object : RafiqAccessTokenProvider {
            override suspend fun getAccessToken(): String? = null
        }

        val httpClient = DefaultRafiqBrainClient(
            backendBaseUrl = "https://mock-backend.supabase.co/functions/v1/rafiq-brain",
            enableLocalServerSimulation = false,
            okHttpClient = mockOkHttpClient,
            tokenProvider = emptyTokenProvider
        )

        val response = httpClient.analyzeCommand(RafiqBrainRequest("ذكرني بالنحو", testContext))
        assertTrue(response is BrainApiResponse.AuthenticationRequired)
        assertEquals(false, networkCalled)
    }

    @Test
    fun `analyzeCommand with access token sends Authorization Bearer header`() = runTest {
        var sentAuthHeader: String? = null
        val mockOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                sentAuthHeader = chain.request().header("Authorization")
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""
                        {
                          "understood": true,
                          "intent": "CREATE_TASK",
                          "confidence": 0.9,
                          "summary": "تمت المصادقة",
                          "actions": [{"type": "CREATE_TASK", "title": "مهمة"}],
                          "requiresConfirmation": true
                        }
                    """.trimIndent().toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val httpClient = DefaultRafiqBrainClient(
            backendBaseUrl = "https://mock-backend.supabase.co/functions/v1/rafiq-brain",
            enableLocalServerSimulation = false,
            okHttpClient = mockOkHttpClient,
            tokenProvider = fakeTokenProvider
        )

        val response = httpClient.analyzeCommand(RafiqBrainRequest("مهمة جديدة", testContext))
        assertTrue(response is BrainApiResponse.Success)
        assertEquals("Bearer test-valid-jwt-token", sentAuthHeader)
    }

    @Test
    fun `analyzeCommand with HTTP 401 returns AuthenticationRequired`() = runTest {
        val mockOkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val httpClient = DefaultRafiqBrainClient(
            backendBaseUrl = "https://mock-backend.supabase.co/functions/v1/rafiq-brain",
            enableLocalServerSimulation = false,
            okHttpClient = mockOkHttpClient,
            tokenProvider = fakeTokenProvider
        )

        val response = httpClient.analyzeCommand(RafiqBrainRequest("مهمة جديدة", testContext))
        assertTrue(response is BrainApiResponse.AuthenticationRequired)
    }
}
