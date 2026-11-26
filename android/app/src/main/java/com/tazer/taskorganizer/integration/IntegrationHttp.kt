package com.tazer.taskorganizer.integration

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URI
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

internal class HttpResponse(val status: Int, private val body: String, val retryAfter: Long?) {
    fun json(): JSONObject = parseJson(body)
    fun value(): Any = parseJsonValue(body)
}

internal fun parseJson(text: String): JSONObject =
    parseJsonValue(text) as? JSONObject ?: fail(IntegrationError.INVALID_RESPONSE)

private fun parseJsonValue(text: String): Any {
    try {
        val parser = JSONTokener(text)
        val value = parser.nextValue() ?: fail(IntegrationError.INVALID_RESPONSE)
        if (parser.nextClean() != '\u0000') fail(IntegrationError.INVALID_RESPONSE)
        return value
    } catch (_: JSONException) {
        fail(IntegrationError.INVALID_RESPONSE)
    }
}

internal fun call(
    url: String,
    method: String = "GET",
    headers: Map<String, String> = emptyMap(),
    body: JSONObject? = null,
    maxResponseBytes: Int = 256 * 1024,
): HttpResponse {
    val uri = URI(url)
    val connection = (if (uri.scheme == "http") {
        uri.toURL().openConnection(Proxy.NO_PROXY)
    } else {
        uri.toURL().openConnection()
    }) as HttpURLConnection

    try {
        connection.requestMethod = method
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Cache-Control", "no-store")
        headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        if (body != null) {
            val bytes = body.toString().toByteArray(Charsets.UTF_8)
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }
        }

        val status = connection.responseCode
        val retryAfter = connection.getHeaderField("Retry-After")?.toLongOrNull()?.takeIf { it > 0 }
        // A 401 only needs its status. Preserve it even when a gateway sends no JSON.
        if (status == 401) return HttpResponse(status, "", retryAfter)
        val stream = if (status >= 400) connection.errorStream else connection.inputStream
        val responseBody = stream?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (output.size() + count > maxResponseBytes) fail(IntegrationError.INVALID_RESPONSE)
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        }.orEmpty()
        return HttpResponse(status, responseBody, retryAfter)
    } catch (_: IOException) {
        fail(IntegrationError.NETWORK_UNAVAILABLE)
    } finally {
        connection.disconnect()
    }
}

internal fun HttpResponse.backendJson(): JSONObject {
    if (status == 200) return json()
    if (status == 401) fail(IntegrationError.INVALID_AUTH)
    val envelope = json().optJSONObject("error") ?: fail(IntegrationError.INVALID_RESPONSE)
    val error = when (status to envelope.string("code")) {
        400 to "VALIDATION_ERROR" -> IntegrationError.VALIDATION_ERROR
        413 to "PAYLOAD_TOO_LARGE" -> IntegrationError.PAYLOAD_TOO_LARGE
        403 to "EMAIL_UNVERIFIED" -> IntegrationError.EMAIL_UNVERIFIED
        403 to "PRO_REQUIRED" -> IntegrationError.PRO_REQUIRED
        429 to "QUOTA_EXHAUSTED" -> IntegrationError.QUOTA_EXHAUSTED
        502 to "AI_UNAVAILABLE" -> IntegrationError.AI_UNAVAILABLE
        503 to "VERIFICATION_UNAVAILABLE", 403 to "REQUEST_FORBIDDEN" ->
            IntegrationError.VERIFICATION_UNAVAILABLE
        else -> IntegrationError.INVALID_RESPONSE
    }
    throw IntegrationException(error, retryAfter)
}
