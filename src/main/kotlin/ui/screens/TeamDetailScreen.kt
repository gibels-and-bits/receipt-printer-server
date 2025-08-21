package com.example.receipt.server.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class TeamDetail(
    val teamId: String,
    val teamName: String,
    val interpreterCode: String,
    val printerEnabled: Boolean,
    val lastActivity: Long? = null,
    val printJobCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    teamDetail: TeamDetail,
    onBackClick: () -> Unit,
    onTogglePrinterAccess: (String) -> Unit,
    onTestPrint: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var showTestDialog by remember { mutableStateOf(false) }
    var testJson by remember { mutableStateOf(
        """
        {
          "elements": [
            {
              "type": "text",
              "content": "Test Receipt - ${teamDetail.teamName}",
              "align": "center",
              "size": "large"
            },
            {
              "type": "feed",
              "lines": 2
            },
            {
              "type": "text",
              "content": "Test print successful!",
              "align": "center"
            },
            {
              "type": "feed",
              "lines": 3
            }
          ]
        }
        """.trimIndent()
    ) }
    
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Top Bar with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF667EEA)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Team Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            // Team Info Card
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
                        text = teamDetail.teamName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    
                    Text(
                        text = "Team ID: ${teamDetail.teamId}",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                    
                    // Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { 
                                scope.launch {
                                    onTogglePrinterAccess(teamDetail.teamId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (teamDetail.printerEnabled) 
                                    Color(0xFFEF4444) else Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (teamDetail.printerEnabled) 
                                    "Disable Printer" else "Enable Printer",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Button(
                            onClick = { showTestDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B82F6)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Test Print",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            // Status Card
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
                        text = "Team Status",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatusItem(
                            label = "Printer Access",
                            value = if (teamDetail.printerEnabled) "✅ Enabled" else "❌ Disabled"
                        )
                        StatusItem(
                            label = "Print Jobs",
                            value = teamDetail.printJobCount.toString()
                        )
                        StatusItem(
                            label = "Last Activity",
                            value = teamDetail.lastActivity?.let {
                                val minutes = (System.currentTimeMillis() - it) / 60000
                                if (minutes < 1) "Just now" 
                                else if (minutes < 60) "$minutes min ago"
                                else "${minutes / 60} hours ago"
                            } ?: "N/A"
                        )
                    }
                }
            }
            
            // Code Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Interpreter Code",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        SelectionContainer {
                            Text(
                                text = teamDetail.interpreterCode,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .padding(16.dp)
                                    .horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Test Print Dialog
    if (showTestDialog) {
        AlertDialog(
            onDismissRequest = { showTestDialog = false },
            title = { 
                Text(
                    text = "Test Print",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter JSON to test:",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = testJson,
                        onValueChange = { testJson = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            onTestPrint(teamDetail.teamId, testJson)
                            showTestDialog = false
                        }
                    }
                ) {
                    Text("Execute Test")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatusItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937)
        )
    }
}