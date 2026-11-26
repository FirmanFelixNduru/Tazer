package com.tazer.taskorganizer.integration

import com.tazer.taskorganizer.BuildConfig
import android.net.Uri
import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

internal enum class AuthFlow(val lifetimeSeconds: Long) {
    GOOGLE(900), SIGN_UP(86_400), RECOVERY(86_400),
}

internal const val AUTH_REDIRECT = BuildConfig.LINK_SCHEME + "://auth/callback"
private val random = SecureRandom()
private val base64Url = Base64.getUrlEncoder().withoutPadding()

internal fun isAuthCallback(uri: Uri?): Boolean =
    uri?.scheme == BuildConfig.LINK_SCHEME && uri.encodedAuthority == "auth" && uri.encodedPath == "/callback"

internal class PendingAuth(
    val flow: AuthFlow,
    val state: String,
    val verifier: String,
    val expiresEpochSeconds: Long,
) {
    val redirect: String get() = "$AUTH_REDIRECT?state=$state"
    val challenge: String get() = base64Url.encodeToString(
        MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)),
    )

    fun callbackCode(uri: Uri, now: Instant): String {
        if (!isAuthCallback(uri)) fail(IntegrationError.INVALID_CALLBACK)
        val fragment = uri.encodedFragment?.let { Uri.parse("$AUTH_REDIRECT?$it") }
        return validateCallback(
            now, uri.getQueryParameters("state"), uri.getQueryParameters("code"),
            uri.getQueryParameters("error") + fragment?.getQueryParameters("error").orEmpty(),
            uri.getQueryParameters("error_code") + fragment?.getQueryParameters("error_code").orEmpty(),
            fragment != null,
        )
    }

    internal fun validateCallback(
        now: Instant, states: List<String>, codes: List<String>, errors: List<String>,
        errorCodes: List<String>, hasFragment: Boolean,
    ): String {
        if (now.epochSecond >= expiresEpochSeconds || states.singleOrNull() != state) {
            fail(IntegrationError.INVALID_CALLBACK)
        }
        if (flow == AuthFlow.GOOGLE && codes.isEmpty() && errors.size <= 1 && errorCodes.size <= 1) {
            when {
                "access_denied" in errors || "access_denied" in errorCodes -> fail(IntegrationError.GOOGLE_CANCELLED)
                "provider_disabled" in errorCodes -> fail(IntegrationError.GOOGLE_UNAVAILABLE)
            }
        }
        if (hasFragment || errors.isNotEmpty() || errorCodes.isNotEmpty()) fail(IntegrationError.INVALID_CALLBACK)
        return codes.singleOrNull()
            ?.takeIf { it.isNotBlank() && it.length <= 512 }
            ?: fail(IntegrationError.INVALID_CALLBACK)
    }

    companion object {
        fun create(flow: AuthFlow, now: Instant = Instant.now()): PendingAuth =
            PendingAuth(flow, randomValue(), randomValue(), now.epochSecond + flow.lifetimeSeconds)

        private fun randomValue(): String =
            base64Url.encodeToString(ByteArray(32).also(random::nextBytes))
    }
}

internal class AuthPkce(noBackupFilesDir: File) {
    private val file = AtomicFile(File(noBackupFilesDir, "tazer_pending_auth"))

    fun start(flow: AuthFlow): PendingAuth {
        val pending = PendingAuth.create(flow)
        val bytes = listOf(
            pending.flow.name,
            pending.state,
            pending.verifier,
            pending.expiresEpochSeconds.toString(),
        ).joinToString("\n").toByteArray()
        try {
            val output = file.startWrite()
            try {
                output.write(bytes)
                file.finishWrite(output)
            } catch (error: IOException) {
                file.failWrite(output)
                throw error
            }
        } catch (_: IOException) {
            fail(IntegrationError.VERIFICATION_UNAVAILABLE)
        }
        return pending
    }

    fun consume(uri: Uri): Pair<PendingAuth, String> {
        val pending = read() ?: fail(IntegrationError.INVALID_CALLBACK)
        val code = try {
            pending.callbackCode(uri, Instant.now())
        } catch (error: IntegrationException) {
            if (error.code == IntegrationError.GOOGLE_CANCELLED || error.code == IntegrationError.GOOGLE_UNAVAILABLE) clear()
            throw error
        }
        clear()
        return pending to code
    }

    private fun read(): PendingAuth? = try {
        val fields = file.readFully().toString(Charsets.UTF_8).split('\n')
        if (fields.size != 4) null else PendingAuth(
            AuthFlow.valueOf(fields[0]), fields[1], fields[2], fields[3].toLong(),
        )
    } catch (_: IOException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    fun clear() = file.delete()

    fun cancelGoogle() {
        if (read()?.flow == AuthFlow.GOOGLE) clear()
    }
}
