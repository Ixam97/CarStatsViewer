package com.ixam97.carStatsViewer.carApp.utils

import android.graphics.Bitmap
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat

fun Bitmap.asCarIcon(): CarIcon = CarIcon.Builder(IconCompat.createWithBitmap(this)).build()