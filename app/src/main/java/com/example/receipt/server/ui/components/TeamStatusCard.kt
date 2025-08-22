package com.example.receipt.server.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receipt.server.CompilationStatus
import com.example.receipt.server.TeamStatsDisplay

@Composable
fun TeamStatusCard(
    team: TeamStatsDisplay,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = when (team.lastCompilationStatus) {
        CompilationStatus.SUCCESS -> Color(0xFF4CAF50)
        CompilationStatus.FAILED -> Color(0xFFF44336)
        CompilationStatus.UNKNOWN -> Color.Gray
    }
    
    val statusIcon = when (team.lastCompilationStatus) {
        CompilationStatus.SUCCESS -> Icons.Default.CheckCircle
        CompilationStatus.FAILED -> Icons.Default.Error
        CompilationStatus.UNKNOWN -> Icons.Default.Help
    }
    
    val statusColor = when (team.lastCompilationStatus) {
        CompilationStatus.SUCCESS -> Color(0xFF4CAF50)
        CompilationStatus.FAILED -> Color(0xFFF44336)
        CompilationStatus.UNKNOWN -> Color.Gray
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(4.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F2937)  // Dark card background
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Main content with team name and status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Icon
                Icon(
                    imageVector = statusIcon,
                    contentDescription = "Status",
                    tint = statusColor,
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Team Name
                Text(
                    text = team.teamName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Footer with stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111827))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID: ${team.teamId}",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Uploads: ${team.uploadCount}",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prints: ${team.printJobCount}",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }
}