package com.ixam97.carStatsViewer.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.airbnb.paris.extensions.style
import com.ixam97.carStatsViewer.R

class FixedSwitchWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    private var clickListener: ClickListener? = null

    private var label: String = ""

    private lateinit var container: View
    private lateinit var switch: Switch
    private lateinit var labelView: TextView

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.FixedSwitchWidget)
        try {
            label = attributes.getString(R.styleable.FixedSwitchWidget_text)?:""
        } finally {
            attributes.recycle()
        }
        init()
    }

    private fun init() {
        this.removeAllViews()
        View.inflate(context, R.layout.widget_fixed_switch, this)

        labelView = findViewById(R.id.switch_label)
        container = findViewById(R.id.container)
        switch = findViewById<Switch>(R.id.switch_switch)

        labelView.text = label

        container.setOnClickListener {
            switch.isChecked = !switch.isChecked
            clickListener?.onClick()
        }

        switch.setOnClickListener {
            clickListener?.onClick()
        }
    }

    var isChecked
        get() = switch.isChecked
        set(value) {switch.isChecked = value}

    var text: String
        get() = labelView.text.toString()
        set(value) {
            labelView.text = value
            label = value
        }

    fun interface ClickListener {
        fun onClick()
    }

    fun setSwitchClickListener(listener: ClickListener?) {
        clickListener = listener
    }

}