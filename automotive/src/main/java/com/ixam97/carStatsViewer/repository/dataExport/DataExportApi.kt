package com.ixam97.carStatsViewer.repository.dataExport

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface DataExportApi {

    @Headers("content-type: application/json")
    @POST("/CSVBackend/chargingSessionDataExport")
    suspend fun chargingSessionDataExport(
        @Header("x-api-key") apiKey: String,
        @Body body: ChargingSessionDataExportBody
    ): Response<DataExportResponse>
}