package com.example.receipt.server

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.UUID

/**
 * Print server data manager
 * Manages print queue and team statistics
 */
class ServerDataManager {
    
    companion object {
        private const val TAG = "ServerDataManager"
        const val MAX_QUEUE_SIZE = 15
    }
    
    // Print queue (FIFO)
    private val printQueue = ConcurrentLinkedQueue<PrintJob>()
    private val _printQueueFlow = MutableStateFlow<List<PrintJob>>(emptyList())
    val printQueueFlow: StateFlow<List<PrintJob>> = _printQueueFlow.asStateFlow()
    
    // Team statistics
    private val teamStats = ConcurrentHashMap<String, TeamStats>()
    private val _teamStatsFlow = MutableStateFlow<List<TeamStatsDisplay>>(emptyList())
    val teamStatsFlow: StateFlow<List<TeamStatsDisplay>> = _teamStatsFlow.asStateFlow()
    
    // Queue status
    private val _queueStatusFlow = MutableStateFlow(QueueStatus(0, MAX_QUEUE_SIZE, false))
    val queueStatusFlow: StateFlow<QueueStatus> = _queueStatusFlow.asStateFlow()
    
    // Current printing job
    private val _currentJobFlow = MutableStateFlow<PrintJob?>(null)
    val currentJobFlow: StateFlow<PrintJob?> = _currentJobFlow.asStateFlow()
    
    // Global statistics
    private val totalUploads = AtomicInteger(0)
    private val totalPrintJobs = AtomicInteger(0)
    private val successfulPrints = AtomicInteger(0)
    private val failedPrints = AtomicInteger(0)
    
    /**
     * Register an interpreter upload with compilation status
     */
    fun registerUpload(
        teamId: String, 
        teamName: String, 
        compilationStatus: String? = null,
        errorMessage: String? = null
    ) {
        val stats = teamStats.getOrPut(teamId) { 
            TeamStats(teamId, teamName, 0, 0, System.currentTimeMillis())
        }
        stats.uploadCount++
        stats.lastActivity = System.currentTimeMillis()
        
        // Update compilation status
        when (compilationStatus?.lowercase()) {
            "success" -> {
                stats.lastCompilationStatus = CompilationStatus.SUCCESS
                stats.lastCompilationError = null
            }
            "failed" -> {
                stats.lastCompilationStatus = CompilationStatus.FAILED
                stats.lastCompilationError = errorMessage
            }
            else -> {
                // Keep existing status if not provided
            }
        }
        
        totalUploads.incrementAndGet()
        updateTeamStatsFlow()
        Log.i(TAG, "Upload registered for team: $teamName ($teamId) - Status: ${stats.lastCompilationStatus}")
    }
    
    /**
     * Add a print job to the queue
     * Returns jobId if successful, null if queue is full
     */
    fun addPrintJob(
        teamId: String, 
        teamName: String,
        commands: List<PrinterCommand>
    ): PrintJobResult {
        // Check if queue is full
        if (printQueue.size >= MAX_QUEUE_SIZE) {
            Log.w(TAG, "Print queue full. Rejecting job from team: $teamId")
            return PrintJobResult.QueueFull
        }
        
        val jobId = "job-${UUID.randomUUID()}"
        val printJob = PrintJob(
            id = jobId,
            teamId = teamId,
            teamName = teamName,
            commands = commands,
            timestamp = System.currentTimeMillis(),
            status = PrintJobStatus.QUEUED,
            position = printQueue.size + 1
        )
        
        // Add to queue
        printQueue.offer(printJob)
        
        // Update team stats
        val stats = teamStats.getOrPut(teamId) { 
            TeamStats(teamId, teamName, 0, 0, System.currentTimeMillis())
        }
        stats.printJobCount++
        stats.lastActivity = System.currentTimeMillis()
        totalPrintJobs.incrementAndGet()
        
        // Update flows
        updatePrintQueueFlow()
        updateTeamStatsFlow()
        updateQueueStatus()
        
        Log.i(TAG, "Print job $jobId added for team: $teamName (position: ${printQueue.size})")
        return PrintJobResult.Success(jobId, printQueue.size)
    }
    
