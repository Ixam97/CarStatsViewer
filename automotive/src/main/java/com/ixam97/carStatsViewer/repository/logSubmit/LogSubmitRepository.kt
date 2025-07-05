package com.ixam97.carStatsViewer.repository.logSubmit

import com.ixam97.carStatsViewer.utils.InAppLogger
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
                when (response.code()) {
                    200 -> {
                        response.body().let { body ->
                            if (body is LogSubmitStatus) {
                                if (body.status == "OK")
                                    return null
                                else {
                                    return "Server response: ${body.status}: ${body.message?: "no message"}"
                                }
                            }
                            return "Malformed response: ${response.body()}"
                        }
                    }
                    404 -> {
                        return "Error 404: API Endpoint not found."
                    }
                    401 -> {
                        return "Error 401: Not authorized."
                    }
                    else -> {
                        return "Error ${response.code()}"
                    }
                }
            }
        }
        if (apiKey == null) {
            return "No API Key available."
        }
        return "Unhandled error"
    }
}