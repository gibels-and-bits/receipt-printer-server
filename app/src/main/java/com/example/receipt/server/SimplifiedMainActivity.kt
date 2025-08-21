package com.example.receipt.server

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receipt.server.ui.theme.ReceiptPrinterServerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SimplifiedMainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start the Ktor server in background
        CoroutineScope(Dispatchers.IO).launch {
            startSimpleServer()
        }
        
        setContent {
            ReceiptPrinterServerTheme {
                SimpleDashboard()
            }
        }
    }
    
    private fun startSimpleServer() {
        // Simple server stub - actual implementation will be added
        println("Server will start on port 8080")
    }
}

@Composable
fun SimpleDashboard() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Receipt Printer Server",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Server Status: Running")
                    Text("Port: 8080")
                    Text("Teams Connected: 0")
                }
            }
        }
    }
}