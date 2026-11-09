package com.example.vpn.ui.profiles

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AegisCyanPrimary
import com.example.ui.theme.AegisEmerald
import com.example.ui.theme.AegisRose
import com.example.vpn.data.SampleProfiles
import com.example.vpn.engine.VpnController
import com.example.vpn.model.ConnectionState
import com.example.vpn.model.VpnProfile
import com.example.vpn.ui.components.CryptoBadge
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    controller: VpnController,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profiles by controller.allProfiles.collectAsState()
    val activeProfile by controller.activeProfile.collectAsState()
    val connectionState by controller.connectionState.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf<VpnProfile?>(null) }
    var showExportDialog by remember { mutableStateOf<String?>(null) }
    var profileToDelete by remember { mutableStateOf<VpnProfile?>(null) }

    // File picker for Aegis / Config files
    val sswanFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val text = stream.bufferedReader().use { r -> r.readText() }.trim()
                    scope.launch {
                        val result = if (text.startsWith("aegis://")) {
                            controller.importAegisUri(text)
                        } else {
                            controller.importSswan(text)
                        }
                        if (result.isSuccess) {
                            Toast.makeText(context, "کانفیگ با موفقیت اضافه شد", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "خطا در خواندن فایل کانفیگ: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Read error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showImportDialog = true },
                containerColor = AegisCyanPrimary,
                contentColor = Color(0xFF001F29),
                modifier = Modifier.testTag("add_profile_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Profile")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                        text = "AEGIS SECURE NODES",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${profiles.size} سرور امن ذخیره‌شده",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AegisCyanPrimary),
                        modifier = Modifier.testTag("add_aegis_config_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("افزودن کانفیگ", fontSize = 12.sp, color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (profiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "هیچ کانفیگی یافت نشد",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "کانفیگ اختصاصی Aegis (aegis://) یا لینک اشتراک هوشمند پنل را اضافه کنید.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showImportDialog = true }) {
                            Text("افزودن کانفیگ / اشتراک")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        val isActive = profile.id == activeProfile?.id
                        ProfileItemCard(
                            profile = profile,
                            isActive = isActive,
                            connectionState = if (isActive) connectionState else ConnectionState.DISCONNECTED,
                            onSelect = {
                                controller.selectActiveProfile(profile)
                                onNavigateToDashboard()
                            },
                            onEdit = { showEditSheet = profile },
                            onExport = {
                                scope.launch {
                                    val exportResult = controller.exportAegisUri(profile)
                                    if (exportResult.isSuccess) {
                                        showExportDialog = exportResult.getOrNull()
                                    } else {
                                        Toast.makeText(context, "Export failed: ${exportResult.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDelete = { profileToDelete = profile }
                        )
                    }
                }
            }
        }
    }

    // Import Dialog (Aegis proprietary URI & Subscription URL)
    if (showImportDialog) {
        ImportAegisConfigDialog(
            onDismiss = { showImportDialog = false },
            onImport = { content ->
                scope.launch {
                    val trimmed = content.trim()
                    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                        val res = controller.importSubscription(trimmed)
                        if (res.isSuccess) {
                            Toast.makeText(context, "اشتراک با موفقیت متصل شد", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        } else {
                            Toast.makeText(context, "خطا در اتصال به اشتراک: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    } else if (trimmed.startsWith("aegis://")) {
                        val res = controller.importAegisUri(trimmed)
                        if (res.isSuccess) {
                            Toast.makeText(context, "کانفیگ اختصاصی با موفقیت افزوده شد", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        } else {
                            Toast.makeText(context, "خطا در رمزگشایی: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val res = controller.importSswan(trimmed)
                        if (res.isSuccess) {
                            Toast.makeText(context, "کانفیگ ذخیره شد", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        } else {
                            Toast.makeText(context, "فرمت نامعتبر است. از لینک aegis:// یا اشتراک استفاده کنید.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }

    // Edit Profile Bottom Sheet
    showEditSheet?.let { profile ->
        EditProfileBottomSheet(
            profile = profile,
            onDismiss = { showEditSheet = null },
            onSave = { updated ->
                scope.launch {
                    controller.updateProfile(updated)
                    showEditSheet = null
                }
            }
        )
    }

    // Export Dialog (Aegis proprietary encrypted URI)
    showExportDialog?.let { aegisUriExport ->
        AlertDialog(
            onDismissRequest = { showExportDialog = null },
            title = { Text("اشتراک‌گذاری کانفیگ Aegis", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "این کانفیگ اختصاصی به‌صورت ۲۵۶ بیتی (AES-256-GCM) رمزنگاری شده است و هویت پروتکل کاملاً پنهان می‌باشد.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = aegisUriExport,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Aegis Config", aegisUriExport))
                    Toast.makeText(context, "کانفیگ در کلیپ‌بورد کپی شد!", Toast.LENGTH_SHORT).show()
                    showExportDialog = null
                }) {
                    Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("کپی کانفیگ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, aegisUriExport)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Aegis Config"))
                }) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("اشتراک‌گذاری")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    profileToDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Delete Profile?") },
            text = { Text("Are you sure you want to delete '${profile.name}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            controller.deleteProfile(profile)
                            profileToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisRose)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileItemCard(
    profile: VpnProfile,
    isActive: Boolean,
    connectionState: ConnectionState,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isActive) AegisCyanPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onSelect() }
            .testTag("profile_item_${profile.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isActive) AegisCyanPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VpnKey,
                            contentDescription = null,
                            tint = if (isActive) AegisCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AegisCyanPrimary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF001F29))
                                }
                            }
                        }
                        Text(
                            text = "${profile.serverAddr}:${profile.remotePort}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Select Profile") },
                            onClick = {
                                menuExpanded = false
                                onSelect()
                            },
                            leadingIcon = { Icon(Icons.Filled.Check, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Metadata") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Aegis Config") },
                            onClick = {
                                menuExpanded = false
                                onExport()
                            },
                            leadingIcon = { Icon(Icons.Filled.FileDownload, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Profile", color = AegisRose) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = AegisRose) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Encryption & Subscription details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CryptoBadge("Aegis Quantum")
                CryptoBadge("AES-256-GCM")
                if (profile.daysLeft != null && profile.daysLeft >= 0) {
                    CryptoBadge("${profile.daysLeft} روز")
                }
                if (profile.remainingBytes != null && profile.remainingBytes >= 0) {
                    val gb = String.format(Locale.US, "%.1f GB", profile.remainingBytes / (1024.0 * 1024.0 * 1024.0))
                    CryptoBadge(gb)
                }
            }

            if (!profile.subUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = AegisCyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "متصل به اشتراک هوشمند پنل",
                        fontSize = 11.sp,
                        color = AegisCyanPrimary
                    )
                }
            } else if (profile.userCertExpiry > 0L) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = AegisEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "اعتبار تا: ${dateFormat.format(Date(profile.userCertExpiry))} • کلید سخت‌افزاری",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportAegisConfigDialog(
    onDismiss: () -> Unit,
    onImport: (content: String) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن کانفیگ اختصاصی Aegis", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "کد رمزنگاری‌شده (aegis://) یا لینک اشتراک پنل (http://.../sub/...) را در کادر زیر وارد کنید:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clip.isNullOrBlank()) {
                                    inputText = clip.trim()
                                }
                            } catch (_: Exception) {}
                        }
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("جای‌گذاری از کلیپ‌بورد", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("لینک aegis:// یا لینک اشتراک") },
                    placeholder = { Text("aegis://v1_... یا http://...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(inputText) },
                enabled = inputText.isNotBlank()
            ) {
                Text("تایید و افزودن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileBottomSheet(
    profile: VpnProfile,
    onDismiss: () -> Unit,
    onSave: (VpnProfile) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf(profile.name) }
    var serverAddr by remember { mutableStateOf(profile.serverAddr) }
    var remotePort by remember { mutableStateOf(profile.remotePort.toString()) }
    var mtu by remember { mutableStateOf(profile.mtu.toString()) }
    var splitTunneling by remember { mutableStateOf(profile.splitTunnelingEnabled) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("Edit Profile Metadata", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = serverAddr,
                    onValueChange = { serverAddr = it },
                    label = { Text("Gateway Address") },
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextField(
                    value = remotePort,
                    onValueChange = { remotePort = it },
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = AegisEmerald, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("پروتکل امنیتی ارتباطی", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AegisEmerald)
                        Text("Aegis Quantum Shield • تونل ۲۵۶ بیتی سخت‌افزاری", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = mtu,
                onValueChange = { mtu = it },
                label = { Text("Tunnel MTU") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val updated = profile.copy(
                        name = name.ifBlank { profile.name },
                        serverAddr = serverAddr.ifBlank { profile.serverAddr },
                        remotePort = remotePort.toIntOrNull() ?: 500,
                        mtu = mtu.toIntOrNull()?.coerceIn(1280, 1500) ?: 1400,
                        splitTunnelingEnabled = splitTunneling
                    )
                    onSave(updated)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    }
}
