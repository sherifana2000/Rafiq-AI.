package com.example.auth

import com.example.brain.client.BackendEnvironment
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RafiqAuthManagerTest {

    @Before
    fun setUp() {
        BackendEnvironment.reset()
        SupabaseClientProvider.reset()
    }

    @Test
    fun `SupabaseAccessTokenProvider delegates to AuthManager successfully`() = runTest {
        val fakeAuthManager = object : RafiqAuthManager {
            override suspend fun ensureSession(): Boolean = true
            override suspend fun getAccessToken(): String = "mock-jwt-token-12345"
            override fun getCurrentUser() = null
            override fun getCurrentSession() = null
        }

        val provider = SupabaseAccessTokenProvider(fakeAuthManager)
        val token = provider.getAccessToken()

        assertNotNull(token)
        assertEquals("mock-jwt-token-12345", token)
    }

    @Test
    fun `DefaultRafiqAuthManager returns false and null token when client is not configured`() = runTest {
        val authManager = DefaultRafiqAuthManager { null }

        assertFalse(authManager.ensureSession())
        assertNull(authManager.getAccessToken())
        assertNull(authManager.getCurrentUser())
        assertNull(authManager.getCurrentSession())
    }

    @Test
    fun `BackendEnvironment manages Supabase config and resets safely`() {
        BackendEnvironment.setSupabaseConfig("https://example.supabase.co", "anon-key-publishable")

        assertEquals("https://example.supabase.co", BackendEnvironment.getSupabaseUrl())
        assertEquals("anon-key-publishable", BackendEnvironment.getSupabaseAnonKey())

        BackendEnvironment.reset()

        assertEquals(BackendEnvironment.DEFAULT_SUPABASE_URL, BackendEnvironment.getSupabaseUrl())
        val expectedDefaultKey = try {
            com.example.BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim().ifBlank { null }
        } catch (_: Exception) { null }
        assertEquals(expectedDefaultKey, BackendEnvironment.getSupabaseAnonKey())
    }

    @Test
    fun `BackendEnvironment endpoint configuration works properly`() {
        assertTrue(BackendEnvironment.isConfigured())
        assertEquals(BackendEnvironment.DEFAULT_ENDPOINT, BackendEnvironment.getEndpoint())

        BackendEnvironment.setEndpoint("https://example.supabase.co/functions/v1/rafiq-brain")
        assertTrue(BackendEnvironment.isConfigured())
        assertEquals("https://example.supabase.co/functions/v1/rafiq-brain", BackendEnvironment.getEndpoint())

        BackendEnvironment.reset()
        assertEquals(BackendEnvironment.DEFAULT_ENDPOINT, BackendEnvironment.getEndpoint())
    }
}
