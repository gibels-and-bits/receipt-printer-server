package com.example.receipt.server

import android.content.Context
import android.util.Log
import com.example.receipt.server.models.PrintJob
import com.example.receipt.server.models.Submission
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Production Ktor server for receipt printer
 */
class KtorServer(
    private val context: Context,
    private val serverDataManager: ServerDataManager,
    private val printerManager: PrinterManager,
    private val port: Int = 8080
) {
    companion object {
        private const val TAG = "KtorServer"
    }
    
    private var server: ApplicationEngine? = null
    
    fun start() {
        try {
            server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                configureServer()
            }.start(wait = false)
            
            Log.i(TAG, "Server started on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server", e)
            throw e
        }
    }
    
    fun stop() {
        server?.stop(1000, 2000)
        Log.i(TAG, "Server stopped")
    }
    
    private fun Application.configureServer() {
        // Install plugins
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowHeader("*")
            anyHost() // Allow any host for hackathon
            allowCredentials = true
            allowNonSimpleContentTypes = true
        }
        
        // Configure routes
        routing {
            // Root endpoint - API info
            get("/") {
                call.respond(HttpStatusCode.OK, RootResponse(
                    service = "Receipt Printer Server",
                    version = "1.0.0",
                    status = "running",
                    endpoints = listOf(
                        "/health",
                        "/api/submit",
                        "/api/print",
                        "/api/admin/teams",
                        "/api/admin/clients",
                        "/api/admin/statistics"
                    )
                ))
            }
            
            // Health check
            get("/health") {
                call.respond(HttpStatusCode.OK, HealthResponse(
                    status = "healthy",
                    timestamp = System.currentTimeMillis()
                ))
            }
            
            // Team submission endpoint
            /* OLD SUBMIT ENDPOINT - COMMENTED OUT
            post("/api/submit") {
                Log.i(TAG, "Received submission request")
                try {
                    val rawBody = try {
                        call.receiveText()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to receive submission body: ${e.message}")
                        ""
                    }
                    
                    Log.d(TAG, "Raw submission body: ${rawBody.take(500)}") // Log first 500 chars
                    
                    if (rawBody.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, SubmissionResponse(
                            success = false,
                            message = "Empty submission body"
                        ))
                        return@post
                    }
                    
                    // Use lenient JSON parser
                    val json = Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        coerceInputValues = true
                    }
                    
                    val submission = try {
                        json.decodeFromString<Submission>(rawBody)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse submission: ${e.message}")
                        // Try to provide helpful error message
                        val missingFields = mutableListOf<String>()
                        if (!rawBody.contains("team_id")) missingFields.add("team_id")
                        if (!rawBody.contains("teamName")) missingFields.add("teamName")
                        if (!rawBody.contains("interpreterCode")) missingFields.add("interpreterCode")
                        
                        val errorMsg = if (missingFields.isNotEmpty()) {
                            "Missing required fields: ${missingFields.joinToString(", ")}"
                        } else {
                            "Invalid JSON format: ${e.message?.take(100)}"
                        }
                        
                        call.respond(HttpStatusCode.BadRequest, SubmissionResponse(
                            success = false,
                            message = errorMsg
                        ))
                        return@post
                    }
                    
                    // Validate submission data
                    if (submission.team_id.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, SubmissionResponse(
                            success = false,
                            message = "Team ID cannot be empty"
                        ))
                        return@post
                    }
                    
                    if (submission.teamName.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, SubmissionResponse(
                            success = false,
                            message = "Team name cannot be empty"
                        ))
                        return@post
                    }
                    
                    Log.i(TAG, "Parsed submission: teamId=${submission.team_id}, teamName=${submission.teamName}")
                    
                    // Register team in data manager
                    serverDataManager.registerTeam(submission)
                    
                    // Register client connection
                    val clientIp = try {
                        call.request.local.remoteHost
                    } catch (e: Exception) {
                        "unknown"
                    }
                    serverDataManager.registerClient(clientIp, submission.teamName)
                    
                    val response = SubmissionResponse(
                        success = true,
                        message = "Submission received successfully for ${submission.teamName}",
                        teamId = submission.team_id
                    )
                    
                    Log.i(TAG, "Submission successful for team: ${submission.teamName} (${submission.team_id})")
                    call.respond(HttpStatusCode.OK, response)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error processing submission", e)
                    call.respond(HttpStatusCode.InternalServerError, SubmissionResponse(
                        success = false,
                        message = "Server error: ${e.message?.take(100) ?: "Unknown error"}"
                    ))
                }
            }
            END OF OLD SUBMIT ENDPOINT */
            
            /* COMMENTED OUT - OLD PRINT ENDPOINTS
            // Print job submission with teamId in path
            post("/api/print/{teamId}") {
                try {
                    val teamId = call.parameters["teamId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest, 
                        PrintResponse(success = false, message = "Team ID required")
                    )
                    
                    // Log raw request for debugging
                    val rawBody = try {
                        call.receiveText()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to receive request body: ${e.message}")
                        ""
                    }
                    
                    Log.i(TAG, "Raw print request body: $rawBody")
                    
                    // Defensive JSON parsing - handle various formats
                    val content = when {
                        rawBody.isBlank() -> {
                            Log.w(TAG, "Received blank print request")
                            "Empty print request"
                        }
                        else -> {
                            try {
                                // Create lenient JSON parser
                                val json = Json {
                                    ignoreUnknownKeys = true
                                    isLenient = true
                                    coerceInputValues = true
                                }
                                
                                when {
                                    // Check for elements format: {"elements": [...]}
                                    rawBody.contains("\"elements\"") -> {
                                        try {
                                            val parsed = json.decodeFromString<PrintElementsRequest>(rawBody)
                                            when {
                                                parsed.elements.isEmpty() -> {
                                                    Log.w(TAG, "Received empty elements array - printing test receipt")
                                                    // Print a test receipt for empty requests
                                                    """
                                                    ================================
                                                    TEST RECEIPT
                                                    Team: $teamId
                                                    Time: ${java.time.LocalDateTime.now()}
                                                    ================================
                                                    No data provided
                                                    ================================
                                                    """.trimIndent()
                                                }
                                                else -> {
                                                    // Filter out blank entries and join
                                                    parsed.elements
                                                        .filterNot { it.isBlank() }
                                                        .joinToString("\n")
                                                        .ifBlank { "Empty print request" }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to parse elements format: ${e.message}")
                                            // Try to extract elements manually as fallback
                                            "Print data parsing error"
                                        }
                                    }
                                    // Check for content format: {"content": "..."}
                                    rawBody.contains("\"content\"") -> {
                                        try {
                                            val parsed = json.decodeFromString<PrintContentRequest>(rawBody)
                                            parsed.content.ifBlank { "Empty print request" }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to parse content format: ${e.message}")
                                            "Print data parsing error"
                                        }
                                    }
                                    // Check if it's a plain array
                                    rawBody.trim().startsWith("[") -> {
                                        try {
                                            val array = json.decodeFromString<List<String>>(rawBody)
                                            when {
                                                array.isEmpty() -> "Empty print request"
                                                else -> array
                                                    .filterNot { it.isBlank() }
                                                    .joinToString("\n")
                                                    .ifBlank { "Empty print request" }
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to parse array format: ${e.message}")
                                            "Print data parsing error"
                                        }
                                    }
                                    // Try as plain text (not JSON)
                                    !rawBody.trim().startsWith("{") && !rawBody.trim().startsWith("[") -> {
                                        Log.i(TAG, "Using raw body as plain text")
                                        rawBody
                                    }
                                    // Last resort - try as quoted string
                                    else -> {
                                        try {
                                            json.decodeFromString<String>(rawBody)
                                        } catch (e: Exception) {
                                            Log.w(TAG, "Could not parse as any known format, using raw")
                                            rawBody
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Unexpected error parsing print data: ${e.message}")
                                "Print data error: ${e.message}"
                            }
                        }
                    }
                    
                    // Check if team has printer access
                    val team = serverDataManager.teamsFlow.value.find { it.teamId == teamId }
                    
                    if (team == null) {
                        call.respond(HttpStatusCode.NotFound, PrintResponse(
                            success = false,
                            message = "Team not found"
                        ))
                        return@post
                    }
                    
                    if (!team.printerEnabled) {
                        call.respond(HttpStatusCode.Forbidden, PrintResponse(
                            success = false,
                            message = "Printer access not enabled for this team"
                        ))
                        return@post
                    }
                    
                    // Add print job
                    val jobId = serverDataManager.addPrintJob(teamId, content)
                    
                    // Execute print job
                    val printer = if (printerManager.isRealPrintEnabled(teamId)) {
                        printerManager.getRealPrinter()
                    } else {
                        printerManager.getMockPrinter()
                    }
                    
                    try {
                        // Get the team's interpreter code
                        val interpreterCode = serverDataManager.getInterpreterCode(teamId)
                        
                        if (interpreterCode != null && interpreterCode.isNotBlank()) {
                            // Execute the interpreter with the provided content
                            executeInterpreter(printer, content, interpreterCode)
                        } else {
                            // No interpreter - just print the content directly
                            Log.w(TAG, "No interpreter for team $teamId, printing content directly")
                            executePrintCommands(printer, content)
                        }
                        
                        serverDataManager.completePrintJob(jobId, true)
                        
                        call.respond(HttpStatusCode.OK, PrintResponse(
                            success = true,
                            message = "Print job completed",
                            jobId = jobId
                        ))
                    } catch (e: Exception) {
                        serverDataManager.completePrintJob(jobId, false)
                        throw e
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing print job", e)
                    call.respond(HttpStatusCode.InternalServerError, PrintResponse(
                        success = false,
                        message = "Print failed: ${e.message}"
                    ))
                }
            }
            
            // Print job submission (legacy endpoint with JSON body)
            post("/api/print") {
                try {
                    val printRequest = call.receive<PrintRequest>()
                    
                    // Check if team has printer access
                    val team = serverDataManager.teamsFlow.value.find { it.teamId == printRequest.teamId }
                    
                    if (team == null) {
                        call.respond(HttpStatusCode.NotFound, PrintResponse(
                            success = false,
                            message = "Team not found"
                        ))
                        return@post
                    }
                    
                    if (!team.printerEnabled) {
                        call.respond(HttpStatusCode.Forbidden, PrintResponse(
                            success = false,
                            message = "Printer access not enabled for this team"
                        ))
                        return@post
                    }
                    
                    // Add print job
                    val jobId = serverDataManager.addPrintJob(printRequest.teamId, printRequest.content)
                    
                    // Execute print job
                    val printer = if (printerManager.isRealPrintEnabled(printRequest.teamId)) {
                        printerManager.getRealPrinter()
                    } else {
                        printerManager.getMockPrinter()
                    }
                    
                    try {
                        // Get the team's interpreter code
                        val interpreterCode = serverDataManager.getInterpreterCode(printRequest.teamId)
                        
                        if (interpreterCode != null && interpreterCode.isNotBlank()) {
                            // Execute the interpreter with the provided content
                            executeInterpreter(printer, printRequest.content, interpreterCode)
                        } else {
                            // No interpreter - just print the content directly
                            Log.w(TAG, "No interpreter for team ${printRequest.teamId}, printing content directly")
                            executePrintCommands(printer, printRequest.content)
                        }
                        
                        serverDataManager.completePrintJob(jobId, true)
                        
                        call.respond(HttpStatusCode.OK, PrintResponse(
                            success = true,
                            message = "Print job completed",
                            jobId = jobId
                        ))
                    } catch (e: Exception) {
                        serverDataManager.completePrintJob(jobId, false)
                        throw e
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing print job", e)
                    call.respond(HttpStatusCode.InternalServerError, PrintResponse(
                        success = false,
                        message = "Print failed: ${e.message}"
                    ))
                }
            }
            END OF OLD PRINT ENDPOINTS */
            
            // Admin endpoints
            /* Commented out - old API
            get("/api/admin/teams") {
                call.respond(serverDataManager.teamsFlow.value)
            }*/
            
            /* get("/api/admin/clients") {
                call.respond(serverDataManager.clientsFlow.value)
            } */
            
            get("/api/admin/statistics") {
                call.respond(serverDataManager.getStatistics())
            }
            
            /* post("/api/admin/team/{teamId}/printer/enable") {
                val teamId = call.parameters["teamId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                serverDataManager.enablePrinterForTeam(teamId)
                printerManager.enableRealPrint(teamId)
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } */
            
            /* post("/api/admin/team/{teamId}/printer/disable") {
                val teamId = call.parameters["teamId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                serverDataManager.disablePrinterForTeam(teamId)
                printerManager.disableRealPrint(teamId)
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } */
            
            /* // Delete specific team
            delete("/api/admin/team/{teamId}") {
                val teamId = call.parameters["teamId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val deleted = serverDataManager.deleteTeam(teamId)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "Team deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "message" to "Team not found"))
                }
            } */
            
            // Clear all data (for testing)
            delete("/api/admin/clear") {
                serverDataManager.clearAllData()
                call.respond(HttpStatusCode.OK, SimpleResponse(
                    success = true,
                    message = "All data cleared"
                ))
            }
            
            // Register upload endpoint (notifies Android server of uploads)
            post("/api/register-upload") {
                try {
                    val request = call.receive<RegisterUploadRequest>()
                    
                    // Register the upload with compilation status
                    serverDataManager.registerUpload(
                        request.team_id, 
                        request.team_name,
                        request.compilation_status,
                        request.error_message
                    )
                    
                    call.respond(HttpStatusCode.OK, SimpleResponse(
                        success = true,
                        message = "Upload registered for team: ${request.team_name}"
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "Error registering upload", e)
                    call.respond(HttpStatusCode.InternalServerError, SimpleResponse(
                        success = false,
                        message = "Failed to register upload: ${e.message}"
                    ))
                }
            }
            
            // New endpoint for pre-compiled printer commands from compilation server
            post("/api/print-commands") {
                try {
                    val request = call.receive<PrintCommandsRequest>()
                    
                    // Extract team name (use team_id as fallback)
                    val teamName = request.team_name ?: request.team_id
                    
                    // Add to print queue
                    val result = serverDataManager.addPrintJob(
                        teamId = request.team_id,
                        teamName = teamName,
                        commands = request.commands
                    )
                    
                    when (result) {
                        is PrintJobResult.Success -> {
                            // Process queue if no current job
                            if (serverDataManager.currentJobFlow.value == null) {
                                processNextPrintJob()
                            }
                            
                            call.respond(HttpStatusCode.OK, PrintResponse(
                                success = true,
                                message = "Print job queued at position ${result.queuePosition}",
                                jobId = result.jobId
                            ))
                        }
                        is PrintJobResult.QueueFull -> {
                            call.respond(HttpStatusCode.ServiceUnavailable, PrintResponse(
                                success = false,
                                message = "Print queue is full. Please try again later."
                            ))
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing compiled print commands", e)
                    call.respond(HttpStatusCode.InternalServerError, PrintResponse(
                        success = false,
                        message = "Failed to process print commands: ${e.message}"
                    ))
                }
            }
        }
    }
    
    private fun processNextPrintJob() {
        val job = serverDataManager.getNextJob()
        if (job != null) {
            // Execute in background
            GlobalScope.launch {
                try {
                    val printer = printerManager.getMockPrinter() // Always use mock for now
                    executeCompiledCommands(printer, job.commands)
                    serverDataManager.completeCurrentJob(true)
                    
                    // Process next job if available
                    processNextPrintJob()
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing print job ${job.id}", e)
                    serverDataManager.completeCurrentJob(false)
                    
                    // Still process next job even if this one failed
                    processNextPrintJob()
                }
            }
        }
    }
    
    private fun executeCompiledCommands(printer: EpsonPrinter, commands: List<PrinterCommand>) {
        Log.i(TAG, "Executing ${commands.size} compiled printer commands")
        
        for (command in commands) {
            try {
                Log.d(TAG, "Executing command: ${command.type} with params: ${command.params}")
                
                when (command.type.uppercase()) {
                    "ADDTEXT", "ADD_TEXT" -> {
                        // Try direct text field first, then params
                        val text = command.text ?: 
                                  command.params?.get("text")?.toString()?.trim('"')
                        text?.let { 
                            Log.d(TAG, "Adding text: $it")
                            printer.addText(it) 
                        }
                    }
                    "ADDTEXTSTYLE", "ADD_TEXT_STYLE" -> {
                        val style = com.example.receipt.server.TextStyle(
                            bold = command.bold ?: false,
                            underline = command.underline ?: false,
                            size = when (command.size) {
                                "SMALL" -> com.example.receipt.server.TextSize.SMALL
                                "LARGE" -> com.example.receipt.server.TextSize.LARGE
                                "XLARGE" -> com.example.receipt.server.TextSize.XLARGE
                                else -> com.example.receipt.server.TextSize.NORMAL
                            }
                        )
                        printer.addTextStyle(style)
                    }
                    "ADDTEXTALIGN", "ADD_TEXT_ALIGN" -> {
                        val alignment = when (command.alignment) {
                            "CENTER" -> com.example.receipt.server.Alignment.CENTER
                            "RIGHT" -> com.example.receipt.server.Alignment.RIGHT
                            else -> com.example.receipt.server.Alignment.LEFT
                        }
                        printer.addTextAlign(alignment)
                    }
                    "ADDQRCODE", "ADD_QR_CODE" -> {
                        command.data?.let { 
                            val options = com.example.receipt.server.QRCodeOptions(
                                size = command.qrSize ?: 3
                            )
                            printer.addQRCode(it, options)
                        }
                    }
                    "ADDFEEDLINE", "ADD_FEED_LINE" -> {
                        // Try direct lines field first, then params
                        val lines = command.lines ?: 
                                   command.params?.get("lines")?.toString()?.trim('"')?.toIntOrNull() ?: 1
                        Log.d(TAG, "Adding feed lines: $lines")
                        printer.addFeedLine(lines)
                    }
                    "CUTPAPER", "CUT_PAPER" -> {
                        Log.d(TAG, "Cutting paper")
                        printer.cutPaper()
                    }
                    else -> {
                        Log.w(TAG, "Unknown printer command type: ${command.type}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing command ${command.type}", e)
            }
        }
    }
    
    private fun executePrintCommands(printer: EpsonPrinter, content: String) {
        // Simple implementation - in production, parse structured commands
        printer.addText(content)
        printer.addFeedLine(2)
        printer.cutPaper()
    }
}

// Request/Response models
@Serializable
data class SubmissionResponse(
    val success: Boolean,
    val message: String,
    val teamId: String? = null
)

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: Long
)

@Serializable
data class RootResponse(
    val service: String,
    val version: String,
    val status: String,
    val endpoints: List<String>
)

@Serializable
data class PrintRequest(
    val teamId: String,
    val content: String
)

@Serializable
data class PrintContentRequest(
    val content: String
)

@Serializable
data class PrintElementsRequest(
    val elements: List<String>
)

@Serializable
data class PrintResponse(
    val success: Boolean,
    val message: String,
    val jobId: String? = null
)

@Serializable
data class RegisterUploadRequest(
    val team_id: String,
    val team_name: String,
    val compilation_status: String? = null,
    val error_message: String? = null
)

@Serializable
data class SimpleResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class PrintCommandsRequest(
    val team_id: String,
    val team_name: String? = null,
    val commands: List<PrinterCommand>
)

@Serializable
data class PrinterCommand(
    val type: String,
    val text: String? = null,
    val alignment: String? = null,
    val bold: Boolean? = null,
    val size: String? = null,
    val underline: Boolean? = null,
    val data: String? = null,
    val qrSize: Int? = null,
    val lines: Int? = null,
    val params: Map<String, String>? = null  // Changed to String instead of JsonElement
)