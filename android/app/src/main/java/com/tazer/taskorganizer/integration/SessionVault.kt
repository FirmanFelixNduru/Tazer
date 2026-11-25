package com.tazer.taskorganizer.integration

import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import org.json.JSONObject

internal class SessionVault(
    noBackupFilesDir: File,
    private val originBinding: String,
    alias: String = "tazer.verified_session",
) {
    private val file = AtomicFile(File(noBackupFilesDir, "tazer_verified_session"))
    private val cipher = CredentialCipher(alias)

    fun read(): JSONObject? = storage {
        val data = try {
            file.readFully()
        } catch (_: FileNotFoundException) {
            return@storage null
        }
        cipher.decrypt(originBinding, data)
    }

    fun save(value: JSONObject): Unit = storage {
        val data = cipher.encrypt(originBinding, value)
        val output = file.startWrite()
        try {
            output.write(data)
            file.finishWrite(output)
        } catch (error: IOException) {
            file.failWrite(output)
            throw error
        }
    }

    fun clear(): Unit = storage {
        // Destroy both the encrypted copy and the key, including if a filesystem entry survives.
        file.delete()
        cipher.delete()
        if (file.baseFile.exists()) throw IOException()
    }

    private inline fun <T> storage(block: () -> T): T =
        credentialStorage(IntegrationError.VERIFICATION_UNAVAILABLE, block)
}
