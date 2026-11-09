package com.example.vpn.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AegisCyanPrimary
import com.example.ui.theme.AegisEmerald
import com.example.vpn.engine.VpnController

@Composable
fun SettingsScreen(
    controller: VpnController,
    onThemeChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val settings by controller.settingsManager.settings.collectAsState()

    var mtuValue by remember(settings.defaultMtu) { mutableFloatStateOf(settings.defaultMtu.toFloat()) }
    var customDnsInput by remember(settings.customDns) { mutableStateOf(settings.customDns) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Header
        Text(
            text = "SETTINGS",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Tunnel parameters, kill switch, and reliability",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Always-On VPN & System Kill Switch Guide Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.VpnLock,
                        contentDescription = null,
                        tint = AegisCyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ALWAYS-ON VPN & KILL SWITCH",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Android provides hardware-enforced kill switch protection. In Android VPN Settings, select 'Aegis VPN' and enable 'Always-on VPN' and 'Block connections without VPN' to ensure zero IP leaks.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_VPN_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Open Android Settings > Network > VPN", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.testTag("open_system_vpn_settings_button")
                ) {
                    Icon(imageVector = Icons.Filled.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Android VPN Settings")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connection Automation
        SettingsGroup(title = "CONNECTION AUTOMATION") {
            ToggleSettingRow(
                title = "Auto-Connect on Launch",
                description = "Automatically connect to active profile when application opens",
                checked = settings.autoConnectOnLaunch,
                onCheckedChange = { controller.settingsManager.updateAutoConnect(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ToggleSettingRow(
                title = "Auto-Reconnect on Network Switch",
                description = "Seamless handover and reconnect with exponential backoff on Wi-Fi/Mobile changes",
                checked = settings.autoReconnectOnNetworkLoss,
                onCheckedChange = { controller.settingsManager.updateAutoReconnect(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DNS Configuration
        SettingsGroup(title = "DNS RESOLUTION") {
            Text(
                text = "Select preferred secure DNS resolver for the IPsec tunnel:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            val presets = listOf(
                "1.1.1.1 (Cloudflare)" to "1.1.1.1",
                "8.8.8.8 (Google)" to "8.8.8.8",
                "9.9.9.9 (Quad9)" to "9.9.9.9",
                "Custom DNS" to customDnsInput
            )

            presets.forEach { (label, ip) ->
                val isSelected = settings.selectedDnsPreset == label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AegisCyanPrimary.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable {
                            controller.settingsManager.updateDns(label, ip)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) AegisCyanPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = ip,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (settings.selectedDnsPreset == "Custom DNS") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customDnsInput,
                    onValueChange = {
                        customDnsInput = it
                        controller.settingsManager.updateDns("Custom DNS", it)
                    },
                    label = { Text("Custom DNS IP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // MTU & Fragmentation
        SettingsGroup(title = "TUNNEL MTU (${mtuValue.toInt()} BYTES)") {
            Text(
                text = "Maximum Transmission Unit for IPsec packets. Lower values avoid carrier packet fragmentation.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = mtuValue,
                onValueChange = { mtuValue = it },
                onValueChangeFinished = {
                    controller.settingsManager.updateMtu(mtuValue.toInt())
                },
                valueRange = 1280f..1500f,
                steps = 21,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1280 (Strict)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("1400 (Default)", fontSize = 11.sp, color = AegisCyanPrimary, fontWeight = FontWeight.Bold)
                Text("1500 (Max)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Theme
        SettingsGroup(title = "APPEARANCE") {
            val themes = listOf("SYSTEM" to "System Default", "DARK" to "Dark (Cybersecurity)", "LIGHT" to "Light")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.forEach { (code, label) ->
                    val isSelected = settings.themeMode == code
                    OutlinedButton(
                        onClick = {
                            controller.settingsManager.updateTheme(code)
                            onThemeChanged(code)
                        },
                        modifier = Modifier.weight(1f),
                        colors = if (isSelected) androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = AegisCyanPrimary.copy(alpha = 0.15f)
                        ) else androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AegisCyanPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Assurance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Security, contentDescription = null, tint = AegisEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ZERO-LOGS & KEYSTORE PROTECTED", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = AegisEmerald)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No user traffic is collected. Private keys and PKCS#12 containers are sealed in hardware-backed Android KeyStore AES-256 storage and never transmitted to external services.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = AegisCyanPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
