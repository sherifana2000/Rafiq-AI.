package com.example.auth

/**
 * واجهة تجريد لتوفير Access Token الخاص بمصادقة المستخدم.
 * تعزل DefaultRafiqBrainClient عن تفاصيل Supabase أو أي مزود مصادقة آخر.
 */
interface RafiqAccessTokenProvider {
    suspend fun getAccessToken(): String?
}
