package com.ixam97.carStatsViewer.liveData.http

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import com.google.gson.Gson
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataManager.DataManager
import com.ixam97.carStatsViewer.dataManager.DrivingState
import com.ixam97.carStatsViewer.liveData.LiveDataApi
import com.ixam97.carStatsViewer.liveData.abrpLiveData.AbrpLiveData
import com.ixam97.carStatsViewer.utils.InAppLogger
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*


class HttpLiveData (
    detailedLog : Boolean = true
): LiveDataApi("com.ixam97.carStatsViewer.http_live_data_connection_broadcast", detailedLog) {


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

        val httpLiveDataSettingsDialog = AlertDialog.Builder(context).apply {
            setView(layout)

            setPositiveButton("OK") { _, _ ->
                AppPreferences(context).httpLiveDataURL = url.text.toString()
                AppPreferences(context).httpLiveDataUsername = username.text.toString()
                AppPreferences(context).httpLiveDataPassword = password.text.toString()
            }

            setTitle(context.getString(R.string.settings_apis_http))
            setMessage(context.getString(R.string.http_description))
            setCancelable(true)
            create()
        }

        val dialog = httpLiveDataSettingsDialog.show()

        httpLiveDataEnabled.isChecked = AppPreferences(context).httpLiveDataEnabled
        httpLiveDataEnabled.setOnClickListener {
            AppPreferences(context).httpLiveDataEnabled = httpLiveDataEnabled.isChecked
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
                    textView.error = context.getString(R.string.http_invalid_url);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    return
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        })
    }

    override fun sendNow(dataManager: DataManager) {
        if (!AppPreferences(CarStatsViewer.appContext).httpLiveDataEnabled) {
            connectionStatus = ConnectionStatus.UNUSED
            return
        }

        var lat: Double? = null
        var lon: Double? = null
        var alt: Double? = null

        dataManager.location?.let {
            if (it.time + 20_000 > System.currentTimeMillis()) {
                lat = it.latitude
                lon = it.longitude
                alt = it.altitude
            }
        }

        connectionStatus = send(
            HttpDataSet(
                timestamp = System.currentTimeMillis(),
                currentSpeed = dataManager.currentSpeed * 3.6f,
                currentPower = dataManager.currentPower / 1_000_000f,
                currentGear = dataManager.currentGear,
                chargePortConnected = dataManager.chargePortConnected,
                batteryLevel = dataManager.batteryLevel,
                stateOfCharge = dataManager.stateOfCharge,
                currentIgnitionState = dataManager.currentIgnitionState,
                instConsumption = dataManager.instConsumption,
                avgConsumption = dataManager.avgConsumption,
                avgSpeed = dataManager.avgSpeed,
                travelTime = dataManager.travelTime,
                chargeTime = dataManager.chargeTime,
                driveState = dataManager.driveState,
                ambientTemperature = dataManager.ambientTemperature,
                maxBatteryLevel = dataManager.maxBatteryLevel,
                tripStartDate = dataManager.tripStartDate,
                usedEnergy = dataManager.usedEnergy,
                traveledDistance = dataManager.traveledDistance,
                chargeStartDate = dataManager.chargeStartDate,
                chargedEnergy = dataManager.chargedEnergy,
                lat = lat,
                lon = lon,
                alt = alt,

                // Helpers
                isCharging = dataManager.chargePortConnected,
                isParked = (dataManager.driveState == DrivingState.PARKED || dataManager.driveState == DrivingState.CHARGE),
                isFastCharging = (dataManager.chargePortConnected && dataManager.currentPower < -11_000_000),

                // ABRP debug
                abrpPackage = (CarStatsViewer.liveDataApis[0] as AbrpLiveData).lastPackage
            )
        )
    }

    private fun send(dataSet: HttpDataSet, context: Context = CarStatsViewer.appContext): ConnectionStatus {
        val username = AppPreferences(context).httpLiveDataUsername
        val password = AppPreferences(context).httpLiveDataPassword
        val responseCode: Int

        val gson = Gson()
        val liveDataJson = gson.toJson(dataSet)

        try {
            val url = URL(AppPreferences(context).httpLiveDataURL) // + "?json=$jsonObject")
            val connection = getConnection(url, username, password)
            DataOutputStream(connection.outputStream).apply {
                writeBytes(liveDataJson)
                flush()
                close()
            }
            responseCode = connection.responseCode

            if (detailedLog) {
                var logString = "HTTP Webhook: Status: ${connection.responseCode}, Msg: ${connection.responseMessage}, Content:"
                logString += try {
                    connection.inputStream.bufferedReader().use {it.readText()}

                } catch (e: java.lang.Exception) {
                    "No response content"
                }
                if (dataSet.lat == null) logString += ". No valid location!"
                InAppLogger.d(logString)
            }

            connection.inputStream.close()
            connection.disconnect()
        } catch (e: java.net.SocketTimeoutException) {
            InAppLogger.e("HTTP Webhook: Network timeout error")
            return ConnectionStatus.ERROR
        } catch (e: java.lang.Exception) {
            InAppLogger.e("HTTP Webhook: Connection error")
            return ConnectionStatus.ERROR
        }

        if (responseCode != 200) {
            InAppLogger.e("HTTP Webhook: Transmission failed. Status code $responseCode")
            return ConnectionStatus.ERROR
        }

        return ConnectionStatus.CONNECTED
    }
}

