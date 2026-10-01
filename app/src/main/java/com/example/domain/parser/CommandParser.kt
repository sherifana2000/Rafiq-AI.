package com.example.domain.parser

import com.example.model.UserCommand

/**
 * واجهة محلل الأوامر الذكي.
 * صُممت لتسمح باستبدال الـ Mock Parser بمحرك AI (مثل Gemini API)
 * في المرحلة الثانية دون المساس بباقي طبقات النظام.
 */
interface CommandParser {
    suspend fun parse(rawText: String): UserCommand
}
