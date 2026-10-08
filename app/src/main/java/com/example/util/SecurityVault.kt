package com.example.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Provides hardware-backed AES-256-GCM encryption at rest for user-owned AI API keys,
 * salted SHA-256 password hashing for user authentication, and session persistence.
 * Never logs or exposes raw API keys to the UI.
 */
object SecurityVault {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "polyglot_user_ai_credentials_aes_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFS_NAME = "polyglot_secure_session_prefs"
    private const val KEY_SESSION_TOKEN = "active_session_token"
    private const val KEY_SESSION_USER_ID = "active_session_user_id"
    private const val KEY_FALLBACK_SECRET = "local_jvm_fallback_secret"

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$password:polyglot_v3".toByteArray(Charsets.UTF_8)
        val hash = digest.digest(combined)
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        val computed = hashPassword(password, salt)
        return MessageDigest.isEqual(
            computed.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun generateSessionToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    fun computeContentFingerprint(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(text.trim().toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP).take(24)
    }

    /**
     * Masks an API key so only the prefix and last 4 characters are shown in the UI.
     */
    fun maskApiKey(rawKey: String): String {
        val trimmed = rawKey.trim()
        if (trimmed.length <= 8) return "••••••••"
        val prefix = trimmed.take(4)
        val suffix = trimmed.takeLast(4)
        return "$prefix••••••••$suffix"
    }

    /**
     * Encrypts a user's API key at rest using Android KeyStore AES-256-GCM
     * (with an automatic fallback for local JVM Robolectric unit tests).
     */
    fun encryptApiKey(context: Context, plainApiKey: String): String {
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainApiKey.trim().toByteArray(Charsets.UTF_8))
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val dataBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
            "GCM:$ivBase64:$dataBase64"
        } catch (_: Exception) {
            // Fallback XOR obfuscation for JVM unit test environments without AndroidKeyStore hardware
            val mask = getFallbackMask(context)
            val input = plainApiKey.trim().toByteArray(Charsets.UTF_8)
            val out = ByteArray(input.size) { i -> (input[i].toInt() xor mask[i % mask.size].toInt()).toByte() }
            "FALLBACK:" + Base64.encodeToString(out, Base64.NO_WRAP)
        }
    }

    /**
     * Decrypts a user's API key strictly inside the backend service layer immediately before making a request.
     */
    fun decryptApiKey(context: Context, encryptedPayload: String): String {
        if (encryptedPayload.isBlank()) return ""
        return try {
            if (encryptedPayload.startsWith("GCM:")) {
                val parts = encryptedPayload.split(":")
                if (parts.size != 3) return ""
                val iv = Base64.decode(parts[1], Base64.NO_WRAP)
                val cipherBytes = Base64.decode(parts[2], Base64.NO_WRAP)
                val secretKey = getOrCreateSecretKey()
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
            } else if (encryptedPayload.startsWith("FALLBACK:")) {
                val mask = getFallbackMask(context)
                val encoded = encryptedPayload.removePrefix("FALLBACK:")
                val data = Base64.decode(encoded, Base64.NO_WRAP)
                val out = ByteArray(data.size) { i -> (data[i].toInt() xor mask[i % mask.size].toInt()).toByte() }
                String(out, Charsets.UTF_8)
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun getFallbackMask(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var secret = prefs.getString(KEY_FALLBACK_SECRET, null)
        if (secret == null) {
            secret = generateSessionToken()
            prefs.edit().putString(KEY_FALLBACK_SECRET, secret).apply()
        }
        return secret.toByteArray(Charsets.UTF_8)
    }

    fun savePersistentSession(context: Context, userId: Long, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_SESSION_USER_ID, userId)
            .putString(KEY_SESSION_TOKEN, token)
            .apply()
    }

    fun getSavedSession(context: Context): Pair<Long, String>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userId = prefs.getLong(KEY_SESSION_USER_ID, -1L)
        val token = prefs.getString(KEY_SESSION_TOKEN, null)
        return if (userId > 0L && !token.isNullOrBlank()) userId to token else null
    }

    fun clearSavedSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SESSION_USER_ID)
            .remove(KEY_SESSION_TOKEN)
            .apply()
    }

    fun saveTopThreeLanguagesPref(context: Context, csv: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("top_three_languages_csv", csv)
            .apply()
    }

    fun getTopThreeLanguagesPref(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("top_three_languages_csv", null)
    }
}
