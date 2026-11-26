package com.tazer.taskorganizer.integration

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidIntegrationTest {
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val task = TaskAnalysisInput(
        "  Implement sorting ",
        " Algorithms ",
        "",
        Instant.parse("2026-12-02T10:00:00Z"),
    )

    @Test
    fun analysisValidatesBoundsAndNumericTypes() {
        assertEquals("Implement sorting", task.toJson().string("title"))
        assertEquals("Algorithms", task.toJson().string("course"))
        expectError(IntegrationError.VALIDATION_ERROR) { task.copy(course = " ").toJson() }
        expectError(IntegrationError.VALIDATION_ERROR) { task.copy(description = "a".repeat(4001)).toJson() }
        val valid = """{"difficultyScore":4,"estimatedMinutes":150}"""
        assertEquals(TaskAnalysisResult(4, 150), parseJson(valid).analysis())
        expectError(IntegrationError.INVALID_RESPONSE) { parseJson(valid.replace("150", "\"150\"")).analysis() }
        expectError(IntegrationError.INVALID_RESPONSE) { parseJson(valid.replace("150", "10081")).analysis() }
        expectError(IntegrationError.INVALID_RESPONSE) { parseJson(valid.replace("150", "150.1")).analysis() }
        expectError(IntegrationError.INVALID_RESPONSE) { parseJson(valid.replace("150", "150,\"extra\":1")).analysis() }
    }

    @Test
    fun backendRequiresExpectedStatusAndSubscriptionShape() {
        val valid = """{"tier":"PRO","monthlyLimit":100,"remaining":1,"resetsAt":"2026-12-01T00:00:00Z"}"""
        assertEquals(SubscriptionTier.PRO, HttpResponse(200, valid, null).backendJson().subscription().tier)
        assertEquals(100, parseJson(valid).subscription().monthlyLimit)
        expectError(IntegrationError.INVALID_RESPONSE) { parseJson(valid.replace("PRO", "FREE")).subscription() }
        expectError(IntegrationError.INVALID_RESPONSE) {
            HttpResponse(403, """{"error":{"code":"QUOTA_EXHAUSTED"}}""", null).backendJson()
        }
        expectError(IntegrationError.INVALID_AUTH) { HttpResponse(401, "unauthorized", null).backendJson() }
        val failure = assertThrows(IntegrationException::class.java) {
            HttpResponse(429, """{"error":{"code":"QUOTA_EXHAUSTED","message":"ignored"}}""", 55).backendJson()
        }
        assertEquals(IntegrationError.QUOTA_EXHAUSTED, failure.code)
        assertEquals(55L, failure.retryAfterSeconds)
    }

    @Test
    fun googleDenialAndExplicitCancellationConsumeOnlyTheMatchingGoogleFlow() {
        val directory = File(context.noBackupFilesDir, UUID.randomUUID().toString())
        assertTrue(directory.mkdirs())
        val pkce = AuthPkce(directory)
        try {
            val google = pkce.start(AuthFlow.GOOGLE)
            expectError(IntegrationError.INVALID_CALLBACK) {
                pkce.consume(Uri.parse("$AUTH_REDIRECT?state=wrong#error=access_denied"))
            }
            expectError(IntegrationError.GOOGLE_CANCELLED) {
                pkce.consume(Uri.parse("${google.redirect}#error=access_denied"))
            }
            expectError(IntegrationError.INVALID_CALLBACK) {
                pkce.consume(Uri.parse("${google.redirect}&code=example"))
            }
            val newGoogle = pkce.start(AuthFlow.GOOGLE)
            pkce.cancelGoogle()
            expectError(IntegrationError.INVALID_CALLBACK) {
                pkce.consume(Uri.parse("${newGoogle.redirect}&code=example"))
            }
            val recovery = pkce.start(AuthFlow.RECOVERY)
            pkce.cancelGoogle()
            assertEquals("recovery-code", pkce.consume(Uri.parse("${recovery.redirect}&code=recovery-code")).second)
        } finally {
            pkce.clear()
            directory.delete()
        }
    }

    @Test
    fun pkceBindsOneCallbackToStateAndExactRedirect() {
        val directory = File(context.noBackupFilesDir, UUID.randomUUID().toString())
        assertTrue(directory.mkdirs())
        val original = AuthPkce(directory).start(AuthFlow.GOOGLE)
        val restored = AuthPkce(directory)
        try {
            expectError(IntegrationError.INVALID_CALLBACK) {
                restored.consume(Uri.parse("$AUTH_REDIRECT?state=wrong&code=example"))
            }
            expectError(IntegrationError.INVALID_CALLBACK) {
                restored.consume(Uri.parse("${original.redirect}&state=${original.state}&code=example"))
            }
            expectError(IntegrationError.INVALID_CALLBACK) {
                restored.consume(Uri.parse("${original.redirect}&code=example#access_token=unexpected"))
            }
            assertFalse(isAuthCallback(Uri.parse("tazer://other/callback")))
            val redirect = Uri.parse("${original.redirect}&code=example")
            val (pending, code) = restored.consume(redirect)
            assertEquals(original.challenge, pending.challenge)
            assertEquals(43, pending.verifier.length)
            assertEquals("example", code)
            assertNotEquals(pending.state, pending.verifier)
            expectError(IntegrationError.INVALID_CALLBACK) { restored.consume(redirect) }
        } finally {
            restored.clear()
            directory.delete()
        }
    }

    @Test
    fun keystoreVaultKeepsByokSeparateAndDeletesCredentials() {
        val first = AccountUid(UUID.randomUUID().toString())
        val second = AccountUid(UUID.randomUUID().toString())
        val settings = ByokSettings(ByokProvider.GROQ, "test/model")
        try {
            val vault = ByokVault(context)
            vault.save(first, settings, "test-key")
            val restored = ByokVault(context)
            assertEquals(settings, restored.read(first)?.settings)
            assertEquals("test-key", restored.read(first)?.apiKey)
            assertNull(restored.read(second))
            restored.delete(first)
            assertNull(vault.read(first))
        } finally {
            ByokVault(context).delete(first)
        }
    }

    @Test
    fun verifiedSessionVaultRestoresOnlyForItsOriginAndClears() {
        val directory = File(context.noBackupFilesDir, UUID.randomUUID().toString())
        assertTrue(directory.mkdirs())
        val alias = "tazer.integration.test.${directory.name}"
        val vault = SessionVault(directory, "test-origin", alias)
        try {
            vault.save(json("marker" to "sentinel-access-token"))
            val ciphertext = File(directory, "tazer_verified_session").readBytes()
            assertFalse(ciphertext.toString(Charsets.UTF_8).contains("sentinel-access-token"))
            assertEquals(
                "sentinel-access-token",
                SessionVault(directory, "test-origin", alias).read()?.string("marker"),
            )
            expectError(IntegrationError.VERIFICATION_UNAVAILABLE) {
                SessionVault(directory, "another-origin", alias).read()
            }
            vault.clear()
            assertNull(vault.read())
        } finally {
            vault.clear()
            directory.delete()
        }
    }

    @Test
    fun configuredLoopbackEnforcesTiersAndChargesOneProRequest() = runBlocking {
        val integration = AndroidIntegration(context)
        assumeTrue(integration.mockEnabled)
        val free = integration.signInMock(SubscriptionTier.FREE)
        assertEquals("demo-free", free.uid.value)
        assertEquals(SubscriptionTier.FREE, free.tier)
        try {
            integration.analyzeCloud(task)
            fail("Expected PRO_REQUIRED")
        } catch (error: IntegrationException) {
            assertEquals(IntegrationError.PRO_REQUIRED, error.code)
        }
        val pro = integration.signInMock(SubscriptionTier.PRO)
        assertEquals("demo-pro", pro.uid.value)
        assertTrue("Mock backend needs a remaining quota slot", pro.subscription.remaining > 0)
        val analysis = integration.analyzeCloud(task)
        assertEquals(TaskAnalysisResult(4, 150), analysis.copy(subtasks = emptyList()))
        assertTrue(analysis.subtasks.size in 3..5 && analysis.subtasks.all { it.isNotBlank() })
        assertEquals(pro.subscription.remaining - 1, integration.subscription().remaining)
        val byok = integration.signInMock(SubscriptionTier.BYOK)
        assertEquals("demo-byok", byok.uid.value)
        assertEquals(0, byok.subscription.remaining)
        integration.signOut()
        assertNull(integration.restoreAccount())
    }

    private fun expectError(expected: IntegrationError, action: () -> Unit) {
        val failure = assertThrows(IntegrationException::class.java) { action() }
        assertEquals(expected, failure.code)
    }
}
