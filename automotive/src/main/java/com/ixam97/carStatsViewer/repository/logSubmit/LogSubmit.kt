package com.ixam97.carStatsViewer.repository.logSubmit

import kotlinx.serialization.Serializable

@Serializable
data class LogSubmitBody(
    val log: Map<Long, String>
)

@Serializable
sealed interface LogSubmitStatus {
    @Serializable
    data object Success: LogSubmitStatus

    @Serializable
    data object Error: LogSubmitStatus
}