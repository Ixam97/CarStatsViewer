package com.ixam97.carStatsViewer.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.ixam97.carStatsViewer.R

class MultiLineRowButtonWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

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
        this.removeAllViews()
        View.inflate(context, R.layout.widget_menu_row, this)
        val topTextView: TextView = findViewById(R.id.row_top_text)
        val bottomTextView: TextView = findViewById(R.id.distance_text)
        topTextView.text = topText
        bottomTextView.text = bottomText
    }
}