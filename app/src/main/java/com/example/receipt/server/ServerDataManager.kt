package com.example.receipt.server

import android.util.Log
import com.example.receipt.server.models.PrintJob
import com.example.receipt.server.models.PrintJobStatus
import com.example.receipt.server.models.Submission
import kotlinx.serialization.json.JsonPrimitive
import com.example.receipt.server.ui.components.ConnectedClient
import com.example.receipt.server.ui.screens.TeamInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Production data manager for the receipt printer server
 * Manages real-time state of teams, print jobs, and connected clients
 */
class ServerDataManager {
    
    companion object {
        private const val TAG = "ServerDataManager"
        const val QUEUE_CAPACITY = 100
    }
    
    // Teams data
    private val teamsMap = ConcurrentHashMap<String, TeamInfo>()
    private val interpreterCodes = ConcurrentHashMap<String, String>() // Store interpreter codes
    private val _teamsFlow = MutableStateFlow<List<TeamInfo>>(emptyList())
    val teamsFlow: StateFlow<List<TeamInfo>> = _teamsFlow.asStateFlow()
    
    // Connected clients
    private val clientsMap = ConcurrentHashMap<String, ConnectedClient>()
    private val _clientsFlow = MutableStateFlow<List<ConnectedClient>>(emptyList())
    val clientsFlow: StateFlow<List<ConnectedClient>> = _clientsFlow.asStateFlow()
    
    // Print queue management
    private val printQueue = ConcurrentHashMap<String, PrintJob>()
    private val queueCounter = AtomicInteger(0)
    private val _queueStatusFlow = MutableStateFlow(Pair(0, QUEUE_CAPACITY))
    val queueStatusFlow: StateFlow<Pair<Int, Int>> = _queueStatusFlow.asStateFlow()
    
    // Printer status for teams
    private val printerEnabledTeams = ConcurrentHashMap<String, Boolean>()
    private val _printerEnabledCountFlow = MutableStateFlow(0)
    val printerEnabledCountFlow: StateFlow<Int> = _printerEnabledCountFlow.asStateFlow()
    
    // Server statistics
    private val totalPrintJobs = AtomicInteger(0)
    private val successfulPrints = AtomicInteger(0)
    private val failedPrints = AtomicInteger(0)
    
    /**
     * Register a new team submission
     */
    fun registerTeam(submission: Submission) {
        val teamId = submission.team_id
        val existingTeam = teamsMap[teamId]
        
        val team = TeamInfo(
            teamId = teamId,
            teamName = submission.teamName,
            hasInterpreter = true,
            printerEnabled = printerEnabledTeams[teamId] ?: false,
            lastActivity = System.currentTimeMillis(),
            printJobCount = existingTeam?.printJobCount ?: 0
        )
        
        teamsMap[teamId] = team
        interpreterCodes[teamId] = submission.interpreterCode // Store the interpreter code
        updateTeamsFlow()
        
        Log.i(TAG, "Team registered: ${team.teamName} (${team.teamId})")
    }
    
    /**
     * Get interpreter code for a team
     */
    fun getInterpreterCode(teamId: String): String? {
        return interpreterCodes[teamId]
    }
    
    /**
     * Add a new print job to the queue
     */
    fun addPrintJob(teamId: String, content: String): String {
        val jobId = "job-${System.currentTimeMillis()}-${queueCounter.incrementAndGet()}"
        
        // Create a simple JSON element for the print content
        val jsonContent = JsonPrimitive(content)
        
        val printJob = PrintJob(
            teamId = teamId,
            json = jsonContent,
            timestamp = System.currentTimeMillis(),
            status = PrintJobStatus.PENDING
        )
        
        printQueue[jobId] = printJob
        totalPrintJobs.incrementAndGet()
        
        // Update team print count
        teamsMap[teamId]?.let { team ->
            teamsMap[teamId] = team.copy(
                printJobCount = team.printJobCount + 1,
                lastActivity = System.currentTimeMillis()
            )
            updateTeamsFlow()
        }
        
        updateQueueStatus()
        Log.i(TAG, "Print job added: $jobId for team $teamId")
        
        return jobId
    }
    
    /**
     * Mark a print job as completed
     */
    fun completePrintJob(jobId: String, success: Boolean) {
        printQueue[jobId]?.let { job ->
            printQueue[jobId] = job.copy(
                status = if (success) PrintJobStatus.COMPLETED else PrintJobStatus.FAILED
            )
            
            if (success) {
                successfulPrints.incrementAndGet()
            } else {
                failedPrints.incrementAndGet()
            }
            
            updateQueueStatus()
            Log.i(TAG, "Print job $jobId ${if (success) "completed" else "failed"}")
        }
    }
    
