package com.ixam97.carStatsViewer.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.Parcelable
import androidx.core.content.getSystemService
import androidx.window.layout.WindowMetricsCalculator
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScreenshotServiceConfig(
    val resultCode: Int,
    val data: Intent
): Parcelable

data class ScreenshotServiceState(
    val isServiceRunning: Boolean = false,
    val numberOfScreenshots: Int = 0
)

class ScreenshotService: Service() {

    companion object {

        private val _screenshotServiceState = MutableStateFlow(ScreenshotServiceState())
        val screenshotServiceState = _screenshotServiceState.asStateFlow()

        private val _mutableScreenshotsList = mutableListOf<Bitmap>()
        val screenshotsList: List<Bitmap>
            get() = _mutableScreenshotsList.toList()

        fun clearScreenshots() {
            _mutableScreenshotsList.clear()
            _screenshotServiceState.value = _screenshotServiceState.value.copy(
                numberOfScreenshots = 0
            )
        }

        const val START_SCREENSHOT_SERVICE = "START_SCREENSHOT_SERVICE"
        const val STOP_SCREENSHOT_SERVICE = "STOP_SCREENSHOT_SERVICE"
        const val TAKE_SCREENSHOT = "TAKE_SCREENSHOT"
        const val KEY_SCREENSHOT_CONFIG = "KEY_SCREENSHOT_CONFIG"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val mediaProjectionManager by lazy {
        applicationContext.getSystemService<MediaProjectionManager>()
    }

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            stopScreenshotService()
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START_SCREENSHOT_SERVICE -> {
                val notification = createScreenshotNotification(applicationContext)
                createNotificationChannel(applicationContext)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        CarStatsViewer.SCREENSHOT_NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                } else {
                    startForeground(
                        CarStatsViewer.SCREENSHOT_NOTIFICATION_ID,
                        notification
                    )
                }
                _screenshotServiceState.value = _screenshotServiceState.value.copy(
                    isServiceRunning = true
                )

                val (width, height) = getWindowSize()
                if (imageReader == null) {
                    imageReader = ImageReader.newInstance(
                        width, height,
                        PixelFormat.RGBA_8888,
                        2
                    )
                }

                startScreenshotService(intent)
            }
            STOP_SCREENSHOT_SERVICE -> {
                mediaProjection?.stop()
            }
            TAKE_SCREENSHOT -> {
                serviceScope.launch {
                    delay(2000)
                    val image = imageReader?.acquireLatestImage()
                    image?.let {
                        val bitmap = Bitmap.createBitmap(it.width, it.height, Bitmap.Config.ARGB_8888)
                        it.planes[0].run {
                            bitmap.copyPixelsFromBuffer(buffer)
                        }
                        _mutableScreenshotsList.add(bitmap)
                        _screenshotServiceState.value = _screenshotServiceState.value.copy(
                            numberOfScreenshots = _mutableScreenshotsList.size
                        )
                        it.close()
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        _screenshotServiceState.value = _screenshotServiceState.value.copy(
            isServiceRunning = false
        )
        serviceScope.coroutineContext.cancelChildren()
    }

    private fun startScreenshotService(intent: Intent) {
        val (width, height) = getWindowSize()
        val config = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                KEY_SCREENSHOT_CONFIG,
                ScreenshotServiceConfig::class.java
            )
        } else {
            intent.getParcelableExtra(KEY_SCREENSHOT_CONFIG)
        }
        if(config == null) {
            return
        }

        mediaProjection = mediaProjectionManager?.getMediaProjection(
            config.resultCode,
            config.data
        )
        mediaProjection?.registerCallback(mediaProjectionCallback, null)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "Screen",
            width, height,
            resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null, null
        )

        InAppLogger.i("[Screenshot] Created Screenshot Service")
    }

    private fun stopScreenshotService() {
        _screenshotServiceState.value = _screenshotServiceState.value.copy(
            isServiceRunning = false
        )
        releaseResources()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        InAppLogger.i("[Screenshot] Screenshots Service Stopped")
    }

    private fun releaseResources() {
        virtualDisplay?.release()
        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        mediaProjection = null
    }

    private fun getWindowSize(): Pair<Int, Int> {
        val calculator = WindowMetricsCalculator.getOrCreate()
        val metrics = calculator.computeMaximumWindowMetrics(applicationContext)
        return metrics.bounds.width() to metrics.bounds.height()
    }

    private fun createScreenshotNotification(context: Context): Notification {
        val takeScreenshotIntent = Intent(context, ScreenshotService::class.java).also {
            it.action = TAKE_SCREENSHOT
        }
        val takeScreenshotPendingIntent = PendingIntent.getService(
            context, 0, takeScreenshotIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(context, CarStatsViewer.SCREENSHOT_CHANNEL_ID)
            .setContentTitle("Screenshot Service")
            .setSmallIcon(R.drawable.ic_camera)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Take Screenshot",
                    takeScreenshotPendingIntent
                ).build()
            )
            .build()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CarStatsViewer.SCREENSHOT_CHANNEL_ID,
                "Screenshot Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = context.getSystemService<NotificationManager>()
            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }

}