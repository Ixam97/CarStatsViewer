package com.ixam97.carStatsViewer.liveDataApi.abrpLiveData

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataProcessor.DrivingState
import com.ixam97.carStatsViewer.dataProcessor.RealTimeData
import com.ixam97.carStatsViewer.liveDataApi.ConnectionStatus
import com.ixam97.carStatsViewer.liveDataApi.LiveDataApi
import com.ixam97.carStatsViewer.ui.views.FixedSwitchWidget
import com.ixam97.carStatsViewer.utils.InAppLogger
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

class AbrpLiveData (
    private val apiKey : String,
    detailedLog : Boolean = true
): LiveDataApi("ABRP", R.string.settings_apis_abrp, detailedLog) {

    var lastPackage: String = ""

    var successCounter: Int = 0

    private fun send(
        abrpDataSet: AbrpDataSet,
        context: Context = CarStatsViewer.appContext
    ) : ConnectionStatus {
        if (!AppPreferences(context).abrpUseApi) return ConnectionStatus.UNUSED

        val token = AppPreferences(context).abrpGenericToken

        if (apiKey.isEmpty() || token.isEmpty()){
            return ConnectionStatus.UNUSED
        }

        val url = URL("https://api.iternio.com/1/tlm/send")
        val con: HttpURLConnection = url.openConnection() as HttpURLConnection

        con.requestMethod = "POST"
        con.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
        con.setRequestProperty("Accept","application/json")
        con.connectTimeout = timeout / 2
        con.readTimeout = timeout / 2
        con.doOutput = true
        con.doInput = true

        val responseCode: Int

        val jsonObject = JSONObject().apply {
            put("token", token)
            put("api_key", apiKey)

            val tlm = JSONObject().apply {
                put("soc", abrpDataSet.stateOfCharge)
                put("utc", System.currentTimeMillis() / 1000)
                put("power", abrpDataSet.power / 1_000_000f)
                put("is_charging", abrpDataSet.isCharging)
                put("is_parked", abrpDataSet.isParked)
                put("speed", abrpDataSet.speed * 3.6f)
                put("ext_temp", abrpDataSet.temp)
                if (AppPreferences(context).abrpUseLocation) {
                    abrpDataSet.lat?.let { put("lat", it) }
                    abrpDataSet.lon?.let { put("lon", it) }
                    abrpDataSet.alt?.let { put("elevation", it) }
                }
                put("is_dcfc", abrpDataSet.power < -11_000_000)
            }
            put("tlm", tlm)

        }
        try {
            DataOutputStream(con.outputStream).apply {
                writeBytes(jsonObject.toString())
                flush()
                close()
            }
            lastPackage = jsonObject.toString(4)
            // InAppLogger.v("ABRP JSON object: ${jsonObject.toString(4)}")
            responseCode = con.responseCode

            if (detailedLog) {
                var logString =
                    "[ABRP] Status: ${con.responseCode}, Msg: ${con.responseMessage}, Content:"
                logString += try {
                    con.inputStream.bufferedReader().use { it.readText() }

                } catch (e: java.lang.Exception) {
                    "No response content"
                }
                if (abrpDataSet.lat == null) logString += ". No valid location!"
                InAppLogger.v(logString)
            }

            if (responseCode == 200) {
                successCounter++
                if (successCounter >= 5 && timeout > originalInterval) {
                    timeout -= 5_000
                    InAppLogger.i("[ABRP] Interval decreased to $timeout ms")
                    successCounter = 0
                }
            }

            con.inputStream.close()
            con.disconnect()
        } catch (e: java.net.SocketTimeoutException) {
            InAppLogger.e("[ABRP] Network timeout error")
            if (timeout < 30_000) {
                timeout += 5_000
                InAppLogger.w("[ABRP] Interval increased to $timeout ms")
            }
            successCounter = 0
            return ConnectionStatus.ERROR
        } catch (e: java.lang.Exception) {
            InAppLogger.e("[ABRP] Network connection error")
            successCounter = 0
            return ConnectionStatus.ERROR
        }
        if (responseCode == 200) {
            return if (timeout == originalInterval)
                ConnectionStatus.CONNECTED
            else ConnectionStatus.LIMITED
        }
        InAppLogger.e("[ABRP] Connection failed. Response code: $responseCode")
        if (responseCode == 401) InAppLogger.e("          Auth error")
        return ConnectionStatus.ERROR
    }

    override fun showSettingsDialog(context: Context) {
        super.showSettingsDialog(context)
        val tokenDialog = AlertDialog.Builder(context).apply {
            val layout = LayoutInflater.from(context).inflate(R.layout.dialog_abrp_token, null)
            val abrp_token = layout.findViewById<EditText>(R.id.abrp_token)
            val abrp_use_api = layout.findViewById<FixedSwitchWidget>(R.id.abrp_use_api)
            val abrp_use_location = layout.findViewById<FixedSwitchWidget>(R.id.abrp_use_location)

            abrp_use_api.isChecked = AppPreferences(context).abrpUseApi
            abrp_use_location.isChecked = AppPreferences(context).abrpUseLocation

            abrp_use_api.setSwitchClickListener() {
                AppPreferences(context).abrpUseApi = abrp_use_api.isChecked
                if(!abrp_use_api.isChecked) connectionStatus = ConnectionStatus.UNUSED
                updateWatchdog()
            }

            abrp_use_location.setSwitchClickListener() {
                AppPreferences(context).abrpUseLocation = abrp_use_location.isChecked
            }

            abrp_token.setText(AppPreferences(context).abrpGenericToken)

            setView(layout)

            setPositiveButton("OK") { dialog, _ ->
                AppPreferences(context).abrpGenericToken = abrp_token.text.toString()
            }
            setTitle(context.getString(R.string.settings_apis_abrp))
            setMessage(context.getString(R.string.abrp_description))
            setCancelable(true)
            create()
        }
        tokenDialog.show()
    }

    override suspend fun sendNow(realTimeData: RealTimeData) {
        if (!AppPreferences(CarStatsViewer.appContext).abrpUseApi) {
            connectionStatus = ConnectionStatus.UNUSED
            return
        }

        if (realTimeData.isInitialized()) {
            connectionStatus = send(AbrpDataSet(
                stateOfCharge = (realTimeData.stateOfCharge!! * 100f).roundToInt(),
                power = realTimeData.power!!,
                speed = realTimeData.speed!!,
                isCharging = realTimeData.chargePortConnected!!,
                isParked = (realTimeData.drivingState == DrivingState.PARKED || realTimeData.drivingState == DrivingState.CHARGE),
                lat = realTimeData.lat?.toDouble(),
                lon = realTimeData.lon?.toDouble(),
                alt = realTimeData.alt?.toDouble(),
                temp = realTimeData.ambientTemperature!!
            ))
        }
    }
}