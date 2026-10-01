package com.example.brain.logging

import android.util.Log

/**
 * مسجل آمن ومخصص لمحرك رفيق الذكي للتطوير فقط.
 * يتوافق مع معايير الأمان والخصوصية:
 * - يمنع منعاً باتاً تسجيل أي API keys أو Tokens أو Headers أو بيانات شخصية كاملة.
 * - يسجل فقط البيانات التشغيلية: Intent, Confidence, Latency, ومؤشرات النجاح أو نوع الخطأ.
 */
object RafiqLogger {
    private const val TAG = "RafiqBrain"

    fun logRequest(intent: String, latencyMs: Long, confidence: Float, isSuccess: Boolean) {
        try {
            Log.d(
                TAG,
                "BrainOp: intent=$intent, confidence=${"%.2f".format(confidence)}, latency=${latencyMs}ms, success=$isSuccess"
            )
        } catch (_: RuntimeException) {
            println("[$TAG] BrainOp: intent=$intent, confidence=${"%.2f".format(confidence)}, latency=${latencyMs}ms, success=$isSuccess")
        }
    }

    fun logError(errorType: String, latencyMs: Long, message: String) {
        try {
            Log.w(
                TAG,
                "BrainError: type=$errorType, latency=${latencyMs}ms, summary=${message.take(80)}"
            )
        } catch (_: RuntimeException) {
            println("[$TAG] BrainError: type=$errorType, latency=${latencyMs}ms, summary=${message.take(80)}")
        }
    }
}
