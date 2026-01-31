package com.ixam97.carStatsViewer.repository.dataExport

import android.os.Build
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.repository.logSubmit.LogSubmitRepository
import com.ixam97.carStatsViewer.utils.StringFormatters
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date

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

    suspend fun exportChargingSessionData(chargingSession: ChargingSession): DataExportStatus {


        var sessionCsvData = "# Charging session recorded by Car Stats Viewer v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})\n#\n"
        sessionCsvData += "# Vehicle brand and model: ${Build.BRAND}, ${Build.MODEL} (${Build.DEVICE})\n"
        sessionCsvData += "# Date and time: ${StringFormatters.getDateString(Date(chargingSession.start_epoch_time))}\n"
        sessionCsvData += "# Charging duration: ${StringFormatters.getElapsedTimeString((chargingSession.end_epoch_time?:chargingSession.start_epoch_time) - chargingSession.start_epoch_time)}\n"
        sessionCsvData += "# Charged energy: ${StringFormatters.getEnergyString(chargingSession.charged_energy.toFloat())}\n"
        if (chargingSession.lon != null && chargingSession.lat != null) {
            sessionCsvData += "# Location: ${chargingSession.lon}, ${chargingSession.lat}\n"
        }
        sessionCsvData += "# Outside temperature: ${"%.1f".format(chargingSession.outside_temp)} degC\n"

        sessionCsvData += "\nTime [s];Power [kW];SoC [%]\n"

        chargingSession.chargingPoints?.forEach { chargingPoint ->
            val pointSeconds = (chargingPoint.charging_point_epoch_time - chargingSession.start_epoch_time) / 1000
            sessionCsvData += "${pointSeconds};${"%.3f".format(chargingPoint.power / -1000000)};${(chargingPoint.state_of_charge * 100).toLong()}\n"
        }

        val body = ChargingSessionDataExportBody(
            receiverMailAddress = CarStatsViewer.appPreferences.dataExportAddress,
            startTime = chargingSession.start_epoch_time,
            lat = chargingSession.lat,
            lon = chargingSession.lon,
            outsideTemp = chargingSession.outside_temp,
            sessionCsvData = sessionCsvData
        )

        apiKey?.let { key ->
            dataExportApi.run {
                val response = chargingSessionDataExport(
                    apiKey = key,
                    body = body
                )
                return evaluateResponse(response)
            }
        }
        if (apiKey == null) {
            return DataExportStatus(
                state = DataExportState.Error,
                message = "No API Key available."
            )
        }
        return DataExportStatus(
            state = DataExportState.Error,
            message = "Unexpected Error."
        )
    }

    private fun evaluateResponse( response: Response<DataExportResponse>): DataExportStatus {
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
        }
        return DataExportStatus(
            state = DataExportState.Error,
            message = "Unexpected Response: (${response.code()}) ${response.errorBody()?.string()}"
        )
    }
}