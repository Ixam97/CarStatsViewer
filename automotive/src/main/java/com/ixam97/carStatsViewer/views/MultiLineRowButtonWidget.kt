package com.ixam97.carStatsViewer.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ixam97.carStatsViewer.R

class MultiLineRowButtonWidget @JvmOverloads constructor(context: Context, private val attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    LinearLayout(context, attrs, defStyleAttr) {

    var topText = ""
        set(value) {
            field = value
            init()
        }
    var bottomText = ""
        set(value) {
            field = value
            init()
        }

    init {
        init()
    }

    private fun init() {
        View.inflate(context, R.layout.widget_multi_line_row_button, this)
        val topTextView: TextView = findViewById(R.id.row_top_text)
        val bottomTextView: TextView = findViewById(R.id.row_bottom_text)
        topTextView.text = topText
        bottomTextView.text = bottomText
    }
}