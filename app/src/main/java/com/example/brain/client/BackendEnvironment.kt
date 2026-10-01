package com.example.brain.client

import com.example.BuildConfig

/**
 * تهيئة بيئة الاتصال بالـ Backend الخاص بـ Rafiq Brain.
 * تتيح تحديد رابط الخادم السحابي بطريقة مرنة وآمنة دون Hardcoding لأي مفاتيح سرية في السورس كود.
 */
object BackendEnvironment {

    const val DEFAULT_SUPABASE_URL = "https://kcthvpxqyirovqpfknnp.supabase.co"
    const val DEFAULT_ENDPOINT = "$DEFAULT_SUPABASE_URL/functions/v1/rafiq-brain"

    @Volatile
    var customEndpoint: String? = null

    @Volatile
    private var _supabaseUrl: String? = null

    @Volatile
    private var _supabaseAnonKey: String? = null

    /**
     * التحقق مما إذا كان الـ Backend مهيئاً
     */
    fun isConfigured(): Boolean {
        return !getEndpoint().isNullOrBlank()
    }

    /**
     * استرجاع رابط الـ Endpoint الفعّال
     */
    fun getEndpoint(): String? {
        val custom = customEndpoint?.trim()
        if (!custom.isNullOrBlank()) return custom

        val url = getSupabaseUrl()
        return if (url.isNotBlank()) {
            "$url/functions/v1/rafiq-brain"
        } else {
            DEFAULT_ENDPOINT
        }
    }

    /**
     * تعيين عنوان الـ Backend
     */
    fun setEndpoint(url: String?) {
        customEndpoint = url?.trim()
    }

    fun getSupabaseUrl(): String {
        val configured = _supabaseUrl?.trim()
        if (!configured.isNullOrBlank()) return configured

        return try {
            BuildConfig.SUPABASE_URL.trim().ifBlank { DEFAULT_SUPABASE_URL }
        } catch (_: Exception) {
            DEFAULT_SUPABASE_URL
        }
    }

    fun getSupabasePublishableKey(): String? {
        val configured = _supabaseAnonKey?.trim()
        if (!configured.isNullOrBlank()) return configured

        return try {
            val pub = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
            if (pub.isNotBlank()) return pub
            val anon = BuildConfig.SUPABASE_ANON_KEY.trim()
            if (anon.isNotBlank()) return anon
            null
        } catch (_: Exception) {
            null
        }
    }

    fun getSupabaseAnonKey(): String? {
        return getSupabasePublishableKey()
    }

    fun setSupabaseConfig(url: String?, anonKey: String?) {
        _supabaseUrl = url?.trim()
        _supabaseAnonKey = anonKey?.trim()
    }

    /**
     * إعادة ضبط التهيئة للاختبارات
     */
    fun reset() {
        customEndpoint = null
        _supabaseUrl = null
        _supabaseAnonKey = null
    }
}