    /**
     * Get next job from queue for printing
     */
    fun getNextJob(): PrintJob? {
        val job = printQueue.poll()
        if (job != null) {
            _currentJobFlow.value = job.copy(status = PrintJobStatus.PRINTING)
            updatePrintQueueFlow()
            updateQueueStatus()
            Log.i(TAG, "Processing job: ${job.id} from team: ${job.teamName}")
        }
        return job
    }
    
    /**
     * Mark current job as completed
     */
    fun completeCurrentJob(success: Boolean) {
        val job = _currentJobFlow.value
        if (job != null) {
            if (success) {
                successfulPrints.incrementAndGet()
                Log.i(TAG, "Job ${job.id} completed successfully")
            } else {
                failedPrints.incrementAndGet()
                Log.e(TAG, "Job ${job.id} failed")
            }
            _currentJobFlow.value = null
        }
    }
    
    /**
     * Clear all data
     */
    fun clearAllData() {
        printQueue.clear()
        teamStats.clear()
        _currentJobFlow.value = null
        totalUploads.set(0)
        totalPrintJobs.set(0)
        successfulPrints.set(0)
        failedPrints.set(0)
        
        updatePrintQueueFlow()
        updateTeamStatsFlow()
        updateQueueStatus()
        
        Log.i(TAG, "All server data cleared")
    }
    
    /**
     * Get server statistics
     */
    fun getStatistics(): ServerStatistics {
        return ServerStatistics(
            totalUploads = totalUploads.get(),
            totalPrintJobs = totalPrintJobs.get(),
            successfulPrints = successfulPrints.get(),
            failedPrints = failedPrints.get(),
            queueSize = printQueue.size,
            uniqueTeams = teamStats.size
        )
    }
    
    // Private helper methods
    
    private fun updatePrintQueueFlow() {
        val queueList = printQueue.toList()
        // Update position numbers
        queueList.forEachIndexed { index, job ->
            job.position = index + 1
        }
        _printQueueFlow.value = queueList
    }
    
    private fun updateTeamStatsFlow() {
        _teamStatsFlow.value = teamStats.values
            .map { stats ->
                TeamStatsDisplay(
                    teamId = stats.teamId,
                    teamName = stats.teamName,
                    uploadCount = stats.uploadCount,
                    printJobCount = stats.printJobCount,
                    lastActivity = stats.lastActivity,
                    lastCompilationStatus = stats.lastCompilationStatus,
                    lastCompilationError = stats.lastCompilationError
                )
            }
            .sortedByDescending { it.lastActivity }
    }
    
    private fun updateQueueStatus() {
        val size = printQueue.size
        _queueStatusFlow.value = QueueStatus(
            currentSize = size,
            maxSize = MAX_QUEUE_SIZE,
            isFull = size >= MAX_QUEUE_SIZE
        )
    }
}

// Data classes

data class PrintJob(
    val id: String,
    val teamId: String,
    val teamName: String,
    val commands: List<PrinterCommand>,
    val timestamp: Long,
    var status: PrintJobStatus,
    var position: Int = 0
)

enum class PrintJobStatus {
    QUEUED,
    PRINTING,
    COMPLETED,
    FAILED
}

data class TeamStats(
    val teamId: String,
    val teamName: String,
    var uploadCount: Int,
    var printJobCount: Int,
    var lastActivity: Long,
    var lastCompilationStatus: CompilationStatus = CompilationStatus.UNKNOWN,
    var lastCompilationError: String? = null
)

data class TeamStatsDisplay(
    val teamId: String,
    val teamName: String,
    val uploadCount: Int,
    val printJobCount: Int,
    val lastActivity: Long,
    val lastCompilationStatus: CompilationStatus,
    val lastCompilationError: String?
)

enum class CompilationStatus {
    SUCCESS,
    FAILED,
    UNKNOWN
}

data class QueueStatus(
    val currentSize: Int,
    val maxSize: Int,
    val isFull: Boolean
)

@kotlinx.serialization.Serializable
data class ServerStatistics(
    val totalUploads: Int,
    val totalPrintJobs: Int,
    val successfulPrints: Int,
    val failedPrints: Int,
    val queueSize: Int,
    val uniqueTeams: Int
)

sealed class PrintJobResult {
    data class Success(val jobId: String, val queuePosition: Int) : PrintJobResult()
    object QueueFull : PrintJobResult()
}