package com.ixam97.carStatsViewer.database.tripData

import androidx.room.*

@Dao
interface TripDao {
    @Insert
    fun addDrivingPoint(drivingPoint: DrivingPoint): Long

    @Ignore
    fun addDrivingPoint(drivingPoint: DrivingPoint, activeSessions: List<Long>): Long {
        for (sessionId in activeSessions) {
            addDrivingSessionPointCrossRef(
                DrivingSessionPointCrossRef(
                    driving_session_id = sessionId,
                    driving_point_epoch_time = drivingPoint.driving_point_epoch_time
                )
            )
        }
        return addDrivingPoint(drivingPoint)
    }

    @Insert
    fun addDrivingSession(drivingSession: DrivingSession): Long

    @Insert
    fun addDrivingSessionPointCrossRef(drivingSessionPointCrossRef: DrivingSessionPointCrossRef)

    @Insert
    fun addChargingPoint(chargingPoint: ChargingPoint): Long

    @Insert
    fun addChargingSession(chargingSession: ChargingSession): Long

    @Ignore
    fun addChargingSession(chargingSession: ChargingSession, activeSessions: List<Long>): Long {
        val chargingSessionId = addChargingSession(chargingSession)
        for (sessionId in activeSessions) {
            addDrivingChargingCrossRef(DrivingChargingCrossRef(
                driving_session_id = sessionId,
                charging_session_id = chargingSessionId
            ))
        }
        return chargingSessionId
    }

    @Insert
    fun addDrivingChargingCrossRef(drivingChargingCrossRef: DrivingChargingCrossRef)

    @Update
    fun updateDrivingSession(drivingSession: DrivingSession)

    @Query("SELECT * FROM DrivingSession WHERE driving_session_id = :sessionId")
    fun getDrivingSessionById(sessionId: Long): DrivingSession

    @Query("SELECT * FROM DrivingSession")
    fun getAllDrivingSessions(): List<DrivingSession>

    @Query("SELECT * FROM DrivingChargingCrossRef")
    fun getDrivingChargingCrossRef(): List<DrivingChargingCrossRef>

    @Transaction
    @Query("SELECT * FROM DrivingSession WHERE driving_session_id = :sessionId")
    fun getCompleteTripData(sessionId: Long): CompleteTripData

    @Transaction
    @Query("SELECT * FROM ChargingSession WHERE charging_session_id = (:sessionIds)")
    fun getChargingSessionsWithPointsByIds(sessionIds: List<Long>): List<ChargingSessionWithPoints>

    @Ignore
    fun getCompleteDrivingSessionById(sessionId: Long): DrivingSession {
        val completeTripData = getCompleteTripData(sessionId)
        val drivingSession = completeTripData.drivingSession
        drivingSession.driving_points = completeTripData.drivingPoints
        drivingSession.charging_sessions = completeTripData.charging_sessions.map {
            it.chargingSession.charging_points = it.chargingPoints
            it.chargingSession
        }
        return drivingSession
    }

}

