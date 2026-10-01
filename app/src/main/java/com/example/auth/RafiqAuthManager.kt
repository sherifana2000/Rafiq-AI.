package com.example.auth

import com.example.brain.logging.RafiqLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession

/**
 * مدير المصادقة لتطبيق رفيق (RafiqAuthManager).
 * يدير جلسات المستخدم المجهول (Anonymous User) واسترجاع الـ Access Token
 * مع الحفاظ على بقاء الجلسة عبر Supabase Kotlin Session Persistence التلقائي.
 */
interface RafiqAuthManager {
    suspend fun ensureSession(): Boolean
    suspend fun getAccessToken(): String?
    fun getCurrentUser(): UserInfo?
    fun getCurrentSession(): UserSession?
}

class DefaultRafiqAuthManager(
    private val clientProvider: () -> SupabaseClient? = { SupabaseClientProvider.getClient() }
) : RafiqAuthManager {

    override suspend fun ensureSession(): Boolean {
        val client = clientProvider() ?: run {
            RafiqLogger.logError("AUTH_CLIENT_NULL", 0, "Supabase client is not initialized")
            return false
        }

        return try {
            val existingSession = client.auth.currentSessionOrNull()
            if (existingSession != null && existingSession.accessToken.isNotBlank()) {
                true
            } else {
                client.auth.signInAnonymously()
                val newSession = client.auth.currentSessionOrNull()
                newSession != null && newSession.accessToken.isNotBlank()
            }
        } catch (e: Exception) {
            RafiqLogger.logError("ANON_SIGNIN_FAILED", 0, e.message ?: "Anonymous sign-in error")
            false
        }
    }

    override suspend fun getAccessToken(): String? {
        val client = clientProvider() ?: return null

        val currentSession = client.auth.currentSessionOrNull()
        if (currentSession != null && currentSession.accessToken.isNotBlank()) {
            return currentSession.accessToken
        }

        val success = ensureSession()
        return if (success) {
            client.auth.currentSessionOrNull()?.accessToken
        } else {
            null
        }
    }

    override fun getCurrentUser(): UserInfo? {
        val client = clientProvider() ?: return null
        return try {
            client.auth.currentUserOrNull()
        } catch (_: Exception) {
            null
        }
    }

    override fun getCurrentSession(): UserSession? {
        val client = clientProvider() ?: return null
        return try {
            client.auth.currentSessionOrNull()
        } catch (_: Exception) {
            null
        }
    }
}
