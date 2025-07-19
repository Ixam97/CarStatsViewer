package com.ixam97.carStatsViewer.repository.logSubmit

import kotlinx.serialization.Serializable

@Serializable
data class LogSubmitBody(
    val log: Map<Long, String>,
    val userID: String,
    val metadata: LogMetadata
) {
    @Serializable
    data class LogMetadata(
        val timestamp: Long,
        val brand: String,
        val model: String,
        val device: String,
        val appInfo: String,
        val cpuInfo: String
    )
}

@Serializable
data class AuthResponse(
    val authorized: String
)

@Serializable
data class LogSubmitStatus(
    val status: String,
    val message: String?
)

@Serializable
sealed interface LogSubmitState {
    @Serializable
    data object Success: LogSubmitState

    @Serializable
    data object Error: LogSubmitState
}