package com.example.receipt.server.models

import kotlinx.serialization.Serializable

/**
 * Data class for interpreter submission
 */
@Serializable
data class Submission(
    val teamName: String,
    val interpreterCode: String
)