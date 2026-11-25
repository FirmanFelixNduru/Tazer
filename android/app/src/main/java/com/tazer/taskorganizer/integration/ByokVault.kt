package com.tazer.taskorganizer.integration

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.ProviderException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class ByokCredential(val settings: ByokSettings, val apiKey: String)

internal class ByokVault(context: Context) {
    private val preferences = context.getSharedPreferences("tazer_integration_byok", Context.MODE_PRIVATE)

    fun save(uid: AccountUid, settings: ByokSettings, apiKey: String) = storage {
        val alias = alias(uid)
        val credential = json(
            "provider" to settings.provider.name,
            "model" to settings.model,
            "apiKey" to apiKey,
        )
        val encrypted = CredentialCipher(alias).encrypt(uid.value, credential)
        val encoded = Base64.getEncoder().encodeToString(encrypted)
        if (!preferences.edit().putString(alias, encoded).commit()) {
            fail(IntegrationError.BYOK_STORAGE_UNAVAILABLE)
        }
    }

    fun read(uid: AccountUid): ByokCredential? = storage {
        val alias = alias(uid)
        val encoded = preferences.getString(alias, null) ?: return@storage null
        val credential = CredentialCipher(alias).decrypt(uid.value, Base64.getDecoder().decode(encoded))
        val provider = ByokProvider.valueOf(credential.string("provider"))
        ByokCredential(ByokSettings(provider, credential.string("model")), credential.string("apiKey"))
    }

    fun delete(uid: AccountUid) = storage {
        val alias = alias(uid)
        CredentialCipher(alias).delete()
        if (!preferences.edit().remove(alias).commit()) {
            fail(IntegrationError.BYOK_STORAGE_UNAVAILABLE)
        }
    }

    private fun alias(uid: AccountUid): String = "tazer.byok." + Base64.getUrlEncoder().withoutPadding()
        .encodeToString(MessageDigest.getInstance("SHA-256").digest(uid.value.toByteArray()))

    private inline fun <T> storage(block: () -> T): T =
        credentialStorage(IntegrationError.BYOK_STORAGE_UNAVAILABLE, block)
}

internal class CredentialCipher(private val alias: String) {
    fun encrypt(aad: String, value: org.json.JSONObject): ByteArray {
        val key = keystore().getKey(alias, null) as? SecretKey
            ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
                init(
                    KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build(),
                )
                generateKey()
            }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(aad.toByteArray())
        val plaintext = value.toString().toByteArray()
        return try {
            cipher.iv + cipher.doFinal(plaintext)
        } finally {
            plaintext.fill(0)
        }
    }

    fun decrypt(aad: String, data: ByteArray): org.json.JSONObject {
        val key = keystore().getKey(alias, null) as? SecretKey ?: throw GeneralSecurityException()
        if (data.size <= IV_SIZE + 16) throw GeneralSecurityException()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, data, 0, IV_SIZE))
        cipher.updateAAD(aad.toByteArray())
        val plaintext = cipher.doFinal(data, IV_SIZE, data.size - IV_SIZE)
        return try {
            parseJson(plaintext.toString(Charsets.UTF_8))
        } finally {
            plaintext.fill(0)
        }
    }

    fun delete() = keystore().deleteEntry(alias)

    private fun keystore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private companion object {
        const val IV_SIZE = 12
    }
}

internal inline fun <T> credentialStorage(error: IntegrationError, block: () -> T): T = try {
    block()
} catch (_: GeneralSecurityException) {
    fail(error)
} catch (_: IOException) {
    fail(error)
} catch (_: ProviderException) {
    fail(error)
} catch (_: IllegalArgumentException) {
    fail(error)
} catch (_: IntegrationException) {
    fail(error)
}
