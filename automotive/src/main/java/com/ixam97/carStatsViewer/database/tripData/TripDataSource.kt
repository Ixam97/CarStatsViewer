package com.ixam97.carStatsViewer.database.tripData

interface TripDataSource {

    /*
        Driving sessions
     */
    suspend fun addDrivingPoint(drivingPoint: DrivingPoint)

    suspend fun getLatestDrivingPoint(): DrivingPoint?

    suspend fun supersedeDrivingSession(prevSessionId: Long, timestamp: Long): Long?

    suspend fun startDrivingSession(timestamp: Long, type: Int): Long

    suspend fun endDrivingSession(timestamp: Long, sessionId: Long): Int?

    suspend fun getActiveDrivingSessionsIds(): List<Long>

    suspend fun getActiveDrivingSessions(): List<DrivingSession>

    suspend fun getDrivingSession(sessionId: Long): DrivingSession?

    suspend fun updateDrivingSession(drivingSession: DrivingSession)

    suspend fun getActiveDrivingSessionsIdsMap(): Map<Int, Long>

    suspend fun getPastDrivingSessionIds(): List<Long>

    suspend fun getPastDrivingSessions(): List<DrivingSession>

    suspend fun getFullDrivingSession(sessionId: Long): DrivingSession?

    suspend fun startMarker(timestamp: Long, type: Int)

    suspend fun endMarker(timestamp: Long)

    suspend fun deleteDrivingSessionById(sessionId: Long)

    /*
        Charging sessions
     */
    suspend fun addChargingPoint(chargingPoint: ChargingPoint)


    suspend fun getLatestChargingPoint(): ChargingPoint?

    suspend fun startChargingSession(timestamp: Long, outsideTemp: Float, lat: Float? = null, lon: Float? = null): Long

    suspend fun endChargingSession(timestamp: Long, sessionId: Long? = null)

    suspend fun getActiveChargingSessionIds(): List<Long>

    suspend fun getChargingSessionById(sessionId: Long): ChargingSession?

    suspend fun getLatestChargingSession(): ChargingSession?

    suspend fun getCompleteChargingSessionById(sessionId: Long): ChargingSession

    suspend fun updateChargingSession(chargingSession: ChargingSession)
}