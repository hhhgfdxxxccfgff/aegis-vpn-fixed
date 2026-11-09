package com.example.vpn.ui.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.theme.AegisAmber
import com.example.ui.theme.AegisCyanPrimary
import com.example.ui.theme.AegisEmerald
import com.example.ui.theme.AegisRose
import com.example.vpn.engine.VpnController
import com.example.vpn.model.LogEntry
import com.example.vpn.model.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(controller: VpnController) {
    val context = LocalContext.current
    val logs by controller.recentLogs.collectAsState()
    var selectedLevel by remember { mutableStateOf<LogLevel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    val filteredLogs = remember(logs, selectedLevel, searchQuery) {
        logs.filter { entry ->
            val levelMatches = selectedLevel == null || entry.level == selectedLevel
            val searchMatches = searchQuery.isBlank() ||
                    entry.message.contains(searchQuery, ignoreCase = true) ||
                    entry.category.contains(searchQuery, ignoreCase = true)
            levelMatches && searchMatches
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CONNECTION LOGS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${filteredLogs.size} diagnostic event entries",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(
                    onClick = {
                        val text = filteredLogs.joinToString("\n") {
                            "[${dateFormat.format(Date(it.timestamp))}] [${it.level}] [${it.category}] ${it.message}"
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Aegis Logs", text))
                        Toast.makeText(context, "Copied ${filteredLogs.size} log lines!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("copy_logs_button")
                ) {
                    Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = "Copy Logs")
                }
                IconButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.testTag("clear_logs_button")
                ) {
                    Icon(imageVector = Icons.Filled.ClearAll, contentDescription = "Clear Logs", tint = AegisRose)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Privacy Guarantee Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AegisCyanPrimary.copy(alpha = 0.08f))
                .border(1.dp, AegisCyanPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Filled.Security, contentDescription = null, tint = AegisCyanPrimary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Credentials & private keys are protected and never logged.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter logs by keyword...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_logs_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Level Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedLevel == null,
                onClick = { selectedLevel = null },
                label = { Text("ALL") }
            )
            FilterChip(
                selected = selectedLevel == LogLevel.SUCCESS,
                onClick = { selectedLevel = if (selectedLevel == LogLevel.SUCCESS) null else LogLevel.SUCCESS },
                label = { Text("SUCCESS") },
                colors = FilterChipDefaults.filterChipColors(selectedLabelColor = AegisEmerald)
            )
            FilterChip(
                selected = selectedLevel == LogLevel.INFO,
                onClick = { selectedLevel = if (selectedLevel == LogLevel.INFO) null else LogLevel.INFO },
                label = { Text("INFO") }
            )
            FilterChip(
                selected = selectedLevel == LogLevel.WARN,
                onClick = { selectedLevel = if (selectedLevel == LogLevel.WARN) null else LogLevel.WARN },
                label = { Text("WARN") },
                colors = FilterChipDefaults.filterChipColors(selectedLabelColor = AegisAmber)
            )
            FilterChip(
                selected = selectedLevel == LogLevel.ERROR,
                onClick = { selectedLevel = if (selectedLevel == LogLevel.ERROR) null else LogLevel.ERROR },
                label = { Text("ERROR") },
                colors = FilterChipDefaults.filterChipColors(selectedLabelColor = AegisRose)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Log List
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No logs matching criteria",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    LogItemCard(log = log, formattedTime = dateFormat.format(Date(log.timestamp)))
                }
            }
        }
    }

    // Clear Confirmation Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Logs?") },
            text = { Text("This will remove all session event records from device memory.") },
            confirmButton = {
                Button(
                    onClick = {
                        controller.clearLogs()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisRose)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LogItemCard(log: LogEntry, formattedTime: String) {
    val levelColor = when (log.level) {
        LogLevel.SUCCESS -> AegisEmerald
        LogLevel.INFO -> AegisCyanPrimary
        LogLevel.WARN -> AegisAmber
        LogLevel.ERROR -> AegisRose
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(levelColor)
                    .padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${log.category} • ${log.level}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelColor,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.message,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
