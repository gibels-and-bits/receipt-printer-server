package com.example.receipt.server

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receipt.server.printer.LibraryDiagnostics
import com.example.receipt.server.printer.PrinterFactory
import com.example.receipt.server.printer.RealEpsonPrinter
import com.example.receipt.server.printer.UsbPrinterHelper
import com.example.receipt.server.TeamStatsDisplay
import com.example.receipt.server.CompilationStatus
import com.example.receipt.server.ui.components.ConnectedClient
import com.example.receipt.server.ui.components.ConnectedClientsCard
import com.example.receipt.server.ui.components.InterpreterCodeDialog
import com.example.receipt.server.ui.components.PrinterError
import com.example.receipt.server.ui.components.PrinterErrorDialog
import com.example.receipt.server.ui.components.createPrinterError
import com.example.receipt.server.ui.screens.CompactDashboardScreen
import com.example.receipt.server.ui.screens.DashboardScreen
import com.example.receipt.server.ui.screens.TeamInfo
import com.example.receipt.server.ui.theme.ReceiptPrinterServerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SimplifiedMainActivity : ComponentActivity() {
    
    private lateinit var printerManager: PrinterManager
    private lateinit var serverDataManager: ServerDataManager
    private var ktorServer: KtorServer? = null
    private var printer: EpsonPrinter? = null
    private var isRealPrinter = false
    private var printerInitError: Exception? = null
    private var usbHelper: UsbPrinterHelper? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize managers
        printerManager = PrinterManager(this)
        serverDataManager = ServerDataManager()
        
        // Initialize USB helper
        usbHelper = UsbPrinterHelper(this)
        
        // Check for USB printers
        Log.i("MainActivity", "Checking for USB printers...")
        val usbPrinter = usbHelper?.checkForEpsonPrinter()
        if (usbPrinter != null) {
            Log.i("MainActivity", "Found USB printer: ${usbPrinter.deviceName}")
        } else {
            Log.w("MainActivity", "No USB printer found or no permission")
        }
        
        // Discover USB printers using Epson SDK
        usbHelper?.discoverUsbPrinters { devices ->
            Log.i("MainActivity", "Discovered ${devices.size} USB printers via SDK")
            devices.forEach { device ->
                Log.i("MainActivity", "  - $device")
            }
        }
        
        // Run diagnostics
        Log.i("MainActivity", "Running library diagnostics...")
        val diagnostics = LibraryDiagnostics.runFullDiagnostics(this)
        
        // Initialize printer using factory with detailed diagnostics
        val printerResult = PrinterFactory.createPrinter(this)
        printer = printerResult.printer
        isRealPrinter = printerResult.isReal
        
        if (printerResult.error != null) {
            Log.e("MainActivity", "Printer initialization issue: ${printerResult.error.message}")
            printerResult.error.details.forEach { detail ->
                Log.e("MainActivity", "  - $detail")
            }
            
            // Create a detailed exception for the error dialog including diagnostics
            printerInitError = RuntimeException(
                "${printerResult.error.message}\n\nDetails:\n${printerResult.error.details.joinToString("\n")}\n\nDIAGNOSTICS:\n$diagnostics"
            )
        } else {
            Log.i("MainActivity", "Printer initialized successfully (Real: $isRealPrinter)")
        }
        
        // Start the Ktor server in background
        CoroutineScope(Dispatchers.IO).launch {
            startProductionServer()
        }
        
        setContent {
            ReceiptPrinterServerTheme {
                ProductionDashboard(
                    serverDataManager = serverDataManager,
                    onTestPrint = { performTestPrint() },
                    printerInitError = printerInitError
                )
            }
        }
    }
    
    private suspend fun startProductionServer() {
        try {
            ktorServer = KtorServer(
                context = this,
                serverDataManager = serverDataManager,
                printerManager = printerManager,
                port = 8080
            )
            ktorServer?.start()
            Log.i("MainActivity", "Production server started on port 8080")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start server", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@SimplifiedMainActivity,
                    "Server failed to start: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun performTestPrint() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (printer == null) {
                    // Show error if printer is not initialized
                    withContext(Dispatchers.Main) {
                        // Error will be shown in UI
                    }
                    return@launch
                }
                
                printer?.let { printer ->
                    // Add header
                    printer.addTextAlign(com.example.receipt.server.Alignment.CENTER)
                    printer.addTextStyle(TextStyle(
                        size = TextSize.XLARGE,
                        bold = true
                    ))
                    printer.addText("\n🧾 TEST RECEIPT\n")
                    
                    // Add separator
                    printer.addTextStyle(TextStyle(
                        size = TextSize.NORMAL,
                        bold = false
                    ))
                    printer.addText("================================\n\n")
                    
                    // Add content
                    printer.addTextAlign(com.example.receipt.server.Alignment.LEFT)
                    printer.addText("Hello World from Epson Printer!\n\n")
                    
                    // Add timestamp
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    printer.addText("Printed at: ${dateFormat.format(Date())}\n\n")
                    
                    // Add system info
                    printer.addTextStyle(TextStyle(
                        size = TextSize.SMALL,
                        bold = false
                    ))
                    printer.addText("Receipt Printer Server v1.0\n")
                    printer.addText("Hackathon Edition\n\n")
                    
                    // Add QR code
                    printer.addTextAlign(com.example.receipt.server.Alignment.CENTER)
                    printer.addQRCode(
                        "https://github.com/epson",
                        QRCodeOptions(size = 5, errorCorrection = QRErrorCorrection.M)
                    )
                    printer.addText("\nScan for Documentation\n\n")
                    
                    // Add footer
                    printer.addTextStyle(TextStyle(
                        size = TextSize.NORMAL,
                        bold = false
                    ))
                    printer.addText("================================\n")
                    printer.addTextStyle(TextStyle(
                        size = TextSize.LARGE,
                        bold = true
                    ))
                    printer.addText("Thank You!\n")
                    
                    // Feed and cut
                    printer.addFeedLine(3)
                    printer.cutPaper()
                    
                    withContext(Dispatchers.Main) {
                        val message = if (isRealPrinter) {
                            "Test receipt printed successfully!"
                        } else {
                            "Test receipt sent to mock printer (check logs)"
                        }
                        Toast.makeText(
                            this@SimplifiedMainActivity,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } ?: run {
                    // Printer is null - error already captured
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Test print failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SimplifiedMainActivity,
                        "Print failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Stop server
        ktorServer?.stop()
        
        // Cleanup USB helper
        usbHelper?.cleanup()
        
        // Disconnect printer
        if (isRealPrinter && printer is RealEpsonPrinter) {
            (printer as RealEpsonPrinter).disconnect()
        }
    }
}

@Composable
fun ProductionDashboard(
    serverDataManager: ServerDataManager,
    onTestPrint: () -> Unit,
    printerInitError: Exception?
) {
    // Collect real data from server manager
    val teamStats by serverDataManager.teamStatsFlow.collectAsStateWithLifecycle()
    val printQueue by serverDataManager.printQueueFlow.collectAsStateWithLifecycle()
    val queueStatus by serverDataManager.queueStatusFlow.collectAsStateWithLifecycle()
    val currentJob by serverDataManager.currentJobFlow.collectAsStateWithLifecycle()
    
    // Error dialog state
    var showPrinterError by remember { mutableStateOf(false) }
    var currentError by remember { mutableStateOf<PrinterError?>(null) }
    
    // Interpreter code dialog state
    var showInterpreterDialog by remember { mutableStateOf(false) }
    var selectedTeam by remember { mutableStateOf<TeamStatsDisplay?>(null) }
    
    // Show initial printer error if exists
    LaunchedEffect(printerInitError) {
        printerInitError?.let {
            currentError = createPrinterError(it)
            showPrinterError = true
        }
    }
    
    // Use the new compact dashboard
    CompactDashboardScreen(
        teams = teamStats,
        printQueue = printQueue,
        currentJob = currentJob,
        queueStatus = queueStatus,
        onTeamClick = { team ->
            // Show team details or interpreter code
            selectedTeam = team
            showInterpreterDialog = true
            Log.d("Dashboard", "Team clicked: ${team.teamName}")
        },
        onTestPrint = {
            // Check if printer is available before attempting print
            if (printerInitError != null) {
                currentError = createPrinterError(printerInitError)
                showPrinterError = true
            } else {
                onTestPrint()
            }
        },
        onRefresh = {
            // In production, this would refresh from the server
            Log.d("Dashboard", "Refreshing dashboard data...")
        }
    )
    
    // Show printer error dialog
    if (showPrinterError && currentError != null) {
        PrinterErrorDialog(
            error = currentError!!,
            onDismiss = { showPrinterError = false },
            onRetry = {
                // Retry printer initialization
                // In production, this would attempt to reinitialize the printer
                Log.d("Dashboard", "Retrying printer initialization...")
            }
        )
    }
    
    // Show interpreter code dialog
    if (showInterpreterDialog && selectedTeam != null) {
        val statusMessage = when(selectedTeam!!.lastCompilationStatus) {
            CompilationStatus.SUCCESS -> "✅ Last compilation successful"
            CompilationStatus.FAILED -> "❌ Last compilation failed: ${selectedTeam!!.lastCompilationError}"
            CompilationStatus.UNKNOWN -> "⚠️ No compilation attempts yet"
        }
        InterpreterCodeDialog(
            teamName = selectedTeam!!.teamName,
            teamId = selectedTeam!!.teamId,
            interpreterCode = statusMessage,
            onDismiss = { 
                showInterpreterDialog = false
                selectedTeam = null
            }
        )
    }
}