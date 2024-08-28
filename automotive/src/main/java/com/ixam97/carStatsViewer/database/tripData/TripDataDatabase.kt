package com.ixam97.carStatsViewer.database.tripData

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities =
    [
        DrivingSession::class,
        DrivingPoint::class,
        ChargingSession::class,
        ChargingPoint::class,
        DrivingSessionPointCrossRef::class,
        DrivingChargingCrossRef::class,
        SessionMarker::class
    ],
    version = 6
)
abstract class TripDataDatabase: RoomDatabase() {

    abstract fun tripDao(): TripDao
}