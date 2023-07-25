package com.ixam97.carStatsViewer.ui.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import kotlin.math.roundToInt

class MenuRowWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    fun interface OnClickListener {
        fun onClick()
    }

    private var mainBody: ConstraintLayout
    private var rowLabel: TextView
    private var rowTopText: TextView
    private var rowBottomText: TextView
    private var startIcon: ImageView
    private var endIcon: ImageView
    private var endTextButton: TextView
    private var divider: View

    private var onRowClickListener: OnClickListener? = null

    var topText: String = ""
        set(value) {
            field = value
            init()
        }
    var bottomText: String = ""
        set(value) {
            field = value
            init()
        }
    var endButtonText: String = ""
        set(value) {
            field = value
            init()
        }
    var startDrawableId: Int = 0
        private set
    var isExternalLink: Boolean = false
        private set
    var reducedSize: Boolean = false
        private set

    fun setOnRowClickListener(listener: OnClickListener) {
        onRowClickListener = listener
        init()
    }

    override fun setEnabled(enabled: Boolean) {
        mainBody.apply {
            alpha = when (enabled) {
                true -> 1.0f
                false -> CarStatsViewer.disabledAlpha
            }
        }

        super.setEnabled(enabled)
    }

    init {
        inflate(context, R.layout.widget_menu_row, this)

        mainBody = findViewById(R.id.row_main_body)
        rowLabel = findViewById(R.id.row_std_label)
        rowTopText = findViewById(R.id.row_top_text)
        rowBottomText = findViewById(R.id.row_bottom_text)
        startIcon = findViewById(R.id.row_start_icon)
        endIcon = findViewById(R.id.row_end_icon)
        endTextButton = findViewById(R.id.row_end_text_button)
        divider = findViewById(R.id.row_divider)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.MenuRowWidget)

        try {
            topText = attributes.getString(R.styleable.MenuRowWidget_topText) ?: ""
            bottomText = attributes.getString(R.styleable.MenuRowWidget_bottomText) ?: ""
            endButtonText = attributes.getString(R.styleable.MenuRowWidget_endButtonText) ?: ""
            startDrawableId = attributes.getResourceId(R.styleable.MenuRowWidget_startDrawable, 0)
            isExternalLink = attributes.getBoolean(R.styleable.MenuRowWidget_isExternalLink, false)
            reducedSize = attributes.getBoolean(R.styleable.MenuRowWidget_reduceSize, false)
        } finally {
            attributes.recycle()
        }

        init()
    }

    private fun init() {
        try {
            val parent = (this.parent as ViewGroup)
            if (parent.children.last() == this) {
                divider.setBackgroundColor(Color.TRANSPARENT)
            }
        } catch (e: Exception) {
            // No parent detected
        }

        if (reducedSize) {
            val iconParams = startIcon.layoutParams
            iconParams.width = resources.getDimension(R.dimen.std_icon_size).roundToInt()
            startIcon.layoutParams = iconParams
            mainBody.minHeight = 0
        }

        if (bottomText != "") {
            rowLabel.visibility = View.GONE
            rowTopText.text = topText
            rowBottomText.text = bottomText
            rowTopText.visibility = View.VISIBLE
            rowBottomText.visibility = View.VISIBLE
        } else {
            rowLabel.text = topText
            rowLabel.visibility = View.VISIBLE
            rowTopText.visibility = View.GONE
            rowBottomText.visibility = View.GONE
        }

        if (startDrawableId > 0) {
            startIcon.setImageResource(startDrawableId)
            startIcon.visibility = View.VISIBLE
        } else {
            startIcon.visibility = View.GONE
        }

        if (endButtonText != "" && onRowClickListener != null) {
            endIcon.visibility = View.GONE
            endTextButton.visibility = View.VISIBLE
            endTextButton.text = endButtonText
            endTextButton.setOnClickListener { if (isEnabled) onRowClickListener?.onClick() }
        } else if (onRowClickListener != null) {
            endIcon.visibility = View.VISIBLE
            endTextButton.visibility = View.GONE
            endIcon.setImageResource(
                if (isExternalLink) {
                    R.drawable.ic_link
                } else {
                    R.drawable.ic_chevron_right
                }
            )
            mainBody.setOnClickListener { if (isEnabled) onRowClickListener?.onClick() }
        } else {
            endTextButton.visibility = View.GONE
            endIcon.visibility = View.GONE
        }
    }
}