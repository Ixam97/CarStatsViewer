package com.ixam97.carStatsViewer.database.tripData

import androidx.annotation.RawRes
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R

object DummyTripData {
    fun getDummyDrivingSession(): DrivingSession = loadDrivingDataFromResource(R.raw.dummy_trip).apply {
        this.chargingSessions = listOf(
            loadChargingDataFromResource(R.raw.dummy_charging_session_1, -1),
            loadChargingDataFromResource(R.raw.dummy_charging_session_2, -2),
            loadChargingDataFromResource(R.raw.dummy_charging_session_3, -3)
        )
    }

    private fun loadDrivingDataFromResource(@RawRes resId: Int): DrivingSession {
        val input = CarStatsViewer.appContext.resources.openRawResource(resId)

        val reader = input.bufferedReader()

        var startEpoch: Long = 0
        var endEpoch: Long? = null
        var sessionType: Int = 0
        var driveTime: Long = 0
        var usedEnergy: Double = 0.0
        var usedSoc: Double = 0.0
        var usedSocEnergy: Double = 0.0
        var drivenDistance: Double = 0.0
        var note: String = ""
        var lastEdited: Long = 0

        val drivingPoints = mutableListOf<DrivingPoint>()

        reader.useLines { lines ->
            lines.forEach { line ->

                // --- Parse DrivingSession header ---
                if (line.startsWith("#")) {
                    val clean = line.removePrefix("#").trim()

                    when {
                        clean.startsWith("Start timestamp:") ->
                            startEpoch = clean.substringAfter(":").trim().toLong()

                        clean.startsWith("End timestamp:") ->
                            clean.substringAfter(":").trim().let {
                                endEpoch = if (it == "0") null else it.toLong()
                            }

                        clean.startsWith("Trip type:") ->
                            sessionType = clean.substringAfter(":").trim().toInt()

                        clean.startsWith("Drive time:") ->
                            driveTime = clean.substringAfter(":").trim().toLong()

                        clean.startsWith("Used energy:") ->
                            usedEnergy = clean.substringAfter(":").trim().toDouble()

                        clean.startsWith("Used SoC:") ->
                            usedSoc = clean.substringAfter(":").trim().toDouble()

                        clean.startsWith("Used Soc (Energy):") ->
                            usedSocEnergy = clean.substringAfter(":").trim().toDouble()

                        clean.startsWith("Driven distance:") ->
                            drivenDistance = clean.substringAfter(":").trim().toDouble()

                        clean.startsWith("Note:") ->
                            note = clean.substringAfter(":").trim()

                        clean.startsWith("Last edited:") ->
                            lastEdited = clean.substringAfter(":").trim().toLong()
                    }

                    return@forEach
                }

                // --- Skip header row ---
                if (line.startsWith("Timestamp") || line.isBlank()) return@forEach

                // --- Parse DrivingPoints ---
                val parts = line.split(";")// .filter { it.isNotEmpty() }
                if (parts.size < 8) return@forEach

                drivingPoints += DrivingPoint(
                    driving_point_epoch_time = parts[0].toLong(),
                    energy_delta = parts[1].toFloat(),
                    distance_delta = parts[2].toFloat(),
                    point_marker_type = parts[3].takeIf { it.isNotBlank() && it != "null" }?.toInt(),
                    state_of_charge = parts[4].toFloat(),
                    lat = parts[5].takeIf { it.isNotBlank() && it != "null" }?.toFloat(),
                    lon = parts[6].takeIf { it.isNotBlank() && it != "null" }?.toFloat(),
                    alt = parts[7].takeIf { it.isNotBlank() && it != "null" }?.toFloat()
                )
            }
        }

        val session = DrivingSession(
            driving_session_id = -1,
            start_epoch_time = startEpoch,
            end_epoch_time = endEpoch,
            session_type = sessionType,
            drive_time = driveTime,
            used_energy = usedEnergy,
            used_soc = usedSoc,
            used_soc_energy = usedSocEnergy,
            driven_distance = drivenDistance,
            note = note,
            last_edited_epoch_time = lastEdited
        )

        return session.apply { this.drivingPoints = drivingPoints }
    }

    private fun loadChargingDataFromResource(@RawRes resId: Int, id: Long): ChargingSession {
        val input = CarStatsViewer.appContext.resources.openRawResource(resId)

        val reader = input.bufferedReader()

        // --- Session fields ---
        var startEpoch: Long = 0
        var endEpoch: Long? = null
        var chargedEnergy: Double = 0.0
        var chargedSoc: Float = 0f
        var outsideTemp: Float = 0f
        var lat: Float? = null
        var lon: Float? = null

        val chargingPoints = mutableListOf<ChargingPoint>()

        reader.useLines { lines ->
            lines.forEach { rawLine ->

                val line = rawLine.trim()

                // --- Parse header metadata ---
                if (line.startsWith("#")) {
                    val clean = line.removePrefix("#").trim()

                    when {
                        clean.startsWith("Start timestamp:") ->
                            startEpoch = clean.substringAfter(":").trim().toLong()

                        clean.startsWith("End timestamp:") ->
                            clean.substringAfter(":").trim().let {
                                endEpoch = if (it == "0") null else it.toLong()
                            }

                        clean.startsWith("Charged Energy:") ->
                            chargedEnergy = clean.substringAfter(":").trim().toDouble()

                        clean.startsWith("Charged SoC:") ->
                            chargedSoc = clean.substringAfter(":").trim().toFloat()

                        clean.startsWith("Outside Temperature:") ->
                            outsideTemp = clean.substringAfter(":").trim().toFloat()

                        clean.startsWith("Lat:") ->
                            lat = clean.substringAfter(":").trim().toFloat()

                        clean.startsWith("Lon:") ->
                            lon = clean.substringAfter(":").trim().toFloat()
                    }

                    return@forEach
                }

                // --- Skip header row ---
                if (line.startsWith("Timestamp") || line.isBlank()) return@forEach

                // --- Parse ChargingPoints ---
                val parts = line.split(";")

                if (parts.size < 6) return@forEach

                val timestamp = parts[0].toLong()
                val sessionId = parts[1].toLong()

                val energyDelta = parts[2].toFloat()
                val power = parts[3].toFloat()
                val soc = parts[4].toFloat()

                val marker = parts.getOrNull(5)?.takeIf { it.isNotBlank() }?.toIntOrNull()

                chargingPoints += ChargingPoint(
                    charging_point_epoch_time = timestamp,
                    charging_session_id = id,
                    energy_delta = energyDelta,
                    power = power,
                    state_of_charge = soc,
                    point_marker_type = marker
                )
            }
        }

        val session = ChargingSession(
            charging_session_id = id,
            start_epoch_time = startEpoch,
            end_epoch_time = endEpoch,
            charged_energy = chargedEnergy,
            charged_soc = chargedSoc,
            outside_temp = outsideTemp,
            lat = lat,
            lon = lon
        )

        return session.apply { this.chargingPoints = chargingPoints }
    }
}