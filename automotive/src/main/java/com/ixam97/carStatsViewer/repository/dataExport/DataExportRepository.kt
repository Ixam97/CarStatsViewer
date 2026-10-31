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
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.GzipSink
import okio.buffer
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object DataExportRepository {

    private const val BASE_URL = "https://ixam97.de/"
    private const val LOG_LINES_PER_EXPORT_OBJECT = 20

    private var apiKey: String? = null

    fun setApiKey(key: String) {
        apiKey = key
    }

    private val dataExportClient = OkHttpClient.Builder()
        .addInterceptor(GzipRequestInterceptor())
        .build()
    private val dataExportApi: DataExportApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(dataExportClient)
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

    private fun buildChargingSessionCsvData(
        chargingSession: ChargingSession,
        onProgressChanged: (index: Int, size: Int) -> Unit
    ): String {
        val sessionCsvData = StringBuilder()
        sessionCsvData.append("# Charging session recorded by Car Stats Viewer v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})\n#\n")
//        sessionCsvData += "# Vehicle brand and model: ${Build.BRAND}, ${Build.MODEL} (${Build.DEVICE})\n"
//        sessionCsvData += "# Date and time: ${StringFormatters.getDateString(Date(chargingSession.start_epoch_time))}\n"
//        sessionCsvData += "# Charging duration: ${StringFormatters.getElapsedTimeString((chargingSession.end_epoch_time?:chargingSession.start_epoch_time) - chargingSession.start_epoch_time)}\n"
//        sessionCsvData += "# Charged energy: ${StringFormatters.getEnergyString(chargingSession.charged_energy.toFloat())}\n"
//        if (chargingSession.lon != null && chargingSession.lat != null) {
//            sessionCsvData += "# Location: ${chargingSession.lat}, ${chargingSession.lon}\n"
//        }
//        sessionCsvData += "# Outside temperature: ${"%.1f".format(chargingSession.outside_temp)} degC\n"

        sessionCsvData.append("# Vehicle brand and model: ${Build.BRAND}, ${Build.MODEL} (${Build.DEVICE})\n")
        sessionCsvData.append("# Start timestamp: ${chargingSession.start_epoch_time}\n")
        sessionCsvData.append("# End timestamp: ${chargingSession.end_epoch_time}\n")
        sessionCsvData.append("# Charged Energy: ${chargingSession.charged_energy}\n")
        sessionCsvData.append("# Charged SoC: ${chargingSession.charged_soc}\n")
        sessionCsvData.append("# Outside Temperature: ${chargingSession.outside_temp}\n")
        sessionCsvData.append("# Lat: ${chargingSession.lat}\n")
        sessionCsvData.append("# Lon: ${chargingSession.lon}\n")
        // sessionCsvData += "\nTime [s];Power [kW];SoC [%]\n"
        sessionCsvData.append("\nTimestamp;Charging session id;energy delta; power;SoC;point marker type\n")

        chargingSession.chargingPoints?.let { chargingPoints ->
            chargingPoints.forEachIndexed { index, point ->
                onProgressChanged(index, chargingPoints.size)
                sessionCsvData.append(point.toCsvLine())
            }
        }

        return sessionCsvData.toString()
    }

    private fun buildChargingSessionDataExportBody(
        chargingSession: ChargingSession,
        onProgressChanged: (index: Int, size: Int) -> Unit
    ): ChargingSessionDataExportBody {
        return ChargingSessionDataExportBody(
            receiverMailAddress = CarStatsViewer.appPreferences.dataExportAddress,
            startTime = chargingSession.start_epoch_time,
            lat = chargingSession.lat,
            lon = chargingSession.lon,
            outsideTemp = chargingSession.outside_temp,
            sessionCsvData = buildChargingSessionCsvData(chargingSession, onProgressChanged)
        )
    }

    suspend fun exportChargingSessionData(
        chargingSession: ChargingSession,
        updateExportProgressState: (progressState: DataExportProgressState) -> Unit = {}
    ): DataExportStatus {

        if (!CarStatsViewer.appPreferences.dataExportEnabled) {
            InAppLogger.w("[DataExportRepository] Data Export has not been enabled in API Settings!")
            return DataExportStatus(
                state = DataExportState.Error,
                message = "Data Export has not been enabled in API Settings!"
            )
        }

        InAppLogger.i("[DataExportRepository] Exporting charging session with ID ${chargingSession.charging_session_id}...")

        val body = buildChargingSessionDataExportBody(chargingSession) { index, size ->
            updateExportProgressState(DataExportProgressState.ParsingChargingSession((index + 1).toFloat() / size))
            if (size <= LOG_LINES_PER_EXPORT_OBJECT || (index + 1) == size || (index + 1).mod(size / LOG_LINES_PER_EXPORT_OBJECT) == 0) {
                InAppLogger.v("[DataExportRepository] Parsed ${index + 1}/$size points")
            }
        }

        updateExportProgressState(DataExportProgressState.Uploading)

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

    private fun buildTripCsvData(
        drivingSession: DrivingSession,
        onProgressChanged: (index: Int, size: Int) -> Unit
    ): String {
        InAppLogger.v("[DataExportRepository] Parsing trip data...")

        val sessionCsvData = StringBuilder()
        sessionCsvData.append("# Trip recorded by Car Stats Viewer v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})\n#\n")
        sessionCsvData.append("# Vehicle brand and model: ${Build.BRAND}, ${Build.MODEL} (${Build.DEVICE})\n")
        sessionCsvData.append("# Start timestamp: ${drivingSession.start_epoch_time}\n")
        sessionCsvData.append("# End timestamp: ${drivingSession.end_epoch_time}\n")
        sessionCsvData.append("# Trip type: ${drivingSession.session_type}\n")
        sessionCsvData.append("# Drive time: ${drivingSession.drive_time}\n")
        sessionCsvData.append("# Used energy: ${drivingSession.used_energy}\n")
        sessionCsvData.append("# Used SoC: ${drivingSession.used_soc}\n")
        sessionCsvData.append("# Used Soc (Energy): ${drivingSession.used_soc_energy}\n")
        sessionCsvData.append("# Driven distance: ${drivingSession.driven_distance}\n")
        sessionCsvData.append("# Note: ${drivingSession.note}\n")
        sessionCsvData.append("# Last edited: ${drivingSession.last_edited_epoch_time}\n")

        sessionCsvData.append("\nTimestamp;Energy delta;Distance delta;Point Marker Type;SoC;lat;lon;alt\n")

        drivingSession.drivingPoints?.let { drivingPoints ->
            drivingPoints.forEachIndexed { index, point ->
                onProgressChanged(index, drivingPoints.size)
                sessionCsvData.append(point.toCsvLine())
            }
        }

        return sessionCsvData.toString()
    }

    suspend fun exportTripData(
        drivingSession: DrivingSession,
        updateExportProgressState: (progressState: DataExportProgressState) -> Unit = {}
    ): DataExportStatus {

        if (!CarStatsViewer.appPreferences.dataExportEnabled) return DataExportStatus(
            state = DataExportState.Error,
            message = "Data Export has not been enabled in API Settings!"
        )

        InAppLogger.i("[DataExportRepository] Exporting trip with ID ${drivingSession.driving_session_id}...")
        val startMillis = System.currentTimeMillis()

        var totalSize = drivingSession.drivingPoints?.size?:0
        drivingSession.chargingSessions?.forEach { totalSize += it.chargingPoints?.size?:0 }
        var totalIndex = 0

        val chargingSessionsExportBodies = drivingSession.chargingSessions?.let { chargingSessions ->
            updateExportProgressState(DataExportProgressState.ParsingChargingSessions(0f))
            chargingSessions.map {
                buildChargingSessionDataExportBody(it) { index, size ->
                    totalIndex++
                    updateExportProgressState(DataExportProgressState.ParsingChargingSessions(totalIndex.toFloat() / totalSize))
                    if (size <= LOG_LINES_PER_EXPORT_OBJECT || (index + 1) == size || (index + 1).mod(size / LOG_LINES_PER_EXPORT_OBJECT) == 0) {
                        InAppLogger.v("[DataExportRepository] Parsed ${index + 1}/$size points")
                    }
                }
            }
        }?:listOf()

        val body = TripDataExportBody(
            receiverMailAddress = CarStatsViewer.appPreferences.dataExportAddress,
            startTime = drivingSession.start_epoch_time,
            tripDrivePointsCsvData = buildTripCsvData(drivingSession) { index, size ->
                totalIndex++
                updateExportProgressState(DataExportProgressState.ParsingTrip(totalIndex.toFloat() / totalSize))
                if (size <= LOG_LINES_PER_EXPORT_OBJECT || (index + 1) == size || (index + 1).mod(size / LOG_LINES_PER_EXPORT_OBJECT) == 0) {
                    InAppLogger.v("[DataExportRepository] Parsed ${index + 1}/$size points")
                }
            },
            chargingSessionsData = chargingSessionsExportBodies
        )

        updateExportProgressState(DataExportProgressState.Uploading)

        apiKey?.let { key ->
            dataExportApi.run {
                val response = tripDataExport(
                    apiKey = key,
                    body = body
                )
                val endMillis = System.currentTimeMillis()
                InAppLogger.i("[DataExportRepository] Export took ${endMillis - startMillis} ms")
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
                InAppLogger.e("[DataExportRepository] Error body decoding error: ${e.message}")
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

class GzipRequestInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val body = originalRequest.body

        val compressedRequest = originalRequest.newBuilder()
            .header("Content-Encoding", "gzip")
            .method(originalRequest.method, body?.compressWithLength())
            .build()

        return chain.proceed(compressedRequest)
    }

    private fun RequestBody.compressWithLength() : RequestBody {
        val buffer = Buffer()
        this.compress().writeTo(buffer)
        InAppLogger.v("[DataExportRepository] Compression done, calculating size ...")
        InAppLogger.d("[DataExportRepository] Compressed request size: ${buffer.size}")
        return object : RequestBody() {
            override fun contentType(): MediaType? = this@compressWithLength.contentType()
            override fun contentLength(): Long = buffer.size
            override fun writeTo(sink: BufferedSink) {
                sink.write(buffer.snapshot())
            }
        }
    }
    private fun RequestBody.compress() : RequestBody {
        val buffer = Buffer();
        this.writeTo(buffer)
        InAppLogger.v("[DataExportRepository] Uncompressed request size: ${buffer.size}")
        InAppLogger.v("[DataExportRepository] Compressing request ...")
        return object : RequestBody() {
            override fun contentType(): MediaType? = this@compress.contentType()
            override fun contentLength(): Long = -1
            override fun writeTo(sink: BufferedSink) {
                val gzipSink = GzipSink(sink).buffer()
                this@compress.writeTo(gzipSink)
                gzipSink.close()
            }
        }
    }
}