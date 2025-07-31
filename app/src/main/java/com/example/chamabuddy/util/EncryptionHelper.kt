package com.example.chamabuddy.util

import android.content.Context
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import javax.crypto.spec.GCMParameterSpec

class EncryptionHelper(context: Context) {

    companion object {
        private const val KEY_ALIAS = "chama_buddy_key"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SEPARATOR = ":"
    }

    private val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
        load(null)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE
            )

            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )

            return keyGenerator.generateKey()
        }

        val secretKeyEntry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return secretKeyEntry.secretKey
    }
    fun encrypt(input: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(input.toByteArray(Charsets.UTF_8))
        val encryptedBase64 = Base64.encodeToString(encrypted, Base64.DEFAULT).trim()
        val ivBase64 = Base64.encodeToString(iv, Base64.DEFAULT).trim()
        return "$ivBase64$IV_SEPARATOR$encryptedBase64"
    }

    fun decrypt(encryptedInput: String): String? {
        return try {
            val parts = encryptedInput.split(IV_SEPARATOR)
            if (parts.size != 2) return null // Graceful failure

            val iv = Base64.decode(parts[0], Base64.DEFAULT)
            val encrypted = Base64.decode(parts[1], Base64.DEFAULT)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            null // Return null on any decryption failure
        }
    }
}
