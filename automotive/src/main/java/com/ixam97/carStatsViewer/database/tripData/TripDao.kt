package com.ixam97.carStatsViewer.database.tripData

import androidx.room.*

@Dao
interface TripDao {

    @Upsert
    fun upsertDrivingPoint(drivingPoint: DrivingPoint)

    @Query("SELECT * FROM DrivingPoint ORDER BY driving_point_epoch_time DESC LIMIT 1")
    fun getLatestDrivingPoint(): DrivingPoint?

    @Upsert
    fun upsertChargingPoint(chargingPoint: ChargingPoint)

    @Query("SELECT * FROM ChargingPoint ORDER BY charging_point_epoch_time DESC LIMIT 1")
    fun getLatestChargingPoint(): ChargingPoint?

    @Upsert
    fun upsertDrivingSession(drivingSession: DrivingSession): Long

    @Upsert
    fun upsertChargingSession(chargingSession: ChargingSession): Long

    @Upsert
    fun upsertSessionMarker(sessionMarker: SessionMarker)

    @Query("SELECT count(*)!=0 FROM DrivingPoint WHERE driving_point_epoch_time = :epoch_time ")
    fun drivingPointExists(epoch_time: Long): Boolean

    @Query("SELECT * FROM DrivingSession WHERE driving_session_id = :sessionId")
    fun getDrivingSessionById(sessionId: Long): DrivingSession?

    @Query("SELECT driving_session_id FROM DrivingSession WHERE end_epoch_time = 0") //
    fun getActiveDrivingSessionIds(): List<Long>

    @Query("SELECT * FROM DrivingSession WHERE end_epoch_time = 0")
    fun getActiveDrivingSessions(): List<DrivingSession>

    @Query("SELECT driving_session_id FROM DrivingSession WHERE end_epoch_time != 0")
    fun getPastDrivingSessionIds(): List<Long>

    @Query("SELECT * FROM DrivingSession WHERE end_epoch_time != 0")
    fun getPastDrivingSessions(): List<DrivingSession>

    @Query("SELECT * FROM ChargingSession WHERE charging_session_id = :sessionId")
    fun getChargingSessionById(sessionId: Long): ChargingSession?

    @Query("SELECT * FROM ChargingSession ORDER BY end_epoch_time DESC LIMIT 1")
    fun getLatestChargingSession(): ChargingSession?

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

    @Ignore
    fun getCompleteChargingSessionById(sessionId: Long): ChargingSession {
        val chargingSessionWithPoints = getChargingSessionWithPoints(sessionId)
        val chargingSession = chargingSessionWithPoints.chargingSession
        chargingSession.chargingPoints = chargingSessionWithPoints.chargingPoints

        return chargingSession
    }

    @Transaction
    @Query("SELECT * FROM DrivingSession WHERE driving_session_id = :sessionId")
    fun getCompleteTripData(sessionId: Long): CompleteTripData

    @Transaction
    @Query("SELECT * FROM ChargingSession WHERE charging_session_id = :sessionId")
    fun getChargingSessionWithPoints(sessionId: Long): ChargingSessionWithPoints

    @Query("SELECT min(start_epoch_time) FROM DrivingSession")
    fun getEarliestEpochTime(): Long

    @Query("SELECT charging_session_id FROM DrivingChargingCrossRef WHERE driving_session_id = :sessionId")
    fun getChargingSessionIdsByDrivingSessionId(sessionId: Long): List<Long>

    @Query("DELETE FROM DrivingPoint WHERE driving_point_epoch_time < :earliestEpochTime")
    fun clearOldDrivingPoints(earliestEpochTime: Long): Int

    @Query("DELETE FROM ChargingPoint WHERE charging_point_epoch_time < :earliestEpochTime")
    fun clearOldChargingPoints(earliestEpochTime: Long): Int

    @Query("DELETE FROM ChargingSession WHERE start_epoch_time < :earliestEpochTime")
    fun clearOldChargingSessions(earliestEpochTime: Long): Int

    @Query("DELETE FROM DrivingSessionPointCrossRef WHERE driving_session_id = :sessionId")
    fun clearOldDrivingSessionPointCrossRefs(sessionId: Long): Int

    @Query("DELETE FROM DrivingChargingCrossRef WHERE driving_session_id = :sessionId")
    fun clearOldDrivingChargingCrossRefs(sessionId: Long): Int

    @Query("DELETE FROM DrivingSession WHERE driving_session_id = :sessionId")
    fun deleteDrivingSessionByID(sessionId: Long): Int

    @Query("DELETE FROM ChargingSession WHERE charging_session_id = :sessionId")
    fun deleteChargingSessionById(sessionId: Long): Int

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

