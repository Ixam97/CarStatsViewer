package com.ixam97.carStatsViewer.database.tripData

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities =
    [
        DrivingSession::class,
        ChargingSession::class,
        DrivingPoint::class,
        ChargingPoint::class,
        Marker::class
    ],
    version = 1
)
abstract class TripDataDatabase: RoomDatabase() {
    abstract fun tripDao(): TripDao
}