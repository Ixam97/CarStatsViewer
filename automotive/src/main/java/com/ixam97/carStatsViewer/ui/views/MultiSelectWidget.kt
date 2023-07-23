package com.ixam97.carStatsViewer.ui.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import kotlin.math.roundToInt


class MultiSelectWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    var entries: ArrayList<String> = arrayListOf()
        set(value) {
            field = value
            init()
        }

    var mListener: OnIndexChangedListener? = null

    private val primaryColor = CarStatsViewer.primaryColor
    private val secondaryColor: Int = context.getColor(R.color.disable_background)

    private val title: String
    private val titleWidth: Float

    private lateinit var titleView: TextView
    private lateinit var selectedView: TextView
    private lateinit var barLayout: LinearLayout
    private lateinit var leftButton: ImageButton
    private lateinit var rightButton: ImageButton
    private lateinit var centerButton: LinearLayout

    var selectedIndex = 0
        set(value) {
            if (entries.isNotEmpty()) {
                field = if (value >= entries.size) entries.size - 1 else if (value < 0) 0 else value
                try {
                    selectedView.text = entries[selectedIndex]
                    barLayout.children.forEach {
                        it.alpha = 1f
                        it.setBackgroundColor(secondaryColor) }
                    barLayout.getChildAt(selectedIndex).apply {
                        setBackgroundColor(primaryColor)
                        if (!isEnabled) alpha = CarStatsViewer.disabledAlpha
                    }
                } finally {
                    invalidate()
                }
            } else {
                field = -1
            }
        }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        leftButton.isEnabled = enabled
        rightButton.isEnabled = enabled
        val enabledAlpha = if (!isEnabled) CarStatsViewer.disabledAlpha else 1f
        selectedView.alpha = enabledAlpha
        titleView.alpha = enabledAlpha
        leftButton.alpha = enabledAlpha
        rightButton.alpha = enabledAlpha
        barLayout.getChildAt(selectedIndex).alpha = enabledAlpha
        this.invalidate()
    }

    fun interface OnIndexChangedListener {
        fun onIndexChanged()
    }

    fun setOnIndexChangedListener(listener: OnIndexChangedListener) {
        mListener = listener
    }

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.MultiSelectWidget)
        try {
            title = attributes.getString(R.styleable.MultiSelectWidget_title)?:""
            titleWidth = attributes.getDimension(R.styleable.MultiSelectWidget_title_width, -2f)
        } finally {
            attributes.recycle()
        }

        // init()
    }

    private fun calcDimen(dimen: Float): Int {
        return when (dimen) {
            -1f -> -1
            -2f -> -2
            else -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dimen, resources.displayMetrics).roundToInt()
        }
    }

    private fun init() {
        this.removeAllViews()
        View.inflate(context, R.layout.widget_multi_select, this)

        titleView = findViewById(R.id.widget_title)
        selectedView = findViewById(R.id.widget_selected_text)
        barLayout = findViewById(R.id.widget_selected_bar)
        leftButton = findViewById(R.id.widget_button_left)
        rightButton = findViewById(R.id.widget_button_right)
        centerButton = findViewById(R.id.widget_button_center)

        titleView.text = title
        CarStatsViewer.typefaceRegular?.let {
            titleView.typeface = it
            // titleView.letterSpacing = -0.02f
            selectedView.typeface = it
            // selectedView.letterSpacing = -0.02f
        }
        titleView.layoutParams.width = calcDimen(titleWidth)

        if (entries.isNotEmpty()) {
            fun increaseIndex() {
                if (selectedIndex == entries.size - 1) selectedIndex = 0
                else selectedIndex++
                selectedView.text = entries[selectedIndex]
                barLayout.children.forEach { it.setBackgroundColor(secondaryColor) }
                barLayout.getChildAt(selectedIndex).setBackgroundColor(primaryColor)
                mListener?.onIndexChanged()
                invalidate()
            }

            leftButton.setOnClickListener {
                if (selectedIndex == 0) selectedIndex = entries.size - 1
                else selectedIndex--
                selectedView.text = entries[selectedIndex]
                barLayout.children.forEach { it.setBackgroundColor(secondaryColor) }
                barLayout.getChildAt(selectedIndex).setBackgroundColor(primaryColor)
                mListener?.onIndexChanged()
                invalidate()
            }

            rightButton.setOnClickListener {
                increaseIndex()
            }

            centerButton.setOnClickListener {
                increaseIndex()
            }



            barLayout.weightSum = entries.size.toFloat()
            entries.forEach {
                val barView = View(context)
                barView.apply {
                    layoutParams = LayoutParams(0, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics).roundToInt(), 1f)
                    setBackgroundColor(context.getColor(R.color.disable_background))
                }
                barLayout.addView(barView)
            }
            selectedView.text = entries[selectedIndex]
            barLayout.getChildAt(selectedIndex).setBackgroundColor(primaryColor)
        }
    }
}