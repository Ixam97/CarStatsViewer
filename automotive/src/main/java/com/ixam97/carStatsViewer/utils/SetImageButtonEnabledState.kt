package com.ixam97.carStatsViewer.utils

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.widget.ImageButton
import com.ixam97.carStatsViewer.R

fun setImageButtonEnabledState(context: Context, button: ImageButton, enabledState: Boolean) {
    button.isEnabled = enabledState
    when (enabledState) {
        true -> button.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        false -> button.setColorFilter(context.getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)
    }
}