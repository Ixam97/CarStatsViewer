package com.ixam97.carStatsViewer.repository.dataExport

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataExportRepositoryTest {

    private fun drivingSession(note: String) = DrivingSession(
        driving_session_id = 1L,
        start_epoch_time = 1_000L,
        end_epoch_time = 5_000L,
        session_type = TripType.MANUAL,
        drive_time = 4_000L,
        used_energy = 1_500.0,
        used_soc = 0.02,
        used_soc_energy = 1_500.0,
        driven_distance = 10_000.0,
        note = note,
        last_edited_epoch_time = 5_000L
    )

    @Test
    fun tripCsv_writesMultiLineNoteAsSingleComment() {
        val csv = DataExportRepository.buildTripCsvData(drivingSession("line 1\nline 2\r\nline 3"))

        assertTrue(csv, csv.contains("# Note: line 1 line 2 line 3\n"))
        // All header lines before the column titles must stay comments
        val headerLines = csv.substringBefore("\nTimestamp;").lines().filter { it.isNotEmpty() }
        assertTrue(csv, headerLines.all { it.startsWith("#") })
    }

    @Test
    fun tripCsv_writesEmptyNote() {
        val csv = DataExportRepository.buildTripCsvData(drivingSession(""))

        assertTrue(csv, csv.contains("# Note: \n"))
    }
}