    /**
     * Enable printer access for a team
     */
    fun enablePrinterForTeam(teamId: String) {
        printerEnabledTeams[teamId] = true
        
        teamsMap[teamId]?.let { team ->
            teamsMap[teamId] = team.copy(printerEnabled = true)
            updateTeamsFlow()
            updatePrinterEnabledCount()
        }
        
        Log.i(TAG, "Printer enabled for team: $teamId")
    }
    
    /**
     * Disable printer access for a team
     */
    fun disablePrinterForTeam(teamId: String) {
        printerEnabledTeams[teamId] = false
        
        teamsMap[teamId]?.let { team ->
            teamsMap[teamId] = team.copy(printerEnabled = false)
            updateTeamsFlow()
            updatePrinterEnabledCount()
        }
        
        Log.i(TAG, "Printer disabled for team: $teamId")
    }
    
    /**
     * Delete a team and its submission
     */
    fun deleteTeam(teamId: String): Boolean {
        val team = teamsMap[teamId]
        val removed = teamsMap.remove(teamId) != null
        if (removed && team != null) {
            // Remove from printer enabled teams
            printerEnabledTeams.remove(teamId)
            
            // Remove interpreter code
            interpreterCodes.remove(teamId)
            
            // Remove associated client connections
            val clientsToRemove = clientsMap.values.filter { it.name == team.teamName }
            clientsToRemove.forEach { client ->
                clientsMap.remove(client.ipAddress)
            }
            
            // Remove print jobs for this team
            printQueue.entries.removeIf { it.value.teamId == teamId }
            
            // Update flows
            updateTeamsFlow()
            updateClientsFlow()
            updatePrinterEnabledCount()
            updateQueueStatus()
            
            Log.i(TAG, "Team deleted: $teamId")
        }
        return removed
    }
    
    /**
     * Register a connected client
     */
    fun registerClient(ipAddress: String, name: String = "Unknown Client"): String {
        val clientId = "client-${System.currentTimeMillis()}"
        
        val client = ConnectedClient(
            id = clientId,
            name = name,
            ipAddress = ipAddress,
            connectedAt = System.currentTimeMillis(),
            isActive = true
        )
        
        clientsMap[clientId] = client
        updateClientsFlow()
        
        Log.i(TAG, "Client connected: $name ($ipAddress)")
        return clientId
    }
    
    /**
     * Update client activity status
     */
    fun updateClientActivity(clientId: String, isActive: Boolean) {
        clientsMap[clientId]?.let { client ->
            clientsMap[clientId] = client.copy(
                isActive = isActive,
                lastActivity = System.currentTimeMillis()
            )
            updateClientsFlow()
        }
    }
    
    /**
     * Remove a disconnected client
     */
    fun removeClient(clientId: String) {
        clientsMap.remove(clientId)
        updateClientsFlow()
        Log.i(TAG, "Client disconnected: $clientId")
    }
    
    /**
     * Get current server statistics
     */
    fun getStatistics(): ServerStatistics {
        return ServerStatistics(
            totalTeams = teamsMap.size,
            printerEnabledTeams = printerEnabledTeams.count { it.value },
            totalPrintJobs = totalPrintJobs.get(),
            successfulPrints = successfulPrints.get(),
            failedPrints = failedPrints.get(),
            queuedJobs = printQueue.count { it.value.status == PrintJobStatus.PENDING },
            connectedClients = clientsMap.count { it.value.isActive }
        )
    }
    
    /**
     * Clear all data (for testing/reset)
     */
    fun clearAllData() {
        teamsMap.clear()
        clientsMap.clear()
        printQueue.clear()
        printerEnabledTeams.clear()
        
        queueCounter.set(0)
        totalPrintJobs.set(0)
        successfulPrints.set(0)
        failedPrints.set(0)
        
        updateTeamsFlow()
        updateClientsFlow()
        updateQueueStatus()
        updatePrinterEnabledCount()
        
        Log.i(TAG, "All server data cleared")
    }
    
    // Private helper methods
    
    private fun updateTeamsFlow() {
        _teamsFlow.value = teamsMap.values.toList()
            .sortedByDescending { it.lastActivity }
    }
    
    private fun updateClientsFlow() {
        _clientsFlow.value = clientsMap.values.toList()
            .sortedByDescending { it.connectedAt }
    }
    
    private fun updateQueueStatus() {
        val used = printQueue.count { it.value.status == PrintJobStatus.PENDING }
        _queueStatusFlow.value = Pair(used, QUEUE_CAPACITY)
    }
    
    private fun updatePrinterEnabledCount() {
        _printerEnabledCountFlow.value = printerEnabledTeams.count { it.value }
    }
}

/**
 * Server statistics data class
 */
data class ServerStatistics(
    val totalTeams: Int,
    val printerEnabledTeams: Int,
    val totalPrintJobs: Int,
    val successfulPrints: Int,
    val failedPrints: Int,
    val queuedJobs: Int,
    val connectedClients: Int
)