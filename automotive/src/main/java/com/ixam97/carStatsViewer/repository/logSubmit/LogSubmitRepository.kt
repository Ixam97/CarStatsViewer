package com.ixam97.carStatsViewer.repository.logSubmit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object LogSubmitRepository {

    private const val BASE_URL = ""
    private const val AUTH_STRING = ""

    private val logSubmitApi: LogSubmitApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LogSubmitApi::class.java)
    }

    suspend fun submitLog(body: LogSubmitBody): Boolean {
        var success = false
        logSubmitApi.run {
            val response = submitLog(
                auth = AUTH_STRING,
                body = body
            )
            success = when (response.body()) {
                is LogSubmitStatus.Success -> true
                else -> false
            }
        }
        return success
    }
}