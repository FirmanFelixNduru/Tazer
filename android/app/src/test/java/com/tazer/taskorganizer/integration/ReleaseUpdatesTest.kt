package com.tazer.taskorganizer.integration

import org.junit.Assert.*
import org.junit.Test

class ReleaseUpdatesTest {
    @Test
    fun comparesSemVerWithoutNumericOverflow() {
        fun v(text: String) = requireNotNull(SemVer.parse(text))
        assertTrue(v("1.10.0") > v("1.9.20"))
        assertTrue(v("1.0.0-rc.11") > v("1.0.0-rc.2"))
        assertTrue(v("1.0.0-11") < v("1.0.0-alpha"))
        assertTrue(v("1.0.0") > v("1.0.0-rc.99"))
        assertTrue(v("999999999999999999999.0.0") > v("1.0.0"))
        assertEquals(0, v("1.5.0+build").compareTo(v("1.5.0+other")))
        listOf("1.5", "1.05.0", "1.5.0-01", "1.5.0+", " 1.5.0").forEach { assertNull(SemVer.parse(it)) }
    }

    @Test
    fun parsesPublicReleaseAndIgnoresForeignOrUnpublishedAssets() {
        val hash = "a".repeat(64)
        val apex = "https://github.com/FirmanFelixNduru/Tazer/releases/download/v1.5.0"
        val apk = "$apex/Tazer-v1.5.0-release.apk"
        val release = parseJson("""
            {"tag_name":"v1.5.0","body":" Update notes ","assets":[
                {"name":"wrong.apk","state":"uploaded","browser_download_url":"https://github.example.org/wrong.apk"},
                {"name":"uploading.apk","state":"starter","browser_download_url":"$apex/uploading.apk"},
                {"name":"Tazer-v1.5.0-release.apk","state":"uploaded","browser_download_url":"$apk","size":1048576,"digest":"sha256:$hash"}
            ]}
        """.trimIndent())
        assertEquals(ReleaseUpdate("1.5.0", "Update notes", apk, 1048576, hash), parseReleaseUpdate(release, "1.4.0"))
        assertNull(parseReleaseUpdate(release, "1.5.0"))
        assertNull(parseReleaseUpdate(release, "1.5.1"))
        assertNull(parseReleaseUpdate(release.put("prerelease", true), "1.4.0"))
        release.put("prerelease", false).getJSONArray("assets").getJSONObject(2)
            .put("browser_download_url", "$apk?other=1")
        assertNull(parseReleaseUpdate(release, "1.4.0")?.downloadUrl)
        assertThrows(IntegrationException::class.java) { parseReleaseUpdate(release.put("tag_name", "latest"), "1.4.0") }
    }
}
