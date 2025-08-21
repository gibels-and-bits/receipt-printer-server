package com.example.receipt.server

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.receipt.server.ui.TeamManagementViewModel
import com.example.receipt.server.ui.screens.DashboardScreen
import com.example.receipt.server.ui.screens.TeamDetailScreen
import com.example.receipt.server.ui.theme.ReceiptPrinterServerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    // Server components
    private lateinit var interpreterManager: InterpreterManager
    private lateinit var printerManager: PrinterManager
    private lateinit var queueManager: QueueManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize server components
        interpreterManager = InterpreterManager()
        printerManager = PrinterManager()
        queueManager = QueueManager(maxConcurrent = 3)
        
        // Start the Ktor server in background
        CoroutineScope(Dispatchers.IO).launch {
            startKtorServer()
        }
        
        setContent {
            ReceiptPrinterServerTheme {
                ReceiptPrinterApp(
                    interpreterManager = interpreterManager,
                    printerManager = printerManager,
                    queueManager = queueManager
                )
            }
        }
    }
    
    private fun startKtorServer() {
        // Start the Ktor server on port 8080
        io.ktor.server.engine.embeddedServer(
            io.ktor.server.netty.Netty, 
            port = 8080,
            host = "0.0.0.0" // Listen on all interfaces
        ) {
            module()
        }.start(wait = false)
        
        println("Ktor server started on port 8080")
    }
}

@Composable
fun ReceiptPrinterApp(
    interpreterManager: InterpreterManager,
    printerManager: PrinterManager,
    queueManager: QueueManager
) {
    val navController = rememberNavController()
    val viewModel = remember {
        TeamManagementViewModel(
            interpreterManager = interpreterManager,
            printerManager = printerManager,
            queueManager = queueManager
        )
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = "dashboard"
        ) {
            composable("dashboard") {
                val dashboardState by viewModel.dashboardState.collectAsState()
                
                DashboardScreen(
                    teams = dashboardState.teams,
                    printerEnabledCount = dashboardState.printerEnabledCount,
                    queueStatus = dashboardState.queueStatus,
                    onTeamClick = { team ->
                        viewModel.loadTeamDetail(team.teamId)
                        navController.navigate("team/${team.teamId}")
                    },
                    onRefresh = {
                        viewModel.loadTeams()
                    }
                )
                
                // Show error if any
                dashboardState.error?.let { error ->
                    LaunchedEffect(error) {
                        // Show snackbar or dialog
                        viewModel.clearError()
                    }
                }
            }
            
            composable("team/{teamId}") { backStackEntry ->
                val teamDetailState by viewModel.teamDetailState.collectAsState()
                val teamId = backStackEntry.arguments?.getString("teamId") ?: return@composable
                
                LaunchedEffect(teamId) {
                    viewModel.loadTeamDetail(teamId)
                }
                
                teamDetailState.teamDetail?.let { teamDetail ->
                    TeamDetailScreen(
                        teamDetail = teamDetail,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onTogglePrinterAccess = { id ->
                            viewModel.togglePrinterAccess(id)
                        },
                        onTestPrint = { id, json ->
                            viewModel.testPrint(id, json)
                        }
                    )
                }
                
                // Show test result if any
                teamDetailState.lastTestResult?.let { result ->
                    LaunchedEffect(result) {
                        // Show snackbar with result
                        kotlinx.coroutines.delay(3000)
                        viewModel.clearTestResult()
                    }
                }
                
                // Show error if any
                teamDetailState.error?.let { error ->
                    LaunchedEffect(error) {
                        // Show snackbar or dialog
                        viewModel.clearError()
                    }
                }
            }
        }
    }
}