package com.ixam97.carStatsViewer.repository.logSubmit

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface LogSubmitApi {

    @Headers("content-type: application/json")
    @POST("")
    suspend fun submitLog(
        @Header("Authorization") auth: String,
        @Body body: LogSubmitBody
    ): Response<LogSubmitStatus>

}