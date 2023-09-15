package com.ixam97.carStatsViewer.liveDataApi.http

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import com.google.gson.Gson
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataProcessor.IgnitionState
import com.ixam97.carStatsViewer.dataProcessor.RealTimeData
import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.liveDataApi.LiveDataApi
import com.ixam97.carStatsViewer.liveDataApi.abrpLiveData.AbrpLiveData
import com.ixam97.carStatsViewer.ui.views.MultiButtonWidget
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.StringFormatters
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.ArrayList


class HttpLiveData (
    detailedLog : Boolean = true
): LiveDataApi("Webhook", R.string.settings_apis_http, detailedLog) {

    private var successCounter: Int = 0
    private val drivingPointBacklog: ArrayList<DrivingPoint> = arrayListOf()
    private val chargingSessionBacklog: ArrayList<ChargingSession> = arrayListOf()
    private val mutex = Mutex()
    private var firstDatapoint = true

    private fun addBasicAuth(connection: HttpURLConnection, username: String, password: String) {
        if (username == ""  && password == "") {
            return
        }

        val encoded: String = Base64.getEncoder()
            .encodeToString("$username:$password".toByteArray(StandardCharsets.UTF_8)) //Java 8

        connection.setRequestProperty("Authorization", "Basic $encoded")
    }

    private fun getConnection(url: URL, username: String, password: String) : HttpURLConnection {
        val con: HttpURLConnection = url.openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
        con.setRequestProperty("Accept","application/json")
        con.setRequestProperty("User-Agent", "CarStatsViewer %s".format(BuildConfig.VERSION_NAME))
        con.connectTimeout = timeout
        con.readTimeout = timeout
        con.doOutput = true
        con.doInput = true

        addBasicAuth(con, username, password)

        return con
    }

    private fun isValidURL(possibleURL: CharSequence?): Boolean {
        if (possibleURL == null) {
            return false
        }

        if (!possibleURL.contains("https://"))
            return false

        return android.util.Patterns.WEB_URL.matcher(possibleURL).matches()
    }

    override fun showSettingsDialog(context: Context) {
        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_http_live_data, null)
        val url = layout.findViewById<EditText>(R.id.http_live_data_url)
        val username = layout.findViewById<EditText>(R.id.http_live_data_username)
        val password = layout.findViewById<EditText>(R.id.http_live_data_password)
        val httpLiveDataEnabled = layout.findViewById<Switch>(R.id.http_live_data_enabled)
        val httpLiveDataLocation = layout.findViewById<Switch>(R.id.http_live_data_location)
        val abrpDebug = layout.findViewById<Switch>(R.id.http_live_data_abrp)
        val apiTypeMultiButton = layout.findViewById<MultiButtonWidget>(R.id.http_live_data_type)
        val confirmButton = layout.findViewById<Button>(R.id.http_live_data_confirm)

        val httpLiveDataSettingsDialog = AlertDialog.Builder(context).apply {
            setView(layout)

            /*
            setPositiveButton("OK") { _, _ ->
                AppPreferences(context).httpLiveDataURL = url.text.toString()
                AppPreferences(context).httpLiveDataUsername = username.text.toString()
                AppPreferences(context).httpLiveDataPassword = password.text.toString()
            }

            setTitle(context.getString(R.string.settings_apis_http))
            setMessage(context.getString(R.string.http_description))
            */
            setCancelable(true)
            create()
        }

        val dialog = httpLiveDataSettingsDialog.show()

        httpLiveDataEnabled.isChecked = AppPreferences(context).httpLiveDataEnabled
        httpLiveDataLocation.isChecked = AppPreferences(context).httpLiveDataLocation
        abrpDebug.isChecked = AppPreferences(context).httpLiveDataSendABRPDataset
        apiTypeMultiButton.selectedIndex = AppPreferences(context).httpApiTelemetryType

        confirmButton.isSelected = true

        confirmButton.setOnClickListener {
            AppPreferences(context).httpLiveDataURL = url.text.toString()
            AppPreferences(context).httpLiveDataUsername = username.text.toString()
            AppPreferences(context).httpLiveDataPassword = password.text.toString()
            dialog.cancel()
        }

        httpLiveDataEnabled.setOnClickListener {
            AppPreferences(context).httpLiveDataEnabled = httpLiveDataEnabled.isChecked
        }
        httpLiveDataLocation.setOnClickListener {
            AppPreferences(context).httpLiveDataLocation = httpLiveDataLocation.isChecked
        }
        abrpDebug.setOnClickListener {
            AppPreferences(context).httpLiveDataSendABRPDataset = abrpDebug.isChecked
        }
        apiTypeMultiButton.setOnIndexChangedListener {
            AppPreferences(context).httpApiTelemetryType = apiTypeMultiButton.selectedIndex
        }

        url.setText(AppPreferences(context).httpLiveDataURL)
        username.setText(AppPreferences(context).httpLiveDataUsername)
        password.setText(AppPreferences(context).httpLiveDataPassword)

        // Enable the Ok button initially only in case the user already entered a valid URL
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isValidURL(url.text.toString())

        url.addTextChangedListener(object : TextValidator(url) {
            override fun validate(textView: TextView?, text: String?) {
                if (text == null || textView == null) {
                    return
                }
                if (!isValidURL(text) && text.isNotEmpty()) {
                    textView.error = context.getString(R.string.http_invalid_url)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    return
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        })
    }

    suspend fun sendWithDrivingPoint(realTimeData: RealTimeData, drivingPoints: List<DrivingPoint>? = null, chargingSessions: List<ChargingSession>? = null): LiveDataApi.ConnectionStatus? {
        // Wrap with mutex lock to prevent concurrent reads of the backlog.
        mutex.withLock {
            if (!AppPreferences(CarStatsViewer.appContext).httpLiveDataEnabled) {
                connectionStatus = ConnectionStatus.UNUSED
                return null
            }


            if (CarStatsViewer.appPreferences.httpApiTelemetryType > 0) {
                // Don't send driving points if disabled in settings
                if (drivingPoints != null) {
                    if (!AppPreferences(CarStatsViewer.appContext).httpLiveDataLocation) {
                        drivingPoints?.forEach {drivingPoint ->
                            drivingPointBacklog.add(drivingPoint.copy(lat = null, lon = null, alt = null))
                        }
                    } else {
                        drivingPointBacklog.addAll(drivingPoints)
                    }
                }
                if (chargingSessions != null) {
                    if (!AppPreferences(CarStatsViewer.appContext).httpLiveDataLocation) {
                        chargingSessions?.forEach { chargingSession ->
                            chargingSessionBacklog.add(chargingSession.copy(lat = null, lon = null))
                        }
                    }
                    else {
                        chargingSessionBacklog.addAll(chargingSessions)
                    }
                }
            } else {
                drivingPointBacklog.clear()
                chargingSessionBacklog.clear()
            }

            if (!realTimeData.isInitialized()) return null

            connectionStatus = try {
                val useLocation = AppPreferences(CarStatsViewer.appContext).httpLiveDataLocation
                val gson = Gson()
                val dataSet = HttpDataSet(
                    timestamp = System.currentTimeMillis(),
                    speed = realTimeData.speed!!,
                    power = realTimeData.power!!,
                    selectedGear = StringFormatters.getGearString(realTimeData.selectedGear!!),
                    ignitionState = IgnitionState.nameMap[realTimeData.ignitionState!!]?:"UNKNOWN",
                    chargePortConnected = realTimeData.chargePortConnected!!,
                    batteryLevel = realTimeData.batteryLevel!!,
                    stateOfCharge = realTimeData.stateOfCharge!!,
                    ambientTemperature = realTimeData.ambientTemperature!!,
                    lat = if (useLocation) realTimeData.lat else null,
                    lon = if (useLocation) realTimeData.lon else null,
                    alt = if (useLocation) realTimeData.alt else null,

                    // ABRP debug
                    abrpPackage = if (CarStatsViewer.appPreferences.httpLiveDataSendABRPDataset) (CarStatsViewer.liveDataApis[0] as AbrpLiveData).lastPackage else null,

                    drivingPoints = if (drivingPointBacklog.size == 0) null else drivingPointBacklog,
                    chargingSessions = if (chargingSessionBacklog.size == 0) null else chargingSessionBacklog,

                    appVersion = BuildConfig.VERSION_NAME,
                    apiVersion = "2.1"
                )

                val liveDataJson = gson.toJson(dataSet)

                val sendResult = send(liveDataJson)

                // drivingPointBacklog.clear()
                if (sendResult == ConnectionStatus.CONNECTED || sendResult == ConnectionStatus.LIMITED) {
                    drivingPointBacklog.clear()
                    chargingSessionBacklog.clear()
                }

                sendResult

            } catch (e: java.lang.Exception) {
                InAppLogger.e("[HTTP] Dataset error")
                ConnectionStatus.ERROR
            }

            return connectionStatus
        }
    }
    override suspend fun sendNow(realTimeData: RealTimeData) {
        if (firstDatapoint || connectionStatus == ConnectionStatus.ERROR) {
            sendWithDrivingPoint(realTimeData)
            if (connectionStatus == ConnectionStatus.CONNECTED || connectionStatus == ConnectionStatus.LIMITED) {
                firstDatapoint = false
            }
            return
        }
        when (CarStatsViewer.appPreferences.httpApiTelemetryType) {
            0, 2 -> sendWithDrivingPoint(realTimeData)
            else -> return
        }
    }

    private fun send(dataSet: String, context: Context = CarStatsViewer.appContext): ConnectionStatus {
        val username = AppPreferences(context).httpLiveDataUsername
        val password = AppPreferences(context).httpLiveDataPassword
        val responseCode: Int

        try {
            val url = URL(AppPreferences(context).httpLiveDataURL) // + "?json=$jsonObject")
            val connection = getConnection(url, username, password)
            DataOutputStream(connection.outputStream).apply {
                writeBytes(dataSet)
                flush()
                close()
            }
            responseCode = connection.responseCode

            if (detailedLog) {
                var logString = "[HTTP] Status: ${connection.responseCode}, Msg: ${connection.responseMessage}, Content:"
                logString += try {
                    if (connection.responseCode in 100..399) {
                        connection.inputStream.bufferedReader().use {it.readText()}
                    } else {
                        connection.errorStream.bufferedReader().use {it.readText()}
                    }
                } catch (e: java.lang.Exception) {
                    "No response content"
                }
                // if (dataSet.lat == null) logString += ". No valid location!"
                InAppLogger.d(logString)
            }

            if (connection.responseCode in 100..399) {
                connection.inputStream.close()
            } else {
                connection.errorStream.close()
            }
            connection.disconnect()
        } catch (e: java.net.SocketTimeoutException) {
            InAppLogger.e("[HTTP] Network timeout error")
            if (timeout < 30_000) {
                timeout += 5_000
                InAppLogger.w("[HTTP] Interval increased to $timeout ms")
            }
            successCounter = 0
            return ConnectionStatus.ERROR
        } catch (e: java.lang.Exception) {
            InAppLogger.e("[HTTP] Connection error: ${e.message?:"No error message"}")
            return ConnectionStatus.ERROR
        }

        if (responseCode != 200) {
            InAppLogger.e("[HTTP] Transmission failed. Status code $responseCode")
            return ConnectionStatus.ERROR
        }


        successCounter++
        if (successCounter >= 5 && timeout > originalInterval) {
            timeout -= 5_000
            InAppLogger.i("[HTTP] Interval decreased to $timeout ms")
            successCounter = 0
        }

        return ConnectionStatus.CONNECTED
    }
}

