package com.ixam97.carStatsViewer.database.tripData

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ixam97.carStatsViewer.plot.enums.PlotMarkerType

@Entity(
    tableName = "DrivingSessions",
    primaryKeys = ["startTime", "sessionType"]
)
data class DrivingSession(
    val startTime: Long,
    val sessionType: Int,
    var endTime: Long?,
    var driveTime: Long,
    var usedEnergy: Double,
    var traveledDistance: Double,
    var note: String?
)

@Entity(tableName = "ChargingSessions")
data class ChargingSession(
    @PrimaryKey val startTime: Long,
    var endTime: Long?,
    var ambientTemp: Float,
    var chargedEnergy: Double,
    var chargedSoC: Float,
    val lat: Float?,
    val lon: Float?
)

@Entity(tableName = "DrivingPoints")
data class DrivingPoint(
    @PrimaryKey val epochTime: Long,
    val usedEnergyDelta: Float,
    val traveledDistanceDelta: Float,
    val stateOfCharge: Float,
    val marker: String?,
    val lat: Float?,
    val lon: Float?,
    val alt: Float?
) {
    override fun toString(): String {
        return "DrivingPoint: time: $epochTime distanceDelta: $traveledDistanceDelta m"
    }
}

@Entity(tableName = "ChargingPoints")
data class ChargingPoint(
    @PrimaryKey val epochTime: Long,
    val chargedEnergyDelta: Float,
    val power: Float,
    val marker: String?,
    val stateOfCharge: Float
)

@Entity(tableName = "Markers")
data class Marker(
    @PrimaryKey(autoGenerate = true) var id: Int? = null,
    val MarkerType: PlotMarkerType,
    val MarkerVersion: Int? = null,
    val StartTime: Long,
    var EndTime: Long? = null,
    val StartDistance: Float,
    var EndDistance: Float? = null
)