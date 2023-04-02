package com.ixam97.carStatsViewer.database.tripData

interface TripDataSource {

    /*
        Driving sessions
     */
    suspend fun addDrivingPoint(drivingPoint: DrivingPoint)

    suspend fun supersedeDrivingSession(prevSessionId: Long, timestamp: Long): Long?

    suspend fun startDrivingSession(timestamp: Long, type: Int): Long

    suspend fun endDrivingSession(timestamp: Long, sessionId: Long): Int?

    suspend fun getActiveDrivingSessionsIds(): List<Long>

    suspend fun getDrivingSession(sessionId: Long): DrivingSession?

    suspend fun updateDrivingSession(drivingSession: DrivingSession)

    suspend fun getActiveDrivingSessionsIdsMap(): Map<Int, Long>

    suspend fun getFullDrivingSession(sessionId: Long): DrivingSession?

    suspend fun startMarker(timestamp: Long, type: Int)

    suspend fun endMarker(timestamp: Long)

    /*
        Charging sessions
     */
    suspend fun addChargingPoint(chargingPoint: ChargingPoint)

    suspend fun startChargingSession(timestamp: Long, outsideTemp: Float, lat: Float? = null, lon: Float? = null): Long

    suspend fun endChargingSession(timestamp: Long, sessionId: Long? = null)
}