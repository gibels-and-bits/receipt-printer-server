package com.example.receipt.server.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receipt.server.PrintJob
import com.example.receipt.server.PrintJobStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintQueueView(
    printQueue: List<PrintJob>,
    currentJob: PrintJob?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F2937)  // Dark card background
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Print Queue",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Badge(
                    containerColor = if (printQueue.isEmpty()) Color(0xFF6B7280) else Color(0xFF60A5FA)
                ) {
                    Text(
                        text = "${printQueue.size}",
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Current Job
            currentJob?.let { job ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF374151)  // Darker blue-gray for current job
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    PrintJobItem(
                        job = job,
                        isCurrent = true,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (printQueue.isNotEmpty()) {
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Queue List
            if (printQueue.isEmpty() && currentJob == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No print jobs in queue",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(printQueue) { job ->
                        PrintJobItem(
                            job = job,
                            isCurrent = false
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrintJobItem(
    job: PrintJob,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position or Status
        if (isCurrent) {
            Badge(
                containerColor = Color(0xFF2196F3)
            ) {
                Text("NOW", fontSize = 10.sp)
            }
        } else {
            Text(
                text = "#${job.position}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.width(30.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Team Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = job.teamName,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${job.commands.size} commands • ${dateFormat.format(Date(job.timestamp))}",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
            )
        }
        
        // Status
        when (job.status) {
            PrintJobStatus.PRINTING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
            PrintJobStatus.QUEUED -> {
                Text(
                    text = "Waiting",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
            else -> {}
        }
    }
}