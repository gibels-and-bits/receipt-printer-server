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
                call.respond(mapOf(
                    "service" to "Receipt Printer Server",
                    "version" to "1.0.0",
                    "status" to "running",
                    "endpoints" to listOf(
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
                call.respond(HttpStatusCode.OK, mapOf("status" to "healthy", "timestamp" to System.currentTimeMillis()))
            }
            
            // Team submission endpoint
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
            
            // Admin endpoints
            get("/api/admin/teams") {
                call.respond(serverDataManager.teamsFlow.value)
            }
            
            get("/api/admin/clients") {
                call.respond(serverDataManager.clientsFlow.value)
            }
            
            get("/api/admin/statistics") {
                call.respond(serverDataManager.getStatistics())
            }
            
            post("/api/admin/team/{teamId}/printer/enable") {
                val teamId = call.parameters["teamId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                serverDataManager.enablePrinterForTeam(teamId)
                printerManager.enableRealPrint(teamId)
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            }
            
            post("/api/admin/team/{teamId}/printer/disable") {
                val teamId = call.parameters["teamId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                serverDataManager.disablePrinterForTeam(teamId)
                printerManager.disableRealPrint(teamId)
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            }
            
            // Delete specific team
            delete("/api/admin/team/{teamId}") {
                val teamId = call.parameters["teamId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val deleted = serverDataManager.deleteTeam(teamId)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "Team deleted successfully"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "message" to "Team not found"))
                }
            }
            
            // Clear all data (for testing)
            delete("/api/admin/clear") {
                serverDataManager.clearAllData()
                call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "All data cleared"))
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