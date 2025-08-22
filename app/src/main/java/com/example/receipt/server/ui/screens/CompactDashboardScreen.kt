package com.example.receipt.server.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receipt.server.PrintJob
import com.example.receipt.server.QueueStatus
import com.example.receipt.server.TeamStatsDisplay
import com.example.receipt.server.ui.components.PrintQueueView
import com.example.receipt.server.ui.components.TeamStatusCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactDashboardScreen(
    teams: List<TeamStatsDisplay>,
    printQueue: List<PrintJob>,
    currentJob: PrintJob?,
    queueStatus: QueueStatus,
    onTeamClick: (TeamStatsDisplay) -> Unit = {},
    onTestPrint: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Receipt Printer Server",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Statistics
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatItem(
                                label = "Teams",
                                value = teams.size.toString(),
                                color = Color(0xFF60A5FA)  // Light blue
                            )
                            StatItem(
                                label = "Queue",
                                value = "${queueStatus.currentSize}/${queueStatus.maxSize}",
                                color = if (queueStatus.isFull) Color(0xFFEF4444) else Color(0xFF10B981)  // Red/Green
                            )
                            StatItem(
                                label = "Uploads",
                                value = teams.sumOf { it.uploadCount }.toString(),
                                color = Color(0xFF8B5CF6)  // Purple
                            )
                            StatItem(
                                label = "Prints",
                                value = teams.sumOf { it.printJobCount }.toString(),
                                color = Color(0xFFF59E0B)  // Amber
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onTestPrint) {
                        Icon(Icons.Default.Print, "Test Print", tint = Color(0xFF9CA3AF))
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color(0xFF9CA3AF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1F2937),  // Dark gray for app bar
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF111827))  // Dark background
        ) {
            // Teams Grid (takes up available space)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (teams.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No teams registered yet",
                                fontSize = 18.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Teams will appear here when they upload code",
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 280.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(teams) { team ->
                            TeamStatusCard(
                                team = team,
                                onClick = { onTeamClick(team) }
                            )
                        }
                    }
                }
            }
            
            // Print Queue (fixed height at bottom)
            PrintQueueView(
                printQueue = printQueue,
                currentJob = currentJob,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 24.sp,  // Increased from 18sp
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,  // Increased from 11sp
            color = Color(0xFF9CA3AF)  // Lighter gray for dark mode
        )
    }
}