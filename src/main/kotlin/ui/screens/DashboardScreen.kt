package com.example.receipt.server.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TeamInfo(
    val teamId: String,
    val teamName: String,
    val hasInterpreter: Boolean = true,
    val printerEnabled: Boolean = false,
    val lastActivity: Long? = null,
    val printJobCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    teams: List<TeamInfo>,
    printerEnabledCount: Int,
    queueStatus: Pair<Int, Int>, // (used, total)
    onTeamClick: (TeamInfo) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Auto-refresh every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            onRefresh()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF667EEA),
                        Color(0xFF764BA2)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "🧾 Receipt Printer Admin",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = "Hackathon Team Management",
                        fontSize = 16.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = teams.size.toString(),
                    label = "Total Teams",
                    color = Color(0xFF667EEA)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = printerEnabledCount.toString(),
                    label = "Printer Access",
                    color = Color(0xFF10B981)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "${queueStatus.first}/${queueStatus.second}",
                    label = "Queue Status",
                    color = Color(0xFFF59E0B)
                )
            }
            
            // Teams Grid
            if (teams.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                ) {
                    Text(
                        text = "No teams have submitted yet",
                        fontSize = 18.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(teams) { team ->
                        TeamCard(
                            team = team,
                            onClick = { onTeamClick(team) }
                        )
                    }
                }
            }
        }
        
        // Refresh FAB
        FloatingActionButton(
            onClick = {
                scope.launch {
                    isRefreshing = true
                    onRefresh()
                    delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color.White,
            contentColor = Color(0xFF667EEA)
        ) {
            Text(
                text = if (isRefreshing) "↻" else "🔄",
                fontSize = 24.sp,
                modifier = if (isRefreshing) {
                    Modifier // Add rotation animation here if needed
                } else Modifier
            )
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCard(
    team: TeamInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (team.printerEnabled) {
                Color.White
            } else {
                Color.White.copy(alpha = 0.95f)
            }
        ),
        border = if (team.printerEnabled) {
            CardDefaults.outlinedCardBorder().copy(
                width = 3.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF10B981), Color(0xFF10B981))
                )
            )
        } else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Printer enabled badge
            if (team.printerEnabled) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981)
                ) {
                    Text(
                        text = "🖨️ ENABLED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = team.teamName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Text(
                        text = "ID: ${team.teamId}",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(
                        text = "Ready",
                        backgroundColor = Color(0xFFE0F2FE),
                        textColor = Color(0xFF0369A1)
                    )
                    if (team.printJobCount > 0) {
                        StatusChip(
                            text = "${team.printJobCount} prints",
                            backgroundColor = Color(0xFFF3E8FF),
                            textColor = Color(0xFF6B21A8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}