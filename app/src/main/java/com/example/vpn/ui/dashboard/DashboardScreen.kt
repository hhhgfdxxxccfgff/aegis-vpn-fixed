package com.example.vpn.ui.dashboard

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AegisAmber
import com.example.ui.theme.AegisCyanPrimary
import com.example.ui.theme.AegisCyanSecondary
import com.example.ui.theme.AegisEmerald
import com.example.ui.theme.AegisRose
import com.example.vpn.engine.VpnController
import com.example.vpn.model.ConnectionState
import com.example.vpn.model.VpnProfile
import com.example.vpn.ui.components.CryptoBadge
import com.example.vpn.ui.components.StatusBadge

@Composable
fun DashboardScreen(
    controller: VpnController,
    onNavigateToProfiles: () -> Unit,
    onNavigateToDetails: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val context = LocalContext.current
    val connectionState by controller.connectionState.collectAsState()
    val stats by controller.connectionStats.collectAsState()
    val activeProfile by controller.activeProfile.collectAsState()
    val allProfiles by controller.allProfiles.collectAsState()
    val pendingVpnIntent by controller.pendingVpnIntent.collectAsState()

    var showProfilePicker by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var targetConnectProfile by remember { mutableStateOf<VpnProfile?>(null) }

    // System VPN permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        controller.clearPendingVpnIntent()
        if (result.resultCode == Activity.RESULT_OK) {
            controller.onVpnPermissionGranted()
        } else {
            controller.updateState(ConnectionState.DISCONNECTED)
        }
    }

    LaunchedEffect(pendingVpnIntent) {
        pendingVpnIntent?.let {
            vpnPermissionLauncher.launch(it)
        }
    }

    val isConnected = connectionState == ConnectionState.CONNECTED
    val isConnecting = connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.RECONNECTING

    // Pulse animation for connecting/connected state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnecting) 1.25f else if (isConnected) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnecting) 900 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val shieldColor by animateColorAsState(
        targetValue = when (connectionState) {
            ConnectionState.CONNECTED -> AegisEmerald
            ConnectionState.CONNECTING -> AegisCyanPrimary
            ConnectionState.RECONNECTING -> AegisAmber
            ConnectionState.FAILED -> AegisRose
            else -> Color(0xFF64748B)
        },
        label = "shieldColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AEGIS IKEv2",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Certificate-Authenticated IPsec",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(state = connectionState)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Center Connect / Disconnect Control
        Box(
            modifier = Modifier
                .size(240.dp)
                .testTag("vpn_connect_button_container"),
            contentAlignment = Alignment.Center
        ) {
            // Concentric pulsating outer wave
            if (isConnecting || isConnected) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(shieldColor.copy(alpha = if (isConnecting) 0.12f else 0.08f))
                        .border(1.dp, shieldColor.copy(alpha = 0.3f), CircleShape)
                )
            }

            // Outer ring
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .border(2.dp, shieldColor.copy(alpha = 0.5f), CircleShape)
            )

            // Interactive Shield core
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                shieldColor.copy(alpha = 0.25f),
                                shieldColor.copy(alpha = 0.10f)
                            )
                        )
                    )
                    .border(2.dp, shieldColor, CircleShape)
                    .clickable {
                        when (connectionState) {
                            ConnectionState.CONNECTED, ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> {
                                controller.disconnect()
                            }
                            else -> {
                                val profile = activeProfile
                                if (profile != null) {
                                    if (controller.profileRequiresPasswordPrompt(profile)) {
                                        targetConnectProfile = profile
                                        showPasswordDialog = true
                                    } else {
                                        controller.connect(profile)
                                    }
                                } else {
                                    onNavigateToProfiles()
                                }
                            }
                        }
                    }
                    .testTag("vpn_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Filled.Security else if (isConnecting) Icons.Filled.Refresh else Icons.Filled.LockOpen,
                        contentDescription = "VPN Shield Status",
                        tint = shieldColor,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (connectionState) {
                            ConnectionState.CONNECTED -> "SECURED"
                            ConnectionState.CONNECTING -> "CONNECTING"
                            ConnectionState.RECONNECTING -> "RETRYING"
                            ConnectionState.FAILED -> "RETRY"
                            else -> "CONNECT"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = shieldColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Selected Profile Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("active_profile_card")
                .clickable { showProfilePicker = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AegisCyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VpnKey,
                            contentDescription = "Profile",
                            tint = AegisCyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = activeProfile?.name ?: "No Profile Selected",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (activeProfile != null) "${activeProfile?.serverAddr}:${activeProfile?.remotePort}" else "Tap to choose or import .sswan",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { showProfilePicker = true }) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = "Switch Profile",
                        tint = AegisCyanPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real-Time Session Statistics (2x2 Grid)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Timer,
                title = "DURATION",
                value = formatDuration(stats.durationSeconds),
                subtext = if (isConnected) "Active Session" else "Standby",
                accentColor = AegisCyanPrimary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Speed,
                title = "LATENCY",
                value = if (stats.latencyMs > 0) "${stats.latencyMs} ms" else "--",
                subtext = stats.networkType,
                accentColor = if (stats.latencyMs in 1..80) AegisEmerald else AegisAmber
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.ArrowDownward,
                title = "DOWNLOAD",
                value = formatSpeed(stats.speedInBps),
                subtext = "Total: ${formatBytes(stats.bytesIn)}",
                accentColor = AegisCyanSecondary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.ArrowUpward,
                title = "UPLOAD",
                value = formatSpeed(stats.speedOutBps),
                subtext = "Total: ${formatBytes(stats.bytesOut)}",
                accentColor = AegisEmerald
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cryptographic Suite Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToDetails() },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = AegisCyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SECURITY SUITE & PROPOSALS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Details →",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AegisCyanPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CryptoBadge("IKE: AES-256")
                    CryptoBadge("ESP: AES-128-GCM")
                    CryptoBadge("MODP2048")
                    CryptoBadge("RSA-PSS")
                }
            }
        }
    }

    // Profile Selector Dialog
    if (showProfilePicker) {
        AlertDialog(
            onDismissRequest = { showProfilePicker = false },
            title = {
                Text("Select VPN Profile", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (allProfiles.isEmpty()) {
                        Text("No profiles imported yet. Import a .sswan file or create a profile.")
                    } else {
                        allProfiles.forEach { profile ->
                            val isSelected = profile.id == activeProfile?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AegisCyanPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        controller.selectActiveProfile(profile)
                                        showProfilePicker = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) AegisCyanPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${profile.serverAddr}:${profile.remotePort}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = AegisCyanPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showProfilePicker = false
                    onNavigateToProfiles()
                }) {
                    Text("Manage Profiles")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfilePicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Password Prompt Dialog
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                passwordInput = ""
            },
            title = {
                Text("Enter PKCS#12 Password", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "The certificate for '${targetConnectProfile?.name}' requires a password to unlock private keys.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val profile = targetConnectProfile
                    val pwd = passwordInput.toCharArray()
                    showPasswordDialog = false
                    passwordInput = ""
                    if (profile != null) {
                        controller.connect(profile, pwd)
                    }
                }) {
                    Text("Unlock & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    passwordInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    subtext: String,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        "%02d:%02d:%02d".format(hrs, mins, secs)
    } else {
        "%02d:%02d".format(mins, secs)
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1024 * 1024 -> "%.1f MB/s".format(bytesPerSec.toDouble() / (1024 * 1024))
        bytesPerSec >= 1024 -> "%.0f KB/s".format(bytesPerSec.toDouble() / 1024)
        else -> "$bytesPerSec B/s"
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "%.2f GB".format(bytes.toDouble() / (1024 * 1024 * 1024))
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
        bytes >= 1024 -> "%.0f KB".format(bytes.toDouble() / 1024)
        else -> "$bytes B"
    }
}
