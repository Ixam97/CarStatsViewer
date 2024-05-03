package com.ixam97.carStatsViewer.ui.views

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.utils.WatchdogState

class ApiRowWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private var mainClickListener: OnClickListener? = null
    private var helpClickListener: OnClickListener? = null

    private var apiName = ""
    private var apiIcon: Int = 0
    private var useConnection: Boolean = false

    private lateinit var statusIcon: ImageView
    private lateinit var mainBody: View
    private lateinit var helpButton: ImageButton

    var connectionStatus: Int = 0
        set(value) {
            field = value
            updateStatusIcon(field)
        }

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ApiRowWidget)
        try {
            apiName = attributes.getString(R.styleable.ApiRowWidget_api_name)?:""
            apiIcon = attributes.getResourceId(R.styleable.ApiRowWidget_icon, R.drawable.ic_api)
            useConnection = attributes.getBoolean(R.styleable.ApiRowWidget_use_connection, true)
        } finally {
            attributes.recycle()
        }
        init()
    }

    fun interface OnClickListener {
        fun onClick()
    }

    fun setOnMainClickListener(listener: OnClickListener) {
        mainClickListener = listener
    }

    fun setOnHelpClickListener(listener: OnClickListener) {
        helpButton.isEnabled = true
        helpButton.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        helpClickListener = listener
    }

    private fun init() {
        this.removeAllViews()
        View.inflate(context, R.layout.widget_api_row, this)

        val api_name_text = findViewById<TextView>(R.id.api_name_text)
        val api_icon = findViewById<ImageView>(R.id.api_icon)
        api_name_text.text = apiName
        api_icon.setImageResource(apiIcon)

        statusIcon = findViewById(R.id.api_status)
        mainBody = findViewById(R.id.row_container)
        helpButton = findViewById(R.id.row_end_button)

        if (!useConnection) statusIcon.visibility = View.GONE

        mainBody.setOnClickListener {
            mainClickListener?.onClick()
        }

        helpButton.isEnabled = false
        helpButton.setColorFilter(context.getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)
        helpButton.setOnClickListener {
            helpClickListener?.onClick()
        }
    }

    private fun updateStatusIcon(status: Int) {
        when (status) {
            WatchdogState.NOMINAL -> {
                statusIcon.setColorFilter(context.getColor(R.color.connected_blue))
                statusIcon.alpha = 1f
            }
            WatchdogState.LIMITED -> {
                statusIcon.setColorFilter(context.getColor(R.color.limited_yellow))
                statusIcon.alpha = 1f
            }
            WatchdogState.ERROR -> {
                statusIcon.setColorFilter(context.getColor(R.color.bad_red))
                statusIcon.alpha = 1f
            }
            else -> {
                statusIcon.setColorFilter(Color.WHITE)
                statusIcon.alpha = CarStatsViewer.disabledAlpha
            }
        }
    }
}