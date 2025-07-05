package com.ixam97.carStatsViewer.repository.logSubmit

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface LogSubmitApi {

    @Headers("content-type: application/json")
    @POST("/CSVBackend/submitLog")
    suspend fun submitLog(
        @Header("x-api-key") apiKey: String,
        @Body body: LogSubmitBody
    ): Response<LogSubmitStatus>

    @POST("/CSVBackend/authCheck")
    suspend fun checkAuth(
        @Header("x-api-key") apiKey: String
    ): Response<AuthResponse>

}