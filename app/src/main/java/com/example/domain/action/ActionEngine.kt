package com.example.domain.action

import com.example.model.Action
import com.example.model.ActionType
import com.example.model.ExecutionStatus

/**
 * نتيجة تنفيذ الإجراء
 */
data class ExecutionResult(
    val isSuccess: Boolean,
    val status: ExecutionStatus,
    val feedbackMessageArabic: String,
    val actionType: ActionType
)

/**
 * واجهة محرك تنفيذ الأوامر (Action Engine).
 * يفصل منطق تنفيذ كل أمر (مثل فتح التطبيقات أو جدولة المنبهات عبر Android Services)
 * عن منطق الواجهة وتحليل النصوص.
 */
interface ActionEngine {
    suspend fun execute(action: Action): ExecutionResult
}
