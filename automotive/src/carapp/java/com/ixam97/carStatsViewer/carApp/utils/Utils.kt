package com.ixam97.carStatsViewer.carApp.utils

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.R

fun Bitmap.asCarIcon(): CarIcon = CarIcon.Builder(IconCompat.createWithBitmap(this)).build()

fun CarContext.carIconFromRes(@DrawableRes resID: Int) = CarIcon.Builder(IconCompat.createWithResource(this, resID)).build()

val CarContext.constraintManager
    get() = getCarService(CarContext.CONSTRAINT_SERVICE) as ConstraintManager

fun CarContext.getContentLimit(id: Int) = if (carAppApiLevel >= 2) {
    constraintManager.getContentLimit(id)
} else {
    when (id) {
        ConstraintManager.CONTENT_LIMIT_TYPE_GRID -> 6
        ConstraintManager.CONTENT_LIMIT_TYPE_LIST -> 6
        ConstraintManager.CONTENT_LIMIT_TYPE_PANE -> 4
        ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST -> 6
        ConstraintManager.CONTENT_LIMIT_TYPE_ROUTE_LIST -> 3
        else -> throw IllegalArgumentException("unknown limit ID")
    }
}

fun manualTripIcon(carContext: CarContext) = CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_hand)).build()
fun sinceChargeTripIcon(carContext: CarContext) = CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_charger)).build()
fun autoTripIcon(carContext: CarContext) = CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_day)).build()
fun monthTripIcon(carContext: CarContext) = CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_month)).build()