package com.example.receipt.server.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    onTeamLongClick: (TeamInfo) -> Unit = {},
    onDeleteTeam: (TeamInfo) -> Unit = {},
    onRefresh: () -> Unit,
    onTestPrint: () -> Unit,
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
            .background(Color(0xFF0F0F0F)) // Dark background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Enhanced Header with Test Print Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color(0xFF667EEA).copy(alpha = 0.3f),
                        spotColor = Color(0xFF667EEA).copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A1A)
                )
            ) {
                Box {
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF667EEA).copy(alpha = 0.15f),
                                        Color(0xFF764BA2).copy(alpha = 0.15f)
                                    )
                                )
                            )
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Animated icon
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF667EEA),
                                                    Color(0xFF764BA2)
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🧾",
                                        fontSize = 28.sp
                                    )
                                }
                                
                                Column {
                                    Text(
                                        text = "Receipt Printer",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFE5E5E5)
                                    )
                                    Text(
                                        text = "ADMIN DASHBOARD",
                                        fontSize = 18.sp,
                                        color = Color(0xFF9CA3AF),
                                        letterSpacing = 2.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        // Enhanced Test Print Button
                        ElevatedButton(
                            onClick = onTestPrint,
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(64.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = Color(0xFF10B981).copy(alpha = 0.4f),
                                    spotColor = Color(0xFF10B981).copy(alpha = 0.4f)
                                ),
                            elevation = ButtonDefaults.elevatedButtonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = "Print",
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Test Print",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Enhanced Stats Row with animations
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedStatCard(
                    modifier = Modifier.weight(1f),
                    value = teams.size.toString(),
                    label = "Total Teams",
                    icon = Icons.Default.Groups,
                    accentColor = Color(0xFF667EEA),
                    delay = 0
                )
                AnimatedStatCard(
                    modifier = Modifier.weight(1f),
                    value = printerEnabledCount.toString(),
                    label = "Printer Access",
                    icon = Icons.Default.Print,
                    accentColor = Color(0xFF10B981),
                    delay = 100
                )
                AnimatedStatCard(
                    modifier = Modifier.weight(1f),
                    value = "${queueStatus.first}/${queueStatus.second}",
                    label = "Queue Status",
                    icon = Icons.Default.Queue,
                    accentColor = Color(0xFFF59E0B),
                    delay = 200
                )
            }
            
            // Teams Grid
            if (teams.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(vertical = 32.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2A2A2A).copy(alpha = 0.5f),
                                Color(0xFF3A3A3A).copy(alpha = 0.5f)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF667EEA).copy(alpha = 0.05f),
                                        Color.Transparent
                                    ),
                                    radius = 600f
                                )
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = "No teams",
                            modifier = Modifier
                                .size(64.dp)
                                .padding(bottom = 16.dp),
                            tint = Color(0xFF667EEA).copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Waiting for Teams",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Teams will appear here once they submit",
                            fontSize = 18.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(teams) { team ->
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        
                        DarkTeamCard(
                            team = team,
                            onClick = { onTeamClick(team) },
                            onLongClick = { onTeamLongClick(team) },
                            onDelete = { showDeleteDialog = true }
                        )
                        
                        // Delete confirmation dialog
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = {
                                    Text(
                                        "Delete Team?",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE5E5E5)
                                    )
                                },
                                text = {
                                    Column {
                                        Text(
                                            "Are you sure you want to delete this team?",
                                            fontSize = 16.sp,
                                            color = Color(0xFF9CA3AF)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFF0F0F0F).copy(alpha = 0.5f)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Text(
                                                    text = team.teamName,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFE5E5E5)
                                                )
                                                Text(
                                                    text = "Team ID: ${team.teamId}",
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF9CA3AF)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "⚠️ This will remove the team's interpreter code and all associated data.",
                                            fontSize = 14.sp,
                                            color = Color(0xFFF59E0B)
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            onDeleteTeam(team)
                                            showDeleteDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEF4444)
                                        )
                                    ) {
                                        Text("Delete", fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showDeleteDialog = false }
                                    ) {
                                        Text("Cancel", color = Color(0xFF9CA3AF))
                                    }
                                },
                                containerColor = Color(0xFF1A1A1A),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Enhanced Refresh FAB with animation
        val rotation by animateFloatAsState(
            targetValue = if (isRefreshing) 360f else 0f,
            animationSpec = if (isRefreshing) {
                infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            } else {
                tween(0)
            }
        )
        
        LargeFloatingActionButton(
            onClick = {
                scope.launch {
                    isRefreshing = true
                    onRefresh()
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .size(80.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFF667EEA).copy(alpha = 0.4f),
                    spotColor = Color(0xFF667EEA).copy(alpha = 0.4f)
                ),
            containerColor = Color(0xFF667EEA),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Refresh",
                modifier = Modifier
                    .size(36.dp)
                    .rotate(rotation)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedStatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    delay: Int = 0,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + 
                slideInVertically(animationSpec = tween(500)) +
                scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = accentColor.copy(alpha = 0.2f),
                    spotColor = accentColor.copy(alpha = 0.2f)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Gradient background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.2f),
                                    Color.Transparent
                                ),
                                radius = 400f
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(bottom = 12.dp),
                        tint = accentColor.copy(alpha = 0.8f)
                    )
                    
                    AnimatedContent(
                        targetState = value,
                        transitionSpec = {
                            slideInVertically { it } + fadeIn() with
                            slideOutVertically { -it } + fadeOut()
                        }
                    ) { targetValue ->
                        Text(
                            text = targetValue,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                    }
                    
                    Text(
                        text = label.uppercase(),
                        fontSize = 16.sp,
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DarkTeamCard(
    team: TeamInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .scale(scale)
            .combinedClickable(
                onClick = {
                    isHovered = false
                    onClick()
                },
                onLongClick = {
                    onLongClick()
                }
            )
            .shadow(
                elevation = if (team.printerEnabled) 12.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (team.printerEnabled) 
                    Color(0xFF10B981).copy(alpha = 0.3f) 
                else 
                    Color.Black.copy(alpha = 0.2f),
                spotColor = if (team.printerEnabled) 
                    Color(0xFF10B981).copy(alpha = 0.3f) 
                else 
                    Color.Black.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        border = if (team.printerEnabled) {
            CardDefaults.outlinedCardBorder().copy(
                width = 3.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF10B981),
                        Color(0xFF059669)
                    )
                )
            )
        } else {
            CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2A2A2A), Color(0xFF2A2A2A))
                )
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (team.printerEnabled) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF10B981).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF667EEA).copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    }
                )
        ) {
            // Top badges and controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Delete button on the left
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Team",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Printer enabled badge on the right
                if (team.printerEnabled) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Enabled",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Text(
                                text = "PRINTER ON",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = team.teamName,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFE5E5E5),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Tag,
                                    contentDescription = "ID",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFF9CA3AF)
                                )
                                Text(
                                    text = team.teamId,
                                    fontSize = 16.sp,
                                    color = Color(0xFF9CA3AF),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // Activity indicator
                        if (team.lastActivity != null) {
                            val minutesAgo = ((System.currentTimeMillis() - team.lastActivity) / 60000).toInt()
                            val isRecent = minutesAgo < 5
                            
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = if (isRecent) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DarkStatusChip(
                        text = "Connected",
                        backgroundColor = Color(0xFF065F46).copy(alpha = 0.3f),
                        textColor = Color(0xFF10B981)
                    )
                    if (team.printJobCount > 0) {
                        DarkStatusChip(
                            text = "${team.printJobCount} prints",
                            backgroundColor = Color(0xFF4C1D95).copy(alpha = 0.3f),
                            textColor = Color(0xFFA78BFA)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DarkStatusChip(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(
            width = 1.dp,
            color = textColor.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}