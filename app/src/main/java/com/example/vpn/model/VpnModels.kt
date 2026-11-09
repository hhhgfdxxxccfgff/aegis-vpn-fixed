package com.example.vpn.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * VPN Profile entity for storing IKEv2/IPsec connection profiles.
 */
@Entity(tableName = "vpn_profiles")
data class VpnProfile(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String = "ikev2-cert",
    val serverAddr: String,
    val remotePort: Int = 500,
    val ikeProposal: String = "aes256-sha256-modp2048",
    val espProposal: String = "aes128gcm16",
    val rsaPss: Boolean = true,
    val encryptedP12Base64: String, // Keystore-encrypted Base64 PKCS#12
    val encryptedPassword: String = "", // Keystore-encrypted password (or empty if unencrypted/prompted)
    val userCertSubject: String = "",
    val userCertIssuer: String = "",
    val userCertExpiry: Long = 0L,
    val userCertFingerprint: String = "",
    val keyAlgorithm: String = "RSA-2048",
    val isDefault: Boolean = false,
    val splitTunnelingEnabled: Boolean = false,
    val excludedPackages: String = "", // Comma-separated package names
    val customDns: String = "", // Custom DNS or empty for default
    val mtu: Int = 1400,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = 0L
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTING,
    FAILED
}

data class ConnectionStats(
    val durationSeconds: Long = 0L,
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val speedInBps: Long = 0L,
    val speedOutBps: Long = 0L,
    val latencyMs: Int = -1,
    val networkType: String = "None",
    val serverIp: String = "",
    val assignedIp: String = "10.10.0.2",
    val activeProtocol: String = "IKEv2 / IPsec ESP",
    val activeIkeProposal: String = "aes256-sha256-modp2048",
    val activeEspProposal: String = "aes128gcm16"
)

@Entity(tableName = "connection_logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val category: String = "SYSTEM",
    val message: String
)

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR
}
