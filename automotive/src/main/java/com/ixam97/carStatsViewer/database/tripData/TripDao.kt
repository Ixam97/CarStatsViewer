package com.ixam97.carStatsViewer.database.tripData

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ixam97.carStatsViewer.plot.objects.PlotLineItem

@Dao
interface TripDao {
    @Upsert
    fun addDrivingPoint(drivingPoint: DrivingPoint)

    @Query("SELECT * FROM DrivingPoints")
    fun getAllDrivingPoints(): List<DrivingPoint>

}