package com.ixam97.carStatsViewer.repository.dataExport

import kotlinx.serialization.Serializable

@Serializable
data class ChargingSessionDataExportBody(
    val receiverMailAddress: String,
    val startTime: Long,
    val lat: Float?,
    val lon: Float?,
    val outsideTemp: Float?,
    val sessionCsvData: String,
)

@Serializable
data class TripDataExportBody(
    val receiverMailAddress: String,
    val startTime: Long,
    val tripDrivePointsCsvData: String,
    val chargingSessionsData: List<ChargingSessionDataExportBody>
)

@Serializable
data class AuthResponse(
    val authorized: String
)

@Serializable
data class DataExportResponse(
    val status: String,
    val message: String?
)

@Serializable
sealed interface DataExportState {
    @Serializable
    data object Success: DataExportState

    @Serializable
    data object Error: DataExportState
}

data class DataExportStatus(
    val state: DataExportState,
    val message: String? = null
)