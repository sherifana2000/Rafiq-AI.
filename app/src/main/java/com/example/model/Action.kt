package com.example.model

/**
 * يمثل الإجراء القابل للتنفيذ المنبثق عن تحليل الأمر
 */
data class Action(
    val type: ActionType,
    val title: String,
    val target: String? = null, // e.g. "Facebook", "سورة آل عمران آية 15", "https://..."
    val scheduledTime: String? = null,
    val repeat: String? = null,
    val payload: Map<String, String> = emptyMap(),
    val description: String = ""
)
