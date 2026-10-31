package com.ixam97.carStatsViewer.repository.dataExport

sealed interface DataExportProgressState {
    data object None: DataExportProgressState
    data class ParsingTrip(val progress: Float) : DataExportProgressState
    data class ParsingChargingSessions(val progress: Float) : DataExportProgressState
    data class ParsingChargingSession(val progress: Float): DataExportProgressState
    data object Compressing: DataExportProgressState
    data object Uploading: DataExportProgressState
    data object Success: DataExportProgressState
    data class Failure(val message: String): DataExportProgressState
}