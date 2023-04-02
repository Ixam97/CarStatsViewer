package com.ixam97.carStatsViewer.database.tripData

import androidx.room.*

@Dao
interface TripDao {

    @Insert
    fun insertDrivingPoint(drivingPoint: DrivingPoint)

    @Insert
    fun insertChargingPoint(chargingPoint: ChargingPoint)

    @Upsert
    fun upsertDrivingSession(drivingSession: DrivingSession): Long

    @Upsert
    fun upsertChargingSession(chargingSession: ChargingSession): Long

    @Upsert
    fun upsertSessionMarker(sessionMarker: SessionMarker)

    @Query("SELECT * FROM DrivingSession WHERE driving_session_id = :sessionId")
    fun getDrivingSessionById(sessionId: Long): DrivingSession?

    @Query("SELECT driving_session_id FROM DrivingSession WHERE end_epoch_time = 0") //
    fun getActiveDrivingSessionIds(): List<Long>

    @Query("SELECT * FROM ChargingSession WHERE charging_session_id = :sessionId")
    fun getChargingSessionById(sessionId: Long): ChargingSession

    @Query("SELECT charging_session_id FROM ChargingSession WHERE end_epoch_time IS null")
    fun getActiveChargingSessionIds(): List<Long>

    @Insert
    fun insertDrivingSessionPointCrossRef(crossRef: DrivingSessionPointCrossRef)

    @Insert
    fun insertDrivingChargingCrossRef(crossRef: DrivingChargingCrossRef)

    @Ignore
    fun getCompleteDrivingSessionById(sessionId: Long): DrivingSession {
        val completeTripData = getCompleteTripData(sessionId)
        val drivingSession = completeTripData.drivingSession
        drivingSession.drivingPoints = completeTripData.drivingPoints
        drivingSession.chargingSessions = completeTripData.charging_sessions.map {
            it.chargingSession.chargingPoints = it.chargingPoints
            it.chargingSession
        }
        return drivingSession
    }

    @Transaction
    @Query("SELECT * FROM DrivingSession WHERE driving_session_id = :sessionId")
    fun getCompleteTripData(sessionId: Long): CompleteTripData

    /*
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

    @Upsert
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



    @Query("SELECT * FROM DrivingSession")
    fun getAllDrivingSessions(): List<DrivingSession>

    @Query("SELECT * FROM DrivingChargingCrossRef")
    fun getDrivingChargingCrossRef(): List<DrivingChargingCrossRef>

    @Query("UPDATE ChargingSession SET end_epoch_time = :timestamp WHERE charging_session_id = :activeChargingSessionId")
    fun endChargingSession(activeChargingSessionId: Long, timestamp: Long)

    @Query("UPDATE ChargingSession SET charged_energy = :chargedEnergy, charged_soc = :chargedSoC WHERE charging_session_id = :activeChargingSessionId")
    fun updateChargingSession(activeChargingSessionId: Long, chargedEnergy: Double, chargedSoC: Float)

    @Transaction
    @Query("SELECT * FROM ChargingSession WHERE charging_session_id = (:sessionIds)")
    fun getChargingSessionsWithPointsByIds(sessionIds: List<Long>): List<ChargingSessionWithPoints>

     */

}

