package com.coderax.carStatsViewer.ui.views

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.coderax.carStatsViewer.R
import com.coderax.carStatsViewer.utils.InAppLogger
import com.coderax.carStatsViewer.utils.applyTypeface
import kotlinx.android.synthetic.main.widget_snackbar.view.*


class SnackbarWidget private constructor(
    private val snackbarParameters: SnackbarParameters,
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private data class SnackbarParameters(
        val message: String,
        val buttonText: String? = null,
        val drawableId: Int? = null,
        val startHidden: Boolean = false,
        val duration: Long = 0,
        val listener: SnackbarInterface? = null,
        val isError: Boolean = false
    )

    fun interface SnackbarInterface {
        fun onClick()
    }

    class Builder(val context: Context, val message: String) {
        private var snackbarParameters = SnackbarParameters(message)

        fun setButton(buttonText: String, listener: SnackbarInterface? = null): Builder {
            snackbarParameters = snackbarParameters.copy(
                buttonText = buttonText,
                listener = listener
            )
            return this
        }

        fun setIsError(isError: Boolean): Builder {
            snackbarParameters = snackbarParameters.copy(
                isError = isError
            )
            return this
        }

        fun setDuration(duration: Long): Builder {
            snackbarParameters = snackbarParameters.copy(
                duration = duration
            )
            return this
        }

        fun setStartDrawable(@DrawableRes resId: Int): Builder {
            snackbarParameters = snackbarParameters.copy(
                drawableId = resId
            )
            return this
        }

        fun show() {
            if (context is Activity) {
                snackbarParameters = snackbarParameters.copy(startHidden = true)
                context.window.findViewById<FrameLayout>(android.R.id.content).addView(build())
            }
        }

        fun build(): SnackbarWidget {
            return SnackbarWidget(snackbarParameters, context)
        }

        fun setButtonEvent() {}

    }

    private val confirmButton: TextView
    private val messageText: TextView
    private val startIcon: ImageView
    private val progressBar: View

    private fun removeSelf() {
        val anim = AnimationUtils.loadAnimation(context, R.anim.snackbar_down)
        anim.setAnimationListener(object: AnimationListener{
            override fun onAnimationStart(animation: Animation?) = Unit
            override fun onAnimationEnd(animation: Animation?) {
                Handler(Looper.getMainLooper()).post {
                    this@SnackbarWidget.rootView.findViewById<FrameLayout>(android.R.id.content).removeView(this@SnackbarWidget)
                }
            }
            override fun onAnimationRepeat(animation: Animation?) = Unit
        })
        this.startAnimation(anim)

    }

    init {
        val root = inflate(context, R.layout.widget_snackbar, this)

        confirmButton = findViewById(R.id.confirm_button)
        messageText = findViewById(R.id.message_text)
        startIcon = findViewById(R.id.start_icon)
        progressBar = findViewById(R.id.progress_bar)

        messageText.text = snackbarParameters.message

        confirmButton.isVisible = false

        snackbarParameters.buttonText?.let {
            confirmButton.text = it
            confirmButton.isVisible = true
        }

        confirmButton.setOnClickListener {
            snackbarParameters.listener?.onClick()
            removeSelf()
        }

        if (snackbarParameters.isError) {
            (progressBar.parent as ViewGroup).setBackgroundColor(context.getColor(R.color.bad_red_dark))
            progressBar.setBackgroundColor(context.getColor(R.color.bad_red))
            startIcon.setImageResource(R.drawable.ic_error)
        }

        snackbarParameters.drawableId?.let {
            startIcon.setImageResource(snackbarParameters.drawableId)
        }

        val anim = AnimationUtils.loadAnimation(context, R.anim.snackbar_up)
        anim.setAnimationListener(object: AnimationListener{
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                if (snackbarParameters.duration > 0) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        removeSelf()
                    }, snackbarParameters.duration)
                }
                val widthAnimator = ValueAnimator.ofInt(1, this@SnackbarWidget.measuredWidth)
                widthAnimator.duration = snackbarParameters.duration
                widthAnimator.interpolator = LinearInterpolator()
                widthAnimator.addUpdateListener { barAnimation ->
                    val layoutParams = progress_bar.layoutParams
                    layoutParams.width = barAnimation.animatedValue as Int
                    progressBar.layoutParams = layoutParams
                }
                widthAnimator.start()
            }
            override fun onAnimationRepeat(animation: Animation?) {}

        })
        this.startAnimation(anim)

        applyTypeface(this)
    }
}