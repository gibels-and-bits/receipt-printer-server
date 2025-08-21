package com.example.receipt.server.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class ConnectedClient(
    val id: String,
    val name: String,
    val ipAddress: String,
    val connectedAt: Long,
    val isActive: Boolean = true,
    val lastActivity: Long = System.currentTimeMillis()
)

@Composable
fun ConnectedClientsCard(
    clients: List<ConnectedClient>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF2A2A2A), Color(0xFF2A2A2A))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF667EEA).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🌐",
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Connected Clients",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE5E5E5)
                    )
                }
                
                // Active count badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFF10B981).copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "${clients.count { it.isActive }} active",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Clients list
            if (clients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PulsingDot()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Waiting for connections...",
                            fontSize = 14.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clients) { client ->
                        ClientItem(client = client)
                    }
                }
            }
        }
    }
}

@Composable
fun ClientItem(
    client: ConnectedClient
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F0F0F).copy(alpha = 0.5f),
        border = BorderStroke(
            width = 1.dp,
            color = if (client.isActive) {
                Color(0xFF10B981).copy(alpha = 0.3f)
            } else {
                Color(0xFF6B7280).copy(alpha = 0.2f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status indicator
                StatusIndicator(isActive = client.isActive)
                
                // Client info
                Column {
                    Text(
                        text = client.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE5E5E5)
                    )
                    Text(
                        text = client.ipAddress,
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
            
            // Connection time
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Connected",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
                Text(
                    text = dateFormat.format(Date(client.connectedAt)),
                    fontSize = 12.sp,
                    color = Color(0xFFE5E5E5)
                )
            }
        }
    }
}

@Composable
fun StatusIndicator(
    isActive: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.4f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier.size(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(8.dp)
                .alpha(if (isActive) alpha else 1f),
            shape = CircleShape,
            color = if (isActive) Color(0xFF10B981) else Color(0xFF6B7280)
        ) {}
    }
}

@Composable
fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size((16 * scale).dp)
                .alpha(alpha),
            shape = CircleShape,
            color = Color(0xFF667EEA).copy(alpha = 0.5f)
        ) {}
        Surface(
            modifier = Modifier.size(8.dp),
            shape = CircleShape,
            color = Color(0xFF667EEA)
        ) {}
    }
}