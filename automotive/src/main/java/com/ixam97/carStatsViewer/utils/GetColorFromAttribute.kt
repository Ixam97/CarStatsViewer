package com.ixam97.carStatsViewer.utils

import android.content.Context
import android.util.TypedValue

fun getColorFromAttribute(context: Context, attrId: Int): Int {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(attrId, typedValue, true)
    return typedValue.data
}