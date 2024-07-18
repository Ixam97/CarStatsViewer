package com.ixam97.carStatsViewer.liveDataApi.uploadService

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.android.gms.common.ConnectionResult
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.dataProcessor.RealTimeData
import com.ixam97.carStatsViewer.database.tripData.ChargingPoint
import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.liveDataApi.ConnectionStatus
import com.ixam97.carStatsViewer.liveDataApi.http.HttpLiveData
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class UploadService: Service() {

    companion object {
        const val chunkSize = 250
        const val maxChunkAttepts = 10
        private const val TAG = "DBU"
    }

    private lateinit var notificationBuilder: Notification.Builder

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        InAppLogger.i("Upload Service started")

        notificationBuilder = Notification.Builder(CarStatsViewer.appContext, CarStatsViewer.UPLOAD_CHANNEL_ID)
        intent?.let {
            if (it.hasExtra("type")) {
                val type = it.extras!!.getString("type")
                when (type) {
                    "full_db" -> {
                        uploadFullDB()
                    }
                    "drivingSession" -> {
                        doneUploading()
                    }
                    "chargingSession" -> {
                        doneUploading()
                    }
                }
            } else {
                doneUploading()
            }
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        InAppLogger.i("Upload Service ended")
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun uploadFullDB() {
        serviceScope.launch {
            val ds = CarStatsViewer.tripDataSource
            val numDrivingPoints = ds.getDrivingPointsSize()
            val numChargingSessions = ds.getChargingSessionsSize()

            InAppLogger.i("[$TAG] Driving points: $numDrivingPoints, charging sessions: $numChargingSessions")

            val numChunks = ceil(numDrivingPoints.toFloat() / chunkSize).roundToInt()
            val totalChunks = numChunks + numChargingSessions

            notificationBuilder.apply {
                setContentTitle("Uploading Database ...")
                setSmallIcon(R.drawable.ic_upload)
                setCategory(Notification.CATEGORY_STATUS)
                setProgress(totalChunks, 0, false)
                setOngoing(true)
            }

            CarStatsViewer.notificationManager.notify(CarStatsViewer.UPLOAD_NOTIFICATION_ID, notificationBuilder.build())

            InAppLogger.i("[$TAG] Divided into $totalChunks")

            var lastTimestamp = 0L

            for (i in 0 until numChunks) {
                var result = false
                var attepts = 0

                val chunkDrivingPoints = ds.getDrivingPointsChunk(lastTimestamp, chunkSize)
                val chunk = ChunkData(
                    realTimeData = CarStatsViewer.dataProcessor.realTimeData,
                    drivingPoints = chunkDrivingPoints
                )

                while (!result) {
                    attepts++

                    result = uploadChunk(chunk)

                    notificationBuilder.apply {
                        val progress = (((i + 1).toFloat() / totalChunks) * 100f).roundToInt()
                        if (result) {
                            setContentText("Uploading driving points ($progress%)")
                            setProgress(totalChunks, i + 1, false)
                        } else {
                            setContentText("An error occurred at $progress%, attempt $attepts/$maxChunkAttepts")
                            InAppLogger.w("[$TAG] Chunk ${i + 1}/$totalChunks failed!")
                        }
                    }

                    updateNotification()

                    if (attepts >= maxChunkAttepts && !result) {
                        uploadFailed((((i + 1).toFloat() / totalChunks) * 100f).roundToInt())
                        return@launch
                    } else if (result) {
                        InAppLogger.i("[$TAG] Chunk ${i + 1}/$totalChunks")
                    }

                    if (!result) delay(5000)
                }
                lastTimestamp = chunkDrivingPoints.last().driving_point_epoch_time
            }

            val chargingSessions = ds.getAllChargingSessions()

            for (i in 0 until numChargingSessions) {

                var result = false
                var attepts = 0
                attepts++
                var chargingSession =
                    ds.getCompleteChargingSessionById(chargingSessions[i].charging_session_id)

                val chargeTime = if (chargingSession.end_epoch_time == null) 0 else {
                    chargingSession.end_epoch_time!! - chargingSession.start_epoch_time
                }

                var chargedSoc = 0f
                var chargingPoints: List<ChargingPoint>? = null
                chargingSession.chargingPoints?.let { cp ->
                    if (cp.size > 1) {
                        val startSoc = (cp.first().state_of_charge * 100f).roundToInt()
                        val endSoc = (cp.last().state_of_charge * 100f).roundToInt()
                        chargedSoc = (endSoc - startSoc).toFloat() / 100f
                        chargingPoints = cp
                    }
                }

                chargingSession = chargingSession.copy(charged_soc = chargedSoc)
                chargingSession.chargingPoints = chargingPoints
                chargingSession.chargeTime = chargeTime

                val chunk = ChunkData(
                    realTimeData = CarStatsViewer.dataProcessor.realTimeData,
                    chargingSessions = listOf(chargingSession)
                )

                while (!result) {

                    result = uploadChunk(chunk)

                    notificationBuilder.apply {
                        val progress =
                            (((i + 1 + numChunks).toFloat() / totalChunks) * 100f).roundToInt()
                        if (result) {
                            setContentText("Uploading charging sessions ($progress%)")
                            setProgress(totalChunks, i + 1 + numChunks, false)
                        } else {
                            setContentText("An error occurred at $progress%, attempt $attepts/$maxChunkAttepts")
                            InAppLogger.w("[$TAG] Chunk ${i + 1 + numChunks}/$totalChunks failed!")
                        }
                    }

                    updateNotification()

                    if (attepts >= maxChunkAttepts && !result) {
                        uploadFailed((((i + 1 + numChunks).toFloat() / totalChunks) * 100f).roundToInt())
                        return@launch
                    } else if (result) {
                        InAppLogger.i("[$TAG] Chunk ${i + 1 + numChunks}/$totalChunks")
                    }

                    if (!result) delay(5000)
                }
            }


            notificationBuilder.apply {
                setContentTitle("Done uploading database!")
                setContentText(null)
                setProgress(0, 0, false)
                setOngoing(false)
            }
            updateNotification()

            doneUploading()
        }
    }

    private suspend fun uploadChunk(chunkData: ChunkData): Boolean {
        val result = (CarStatsViewer.liveDataApis[1] as HttpLiveData).sendWithDrivingPoint(
            realTimeData = chunkData.realTimeData,
            drivingPoints = chunkData.drivingPoints,
            chargingSessions = chunkData.chargingSessions,
            useBacklog = false
        )

        return result != ConnectionStatus.ERROR
    }

    private fun doneUploading() {
        stopSelf()
    }

    private fun uploadFailed(progress: Int) {
        notificationBuilder.apply {
            setContentTitle("Upload failed!")
            setContentText("The upload failed at $progress%. Please check the uploaded data and try again!")
            setProgress(0, 0, false)
            setOngoing(false)
        }
        updateNotification()
    }

    private fun updateNotification() {
        CarStatsViewer.notificationManager.notify(CarStatsViewer.UPLOAD_NOTIFICATION_ID, notificationBuilder.build())
    }
}

data class ChunkData(
    val realTimeData: RealTimeData,
    val drivingPoints: List<DrivingPoint>? = null,
    val chargingSessions: List<ChargingSession>? = null
)