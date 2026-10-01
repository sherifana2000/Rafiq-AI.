package com.example.brain.client

import com.example.brain.model.RafiqUserContext
import com.example.brain.model.StructuredRafiqResponse
import com.squareup.moshi.JsonClass

/**
 * طلب التحليل المرسل من التطبيق إلى Backend API الخاص بـ Rafiq Brain
 */
@JsonClass(generateAdapter = true)
data class RafiqBrainRequest(
    val userCommand: String,
    val context: RafiqUserContext
)

/**
 * نتيجة الاستجابة من طبقة الـ Client
 */
sealed class BrainApiResponse {
    data class Success(val response: StructuredRafiqResponse) : BrainApiResponse()
    data class NeedsClarification(val question: String, val response: StructuredRafiqResponse) : BrainApiResponse()
    data class NetworkError(val message: String) : BrainApiResponse()
    data class ServerError(val statusCode: Int, val message: String) : BrainApiResponse()
    data class AIUnavailable(val message: String) : BrainApiResponse()
    data class AuthenticationRequired(val message: String) : BrainApiResponse()
    data class MalformedResponse(val reason: String) : BrainApiResponse()
}

/**
 * واجهة الاتصال بمحرك رفيق عبر الـ Backend (RafiqBrainClient).
 * تفصل كود تطبيق الأندرويد عن مفاتيح الـ API أو تفاصيل استدعاء Gemini المباشرة
 * لضمان الأمان التام والامتثال لعدم تضمين أسرار داخل حزمة الـ APK.
 */
interface RafiqBrainClient {
    suspend fun analyzeCommand(request: RafiqBrainRequest): BrainApiResponse
}
