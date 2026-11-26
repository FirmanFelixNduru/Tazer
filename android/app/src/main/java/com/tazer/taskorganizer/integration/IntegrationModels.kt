package com.tazer.taskorganizer.integration

import java.time.Instant
import org.json.JSONObject

@JvmInline
value class AccountUid internal constructor(val value: String)

enum class SubscriptionTier { FREE, PRO, BYOK }

data class Subscription(
    val tier: SubscriptionTier,
    val monthlyLimit: Int,
    val remaining: Int,
    val resetsAt: Instant,
)

data class Account(val uid: AccountUid, val subscription: Subscription) {
    val tier: SubscriptionTier get() = subscription.tier
}

data class TaskAnalysisInput(
    val title: String,
    val course: String,
    val description: String,
    val deadlineAt: Instant,
)

data class TaskAnalysisResult(
    val difficultyScore: Int,
    val estimatedMinutes: Int,
    val subtasks: List<String> = emptyList(),
)

enum class ByokProvider { GEMINI, GROQ }

data class ByokSettings(val provider: ByokProvider, val model: String)

sealed interface AuthCallback {
    data class SignedIn(val account: Account) : AuthCallback
    data object PasswordRecovery : AuthCallback
}

enum class IntegrationError(val message: String) {
    NOT_CONFIGURED("Integrasi ini belum dikonfigurasi."),
    INVALID_AUTH("Sesi tidak valid. Silakan masuk kembali."),
    EMAIL_UNVERIFIED("Verifikasi email diperlukan."),
    AUTH_REJECTED("Autentikasi ditolak. Periksa data dan coba lagi."),
    INVALID_CALLBACK("Tautan autentikasi tidak valid atau kedaluwarsa."),
    GOOGLE_UNAVAILABLE("Login Google belum tersedia. Kamu tetap bisa masuk dengan email."),
    GOOGLE_CANCELLED("Login Google dibatalkan. Coba lagi kapan saja."),
    BROWSER_UNAVAILABLE("Browser tidak tersedia di perangkat ini."),
    VALIDATION_ERROR("Input tidak valid."),
    PAYLOAD_TOO_LARGE("Ukuran input analisis melebihi 32 KiB."),
    PRO_REQUIRED("Analisis melalui server memerlukan PRO."),
    BYOK_REQUIRED("Akses ini memerlukan tier BYOK."),
    BYOK_KEY_MISSING("Isi kunci dan model provider terlebih dahulu."),
    BYOK_STORAGE_UNAVAILABLE("Penyimpanan kunci tidak tersedia."),
    QUOTA_EXHAUSTED("Kuota analisis bulanan habis."),
    AI_UNAVAILABLE("Analisis AI tidak tersedia. Gunakan analisis offline."),
    VERIFICATION_UNAVAILABLE("Verifikasi tidak tersedia."),
    INVALID_RESPONSE("Respons integrasi tidak valid."),
    NETWORK_UNAVAILABLE("Layanan tidak dapat dihubungi. Coba lagi."),
}

class IntegrationException(
    val code: IntegrationError,
    val retryAfterSeconds: Long? = null,
) : Exception(code.message)

internal fun fail(code: IntegrationError): Nothing = throw IntegrationException(code)

internal fun json(vararg fields: Pair<String, Any>): JSONObject =
    JSONObject().apply { fields.forEach { (key, value) -> put(key, value) } }

internal fun JSONObject.string(key: String): String =
    opt(key) as? String ?: fail(IntegrationError.INVALID_RESPONSE)

private fun JSONObject.integer(key: String, range: IntRange): Int {
    val number = (opt(key) as? Number)?.toDouble() ?: fail(IntegrationError.INVALID_RESPONSE)
    val integer = number.toInt()
    if (number != integer.toDouble() || integer !in range) fail(IntegrationError.INVALID_RESPONSE)
    return integer
}

internal fun JSONObject.subscription(): Subscription {
    val tier = SubscriptionTier.entries.find { it.name == opt("tier") }
        ?: fail(IntegrationError.INVALID_RESPONSE)
    val limit = integer("monthlyLimit", 0..Int.MAX_VALUE)
    val remaining = integer("remaining", 0..limit)
    if (tier != SubscriptionTier.PRO && limit != 0) fail(IntegrationError.INVALID_RESPONSE)
    val resetsAt = string("resetsAt")
    if (!isUtcTimestamp(resetsAt)) fail(IntegrationError.INVALID_RESPONSE)
    return Subscription(tier, limit, remaining, Instant.parse(resetsAt))
}

internal fun JSONObject.analysis(): TaskAnalysisResult {
    if (keys().asSequence().any { it != "difficultyScore" && it != "estimatedMinutes" && it != "subtasks" }) {
        fail(IntegrationError.INVALID_RESPONSE)
    }
    return TaskAnalysisResult(
        integer("difficultyScore", 1..5),
        integer("estimatedMinutes", 1..10080),
        analysisSubtasks(),
    )
}

private fun JSONObject.analysisSubtasks(): List<String> {
    val array = optJSONArray("subtasks") ?: return emptyList()
    if (array.length() !in 3..5) return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            val text = (array.opt(index) as? String)?.trim() ?: return emptyList()
            if (text.codePointCount(0, text.length) !in 1..120) return emptyList()
            add(text)
        }
    }
}

private val utcTimestamp = Regex(
    "[0-9]{4}-[0-9]{2}-[0-9]{2}T(?:[01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]" +
        "(?:\\.[0-9]+)?(?:Z|\\+00:00)",
    RegexOption.IGNORE_CASE,
)

private fun isUtcTimestamp(value: String): Boolean =
    utcTimestamp.matches(value) && runCatching { Instant.parse(value) }.isSuccess

internal fun TaskAnalysisInput.toJson(): JSONObject {
    val trimmedTitle = title.trim()
    val trimmedCourse = course.trim()
    val deadline = deadlineAt.toString()
    if (
        trimmedTitle.codePointCount(0, trimmedTitle.length) !in 1..120 ||
        trimmedCourse.codePointCount(0, trimmedCourse.length) !in 1..120 ||
        description.codePointCount(0, description.length) > 4000 ||
        !isUtcTimestamp(deadline)
    ) {
        fail(IntegrationError.VALIDATION_ERROR)
    }
    val body = json(
        "title" to trimmedTitle,
        "course" to trimmedCourse,
        "description" to description,
        "deadlineAt" to deadline,
    )
    if (body.toString().toByteArray(Charsets.UTF_8).size > 32 * 1024) {
        fail(IntegrationError.PAYLOAD_TOO_LARGE)
    }
    return body
}
