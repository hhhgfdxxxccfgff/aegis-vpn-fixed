package com.example.vpn.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed AES-256-GCM encryption manager using AndroidKeyStore.
 * Ensures that imported PKCS#12 credentials and passwords are encrypted at rest.
 */
object KeystoreManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "AegisVpnMasterKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val keyGenSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keyGenSpec)
            return keyGenerator.generateKey()
        }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypts plaintext bytes and returns Base64(IV + Ciphertext).
     */
    fun encrypt(plaintext: ByteArray): String {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Encrypts string to Base64(IV + Ciphertext).
     */
    fun encryptString(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        return encrypt(plaintext.toByteArray(Charsets.UTF_8))
    }

    /**
     * Decrypts Base64(IV + Ciphertext) back to raw bytes.
     */
    fun decrypt(encryptedBase64: String): ByteArray {
        if (encryptedBase64.isBlank()) return ByteArray(0)
        val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)
        if (combined.size < IV_LENGTH) {
            throw IllegalArgumentException("Invalid encrypted payload: too short.")
        }
        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
        val ciphertext = ByteArray(combined.size - IV_LENGTH)
        System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.size)

        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(ciphertext)
    }

    /**
     * Decrypts Base64(IV + Ciphertext) back to UTF-8 string.
     */
    fun decryptString(encryptedBase64: String): String {
        if (encryptedBase64.isBlank()) return ""
        val bytes = decrypt(encryptedBase64)
        return String(bytes, Charsets.UTF_8)
    }
}
