package com.example.receipt.server.models

import kotlinx.serialization.Serializable

/**
 * Data class for interpreter submission
 */
@Serializable
data class Submission(
    val team_id: String,
    val teamName: String,
    val interpreterCode: String
)