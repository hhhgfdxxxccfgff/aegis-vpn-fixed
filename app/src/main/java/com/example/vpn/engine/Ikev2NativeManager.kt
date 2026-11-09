package com.example.vpn.engine

import android.content.Context
import android.content.Intent
import android.net.Ikev2VpnProfile
import android.net.VpnManager
import android.net.VpnProfileState
import android.net.ipsec.ike.IkeFqdnIdentification
import android.net.ipsec.ike.IkeSessionParams
import android.net.ipsec.ike.IkeTunnelConnectionParams
import android.net.ipsec.ike.TunnelModeChildSessionParams
import android.os.Build
import android.system.OsConstants
import com.example.vpn.model.LogLevel
import com.example.vpn.model.VpnProfile
import com.example.vpn.protocol.Ikev2ProposalParser
import com.example.vpn.security.CertificateManager
import com.example.vpn.security.KeystoreManager

object Ikev2NativeManager {

    fun isNativeIkev2Supported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    /**
     * Extracts the cleanest identity from parsed certificate DN.
     * In strongSwan / IKEv2, server configs typically match identity against
     * the certificate CN (e.g., "android") or the SAN, rather than the full RFC 2253 DN.
     */
    fun extractIdentity(subjectDn: String): String {
        if (subjectDn.isBlank()) return "android"
        // Try to extract CN=...
        val parts = subjectDn.split(",")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.startsWith("CN=", ignoreCase = true)) {
                val cn = trimmed.substring(3).trim()
                if (cn.isNotEmpty()) return cn
            }
        }
        return subjectDn.trim()
    }

    /**
     * Provisions the IKEv2 profile into Android system VpnManager.
     * Returns an Intent if system permission dialog is required, or null if already provisioned.
     */
    fun provisionProfile(context: Context, profile: VpnProfile, passwordChars: CharArray): Intent? {
        if (!isNativeIkev2Supported()) return null

        val vpnManager = context.getSystemService(VpnManager::class.java) ?: return null

        try {
            // Decrypt P12 material using Android Keystore
            val p12Encrypted = profile.encryptedP12Base64
            val p12Bytes = KeystoreManager.decrypt(p12Encrypted)
            val parsedCert = CertificateManager.parsePkcs12(p12Bytes, passwordChars)

            val identity = extractIdentity(parsedCert.subjectDn)
            VpnController.getInstance(context).log(
                LogLevel.INFO,
                "NATIVE_IKE",
                "Building native IKEv2 profile for ${profile.serverAddr} with identity: '$identity'"
            )

            // strongSwan on Android uses standard Ikev2VpnProfile.Builder(serverAddr, identity)
            // which handles UDP 500 -> UDP 4500 NAT traversal transparently without restrictive options.
            val ikeProfile = buildClassicProfile(profile, identity, parsedCert)

            val consentIntent = vpnManager.provisionVpnProfile(ikeProfile)
            if (consentIntent == null) {
                VpnController.getInstance(context).log(
                    LogLevel.INFO,
                    "NATIVE_IKE",
                    "Native IKEv2 profile successfully provisioned into OS VpnManager."
                )
            }
            return consentIntent

        } catch (e: Exception) {
            VpnController.getInstance(context).log(
                LogLevel.ERROR,
                "NATIVE_IKE",
                "Failed to provision native Ikev2VpnProfile: ${e.message}"
            )
            return null
        }
    }

    private fun buildClassicProfile(
        profile: VpnProfile,
        identity: String,
        parsedCert: com.example.vpn.security.ParsedCertificate
    ): Ikev2VpnProfile {
        val builder = Ikev2VpnProfile.Builder(profile.serverAddr, identity)
            .setAuthDigitalSignature(parsedCert.userCert, parsedCert.privateKey, parsedCert.caCert)
            .setBypassable(false)
            .setMaxMtu(profile.mtu)

        // Android 14+ (API 34) automated NAT-T keepalive and IP selection
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                val methodNat = Ikev2VpnProfile.Builder::class.java.getMethod(
                    "setAutomaticNattKeepaliveTimerEnabled",
                    Boolean::class.javaPrimitiveType
                )
                methodNat.invoke(builder, true)
            } catch (_: Exception) {}
            try {
                val methodIp = Ikev2VpnProfile.Builder::class.java.getMethod(
                    "setAutomaticIpVersionSelectionEnabled",
                    Boolean::class.javaPrimitiveType
                )
                methodIp.invoke(builder, true)
            } catch (_: Exception) {}
        }

        return builder.build()
    }

    /**
     * Starts the provisioned IKEv2 session using Android VpnManager.
     */
    fun startNativeSession(context: Context): Boolean {
        if (!isNativeIkev2Supported()) return false
        val vpnManager = context.getSystemService(VpnManager::class.java) ?: return false

        return try {
            vpnManager.startProvisionedVpnProfileSession()
            VpnController.getInstance(context).log(
                LogLevel.SUCCESS,
                "NATIVE_IKE",
                "Started native OS IKEv2 session via VpnManager. Kernel IPsec stack is connecting..."
            )
            true
        } catch (e: Exception) {
            VpnController.getInstance(context).log(
                LogLevel.ERROR,
                "NATIVE_IKE",
                "startProvisionedVpnProfileSession failed: ${e.message}"
            )
            false
        }
    }

    /**
     * Stops the provisioned IKEv2 session.
     */
    fun stopNativeSession(context: Context) {
        if (!isNativeIkev2Supported()) return
        val vpnManager = context.getSystemService(VpnManager::class.java) ?: return
        try {
            vpnManager.stopProvisionedVpnProfile()
            VpnController.getInstance(context).log(
                LogLevel.INFO,
                "NATIVE_IKE",
                "Stopped native OS IKEv2 session via VpnManager."
            )
        } catch (_: Exception) {}
    }

    /**
     * Checks current VpnProfileState if supported.
     */
    fun getProfileState(context: Context): Int {
        if (!isNativeIkev2Supported()) return -1
        val vpnManager = context.getSystemService(VpnManager::class.java) ?: return -1
        return try {
            vpnManager.provisionedVpnProfileState?.state ?: VpnProfileState.STATE_DISCONNECTED
        } catch (_: Exception) {
            -1
        }
    }
}
