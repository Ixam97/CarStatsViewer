package com.ixam97.carStatsViewer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.database.log.LogDatabase
import com.ixam97.carStatsViewer.database.tripData.LocalTripDataSource
import com.ixam97.carStatsViewer.database.tripData.TripDataDatabase

/**
 * Initializes the static members of [CarStatsViewer] that are normally set up in
 * CarStatsViewer.onCreate() (which is not run in unit tests, see robolectric.properties), backed
 * by in-memory databases. Needed by code that logs via InAppLogger or reads app preferences.
 */
class TestAppEnvironment {

    val context: Context = ApplicationProvider.getApplicationContext()

    val tripDatabase: TripDataDatabase = Room.inMemoryDatabaseBuilder(context, TripDataDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    private val logDatabase: LogDatabase = Room.inMemoryDatabaseBuilder(context, LogDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    val tripDataSource = LocalTripDataSource(tripDatabase.tripDao())

    init {
        CarStatsViewer.appContext = context
        CarStatsViewer.appPreferences = AppPreferences(context)
        CarStatsViewer.logDao = logDatabase.logDao()
        CarStatsViewer.tripDatabase = tripDatabase
        CarStatsViewer.tripDataSource = tripDataSource
    }

    fun close() {
        tripDatabase.close()
        logDatabase.close()
    }
}
