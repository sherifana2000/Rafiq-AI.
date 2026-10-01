package com.example.brain.client

import com.example.auth.RafiqAccessTokenProvider
import com.example.auth.SupabaseAccessTokenProvider
import com.example.brain.logging.RafiqLogger
import com.example.brain.model.StructuredActionItem
import com.example.brain.model.StructuredRafiqResponse
import com.example.model.ActionType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * العميل الفعلي للربط مع Backend API (Supabase Edge Function / Proxy) الخاص بـ Rafiq Brain.
 * يتميز بالأمان التام:
 * - لا يحتوي على أي مفاتيح API أو Secrets داخل تطبيق الأندرويد.
 * - يتصل بالـ Backend عبر HTTPS فقط ومزود بـ User JWT في Authorization Bearer.
 */
class DefaultRafiqBrainClient(
    private val backendBaseUrl: String? = BackendEnvironment.getEndpoint(),
    private val enableLocalServerSimulation: Boolean = false,
    private val okHttpClient: OkHttpClient = defaultOkHttpClient,
    private val tokenProvider: RafiqAccessTokenProvider? = SupabaseAccessTokenProvider()
) : RafiqBrainClient {

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private val defaultOkHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
        }
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val responseAdapter = moshi.adapter(StructuredRafiqResponse::class.java)

    override suspend fun analyzeCommand(request: RafiqBrainRequest): BrainApiResponse = withContext(Dispatchers.IO) {
        val rawInput = request.userCommand.trim()
        val context = request.context
        val startTime = System.currentTimeMillis()

        // 1. استخدام المحاكاة في الاختبارات فقط عند تفعيلها صراحة
        if (enableLocalServerSimulation) {
            delay(300)
            return@withContext simulateBackendGeminiResponse(rawInput, context)
        }

        // 2. التحقق من وجود عنوان Backend مضبوط
        val endpoint = backendBaseUrl?.trim()
        if (endpoint.isNullOrBlank()) {
            RafiqLogger.logError("BACKEND_NOT_CONFIGURED", 0, "Backend endpoint is not configured")
            return@withContext BrainApiResponse.AIUnavailable(
                "خدمة رفيق السحابية غير مهيأة حالياً. يرجى ضبط عنوان الخادم في الإعدادات."
            )
        }

        // 3. التحقق الأمني من استخدام HTTPS
        if (!endpoint.startsWith("https://", ignoreCase = true)) {
            RafiqLogger.logError("INSECURE_PROTOCOL", 0, "Endpoint must use HTTPS protocol")
            return@withContext BrainApiResponse.NetworkError(
                "يجب استخدام بروتوكول آمن (HTTPS) للاتصال بخادم رفيق."
            )
        }

        // 4. التحقق من وجود Access Token للمستخدم
        val accessToken = tokenProvider?.getAccessToken()?.trim()
        if (accessToken.isNullOrBlank()) {
            RafiqLogger.logError("AUTH_TOKEN_MISSING", 0, "No access token available for request")
            return@withContext BrainApiResponse.AuthenticationRequired(
                "جلسة المصادقة غير متوفرة. يرجى تهيئة الاتصال بحساب المستخدم أولاً."
            )
        }

        // 5. بناء الـ Payload الموجه لـ Supabase Edge Function
        val payloadMap = mapOf(
            "command" to rawInput,
            "userCommand" to rawInput,
            "context" to mapOf(
                "locale" to context.locale,
                "timezone" to context.timeZone,
                "timeZone" to context.timeZone,
                "currentDate" to context.currentDate,
                "currentTime" to context.currentTime,
                "currentDateTime" to "${context.currentDate}T${context.currentTime}:00"
            )
        )

        val jsonPayload = try {
            moshi.adapter(Map::class.java).toJson(payloadMap)
        } catch (e: Exception) {
            return@withContext BrainApiResponse.MalformedResponse("تعذر تجهيز بيانات الطلب: ${e.message}")
        }

        val httpRequest = Request.Builder()
            .url(endpoint)
            .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .build()

        // 6. إرسال الطلب ومعالجة رموز الاستجابة
        try {
            val response = okHttpClient.newCall(httpRequest).execute()
            val latencyMs = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string().orEmpty()
            val statusCode = response.code

            when (statusCode) {
                in 200..299 -> {
                    val parsed = try {
                        responseAdapter.fromJson(responseBody)
                    } catch (e: Exception) {
                        null
                    }

                    if (parsed != null) {
                        RafiqLogger.logRequest(parsed.intent, latencyMs, parsed.confidence, isSuccess = true)
                        BrainApiResponse.Success(parsed)
                    } else {
                        RafiqLogger.logError("MALFORMED_JSON", latencyMs, "Failed to parse structured JSON from server")
                        BrainApiResponse.MalformedResponse("استجابة الخادم غير متوافقة مع نموذج رفيق المنظم.")
                    }
                }

                400 -> {
                    RafiqLogger.logError("HTTP_400", latencyMs, "Bad request sent to backend")
                    BrainApiResponse.ServerError(400, "طلب غير صالح تم إرساله إلى خادم رفيق.")
                }

                401 -> {
                    RafiqLogger.logError("HTTP_401", latencyMs, "Unauthorized backend access")
                    BrainApiResponse.AuthenticationRequired("جلسة المستخدم غير صالحة أو منتهية. يرجى تجديد الدخول.")
                }

                403 -> {
                    RafiqLogger.logError("HTTP_403", latencyMs, "Forbidden backend access")
                    BrainApiResponse.ServerError(403, "غير مصرح بالوصول إلى خدمة رفيق السحابية.")
                }

                429 -> {
                    RafiqLogger.logError("HTTP_429", latencyMs, "Rate limited by Gemini/Backend")
                    BrainApiResponse.ServerError(429, "خادم رفيق مشغول حاليًا بسبب كثرة الطلبات. يرجى المحاولة بعد قليل.")
                }

                500 -> {
                    RafiqLogger.logError("HTTP_500", latencyMs, "Internal server error")
                    BrainApiResponse.ServerError(500, "حدث خطأ داخلي في خادم رفيق السحابي. يرجى المحاولة لاحقاً.")
                }

                502, 503 -> {
                    RafiqLogger.logError("HTTP_$statusCode", latencyMs, "Backend service unavailable")
                    BrainApiResponse.ServerError(statusCode, "تعذر الاتصال بعقل رفيق حالياً. حاول مرة أخرى بعد قليل.")
                }

                else -> {
                    RafiqLogger.logError("HTTP_$statusCode", latencyMs, "Unexpected status code")
                    BrainApiResponse.ServerError(statusCode, "تعذر معالجة الطلب على الخادم (رمز الحالة: $statusCode).")
                }
            }
        } catch (e: SocketTimeoutException) {
            val latencyMs = System.currentTimeMillis() - startTime
            RafiqLogger.logError("TIMEOUT", latencyMs, e.message ?: "SocketTimeout")
            BrainApiResponse.NetworkError("استغرق خادم رفيق وقتاً طويلاً للاستجابة. يرجى المحاولة مرة أخرى.")
        } catch (e: IOException) {
            val latencyMs = System.currentTimeMillis() - startTime
            RafiqLogger.logError("NETWORK_IO_ERROR", latencyMs, e.message ?: "IOException")
            BrainApiResponse.NetworkError("تعذر الاتصال بعقل رفيق حالياً. تأكد من اتصال الإنترنت وحاول مرة أخرى.")
        } catch (e: Exception) {
            val latencyMs = System.currentTimeMillis() - startTime
            RafiqLogger.logError("UNEXPECTED_ERROR", latencyMs, e.message ?: "Exception")
            BrainApiResponse.ServerError(500, "حدث خطأ غير متوقع أثناء معالجة الطلب.")
        }
    }

    /**
     * يحاكي ما يعيده Backend بعد استدعاء Gemini بنموذج JSON المنظم
     */
    private fun simulateBackendGeminiResponse(rawInput: String, context: com.example.brain.model.RafiqUserContext): BrainApiResponse {
        val text = rawInput.trim()

        // 1. فحص الأوامر الغامضة ذات الثقة المنخفضة (< 0.70) - Requirement 9
        if (isAmbiguousCommand(text)) {
            val clarification = generateClarification(text)
            val response = StructuredRafiqResponse(
                understood = true,
                intent = ActionType.CREATE_REMINDER.name,
                confidence = 0.65f, // ثقة منخفضة تمنع التنفيذ العشوائي
                summary = "الأمر يحتاج إلى مزيد من التوضيح بخصوص التوقيت.",
                actions = emptyList(),
                requiresConfirmation = true,
                clarificationQuestion = clarification
            )
            return BrainApiResponse.NeedsClarification(clarification, response)
        }

        // 2. الأمر المركب (Requirement 11): ملف + تلخيص + بطاقات
        if (text.contains("لخص") && (text.contains("بطاقات") || text.contains("كروت"))) {
            val response = StructuredRafiqResponse(
                understood = true,
                intent = ActionType.OPEN_FILE.name,
                confidence = 0.94f,
                summary = "سأقوم بفتح ملف النحو وتلخيصه ثم إنشاء 10 بطاقات استذكار غدًا الساعة 8:00 صباحًا.",
                actions = listOf(
                    StructuredActionItem(
                        type = ActionType.OPEN_FILE.name,
                        title = "فتح ملف النحو",
                        target = "ملف النحو",
                        scheduledTime = "${context.currentDate}T08:00:00"
                    ),
                    StructuredActionItem(
                        type = ActionType.SUMMARIZE_DOCUMENT.name,
                        title = "تلخيص ملف النحو",
                        target = "ملف النحو"
                    ),
                    StructuredActionItem(
                        type = ActionType.CREATE_FLASHCARDS.name,
                        title = "إنشاء 10 بطاقات استذكار",
                        payload = mapOf("count" to "10")
                    )
                ),
                requiresConfirmation = true,
                clarificationQuestion = null
            )
            return BrainApiResponse.Success(response)
        }

        // 3. إعادة تخطيط اليوم (REPLAN_DAY)
        if (text.contains("متأخر") || text.contains("تاخرت") || text.contains("أعد ترتيب") || text.contains("اعد ترتيب") || text.contains("رتب باقي اليوم")) {
            val hours = extractHours(text) ?: 2
            val response = StructuredRafiqResponse(
                understood = true,
                intent = ActionType.REPLAN_DAY.name,
                confidence = 0.96f,
                summary = "سأعيد ترتيب وتأخير جدول باقي اليوم بمقدار $hours ساعة ليتناسب مع وقتك الحالي.",
                actions = listOf(
                    StructuredActionItem(
                        type = ActionType.REPLAN_DAY.name,
                        title = "إعادة ترتيب اليوم",
                        payload = mapOf("delay_hours" to hours.toString()),
                        scheduledTime = context.currentTime
                    )
                ),
                requiresConfirmation = true,
                clarificationQuestion = null
            )
            return BrainApiResponse.Success(response)
        }

        // 4. أوامر القرآن الكريم (OPEN_QURAN)
        if (text.contains("القرآن") || text.contains("سورة") || text.contains("آية") || text.contains("آل عمران") || text.contains("البقرة")) {
            if (text.contains("شغل") || text.contains("صوت") || text.contains("استمع")) {
                // تشغيل صوتي للقرآن
                val duration = extractMinutes(text) ?: 30
                val target = if (text.contains("البقرة")) "سورة البقرة" else "القرآن الكريم"
                val response = StructuredRafiqResponse(
                    understood = true,
                    intent = ActionType.PLAY_AUDIO.name,
                    confidence = 0.96f,
                    summary = "تشغيل تلاوة $target لمدة $duration دقيقة في التوقيت المحدد.",
                    actions = listOf(
                        StructuredActionItem(
                            type = ActionType.PLAY_AUDIO.name,
                            title = "تلاوة $target",
                            target = target,
                            durationMinutes = duration,
                            scheduledTime = "08:00:00"
                        )
                    ),
                    requiresConfirmation = true,
                    clarificationQuestion = null
                )
                return BrainApiResponse.Success(response)
            } else {
                // فتح المصحف على سورة محددة
                val target = extractSurah(text)
                val response = StructuredRafiqResponse(
                    understood = true,
                    intent = ActionType.OPEN_QURAN.name,
                    confidence = 0.97f,
                    summary = "فتح المصحف على $target.",
                    actions = listOf(
                        StructuredActionItem(
                            type = ActionType.OPEN_QURAN.name,
                            title = target,
                            target = target,
                            scheduledTime = context.currentTime
                        )
                    ),
                    requiresConfirmation = false,
                    clarificationQuestion = null
                )
                return BrainApiResponse.Success(response)
            }
        }

        // 5. بحث يوتيوب (SEARCH_YOUTUBE)
        if (text.contains("يوتيوب") || text.contains("فيديو")) {
            val query = extractQuery(text, "يوتيوب")
            val response = StructuredRafiqResponse(
                understood = true,
                intent = ActionType.SEARCH_YOUTUBE.name,
                confidence = 0.95f,
                summary = "البحث في يوتيوب عن: '$query' في الساعة العاشرة.",
                actions = listOf(
                    StructuredActionItem(
                        type = ActionType.SEARCH_YOUTUBE.name,
                        title = query,
                        target = query,
                        scheduledTime = "10:00:00"
                    )
                ),
                requiresConfirmation = false,
                clarificationQuestion = null
            )
            return BrainApiResponse.Success(response)
        }

        // 6. فتح ملف (OPEN_FILE)
        if (text.contains("ملف") || text.contains("مستند")) {
            val fileName = if (text.contains("النحو")) "ملف النحو" else "المستند"
            val response = StructuredRafiqResponse(
                understood = true,
                intent = ActionType.OPEN_FILE.name,
                confidence = 0.95f,
                summary = "فتح $fileName غدًا في تمام الساعة 9:00 صباحًا.",
                actions = listOf(
                    StructuredActionItem(
                        type = ActionType.OPEN_FILE.name,
                        title = fileName,
                        target = fileName,
                        scheduledTime = "09:00:00"
                    )
                ),
                requiresConfirmation = true,
                clarificationQuestion = null
            )
            return BrainApiResponse.Success(response)
        }

        // 7. فتح تطبيق (OPEN_APP)
        if (text.contains("افتح") && (text.contains("Facebook") || text.contains("فيسبوك") || text.contains("WhatsApp") || text.contains("واتساب"))) {
            val app = if (text.contains("Facebook") || text.contains("فيسبوك")) "Facebook" else "WhatsApp"
            val response = StructuredRafiqResponse(
                understood = true,
                intent = ActionType.OPEN_APP.name,
                confidence = 0.96f,
                summary = "فتح تطبيق $app الساعة 8:00 مساءً.",
                actions = listOf(
                    StructuredActionItem(
                        type = ActionType.OPEN_APP.name,
                        title = "فتح $app",
                        target = app,
                        scheduledTime = "20:00:00"
                    )
                ),
                requiresConfirmation = false,
                clarificationQuestion = null
            )
            return BrainApiResponse.Success(response)
        }

        // 8. التذكيرات اليومية أو المحددة (CREATE_REMINDER)
        if (text.contains("ذكرني") || text.contains("تنبيه") || text.contains("منبه")) {
            val isDaily = text.contains("كل يوم") || text.contains("يومياً") || text.contains("يوميا")
            val topic = extractTopic(text)
            val time = extractTime(text) ?: if (isDaily) "06:00:00" else "08:00:00"
            val repeat = if (isDaily) "DAILY" else "NONE"

            val summary = if (isDaily) {
                "سأذكرك يوميًا الساعة 6:00 صباحًا بـ $topic."
            } else {
                "سأذكرك غدًا الساعة 8:00 صباحًا بـ $topic."
            }

            val response = StructuredRafiqResponse(
                understood = true,
                intent = ActionType.CREATE_REMINDER.name,
                confidence = 0.98f,
                summary = summary,
                actions = listOf(
                    StructuredActionItem(
                        type = ActionType.CREATE_REMINDER.name,
                        title = topic,
                        scheduledTime = time,
                        repeat = repeat
                    )
                ),
                requiresConfirmation = true,
                clarificationQuestion = null
            )
            return BrainApiResponse.Success(response)
        }

        // 9. جلسات التركيز (START_FOCUS_SESSION)
        if (text.contains("تركيز") || text.contains("focus", ignoreCase = true)) {
            val minutes = extractMinutes(text) ?: 50
            val response = StructuredRafiqResponse(
                understood = true,
                intent = ActionType.START_FOCUS_SESSION.name,
                confidence = 0.95f,
                summary = "بدء جلسة تركيز هادئة لمدة $minutes دقيقة.",
                actions = listOf(
                    StructuredActionItem(
                        type = ActionType.START_FOCUS_SESSION.name,
                        title = "جلسة تركيز ($minutes دقيقة)",
                        durationMinutes = minutes,
                        scheduledTime = context.currentTime
                    )
                ),
                requiresConfirmation = true,
                clarificationQuestion = null
            )
            return BrainApiResponse.Success(response)
        }

        // 10. إنشاء مهمة عامة (CREATE_TASK)
        val response = StructuredRafiqResponse(
            understood = true,
            intent = ActionType.CREATE_TASK.name,
            confidence = 0.88f,
            summary = "تم فهم المهمة وتسجيلها: $text",
            actions = listOf(
                StructuredActionItem(
                    type = ActionType.CREATE_TASK.name,
                    title = text,
                    scheduledTime = context.currentTime
                )
            ),
            requiresConfirmation = true,
            clarificationQuestion = null
        )
        return BrainApiResponse.Success(response)
    }

    private fun isAmbiguousCommand(text: String): Boolean {
        val trimmed = text.trim()
        val ambiguousPhrases = listOf("ذكرني أذاكر", "ذكرني اذاكر", "ذكرني", "فكرني", "تنبيه", "سجل لي")
        if (ambiguousPhrases.any { trimmed == it || trimmed == "$it فقط" }) return true
        // أوامر قصيرة بدون توقيت أو موضوع كافٍ
        if (trimmed.startsWith("ذكرني") && !trimmed.contains("الساعة") && !trimmed.contains("بكرة") && !trimmed.contains("غدا") && !trimmed.contains("الصبح") && !trimmed.contains("مساء") && trimmed.split(" ").size <= 2) {
            return true
        }
        return false
    }

    private fun generateClarification(text: String): String {
        return when {
            text.contains("أذاكر") || text.contains("اذاكر") || text.contains("مذاكرة") -> "أكيد، تحب أذكرك الساعة كام؟"
            text.contains("ذكرني") || text.contains("فكرني") -> "أكيد، تحب أذكرك بإيه والساعة كام؟"
            else -> "ممكن توضح لي أكثر ما الذي ترغب به والوقت المحدد؟"
        }
    }

    private fun extractHours(text: String): Int? {
        if (text.contains("ساعتين")) return 2
        if (text.contains("ساعة")) return 1
        return Regex("""\d+""").find(text)?.value?.toIntOrNull()
    }

    private fun extractMinutes(text: String): Int? {
        if (text.contains("نصف ساعة") || text.contains("نص ساعة")) return 30
        if (text.contains("ساعة")) return 60
        return Regex("""\d+""").find(text)?.value?.toIntOrNull()
    }

    private fun extractSurah(text: String): String {
        val idx = text.indexOf("سورة")
        if (idx != -1) return text.substring(idx).trim()
        if (text.contains("آل عمران")) {
            val ayah = Regex("""آية\s*(\d+)""").find(text)?.groupValues?.get(1) ?: "15"
            return "سورة آل عمران آية $ayah"
        }
        if (text.contains("البقرة")) return "سورة البقرة"
        return "القرآن الكريم"
    }

    private fun extractQuery(text: String, keyword: String): String {
        val after = text.substringAfter("عن", "").substringBefore(keyword).trim()
        if (after.isNotBlank()) return after
        val clean = text.replace("الساعة 10", "")
            .replace("ابحث لي عن", "")
            .replace("ابحث عن", "")
            .replace("على يوتيوب", "")
            .replace("في يوتيوب", "")
            .replace("يوتيوب", "")
            .trim()
        return clean.ifBlank { "شرح المفعول المطلق" }
    }

    private fun extractTopic(text: String): String {
        val clean = text.replace(Regex("""(بكرة|غداً|غدا|كل يوم|يومياً|يوميا|الساعة\s*\d+|الصبح|صباحاً|مساءً|مساء|ذكرني|فكرني|أن|ان|بـ|ب)"""), " ").trim()
        val normalized = clean.replace(Regex("""\s+"""), " ")
        if (normalized.contains("النحو")) return "مذاكرة النحو"
        if (normalized.contains("القرآن")) return "قراءة ورد القرآن الكريم"
        return normalized.ifBlank { "المذاكرة والمهام" }
    }

    private fun extractTime(text: String): String? {
        val regex = Regex("""الساعة\s*(\d{1,2})""")
        val match = regex.find(text) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val isPm = text.contains("مساءً") || text.contains("مساء") || text.contains("بالليل")
        val finalHour = if (isPm && hour < 12) hour + 12 else if (!isPm && hour == 12) 0 else hour
        return "%02d:00:00".format(finalHour)
    }
}
