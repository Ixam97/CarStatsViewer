package com.ixam97.carStatsViewer.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.view.children
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.airbnb.paris.utils.setPaddingHorizontal
import com.airbnb.paris.utils.setPaddingVertical
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.ui.views.SnackbarWidget
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

object ScreenshotButton {

    private fun Float.toPx(context: Context = CarStatsViewer.appContext) = (this * context.resources.displayMetrics.density).roundToInt()

    fun init(activity: Activity) {
        if (activity !is LifecycleOwner) return
        val root: FrameLayout = activity.window.findViewById(android.R.id.content)
        root.children.find { it.tag == "ScreenshotButtonContainer" }?.let {
            root.removeView(it)
        }

        if (!CarStatsViewer.appPreferences.showScreenshotButton) return

        val buttonContainer = RelativeLayout(activity).apply {
            tag = "ScreenshotButtonContainer"
            val layoutParams = FrameLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.gravity = Gravity.BOTTOM or Gravity.LEFT
            setPaddingVertical(15f.toPx())
            setPaddingHorizontal(15f.toPx())
            this.layoutParams = layoutParams
            alpha = 0.7f

        }
        val button = ImageButton(activity).apply {
            // background = activity.getDrawable(R.drawable.bg_floating)
            setBackgroundColor(activity.getColor(R.color.transparent_bad_red))
            setImageDrawable(activity.getDrawable(R.drawable.ic_camera))
            val layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            this.layoutParams = layoutParams
            setPaddingVertical(5f.toPx())
            setPaddingHorizontal(5f.toPx())

        }
        buttonContainer.addView(button)
        root.addView(buttonContainer)

        button.setOnClickListener {
            button.isVisible = false
            activity.lifecycleScope.launch {
                val screenshotView: View = root.rootView
                val tempBitmap = screenshotView.drawToBitmap(Bitmap.Config.ARGB_8888)
                val locationInWindow = IntArray(2)
                val pixels = IntArray(root.width * root.height)
                root.getLocationInWindow(locationInWindow)
                tempBitmap.getPixels(pixels, 0, root.width, locationInWindow[0], locationInWindow[1], root.width, root.height)
                val outputBitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                outputBitmap.setPixels(pixels, 0, root.width, 0, 0, root.width, root.height)
                CarStatsViewer.screenshotBitmap.add(outputBitmap)
                activity.runOnUiThread {
                    button.isVisible = true
                    SnackbarWidget.Builder(activity, activity.getString(R.string.screenshot_taken, CarStatsViewer.screenshotBitmap.size.toString()))
                        .setButton("OK")
                        .setStartDrawable(R.drawable.ic_camera)
                        .setDuration(2000)
                        .show()
                }
            }
        }
    }
}