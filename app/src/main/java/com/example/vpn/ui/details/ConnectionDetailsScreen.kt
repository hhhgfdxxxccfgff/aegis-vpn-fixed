package com.example.vpn.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AegisCyanPrimary
import com.example.ui.theme.AegisEmerald
import com.example.vpn.engine.VpnController
import com.example.vpn.model.ConnectionState
import com.example.vpn.protocol.Ikev2ProposalParser
import com.example.vpn.ui.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConnectionDetailsScreen(controller: VpnController) {
    val connectionState by controller.connectionState.collectAsState()
    val stats by controller.connectionStats.collectAsState()
    val profile by controller.activeProfile.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CONNECTION DETAILS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Real-time cryptographic & tunnel diagnostics",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(state = connectionState)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section 1: Network & Gateway
        SectionCard(
            title = "NETWORK GATEWAY & TUNNEL",
            icon = Icons.Filled.Router
        ) {
            DetailRow("Gateway Address", profile?.serverAddr ?: "Not Connected")
            DetailRow("Control Port", "UDP ${profile?.remotePort ?: 500} (IKE_SA)")
            DetailRow("NAT Traversal", "UDP 4500 (Encap ESP in UDP)")
            DetailRow("Virtual IP Assigned", if (connectionState == ConnectionState.CONNECTED) stats.assignedIp else "None")
            DetailRow("Tunnel Route", "0.0.0.0/0 (Default Gateway via TUN)")
            DetailRow("Tunnel MTU", "${profile?.mtu ?: 1400} bytes")
            DetailRow("Underlying Network", stats.networkType)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: Cryptographic Suite
        SectionCard(
            title = "CRYPTOGRAPHIC SUITE (RFC 7296 / RFC 4303)",
            icon = Icons.Filled.Security
        ) {
            DetailRow("Protocol", "IKEv2 (Internet Key Exchange v2)")
            DetailRow("IKE SA Proposal", Ikev2ProposalParser.describeIkeProposal(profile?.ikeProposal ?: "aes256-sha256-modp2048"))
            DetailRow("Child SA (ESP)", Ikev2ProposalParser.describeEspProposal(profile?.espProposal ?: "aes128gcm16"))
            DetailRow("Key Exchange (DH)", "Group 14: 2048-bit MODP (RFC 3526)")
            DetailRow("Authentication Type", if (profile?.rsaPss == true) "RSA-PSS (RFC 7427)" else "RSA Digital Signature")
            DetailRow("Session Key Protection", "Android Keystore hardware-backed AES-256")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Certificate Details
        SectionCard(
            title = "CLIENT CERTIFICATE & IDENTITY",
            icon = Icons.Filled.VerifiedUser
        ) {
            DetailRow("Subject DN", profile?.userCertSubject ?: "Not Loaded")
            DetailRow("Issuer DN", profile?.userCertIssuer ?: "Not Loaded")
            DetailRow("Key Algorithm", profile?.keyAlgorithm ?: "RSA-2048")
            if ((profile?.userCertExpiry ?: 0L) > 0L) {
                DetailRow("Expires On", dateFormat.format(Date(profile!!.userCertExpiry)))
            }
            if (profile?.userCertFingerprint?.isNotBlank() == true) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "SHA-256 Fingerprint",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = profile!!.userCertFingerprint,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 4: DNS & Traffic Stats
        SectionCard(
            title = "DNS & TRAFFIC DIAGNOSTICS",
            icon = Icons.Filled.Dns
        ) {
            DetailRow("Active DNS Server", if (profile?.customDns?.isNotBlank() == true) profile!!.customDns else "1.1.1.1 (Cloudflare / Fallback 8.8.8.8)")
            DetailRow("Latency", if (stats.latencyMs > 0) "${stats.latencyMs} ms" else "Not Measured")
            DetailRow("Active Session", if (connectionState == ConnectionState.CONNECTED) "${stats.durationSeconds}s" else "Inactive")
            DetailRow("Bytes Transferred", "↓ ${stats.bytesIn / 1024} KB  /  ↑ ${stats.bytesOut / 1024} KB")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AegisCyanPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AegisCyanPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.3f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
