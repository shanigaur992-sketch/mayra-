package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.BuildConfig
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureKeyManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureKeyStoreKey()
    }

    private fun ensureKeyStoreKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            // KeyStore fallback handled gracefully
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        } catch (_: Exception) {
            null
        }
    }

    private fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val secretKey = getSecretKey() ?: return Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    private fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        val secretKey = getSecretKey() ?: return try {
            String(Base64.decode(encryptedBase64, Base64.NO_WRAP), Charsets.UTF_8)
        } catch (_: Exception) { "" }

        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < 12) {
                return String(combined, Charsets.UTF_8)
            }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(128, combined, 0, 12)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val decryptedBytes = cipher.doFinal(combined, 12, combined.size - 12)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            try {
                String(Base64.decode(encryptedBase64, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (_: Exception) {
                ""
            }
        }
    }

    fun getGeminiKey(): String {
        val encrypted = prefs.getString(KEY_GEMINI_KEY, null)
        if (!encrypted.isNullOrEmpty()) {
            val decrypted = decrypt(encrypted)
            if (decrypted.isNotBlank()) return decrypted
        }
        // Fallback to BuildConfig if provided at build time
        val buildKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    fun saveGeminiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_GEMINI_KEY, encrypt(trimmed)).apply()
    }

    fun removeGeminiKey() {
        prefs.edit().remove(KEY_GEMINI_KEY).apply()
    }

    fun getDeepSeekKey(): String {
        val encrypted = prefs.getString(KEY_DEEPSEEK_KEY, null)
        return if (!encrypted.isNullOrEmpty()) decrypt(encrypted) else ""
    }

    fun saveDeepSeekKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_DEEPSEEK_KEY, encrypt(trimmed)).apply()
    }

    fun removeDeepSeekKey() {
        prefs.edit().remove(KEY_DEEPSEEK_KEY).apply()
    }

    fun isGeminiConfigured(): Boolean = getGeminiKey().isNotBlank()

    fun isDeepSeekConfigured(): Boolean = getDeepSeekKey().isNotBlank()

    fun maskKey(key: String): String {
        if (key.isBlank()) return "Not configured"
        return if (key.length <= 8) {
            "••••••••••••"
        } else {
            val prefix = key.take(3)
            val suffix = key.takeLast(4)
            "$prefix••••••••••••$suffix"
        }
    }

    companion object {
        private const val PREFS_NAME = "myraa_secure_prefs"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "myraa_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_GEMINI_KEY = "encrypted_gemini_key"
        private const val KEY_DEEPSEEK_KEY = "encrypted_deepseek_key"
    }
}
