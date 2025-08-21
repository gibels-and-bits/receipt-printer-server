package com.example.receipt.server

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.*
import com.example.receipt.server.models.*

fun Routing.setupRoutes(
    interpreterManager: InterpreterManager,
    printerManager: PrinterManager,
    queueManager: QueueManager
) {
    
    // Helper function to validate team name
    fun validateTeamName(teamName: String): String? {
        return when {
            teamName.isBlank() -> "Team name cannot be empty"
            teamName.length < 3 -> "Team name must be at least 3 characters long"
            teamName.length > 50 -> "Team name must be less than 50 characters"
            !teamName.matches(Regex("^[a-zA-Z0-9_-]+$")) -> "Team name can only contain letters, numbers, underscores, and hyphens"
            else -> null
        }
    }
    
    // Helper function to validate Kotlin code
    fun validateKotlinCode(code: String): String? {
        return when {
            code.isBlank() -> "Interpreter code cannot be empty"
            code.length < 50 -> "Interpreter code seems too short. Please provide a complete implementation"
            !code.contains("fun interpret") -> "Interpreter must contain a function named 'interpret'"
            !code.contains("EpsonPrinter") -> "Interpreter must use the EpsonPrinter parameter"
            else -> null
        }
    }
    
    // Submit interpreter endpoint
    post("/submit") {
        try {
            val submission = call.receive<Submission>()
            
            // Validate team name
            val teamNameError = validateTeamName(submission.teamName)
            if (teamNameError != null) {
                call.respond(
                    HttpStatusCode.BadRequest, 
                    mapOf(
                        "error" to "Invalid team name",
                        "details" to teamNameError
                    )
                )
                return@post
            }
            
            // Validate Kotlin code
            val codeError = validateKotlinCode(submission.interpreterCode)
            if (codeError != null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "Invalid interpreter code",
                        "details" to codeError
                    )
                )
                return@post
            }
            
            // Store interpreter and get team ID
            val teamId = interpreterManager.store(submission.teamName, submission.interpreterCode)
            
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "endpoint" to "/print/$teamId",
                    "status" to "ready",
                    "teamId" to teamId
                )
            )
            
            println("Interpreter submitted for team: ${submission.teamName} (ID: $teamId)")
        } catch (e: ContentTransformationException) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "error" to "Invalid request format",
                    "details" to "Please ensure your request is valid JSON with 'teamName' and 'interpreterCode' fields"
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "error" to "Server error occurred",
                    "details" to "Failed to store interpreter: ${e.message}"
                )
            )
        }
    }
    
    // Print using team's interpreter
    post("/print/{teamId}") {
        val teamId = call.parameters["teamId"] 
            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing teamId"))
        
        // Check if server is busy
        if (!queueManager.tryAcquire()) {
            call.respond(
                HttpStatusCode.TooManyRequests,
                mapOf(
                    "error" to "Server busy, please try again",
                    "availableSlots" to queueManager.availableSlots()
                )
            )
            return@post
        }
        
        try {
            // Receive JSON body
            val jsonString = call.receiveText()
            
            // Load interpreter for team
            val interpreter = interpreterManager.load(teamId)
            
            // Get appropriate printer (mock or real)
            val printer = if (printerManager.isRealPrintEnabled(teamId)) {
                println("Using REAL printer for team: $teamId")
                printerManager.getRealPrinter()
            } else {
                println("Using MOCK printer for team: $teamId")
                printerManager.getMockPrinter()
            }
            
            // Execute interpreter with JSON
            println("Executing interpreter for team: $teamId")
            
            try {
                interpreter.execute(jsonString, printer)
                
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "success" to true,
                        "message" to "Print job completed",
                        "mode" to if (printerManager.isRealPrintEnabled(teamId)) "real" else "mock"
                    )
                )
            } catch (e: RuntimeException) {
                // Interpreter execution failed
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "error" to "Interpreter execution failed",
                        "details" to (e.message ?: "Your interpreter crashed during execution. Check your code for errors.")
                    )
                )
                println("Interpreter crashed for team $teamId: ${e.message}")
                e.printStackTrace()
            }
            
            println("Print job completed for team: $teamId")
        } catch (e: NoSuchElementException) {
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "No interpreter found for team: $teamId")
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Print failed: ${e.message}")
            )
            e.printStackTrace()
        } finally {
            queueManager.release()
        }
    }
    
    // Admin endpoint to enable real printing for a team
    post("/admin/enable-printer/{teamId}") {
        val teamId = call.parameters["teamId"]
            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing teamId"))
        
        // Check for admin key (simple security for hackathon)
        val adminKey = call.request.header("X-Admin-Key")
        if (adminKey != "hackathon2024") {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid admin key"))
            return@post
        }
        
        printerManager.enableRealPrint(teamId)
        
        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "status" to "Real printer enabled for $teamId",
                "teamId" to teamId
            )
        )
        
        println("Real printer ENABLED for team: $teamId")
    }
    
    // Update interpreter (before submission freeze)
    put("/submit/{teamId}") {
        val teamId = call.parameters["teamId"]
            ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing teamId"))
        
        try {
            val update = call.receive<InterpreterUpdate>()
            
            // Validate Kotlin code
            val codeError = validateKotlinCode(update.interpreterCode)
            if (codeError != null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "error" to "Invalid interpreter code",
                        "details" to codeError
                    )
                )
                return@put
            }
            
            interpreterManager.update(teamId, update.interpreterCode)
            
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "updated",
                    "teamId" to teamId,
                    "message" to "Interpreter updated successfully"
                )
            )
            
            println("Interpreter updated for team: $teamId")
        } catch (e: NoSuchElementException) {
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "No interpreter found for team: $teamId")
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Failed to update interpreter: ${e.message}")
            )
        }
    }
    
    // Get team status
    get("/status/{teamId}") {
        val teamId = call.parameters["teamId"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing teamId"))
        
        try {
            val hasInterpreter = interpreterManager.exists(teamId)
            val realPrintEnabled = printerManager.isRealPrintEnabled(teamId)
            
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "teamId" to teamId,
                    "hasInterpreter" to hasInterpreter,
                    "realPrintEnabled" to realPrintEnabled,
                    "queueStatus" to mapOf(
                        "availableSlots" to queueManager.availableSlots(),
                        "maxSlots" to 3
                    )
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Failed to get status: ${e.message}")
            )
        }
    }
    
    // List all teams (admin)
    get("/admin/teams") {
        val adminKey = call.request.header("X-Admin-Key")
        if (adminKey != "hackathon2024") {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid admin key"))
            return@get
        }
        
        val teams = interpreterManager.listTeams()
        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "teams" to teams,
                "count" to teams.size
            )
        )
    }
}