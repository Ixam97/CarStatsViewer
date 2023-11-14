package com.ixam97.carStatsViewer.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import com.ixam97.carStatsViewer.R

class MultiButtonWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        var isPolestar = false
    }

    fun interface OnIndexChangedListener {
        fun indexChanged()
    }

    private var onIndexChangedListener: OnIndexChangedListener? = null

    val buttonList: List<Button>
    val dividerList: List<View>

    var maxIndex: Int = 3
        set(value) {
            field = value
            setupVisibleButtons()
        }

    var selectedIndex: Int = 0
        set(value) {
            field = value
            updateView()
        }

    var buttonNames: List<String>
        set(value) {
            buttonList.forEachIndexed {index, button ->
                button.text = value[index]
            }
        }
        get() {
            return buttonList.map { it.text.toString() }
        }

    init {
        inflate(context, R.layout.widget_multi_button, this)

        buttonList = listOf(
            findViewById(R.id.button_1),
            findViewById(R.id.button_2),
            findViewById(R.id.button_3),
            findViewById(R.id.button_4)
        )

        dividerList = listOf(
            findViewById(R.id.divider_1),
            findViewById(R.id.divider_2),
            findViewById(R.id.divider_3),
        )

        if (!isPolestar) {
            buttonList.forEachIndexed { index, button ->
                if (index == 0) return@forEachIndexed
                val layoutParams = button.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.marginStart = 10
                button.layoutParams = layoutParams
            }
        }

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.MultiButtonWidget)

        try {
            maxIndex = attributes.getInteger(R.styleable.MultiButtonWidget_numButtons, 4) - 1
            selectedIndex = attributes.getInteger(R.styleable.MultiButtonWidget_defaultIndex, 0)
            buttonList[0].text = attributes.getString(R.styleable.MultiButtonWidget_textButton1)?: "Button 1"
            buttonList[1].text = attributes.getString(R.styleable.MultiButtonWidget_textButton2)?: "Button 2"
            buttonList[2].text = attributes.getString(R.styleable.MultiButtonWidget_textButton3)?: "Button 3"
            buttonList[3].text = attributes.getString(R.styleable.MultiButtonWidget_textButton4)?: "Button 4"
        } finally {
            attributes.recycle()
        }
    }

    fun setOnIndexChangedListener(listener: OnIndexChangedListener) {
        onIndexChangedListener = listener
    }

    private fun setupVisibleButtons() {
        buttonList.forEachIndexed { index, button ->
            if (index > maxIndex) {
                button.isVisible = false
            } else {
                button.setOnClickListener {
                    selectedIndex = index
                    onIndexChangedListener?.indexChanged()
                }
            }
        }
    }

    private fun updateView() {
        buttonList.forEachIndexed { index, button ->
            button.isSelected = (index == selectedIndex)
        }
        dividerList.forEachIndexed { index, divider ->
            divider.isVisible = (index != selectedIndex && index != selectedIndex - 1 && index < maxIndex && isPolestar)
        }
    }
}