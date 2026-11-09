package com.example.vpn.data

import com.example.vpn.model.VpnProfile
import com.example.vpn.security.KeystoreManager

object SampleProfiles {
    // Valid PKCS#12 base64 with RSA 2048 client certificate, protected with password 'aegis123'
    const val SAMPLE_P12_BASE64 = "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"

    fun createSampleProfile(): VpnProfile {
        val encryptedP12 = KeystoreManager.encryptString(SAMPLE_P12_BASE64)
        val encryptedPassword = KeystoreManager.encryptString("aegis123")

        return VpnProfile(
            id = "sample-gateway-01",
            name = "Aegis Shield - Fast Node 1",
            type = "aegis-shield",
            serverAddr = "3.142.134.245",
            remotePort = 500,
            ikeProposal = "aes256-sha256-modp2048",
            espProposal = "aes128gcm16",
            rsaPss = true,
            encryptedP12Base64 = encryptedP12,
            encryptedPassword = encryptedPassword,
            userCertSubject = "Aegis Quantum Shield Node",
            userCertIssuer = "Aegis Root Authority",
            userCertExpiry = System.currentTimeMillis() + (365L * 24 * 3600 * 1000 * 5), // 5 years
            userCertFingerprint = "7F:C4:8A:23:41:99:B8:3E:D0:E1:92:AF:74:BB:AA:32:01:DF:EE:89:12:44:AC:EE",
            keyAlgorithm = "Quantum-256",
            isDefault = true,
            createdAt = System.currentTimeMillis()
        )
    }
}
