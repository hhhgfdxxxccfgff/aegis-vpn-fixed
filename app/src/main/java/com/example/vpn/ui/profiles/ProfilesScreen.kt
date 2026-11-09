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

    // File picker for .sswan
    val sswanFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val text = stream.bufferedReader().use { r -> r.readText() }
                    scope.launch {
                        val result = controller.importSswan(text)
                        if (result.isSuccess) {
                            Toast.makeText(context, "Imported ${result.getOrNull()?.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Import failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
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
                        text = "VPN PROFILES",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${profiles.size} saved IKEv2 profiles",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { sswanFilePicker.launch("*/*") },
                        modifier = Modifier.testTag("pick_sswan_file_button")
                    ) {
                        Icon(imageVector = Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import .sswan", fontSize = 12.sp)
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
                            text = "No VPN Profiles",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Import a .sswan configuration or create a new profile with PKCS#12 certificate.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showImportDialog = true }) {
                            Text("Import Profile")
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
                                    val exportResult = controller.exportSswan(profile)
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

    // Import Dialog (.sswan paste / creation)
    if (showImportDialog) {
        ImportSswanDialog(
            onDismiss = { showImportDialog = false },
            onImport = { json, password ->
                scope.launch {
                    val res = controller.importSswan(json, password.toCharArray())
                    if (res.isSuccess) {
                        Toast.makeText(context, "Profile imported successfully!", Toast.LENGTH_SHORT).show()
                        showImportDialog = false
                    } else {
                        Toast.makeText(context, "Import error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
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

    // Export Dialog
    showExportDialog?.let { jsonExport ->
        AlertDialog(
            onDismissRequest = { showExportDialog = null },
            title = { Text("Export .sswan Configuration", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "The .sswan file contains the server address, proposals, and Base64 PKCS#12 certificate. Private keys are never exported in plaintext.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = jsonExport,
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
                    clipboard.setPrimaryClip(ClipData.newPlainText("Aegis .sswan", jsonExport))
                    Toast.makeText(context, "Copied .sswan to clipboard!", Toast.LENGTH_SHORT).show()
                    showExportDialog = null
                }) {
                    Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy JSON")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, jsonExport)
                        type = "application/json"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share .sswan"))
                }) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
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
                            text = { Text("Export .sswan") },
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

            // Proposals & Cert details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CryptoBadge(profile.ikeProposal)
                CryptoBadge(profile.espProposal)
                if (profile.rsaPss) CryptoBadge("RSA-PSS")
            }

            if (profile.userCertExpiry > 0L) {
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
                        text = "Cert Valid to: ${dateFormat.format(Date(profile.userCertExpiry))} • ${profile.keyAlgorithm}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportSswanDialog(
    onDismiss: () -> Unit,
    onImport: (json: String, password: String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val sampleJson = remember {
        """{
  "uuid": "gateway-01",
  "name": "IKEv2 Corporate Gateway",
  "type": "ikev2-cert",
  "remote": {
    "addr": "vpn.aegis-security.net",
    "port": 500
  },
  "local": {
    "p12": "${SampleProfiles.SAMPLE_P12_BASE64}"
  },
  "rsa-pss": "true",
  "ike-proposal": "aes256-sha256-modp2048",
  "esp-proposal": "aes128gcm16"
}"""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import .sswan Configuration", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Paste raw .sswan JSON format below, or use the pre-loaded template to test.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            jsonText = sampleJson
                            password = "aegis123"
                        },
                        modifier = Modifier.testTag("load_sample_sswan_button")
                    ) {
                        Text("Load Sample .sswan", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    label = { Text(".sswan JSON") },
                    placeholder = { Text("Paste JSON here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("PKCS#12 Password (Leave empty if no password)") },
                    placeholder = { Text("Leave empty for standard strongSwan profiles") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(jsonText, password) },
                enabled = jsonText.isNotBlank(),
                modifier = Modifier.testTag("submit_import_sswan_button")
            ) {
                Text("Validate & Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
    var ikeProposal by remember { mutableStateOf(profile.ikeProposal) }
    var espProposal by remember { mutableStateOf(profile.espProposal) }
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

            OutlinedTextField(
                value = ikeProposal,
                onValueChange = { ikeProposal = it },
                label = { Text("IKE Proposal") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = espProposal,
                onValueChange = { espProposal = it },
                label = { Text("ESP Proposal") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
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
                        ikeProposal = ikeProposal.ifBlank { profile.ikeProposal },
                        espProposal = espProposal.ifBlank { profile.espProposal },
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
