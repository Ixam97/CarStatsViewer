package com.ixam97.carStatsViewer.abrpLiveData

import com.ixam97.carStatsViewer.InAppLogger
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AbrpLiveData (val apiKey : String? = null, val token : String? = null) {
    fun send(
        stateOfCharge: Int,
        power: Float,
        isCharging: Boolean,
        speed: Float,
        isParked: Boolean
    ) : Int {
        if (apiKey == null || token == null) return 0
        if (apiKey.isEmpty() || token.isEmpty()) return 0

        val url = URL("https://api.iternio.com/1/tlm/send")
        val con: HttpURLConnection = url.openConnection() as HttpURLConnection

        con.requestMethod = "POST"
        con.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        con.setRequestProperty("Accept","application/json");
        con.doOutput = true
        con.doInput = true

        var responseCode = 0

        val jsonObject = JSONObject().apply {
            put("token", token)
            put("api_key", apiKey)

            val tlm = JSONObject().apply {
                put("soc", stateOfCharge)
                put("utc", System.currentTimeMillis() / 1000)
                put("power", power / 1_000_000f)
                put("is_charging", isCharging)
                put("is_parked", isParked)
                put("speed", speed * 3.6f)
                put("car_model", "polestar:2:23:78:single:lr")
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
            return 2
        }
/*
        InAppLogger.log("SENT: $jsonObject")
        InAppLogger.log("STATUS: ${con.responseCode.toString()}");
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

        if (responseCode == 200) return 1
        InAppLogger.log("ABRP connection failed. Response code: $responseCode")
        return 2
    }
}