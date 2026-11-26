package com.tazer.taskorganizer.integration

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tazer.taskorganizer.BuildConfig
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AndroidIntegration(context: Context) {
    private val app = context.applicationContext
    private val pkce = AuthPkce(app.noBackupFilesDir)
    private val vault = ByokVault(app)
    private val sessionVault = SessionVault(
        app.noBackupFilesDir,
        "${BuildConfig.SUPABASE_URL}\n${BuildConfig.BACKEND_URL}",
    )
    private val lock = Mutex()
    private var active: Session? = null
    private var recovery: Session? = null

    val enabled: Boolean = BuildConfig.SUPABASE_URL.isNotEmpty() &&
        BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotEmpty() && BuildConfig.BACKEND_URL.isNotEmpty()
    val mockEnabled: Boolean = BuildConfig.DEBUG && BuildConfig.MOCK_ENABLED

    suspend fun restoreAccount(): Account? = serialized {
        if (!enabled || active != null || recovery != null) return@serialized null
        try {
            val saved = sessionVault.read() ?: return@serialized null
            val uid = saved.optJSONObject("account")?.uid(mock = false)
                ?: fail(IntegrationError.INVALID_RESPONSE)
            active = Session(saved.tokens()).apply { this.uid = uid }
            val nextMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).plusMonths(1)
                .atStartOfDay().toInstant(ZoneOffset.UTC)
            Account(uid, Subscription(SubscriptionTier.FREE, 0, 0, nextMonth))
        } catch (_: IntegrationException) {
            clearSavedSession()
            null
        }
    }

    suspend fun signIn(email: String, password: String): Account = serialized {
        requireConfig()
        clearAuth()
        val response = auth(
            "/token?grant_type=password",
            json("email" to email.trim(), "password" to password),
        )
        activate(Session(response.tokens()))
    }

    suspend fun signUp(email: String, password: String): Unit = serialized {
        startEmailFlow(
            AuthFlow.SIGN_UP,
            "/signup",
            json("email" to email.trim(), "password" to password),
        )
    }

    suspend fun resetPassword(email: String): Unit = serialized {
        startEmailFlow(AuthFlow.RECOVERY, "/recover", json("email" to email.trim()))
    }

    suspend fun googleSignInIntent(): Intent = serialized {
        requireConfig()
        val providers = auth("/settings", method = "GET").optJSONObject("external")
            ?: fail(IntegrationError.INVALID_RESPONSE)
        if (!providers.optBoolean("google")) fail(IntegrationError.GOOGLE_UNAVAILABLE)
        val pending = pkce.start(AuthFlow.GOOGLE)
        val url = Uri.parse("${BuildConfig.SUPABASE_URL}/auth/v1/authorize").buildUpon()
            .appendQueryParameter("provider", "google")
            .appendQueryParameter("redirect_to", pending.redirect)
            .appendQueryParameter("code_challenge", pending.challenge)
            .appendQueryParameter("code_challenge_method", "s256")
            .build()
        Intent(Intent.ACTION_VIEW, url).addCategory(Intent.CATEGORY_BROWSABLE)
    }

    suspend fun cancelGoogleSignIn() = serialized { pkce.cancelGoogle() }

    fun isAuthCallback(uri: Uri?): Boolean = com.tazer.taskorganizer.integration.isAuthCallback(uri)

    suspend fun handleAuthCallback(uri: Uri): AuthCallback = serialized {
        requireConfig()
        val (pending, code) = pkce.consume(uri)
        clearAuth()
        val response = auth(
            "/token?grant_type=pkce",
            json("auth_code" to code, "code_verifier" to pending.verifier),
        )
        val session = Session(response.tokens())
        if (pending.flow == AuthFlow.RECOVERY) {
            verify(session)
            recovery = session
            AuthCallback.PasswordRecovery
        } else {
            AuthCallback.SignedIn(activate(session))
        }
    }

    suspend fun updatePassword(password: String): Account = serialized {
        val session = recovery ?: fail(IntegrationError.INVALID_CALLBACK)
        val payload = json("password" to password)
        try {
            auth("/user", payload, "PUT", session.tokens.access)
        } catch (error: IntegrationException) {
            if (error.code != IntegrationError.INVALID_AUTH) throw error
            refresh(session)
            auth("/user", payload, "PUT", session.tokens.access)
        }
        val account = activate(session)
        recovery = null
        account
    }

    suspend fun signInMock(tier: SubscriptionTier): Account = serialized {
        if (!mockEnabled) fail(IntegrationError.NOT_CONFIGURED)
        clearAuth()
        val mockUid = when (tier) {
            SubscriptionTier.FREE -> "demo-free"
            SubscriptionTier.PRO -> "demo-pro"
            SubscriptionTier.BYOK -> "demo-byok"
        }
        activate(Session(Tokens(mockUid, null), mock = true))
    }

    suspend fun subscription(): Subscription = serialized {
        subscription(session())
    }

    suspend fun analyzeCloud(input: TaskAnalysisInput): TaskAnalysisResult = serialized {
        val payload = input.toJson()
        val session = session()
        if (subscription(session).tier != SubscriptionTier.PRO) fail(IntegrationError.PRO_REQUIRED)
        backend(session, "/analyze-task", payload).analysis()
    }

    internal suspend fun syncRpc(uid: String, name: String, body: JSONObject): Any = serialized {
        require(name == "tazer_sync_push" || name == "tazer_sync_pull")
        val session = session()
        if (session.mock || session.verifiedUid().value != uid) fail(IntegrationError.INVALID_AUTH)
        fun request() = call(
            "${BuildConfig.SUPABASE_URL}/rest/v1/rpc/$name", "POST",
            mapOf("apikey" to BuildConfig.SUPABASE_PUBLISHABLE_KEY, "Authorization" to "Bearer ${session.tokens.access}"),
            body, maxResponseBytes = 2 * 1024 * 1024,
        )
        try {
            var response = request()
            if (response.status == 401) {
                refresh(session)
                response = request()
            }
            when (response.status) {
                200 -> response.value()
                401 -> fail(IntegrationError.INVALID_AUTH)
                404 -> fail(IntegrationError.NOT_CONFIGURED)
                429, in 500..599 -> fail(IntegrationError.VERIFICATION_UNAVAILABLE)
                else -> fail(IntegrationError.INVALID_RESPONSE)
            }
        } catch (failure: IntegrationException) {
            if (failure.code == IntegrationError.INVALID_AUTH || failure.code == IntegrationError.EMAIL_UNVERIFIED) {
                active = null
                clearSavedSession()
            }
            throw failure
        }
    }

    suspend fun analyzeByok(input: TaskAnalysisInput): TaskAnalysisResult = serialized {
        val payload = input.toJson()
        val uid = requireByok()
        val credential = vault.read(uid) ?: fail(IntegrationError.BYOK_KEY_MISSING)
        analyzeWithProvider(payload, credential)
    }

    suspend fun saveByok(provider: ByokProvider, model: String, apiKey: String): Unit = serialized {
        val settings = ByokSettings(provider, model.trim())
        val key = apiKey.trim()
        validateByok(settings, key)
        vault.save(requireByok(), settings, key)
    }

    suspend fun byokSettings(): ByokSettings? = serialized {
        vault.read(session().verifiedUid())?.settings
    }

    suspend fun deleteByok(): Unit = serialized {
        vault.delete(session().verifiedUid())
    }

    suspend fun signOut(): Unit = serialized {
        val session = active ?: recovery
        clearAuth()
        if (session != null && !session.mock) {
            try {
                auth("/logout?scope=local", method = "POST", access = session.tokens.access, status = 204)
            } catch (_: IntegrationException) {
                // Local sign-out must also work offline and for expired sessions.
            }
        }
    }

    private fun startEmailFlow(flow: AuthFlow, path: String, payload: JSONObject) {
        requireConfig()
        val pending = pkce.start(flow)
        payload.put("code_challenge", pending.challenge)
        payload.put("code_challenge_method", "s256")
        try {
            val reply = auth("$path?redirect_to=${Uri.encode(pending.redirect)}", payload)
            if (flow == AuthFlow.SIGN_UP && reply.opt("access_token") is String) {
                fail(IntegrationError.NOT_CONFIGURED)
            }
        } catch (error: IntegrationException) {
            pkce.clear()
            throw error
        }
    }

    private fun verify(session: Session): AccountUid {
        val uid = backend(session, "/auth/session").uid(session.mock)
        if (session.uid != null && session.uid != uid) fail(IntegrationError.INVALID_AUTH)
        session.uid = uid
        return uid
    }

    private fun activate(session: Session): Account {
        val uid = verify(session)
        val account = Account(uid, subscription(session))
        if (!session.mock) persist(session)
        active = session
        return account
    }

    private fun subscription(session: Session): Subscription =
        backend(session, "/subscription").subscription()

    private fun requireByok(): AccountUid {
        val session = session()
        if (subscription(session).tier != SubscriptionTier.BYOK) fail(IntegrationError.BYOK_REQUIRED)
        return session.verifiedUid()
    }

    private fun session(): Session = active ?: fail(IntegrationError.INVALID_AUTH)

    private fun clearAuth() {
        active = null
        recovery = null
        pkce.clear()
        sessionVault.clear()
    }

    private fun clearSavedSession() {
        try {
            sessionVault.clear()
        } catch (_: IntegrationException) {
            // The storage error must not reactivate a rejected in-memory session.
        }
    }

    private fun persist(session: Session) {
        sessionVault.save(
            json(
                "account" to json("uid" to session.verifiedUid().value),
                "access_token" to session.tokens.access,
                "refresh_token" to (session.tokens.refresh ?: fail(IntegrationError.INVALID_AUTH)),
            ),
        )
    }

    private fun requireConfig() {
        if (!enabled) fail(IntegrationError.NOT_CONFIGURED)
    }

    private fun backend(session: Session, path: String, payload: JSONObject? = null): JSONObject {
        val origin = if (session.mock) BuildConfig.MOCK_URL else BuildConfig.BACKEND_URL
        fun request() = call(
            origin + path,
            if (payload == null) "GET" else "POST",
            buildMap {
                put("Authorization", "Bearer ${session.tokens.access}")
                if (path == "/analyze-task") put("X-Tazer-Analysis-Subtasks", "1")
            },
            payload,
        )
        try {
            val first = request()
            if (first.status != 401 || session.mock) return first.backendJson()
            refresh(session)
            return request().backendJson()
        } catch (error: IntegrationException) {
            if (error.code == IntegrationError.INVALID_AUTH || error.code == IntegrationError.EMAIL_UNVERIFIED) {
                if (active === session) {
                    active = null
                    if (!session.mock) clearSavedSession()
                }
                if (recovery === session) recovery = null
            }
            throw error
        }
    }

    private fun refresh(session: Session) {
        val token = session.tokens.refresh ?: fail(IntegrationError.INVALID_AUTH)
        val refreshed = auth("/token?grant_type=refresh_token", json("refresh_token" to token)).tokens()
        val expectedUid = session.uid
        if (expectedUid != null) {
            val actualUid = call(
                "${BuildConfig.BACKEND_URL}/auth/session",
                headers = mapOf("Authorization" to "Bearer ${refreshed.access}"),
            ).backendJson().uid(mock = false)
            if (actualUid != expectedUid) fail(IntegrationError.INVALID_AUTH)
        }
        session.tokens = refreshed
        if (active === session) persist(session)
    }

    private fun auth(
        path: String,
        payload: JSONObject? = null,
        method: String = "POST",
        access: String? = null,
        status: Int = 200,
    ): JSONObject {
        val headers = mutableMapOf("apikey" to BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        if (access != null) headers["Authorization"] = "Bearer $access"
        val response = call("${BuildConfig.SUPABASE_URL}/auth/v1$path", method, headers, payload)
        if (response.status == status) return if (status == 204) JSONObject() else response.json()
        if (response.status in 500..599) fail(IntegrationError.VERIFICATION_UNAVAILABLE)
        val code = runCatching { response.json().opt("error_code") }.getOrNull()
        if (code == "email_not_confirmed") fail(IntegrationError.EMAIL_UNVERIFIED)
        if (
            response.status == 401 ||
            (path.endsWith("grant_type=refresh_token") && response.status in setOf(400, 403, 422))
        ) {
            fail(IntegrationError.INVALID_AUTH)
        }
        fail(IntegrationError.AUTH_REJECTED)
    }

    private suspend fun <T> serialized(block: () -> T): T =
        lock.withLock { withContext(Dispatchers.IO) { block() } }

    private class Tokens(val access: String, val refresh: String?)

    private class Session(var tokens: Tokens, val mock: Boolean = false) {
        var uid: AccountUid? = null
        fun verifiedUid(): AccountUid = uid ?: fail(IntegrationError.INVALID_AUTH)
    }

    private fun JSONObject.tokens(): Tokens {
        val access = string("access_token")
        val refresh = string("refresh_token")
        if (!bearer.matches(access) || refresh.isBlank() || refresh.length > 4096) {
            fail(IntegrationError.INVALID_RESPONSE)
        }
        return Tokens(access, refresh)
    }

    private fun JSONObject.uid(mock: Boolean): AccountUid {
        val value = string("uid")
        val valid = if (mock) value in setOf("demo-free", "demo-pro", "demo-byok") else uuid.matches(value)
        if (length() != 1 || !valid) fail(IntegrationError.INVALID_RESPONSE)
        return AccountUid(value)
    }

    private companion object {
        val bearer = Regex("[A-Za-z0-9._~+/-]{1,16384}={0,2}")
        val uuid = Regex("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}", RegexOption.IGNORE_CASE)
    }
}
