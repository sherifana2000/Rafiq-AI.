package com.example.auth

import com.example.brain.client.BackendEnvironment
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

/**
 * مزود مركزي لعميل Supabase في التطبيق.
 * يعتمد على SUPABASE_URL و Publishable/Anon Key دون أي أسرار حساسة داخل كود أندرويد.
 */
object SupabaseClientProvider {
    @Volatile
    private var customClient: SupabaseClient? = null

    fun getClient(): SupabaseClient? {
        val existing = customClient
        if (existing != null) return existing

        val url = BackendEnvironment.getSupabaseUrl()
        val publishableKey = BackendEnvironment.getSupabasePublishableKey() ?: return null

        return synchronized(this) {
            customClient ?: try {
                createSupabaseClient(
                    supabaseUrl = url,
                    supabaseKey = publishableKey
                ) {
                    install(Auth)
                }.also { customClient = it }
            } catch (_: Exception) {
                null
            }
        }
    }

    fun setClientForTesting(client: SupabaseClient?) {
        customClient = client
    }

    fun reset() {
        customClient = null
    }
}
