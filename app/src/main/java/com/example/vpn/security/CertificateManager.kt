package com.example.vpn.security

import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

data class ParsedCertificate(
    val userCert: X509Certificate,
    val privateKey: PrivateKey,
    val caCert: X509Certificate?,
    val certChain: List<X509Certificate>,
    val subjectDn: String,
    val issuerDn: String,
    val validFrom: Long,
    val validTo: Long,
    val fingerprintSha256: String,
    val keyAlgorithm: String,
    val isExpired: Boolean
)

object CertificateManager {

    /**
     * Attempts to unlock and parse a PKCS#12 (.p12/.pfx) byte stream.
     * Throws an exception if the password is incorrect or file is corrupt.
     */
    fun parsePkcs12(p12Bytes: ByteArray, password: CharArray): ParsedCertificate {
        val keyStore = KeyStore.getInstance("PKCS12")
        ByteArrayInputStream(p12Bytes).use { stream ->
            keyStore.load(stream, password)
        }

        val aliases = keyStore.aliases()
        var foundKey: PrivateKey? = null
        var foundCert: X509Certificate? = null
        var foundChain: List<X509Certificate> = emptyList()
        var foundCa: X509Certificate? = null

        while (aliases.hasMoreElements()) {
            val alias = aliases.nextElement()
            if (keyStore.isKeyEntry(alias)) {
                val key = keyStore.getKey(alias, password) as? PrivateKey
                val chain = keyStore.getCertificateChain(alias)?.filterIsInstance<X509Certificate>() ?: emptyList()
                val cert = chain.firstOrNull() ?: (keyStore.getCertificate(alias) as? X509Certificate)

                if (key != null && cert != null) {
                    foundKey = key
                    foundCert = cert
                    foundChain = chain
                    if (chain.size > 1) {
                        foundCa = chain.last()
                    }
                    break
                }
            }
        }

        if (foundKey == null || foundCert == null) {
            throw CertificateException("PKCS#12 does not contain a valid private key and X.509 certificate entry.")
        }

        val now = System.currentTimeMillis()
        val isExpired = now > foundCert.notAfter.time || now < foundCert.notBefore.time
        val fingerprint = calculateFingerprint(foundCert)
        val algorithmDesc = describeKey(foundCert, foundKey)

        return ParsedCertificate(
            userCert = foundCert,
            privateKey = foundKey,
            caCert = foundCa,
            certChain = foundChain,
            subjectDn = foundCert.subjectX500Principal.name,
            issuerDn = foundCert.issuerX500Principal.name,
            validFrom = foundCert.notBefore.time,
            validTo = foundCert.notAfter.time,
            fingerprintSha256 = fingerprint,
            keyAlgorithm = algorithmDesc,
            isExpired = isExpired
        )
    }

    /**
     * Checks if password opens the PKCS#12 container.
     */
    fun testPassword(p12Bytes: ByteArray, password: CharArray): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("PKCS12")
            ByteArrayInputStream(p12Bytes).use { stream ->
                keyStore.load(stream, password)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns true if the PKCS#12 container requires a non-empty password to unlock.
     */
    fun requiresPassword(p12Bytes: ByteArray): Boolean {
        return !testPassword(p12Bytes, CharArray(0))
    }

    private fun calculateFingerprint(cert: X509Certificate): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(cert.encoded)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            "SHA256:Unavailable"
        }
    }

    private fun describeKey(cert: X509Certificate, key: PrivateKey): String {
        val pub = cert.publicKey
        return when {
            pub is RSAPublicKey -> "RSA-${pub.modulus.bitLength()} (${cert.sigAlgName})"
            key is RSAPrivateKey -> "RSA-${key.modulus.bitLength()}"
            else -> "${pub.algorithm} (${cert.sigAlgName})"
        }
    }
}
