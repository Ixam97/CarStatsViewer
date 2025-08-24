package com.ixam97.carStatsViewer.repository.logSubmit

import android.graphics.Bitmap
import android.os.Build
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream

object LogSubmitRepository {

    private const val BASE_URL = "https://ixam97.de/"

    private var apiKey: String? = null

    fun setApiKey(key: String) {
        apiKey = key
    }

    private val logSubmitApi: LogSubmitApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LogSubmitApi::class.java)
    }

    suspend fun submitLog(body: LogSubmitBody): String? {
        apiKey?.let { key ->
            logSubmitApi.run {
                val response = submitLog(
                    apiKey = key,
                    body = body
                )
                return evaluateResponse(response)
            }
        }
        if (apiKey == null) {
            return "No API Key available, contact track maintainer to acquire it from the developer."
        }
        return "Unhandled error"
    }

    suspend fun uploadImage(bitmaps: List<Bitmap>, additionalAddress: String? = null): String? {
        apiKey?.let { key ->
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            bitmaps.forEachIndexed { index, bitmap ->
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()
                body.addFormDataPart("image_$index", "Screenshot_$index.png",
                    byteArray.toRequestBody("image/png".toMediaTypeOrNull(), 0, byteArray.size)
                )
                outputStream.close()
                additionalAddress?.let {
                    body.addFormDataPart("address", it)
                }
                body.addFormDataPart("userID", CarStatsViewer.appPreferences.debugUserID)
                body.addFormDataPart("appInfo", "${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID}")
                body.addFormDataPart("brand", "${Build.BRAND}")
                body.addFormDataPart("model", "${Build.MODEL}")
                body.addFormDataPart("device", "${Build.DEVICE}")
                val cpuInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}"
                else
                    "Unknown"
                body.addFormDataPart("cpuInfo", cpuInfo)
            }
            logSubmitApi.run {
                val response = uploadImage(
                    apiKey = key,
                    body = body.build()
                )
                return evaluateResponse(response)
            }
        }
        if (apiKey == null) {
            return "No API Key available, contact track maintainer to acquire it from the developer."
        }
        return "Unhandled error"
    }

    private fun evaluateResponse(response: Response<LogSubmitStatus>): String? {
        if (response.body() is LogSubmitStatus) {
            response.body()?.let { body ->
                if (body.status == "OK") {
                    return null
                } else {
                    return "Server response: (${response.code()})\n\r${body.status}: ${body.message}"
                }
            }
        }
        return "Unexpected Response: (${response.code()}) ${response.errorBody()?.string()}"
    }
}