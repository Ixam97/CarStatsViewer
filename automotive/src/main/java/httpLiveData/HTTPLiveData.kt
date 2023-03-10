package com.ixam97.carStatsViewer.httpLiveData

import com.ixam97.carStatsViewer.InAppLogger
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class HTTPLiveData (val dataURL : String? = null) {
    fun send(
        stateOfCharge: Int,
        power: Float,
        isCharging: Boolean,
        speed: Float,
        isParked: Boolean
    ) : Int {
        if (dataURL == "") return 0

        val url = URL(dataURL)

        val con: HttpURLConnection = url.openConnection() as HttpURLConnection

        con.requestMethod = "POST"
        con.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        con.setRequestProperty("Accept","application/json");
        con.doOutput = true
        con.doInput = true

        var responseCode = 0

        val jsonObject = JSONObject().apply {
                put("soc", stateOfCharge)
                put("utc", System.currentTimeMillis() / 1000)
                put("power", power / 1_000_000f)
                put("is_charging", isCharging)
                put("is_parked", isParked)
                put("speed", speed * 3.6f)
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
            InAppLogger.log("Network connection error: $e")
            return 2
        }

        if (responseCode == 200) return 1

        InAppLogger.log("HTTP connection failed. Response code: $responseCode")
        return 2
    }
}
