package com.example.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AegisAmber
import com.example.ui.theme.AegisCyanPrimary
import com.example.ui.theme.AegisEmerald
import com.example.ui.theme.AegisRose
import com.example.vpn.model.ConnectionState

@Composable
fun StatusBadge(state: ConnectionState, modifier: Modifier = Modifier) {
    val (color, text) = when (state) {
        ConnectionState.CONNECTED -> AegisEmerald to "SECURED"
        ConnectionState.CONNECTING -> AegisCyanPrimary to "CONNECTING"
        ConnectionState.RECONNECTING -> AegisAmber to "RECONNECTING"
        ConnectionState.DISCONNECTING -> AegisAmber to "DISCONNECTING"
        ConnectionState.FAILED -> AegisRose to "FAILED"
        ConnectionState.DISCONNECTED -> Color(0xFF64748B) to "DISCONNECTED"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun CryptoBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
