package com.tazer.taskorganizer.integration

import java.net.URI
import org.json.JSONObject

internal const val RELEASES_API = "https://api.github.com/repos/FirmanFelixNduru/Tazer/releases/latest"
private const val DOWNLOAD_PREFIX = "https://github.com/FirmanFelixNduru/Tazer/releases/download/"

internal data class ReleaseUpdate(
    val version: String,
    val notes: String,
    val downloadUrl: String?,
    val sizeBytes: Long?,
    val sha256: String?,
)

internal fun parseReleaseUpdate(release: JSONObject, installed: String): ReleaseUpdate? {
    val tag = release.string("tag_name")
    val version = tag.removePrefix("v")
    val latest = SemVer.parse(version) ?: fail(IntegrationError.INVALID_RESPONSE)
    val current = SemVer.parse(installed) ?: fail(IntegrationError.INVALID_RESPONSE)
    if (release.optBoolean("draft") || release.optBoolean("prerelease") || latest <= current) return null

    val assets = release.optJSONArray("assets")
    val asset = (0 until (assets?.length() ?: 0)).asSequence().mapNotNull { assets?.optJSONObject(it) }
        .firstOrNull { item ->
            val name = item.opt("name") as? String ?: return@firstOrNull false
            val url = item.opt("browser_download_url") as? String ?: return@firstOrNull false
            val uri = runCatching { URI(url) }.getOrNull() ?: return@firstOrNull false
            validApkName.matches(name) && item.optString("state") == "uploaded" &&
                uri.toASCIIString() == "$DOWNLOAD_PREFIX$tag/$name"
        }
    val digest = (asset?.opt("digest") as? String)?.takeIf(sha256Digest::matches)
    return ReleaseUpdate(
        version,
        (release.opt("body") as? String).orEmpty().trim().take(900),
        asset?.optString("browser_download_url"),
        (asset?.opt("size") as? Number)?.toLong()?.takeIf { it > 0 },
        digest?.removePrefix("sha256:"),
    )
}

private val validApkName = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,119}\\.apk")
private val sha256Digest = Regex("sha256:[a-fA-F0-9]{64}")

internal class SemVer private constructor(
    private val core: List<String>,
    private val preRelease: List<String>,
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        core.indices.forEach { index -> compareNumbers(core[index], other.core[index]).let { if (it != 0) return it } }
        if (preRelease.isEmpty() || other.preRelease.isEmpty()) {
            return preRelease.isEmpty().compareTo(other.preRelease.isEmpty())
        }
        for ((first, second) in preRelease.zip(other.preRelease)) {
            val firstNumeric = first.all(Char::isDigit)
            val secondNumeric = second.all(Char::isDigit)
            val order = when {
                firstNumeric && secondNumeric -> compareNumbers(first, second)
                firstNumeric != secondNumeric -> secondNumeric.compareTo(firstNumeric)
                else -> first.compareTo(second)
            }
            if (order != 0) return order
        }
        return preRelease.size.compareTo(other.preRelease.size)
    }

    companion object {
        private val pattern = Regex(
            "(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)" +
                "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?",
        )

        fun parse(value: String): SemVer? {
            if (value.length > 128) return null
            val match = pattern.matchEntire(value) ?: return null
            val pre = match.groups[4]?.value?.split('.').orEmpty()
            if (pre.any { it.length > 1 && it[0] == '0' && it.all(Char::isDigit) }) return null
            return SemVer(match.groupValues.subList(1, 4), pre)
        }

        private fun compareNumbers(a: String, b: String): Int =
            if (a.length == b.length) a.compareTo(b) else a.length.compareTo(b.length)
    }
}
