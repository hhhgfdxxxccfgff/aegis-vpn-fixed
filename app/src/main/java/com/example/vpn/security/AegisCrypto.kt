package com.example.vpn.security

import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class AegisConfig(
    val version: Int = 1,
    val name: String,
    val host: String,
    val port: Int = 500,
    val user: String = "",
    val pass: String = "",
    val p12Base64: String = "",
    val p12Password: String = "",
    val dns: String = "1.1.1.1",
    val subUrl: String = ""
)

object AegisCrypto {

    const val DEFAULT_MASTER_KEY = "AegisShield_QuantumSec_Key2026_99x"

    /**
     * Derives a 256-bit AES key from the master secret string using SHA-256.
     */
    private fun deriveKey(secret: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(secret.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts AegisConfig into proprietary aegis://v1_<base64> URI.
     */
    fun encryptAegisUri(config: AegisConfig, secret: String = DEFAULT_MASTER_KEY): String {
        val json = JSONObject().apply {
            put("v", config.version)
            put("name", config.name)
            put("host", config.host)
            put("port", config.port)
            put("user", config.user)
            put("pass", config.pass)
            put("p12", config.p12Base64)
            put("p12_pass", config.p12Password)
            put("dns", config.dns)
            if (config.subUrl.isNotBlank()) {
                put("sub_url", config.subUrl)
            }
        }
        val jsonBytes = json.toString().toByteArray(Charsets.UTF_8)

        val secretKey = deriveKey(secret)
        val iv = ByteArray(12)
        java.security.SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val cipherWithTag = cipher.doFinal(jsonBytes)
        val combined = ByteArray(iv.size + cipherWithTag.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherWithTag, 0, combined, iv.size, cipherWithTag.size)

        val b64 = Base64.encodeToString(combined, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        return "aegis://v1_$b64"
    }

    /**
     * Decrypts proprietary aegis://v1_<base64> URI into structured AegisConfig.
     */
    fun decryptAegisUri(uri: String, secret: String = DEFAULT_MASTER_KEY): Result<AegisConfig> {
        return try {
            val trimmed = uri.trim()
            if (!trimmed.startsWith("aegis://v1_")) {
                return Result.failure(IllegalArgumentException("قالب کانفیگ اختصاصی نامعتبر است."))
            }

            val b64Part = trimmed.removePrefix("aegis://v1_")
            val combined = Base64.decode(b64Part, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

            if (combined.size < 28) { // 12-byte IV + 16-byte Tag
                return Result.failure(IllegalArgumentException("طول بسته داده رمزنگاری‌شده کوتاه است."))
            }

            val iv = combined.copyOfRange(0, 12)
            val ciphertext = combined.copyOfRange(12, combined.size)

            val secretKey = deriveKey(secret)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            val jsonStr = String(decryptedBytes, Charsets.UTF_8)
            val json = JSONObject(jsonStr)

            val config = AegisConfig(
                version = json.optInt("v", 1),
                name = json.optString("name", "Aegis Secure Shield"),
                host = json.getString("host"),
                port = json.optInt("port", 500),
                user = json.optString("user", ""),
                pass = json.optString("pass", ""),
                p12Base64 = json.optString("p12", ""),
                p12Password = json.optString("p12_pass", ""),
                dns = json.optString("dns", "1.1.1.1"),
                subUrl = json.optString("sub_url", "")
            )

            Result.success(config)
        } catch (e: Exception) {
            Result.failure(Exception("رمزگشایی کانفیگ ناموفق بود (کلید نامعتبر یا بسته مخدوش است): ${e.message}"))
        }
    }
}
