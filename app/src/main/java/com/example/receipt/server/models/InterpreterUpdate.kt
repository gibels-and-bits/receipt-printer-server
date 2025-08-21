package com.example.receipt.server.models

import kotlinx.serialization.Serializable

/**
 * Data class for updating an existing interpreter
 */
@Serializable
data class InterpreterUpdate(
    val interpreterCode: String
)