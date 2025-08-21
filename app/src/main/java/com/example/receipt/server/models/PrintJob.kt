package com.example.receipt.server.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Data class representing a print job
 */
@Serializable
data class PrintJob(
    val teamId: String,
    val json: JsonElement,
    val timestamp: Long = System.currentTimeMillis(),
    val status: PrintJobStatus = PrintJobStatus.PENDING
)

/**
 * Status of a print job
 */
enum class PrintJobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}