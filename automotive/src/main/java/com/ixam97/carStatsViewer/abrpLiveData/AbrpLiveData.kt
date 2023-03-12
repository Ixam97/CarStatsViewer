package com.ixam97.carStatsViewer.abrpLiveData

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import com.ixam97.carStatsViewer.LiveDataApi
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.InAppLogger
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataManager.DataManager
import com.ixam97.carStatsViewer.dataManager.DrivingState
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AbrpLiveData (
    val apiKey : String,
    var token : String = ""
): LiveDataApi("com.ixam97.carStatsViewer_dev.abrp_connection_broadcast") {

    private fun send(
        abrpDataSet: AbrpDataSet,
        context: Context = CarStatsViewer.appContext
    ) : Int {

        token = AppPreferences(context).abrpGenericToken

        if (apiKey.isEmpty() || token.isEmpty()){
            return 0
        }

        val url = URL("https://api.iternio.com/1/tlm/send")
        val con: HttpURLConnection = url.openConnection() as HttpURLConnection

        con.requestMethod = "POST"
        con.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        con.setRequestProperty("Accept","application/json");
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
                put("ext_temp ", abrpDataSet.temp)
                abrpDataSet.lat?.let { put("lat", it) }
                abrpDataSet.lon?.let { put("lon", it) }
                abrpDataSet.alt?.let { put("elevation", it) }
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
            responseCode = con.responseCode
            con.disconnect()
        } catch (e: java.lang.Exception) {
            InAppLogger.log("ABRP network connection error")
            return 0
        }

        // InAppLogger.log("SENT: $jsonObject")
        /* InAppLogger.log("STATUS: ${con.responseCode.toString()}");
        InAppLogger.log("MSG: ${con.responseMessage}")
        try {
            InAppLogger.log("JSON: ${con.inputStream.bufferedReader().use {it.readText()}}")
        }
        catch (e: java.lang.Exception) {
            InAppLogger.log("ABRP API Auth Error")
        }
        finally {
            con.disconnect()
        }
*/
        if (responseCode == 200) {
            return 1
        }
        InAppLogger.log("ABRP connection failed. Response code: $responseCode")
        return 2
    }

    override fun showSettingsDialog(context: Context) {
        val tokenDialog = AlertDialog.Builder(context).apply {
            val layout = LayoutInflater.from(context).inflate(R.layout.dialog_abrp_token, null)
            val abrp_token = layout.findViewById<EditText>(R.id.abrp_token)

            abrp_token.setText(AppPreferences(context).abrpGenericToken)

            setView(layout)

            setPositiveButton("OK") { dialog, _ ->
                AppPreferences(context).abrpGenericToken = abrp_token.text.toString()
            }
            setTitle("ABRP Generic Token")
            setMessage("Enter ABRP Generic Token to transmit live data to the ABRP servers.")
            setCancelable(true)
            create()
        }
        tokenDialog.show()
    }
/*
    override fun createLiveDataTask(
        dataManager: DataManager,
        handler: Handler,
        interval: Long
    ): Runnable {
        return object : Runnable {
            override fun run() {
                sendNow(dataManager)
                handler.postDelayed(this, interval)
            }
        }
    }
*/

    override fun sendNow(dataManager: DataManager) {
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

        if (lat == null) InAppLogger.log("No valid location")

        connectionStatus = send(AbrpDataSet(
            stateOfCharge = dataManager.stateOfCharge,
            power = dataManager.currentPower,
            speed = dataManager.currentSpeed,
            isCharging = dataManager.chargePortConnected,
            isParked = (dataManager.driveState == DrivingState.PARKED || dataManager.driveState == DrivingState.CHARGE),
            lat = lat,
            lon = lon,
            alt = alt,
            temp = dataManager.ambientTemperature
        ))
    }
}