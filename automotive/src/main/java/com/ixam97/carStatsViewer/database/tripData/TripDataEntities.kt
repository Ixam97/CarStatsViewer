package com.ixam97.carStatsViewer.database.tripData

import androidx.room.*


/**
 * Database tables:
 */

@Entity
data class DrivingSession(
    @PrimaryKey(autoGenerate = true) var driving_session_id: Long = 0,
    val start_epoch_time: Long,
    val end_epoch_time: Long?,
    val session_type: Int,
    val drive_time: Long,
    val used_energy: Double,
    val used_soc: Double,
    val used_soc_energy: Double,
    val driven_distance: Double,
    val note: String
) {
    @Ignore var drivingPoints: List<DrivingPoint>? = null
    @Ignore var chargingSessions: List<ChargingSession>? = null
    @Ignore var sessionMarkers: List<SessionMarker>? = null
    @Ignore var deleteMarker = false
}

@Entity
data class DrivingPoint(
    @PrimaryKey val driving_point_epoch_time: Long,
    val energy_delta: Float,
    val distance_delta: Float,
    val point_marker_type: Int?,
    val state_of_charge: Float,
    val lat: Float?,
    val lon: Float?,
    val alt: Float?
)

@Entity
data class ChargingSession(
    @PrimaryKey(autoGenerate = true) var charging_session_id: Long = 0,
    val start_epoch_time: Long,
    val end_epoch_time: Long?,
    val charged_energy: Double,
    val charged_soc: Float,
    val outside_temp: Float,
    val lat: Float?,
    val lon: Float?
) {
    @Ignore var chargingPoints: List<ChargingPoint>? = null
}

@Entity
data class ChargingPoint(
    @PrimaryKey val charging_point_epoch_time: Long,
    val charging_session_id: Long,
    val energy_delta: Float,
    val power: Float,
    val state_of_charge: Float,
    val point_marker_type: Int?
)

@Entity
data class SessionMarker(
    @PrimaryKey val marker_start_epoch_time: Long,
    val marker_end_epoch_time: Long?,
    val session_marker_type: Int
)

/**
 * Cross references:
 */

@Entity(primaryKeys = ["driving_session_id", "driving_point_epoch_time"])
data class DrivingSessionPointCrossRef(
    val driving_session_id: Long,
    val driving_point_epoch_time: Long
)

@Entity(primaryKeys = ["driving_session_id", "charging_session_id"])
data class DrivingChargingCrossRef(
    val driving_session_id: Long,
    val charging_session_id: Long
)

/**
 * Relations:
 */

data class CompleteTripData(
    @Embedded val drivingSession: DrivingSession,
    @Relation(
        parentColumn = "driving_session_id",
        entityColumn = "driving_point_epoch_time",
        associateBy = Junction(DrivingSessionPointCrossRef::class)
    ) val drivingPoints: List<DrivingPoint>,
    @Relation(
        parentColumn = "driving_session_id",
        entityColumn = "charging_session_id",
        entity = ChargingSession::class,
        associateBy = Junction(
            value = DrivingChargingCrossRef::class
        )
    ) val charging_sessions: List<ChargingSessionWithPoints>
)

data class ChargingSessionWithPoints(
    @Embedded val chargingSession: ChargingSession,
    @Relation(
        parentColumn = "charging_session_id",
        entityColumn = "charging_session_id"
    ) val chargingPoints: List<ChargingPoint>
)















