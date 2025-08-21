package com.example.receipt.server

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class TeamDetails(
    val teamId: String,
    val teamName: String,
    val interpreterCode: String,
    val printerEnabled: Boolean,
    val lastActivity: Long? = null,
    val printJobCount: Int = 0
)

@Serializable
data class DashboardData(
    val teams: List<TeamInfo>,
    val printerEnabledTeams: List<String>
)

@Serializable
data class TeamInfo(
    val teamId: String,
    val teamName: String
)

@Serializable
data class QueueStatus(
    val availableSlots: Int,
    val maxSlots: Int
)

fun Routing.setupAdminRoutes(
    interpreterManager: InterpreterManager,
    printerManager: PrinterManager,
    queueManager: QueueManager
) {
    // Serve static HTML files
    get("/admin/dashboard") {
        val file = File("src/main/resources/static/dashboard.html")
        if (file.exists()) {
            call.respondFile(file)
        } else {
            call.respond(HttpStatusCode.NotFound, "Dashboard not found")
        }
    }
    
    get("/admin/team/{teamId}") {
        val file = File("src/main/resources/static/team-detail.html")
        if (file.exists()) {
            call.respondFile(file)
        } else {
            call.respond(HttpStatusCode.NotFound, "Team detail page not found")
        }
    }
    
    // API endpoint for dashboard data
    get("/api/admin/teams") {
        try {
            val teams = interpreterManager.listTeams()
            val teamInfoList = teams.map { teamEntry ->
                TeamInfo(
                    teamId = teamEntry["teamId"] as String,
                    teamName = teamEntry["teamName"] as String
                )
            }
            
            val enabledTeams = printerManager.getEnabledTeams()
            
            call.respond(
                HttpStatusCode.OK,
                DashboardData(
                    teams = teamInfoList,
                    printerEnabledTeams = enabledTeams
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Failed to load teams: ${e.message}")
            )
        }
    }
    
    // API endpoint for team details
    get("/api/admin/team/{teamId}") {
        val teamId = call.parameters["teamId"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing teamId"))
        
        try {
            val teamData = interpreterManager.getTeamData(teamId)
            val printerEnabled = printerManager.isRealPrintEnabled(teamId)
            val stats = interpreterManager.getTeamStats(teamId)
            
            call.respond(
                HttpStatusCode.OK,
                TeamDetails(
                    teamId = teamId,
                    teamName = teamData.teamName,
                    interpreterCode = teamData.interpreterCode,
                    printerEnabled = printerEnabled,
                    lastActivity = stats.lastActivity,
                    printJobCount = stats.printJobCount
                )
            )
        } catch (e: NoSuchElementException) {
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "Team not found: $teamId")
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Failed to load team details: ${e.message}")
            )
        }
    }
    
    // API endpoint for queue status
    get("/api/admin/queue-status") {
        call.respond(
            HttpStatusCode.OK,
            QueueStatus(
                availableSlots = queueManager.availableSlots(),
                maxSlots = 3
            )
        )
    }
    
    // Enable printer for a team (no admin key required from dashboard)
    post("/api/admin/enable-printer/{teamId}") {
        val teamId = call.parameters["teamId"]
            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing teamId"))
        
        try {
            printerManager.enableRealPrint(teamId)
            
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "success",
                    "message" to "Printer enabled for team $teamId"
                )
            )
            
            println("Admin Dashboard: Printer ENABLED for team: $teamId")
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Failed to enable printer: ${e.message}")
            )
        }
    }
    
    // Disable printer for a team
    post("/api/admin/disable-printer/{teamId}") {
        val teamId = call.parameters["teamId"]
            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing teamId"))
        
        try {
            printerManager.disableRealPrint(teamId)
            
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "success",
                    "message" to "Printer disabled for team $teamId"
                )
            )
            
            println("Admin Dashboard: Printer DISABLED for team: $teamId")
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Failed to disable printer: ${e.message}")
            )
        }
    }
}