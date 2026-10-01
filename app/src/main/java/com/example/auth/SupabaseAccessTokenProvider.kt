package com.example.auth

/**
 * التطبيق المعتمد لـ RafiqAccessTokenProvider استناداً إلى RafiqAuthManager.
 */
class SupabaseAccessTokenProvider(
    private val authManager: RafiqAuthManager = DefaultRafiqAuthManager()
) : RafiqAccessTokenProvider {

    override suspend fun getAccessToken(): String? {
        return authManager.getAccessToken()
    }
}
