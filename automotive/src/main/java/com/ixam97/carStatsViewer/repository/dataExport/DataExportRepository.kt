package com.ixam97.carStatsViewer.repository.dataExport

import android.os.Build
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.database.tripData.ChargingPoint
import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.serialization.json.Json
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object DataExportRepository {

    private const val BASE_URL = "https://ixam97.de/"

    private var apiKey: String? = null

    fun setApiKey(key: String) {
        apiKey = key
    }

    private val dataExportApi: DataExportApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DataExportApi::class.java)
    }

    private fun ChargingPoint.toCsvLine(): String {
        return "${"%d".format(this.charging_point_epoch_time)};" +
               "${"%d".format(this.charging_session_id)};" +
               "${"%.04f".format(this.energy_delta)};" +
               "${"%.04f".format(this.power)};" +
               "${"%.02f".format(this.state_of_charge)};" +
               "${this.point_marker_type?:""}\n"
    }

    private fun DrivingPoint.toCsvLine(): String {
        return "${"%d".format(this.driving_point_epoch_time)};" +
               "${"%.04f".format(this.energy_delta)};" +
               "${"%.04f".format(this.distance_delta)};" +
               "${this.point_marker_type?:""};" +
               "${"%.02f".format(this.state_of_charge)};" +
               "${this.lat?.let { lat -> "%.06f".format(lat) }};" +
               "${this.lon?.let { lon -> "%.06f".format(lon) }};" +
               "${this.alt?.let { alt -> "%.0f".format(alt) }}\n"
    }

    private fun buildChargingSessionCsvData(chargingSession: ChargingSession): String {
        var sessionCsvData = "# Charging session recorded by Car Stats Viewer v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})\n#\n"
//        sessionCsvData += "# Vehicle brand and model: ${Build.BRAND}, ${Build.MODEL} (${Build.DEVICE})\n"
//        sessionCsvData += "# Date and time: ${StringFormatters.getDateString(Date(chargingSession.start_epoch_time))}\n"
//        sessionCsvData += "# Charging duration: ${StringFormatters.getElapsedTimeString((chargingSession.end_epoch_time?:chargingSession.start_epoch_time) - chargingSession.start_epoch_time)}\n"
//        sessionCsvData += "# Charged energy: ${StringFormatters.getEnergyString(chargingSession.charged_energy.toFloat())}\n"
//        if (chargingSession.lon != null && chargingSession.lat != null) {
//            sessionCsvData += "# Location: ${chargingSession.lat}, ${chargingSession.lon}\n"
//        }
//        sessionCsvData += "# Outside temperature: ${"%.1f".format(chargingSession.outside_temp)} degC\n"

        sessionCsvData += "# Vehicle brand and model: ${Build.BRAND}, ${Build.MODEL} (${Build.DEVICE})\n"
        sessionCsvData += "# Start timestamp: ${chargingSession.start_epoch_time}\n"
        sessionCsvData += "# End timestamp: ${chargingSession.end_epoch_time}\n"
        sessionCsvData += "# Charged Energy: ${chargingSession.charged_energy}\n"
        sessionCsvData += "# Charged SoC: ${chargingSession.charged_soc}\n"
        sessionCsvData += "# Outside Temperature: ${chargingSession.outside_temp}\n"
        sessionCsvData += "# Lat: ${chargingSession.lat}\n"
        sessionCsvData += "# Lon: ${chargingSession.lon}\n"
        // sessionCsvData += "\nTime [s];Power [kW];SoC [%]\n"
        sessionCsvData += "\nTimestamp;Charging session id;energy delta; power;SoC;point marker type\n"

        chargingSession.chargingPoints?.forEach { chargingPoint ->
            sessionCsvData += chargingPoint.toCsvLine()
            // val pointSeconds = (chargingPoint.charging_point_epoch_time - chargingSession.start_epoch_time) / 1000
            // sessionCsvData += "${pointSeconds};${"%.3f".format(chargingPoint.power / -1000000)};${(chargingPoint.state_of_charge * 100).toLong()}\n"
        }

        return sessionCsvData
    }

    private fun buildChargingSessionDataExportBody(chargingSession: ChargingSession): ChargingSessionDataExportBody {
        return ChargingSessionDataExportBody(
            receiverMailAddress = CarStatsViewer.appPreferences.dataExportAddress,
            startTime = chargingSession.start_epoch_time,
            lat = chargingSession.lat,
            lon = chargingSession.lon,
            outsideTemp = chargingSession.outside_temp,
            sessionCsvData = buildChargingSessionCsvData(chargingSession)
        )
    }

    suspend fun exportChargingSessionData(chargingSession: ChargingSession): DataExportStatus {

        if (!CarStatsViewer.appPreferences.dataExportEnabled) {
            InAppLogger.w("[DataExportRepository] Data Export has not been enabled in API Settings!")
            return DataExportStatus(
                state = DataExportState.Error,
                message = "Data Export has not been enabled in API Settings!"
            )
        }

        InAppLogger.i("[DataExportRepository] Exporting charging session with ID ${chargingSession.charging_session_id}...")

        val body = buildChargingSessionDataExportBody(chargingSession)

        apiKey?.let { key ->
            dataExportApi.run {
                val response = chargingSessionDataExport(
                    apiKey = key,
                    body = body
                )
                val status = evaluateResponse(response)
                if (status.state == DataExportState.Success) {
                    InAppLogger.i("[DataExportRepository] Export Successful. Message: ${status.message}")
                } else {
                    InAppLogger.e("[DataExportRepository] Export failed. Message: ${status.message}")
                }
                return status
            }
        }
        if (apiKey == null) {
            InAppLogger.w("[DataExportRepository] No API Key configured.")
            return DataExportStatus(
                state = DataExportState.Error,
                message = "No API Key configured."
            )
        }
        return DataExportStatus(
            state = DataExportState.Error,
            message = "Unexpected Error."
        )
    }

    private fun buildTripCsvData(drivingSession: DrivingSession): String {
        var sessionCsvData = "# Trip recorded by Car Stats Viewer v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})\n#\n"
        sessionCsvData += "# Vehicle brand and model: ${Build.BRAND}, ${Build.MODEL} (${Build.DEVICE})\n"
        sessionCsvData += "# Start timestamp: ${drivingSession.start_epoch_time}\n"
        sessionCsvData += "# End timestamp: ${drivingSession.end_epoch_time}\n"
        sessionCsvData += "# Trip type: ${drivingSession.session_type}\n"
        sessionCsvData += "# Drive time: ${drivingSession.drive_time}\n"
        sessionCsvData += "# Used energy: ${drivingSession.used_energy}\n"
        sessionCsvData += "# Used SoC: ${drivingSession.used_soc}\n"
        sessionCsvData += "# Used Soc (Energy): ${drivingSession.used_soc_energy}\n"
        sessionCsvData += "# Driven distance: ${drivingSession.driven_distance}\n"
        sessionCsvData += "# Note: ${drivingSession.note}\n"
        sessionCsvData += "# Last edited: ${drivingSession.last_edited_epoch_time}\n"

        sessionCsvData += "\nTimestamp;Energy delta;Distance delta;Point Marker Type;SoC;lat;lon;alt\n"

        drivingSession.drivingPoints?.forEach {
            sessionCsvData += it.toCsvLine()
        }

        return sessionCsvData
    }

    suspend fun exportTripData(drivingSession: DrivingSession): DataExportStatus {

        if (!CarStatsViewer.appPreferences.dataExportEnabled) return DataExportStatus(
            state = DataExportState.Error,
            message = "Data Export has not been enabled in API Settings!"
        )

        InAppLogger.i("[DataExportRepository] Exporting trip with ID ${drivingSession.driving_session_id}...")

        val chargingSessions = drivingSession.chargingSessions?.map {
            buildChargingSessionDataExportBody(it)
        }?:listOf()

        val body = TripDataExportBody(
            receiverMailAddress = CarStatsViewer.appPreferences.dataExportAddress,
            startTime = drivingSession.start_epoch_time,
            tripDrivePointsCsvData = buildTripCsvData(drivingSession),
            chargingSessionsData = chargingSessions
        )

        apiKey?.let { key ->
            dataExportApi.run {
                val response = tripDataExport(
                    apiKey = key,
                    body = body
                )
                return evaluateResponse(response)
            }
        }
        if (apiKey == null) {
            return DataExportStatus(
                state = DataExportState.Error,
                message = "No API Key configured."
            )
        }
        return DataExportStatus(
            state = DataExportState.Error,
            message = "Unexpected Error."
        )
    }

    private fun evaluateResponse( response: Response<DataExportResponse>): DataExportStatus {
        val json = Json { ignoreUnknownKeys = true }
        if (response.body() is DataExportResponse) {
            response.body()?.let { body ->
                return if (body.status == "OK") {
                    DataExportStatus(state = DataExportState.Success)
                } else {
                    DataExportStatus(
                        state = DataExportState.Error,
                        message = "Server response: (${response.code()})\n\r${body.status}: ${body.message}"
                    )
                }
            }
        } else if (response.code() == 422 && response.errorBody() != null) {
            try {
                val body = json.decodeFromString<DataExportResponse>(response.errorBody()!!.string())
                return DataExportStatus(
                    state = DataExportState.Error,
                    message = body.message
                )
            } catch (e: Exception) {
                InAppLogger.e("[Data Export Repository] Error body decoding error: ${e.message}")
                return DataExportStatus(
                    state = DataExportState.Error,
                    message = "Unexpected Response: (${response.code()}) ${response.errorBody()?.string()}"
                )
            }

        }
        return DataExportStatus(
            state = DataExportState.Error,
            message = "Unexpected Response: (${response.code()}) ${response.errorBody()?.string()}"
        )
    }
}