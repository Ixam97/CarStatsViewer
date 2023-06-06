package com.ixam97.carStatsViewer.views

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.paris.extensions.style
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.utils.StringFormatters
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.widget_trip_history_row.view.*
import java.util.*

class TripHistoryRowWidget(
    context: Context,
    private val attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private var session: DrivingSession? = null
) :
    LinearLayout(context, attrs, defStyleAttr) {

    private var mainClickListener: OnMainClickListener? = null
    private var deleteClickListener: OnDeleteClickListener? = null
    private var mainLongClickListener: OnDeleteClickListener? = null

    init {
        init()
    }

    fun interface OnMainClickListener {
        fun onMainClicked()
    }

    fun interface OnDeleteClickListener {
        fun onDeleteClicked()
    }

    fun setSession(newSession: DrivingSession) {
        session = newSession
        init()
    }

    fun setOnMainClickListener(listener: OnMainClickListener) {
        mainClickListener = listener
    }

    fun setOnMainLongClickListener(listener: OnDeleteClickListener) {
        mainLongClickListener = listener
    }

    fun setOnDeleteClickListener(listener: OnDeleteClickListener) {
        deleteClickListener = listener
    }

    private fun init() {
        this.removeAllViews()
        View.inflate(context, R.layout.widget_trip_history_row, this)

        val rowIcon: ImageView = findViewById(R.id.row_start_icon)
        val rowEndButton: ImageButton = findViewById(R.id.row_end_button)

        val topText: TextView = findViewById(R.id.row_top_text)
        val distanceText: TextView = findViewById(R.id.distance_text)
        val energyText: TextView = findViewById(R.id.energy_text)
        val consText: TextView = findViewById(R.id.consumption_text)
        val timeText: TextView = findViewById(R.id.time_text)

        val rowBody: ConstraintLayout = findViewById(R.id.row_container_body)


        rowBody.setOnClickListener {
            mainClickListener?.onMainClicked()
        }

        rowBody.setOnLongClickListener {
            mainLongClickListener?.onDeleteClicked()
            true
        }

        session?.let {

            if (it.session_type == 5) {
                val sectionTitleContainer: LinearLayout = findViewById(R.id.section_title_container)
                val sectionTitleText: TextView = findViewById(R.id.section_title_text)
                val rowContainer: ConstraintLayout = findViewById(R.id.row_container)
                sectionTitleContainer.visibility = View.VISIBLE
                rowContainer.visibility = View.GONE
                sectionTitleText.text = it.note
            }

            if (it.session_type != TripType.MANUAL && (it.end_epoch_time?:0) <= 0) {
                rowEndButton.setOnLongClickListener {
                    deleteClickListener?.onDeleteClicked()
                    true
                }
            } else {
                rowEndButton.setOnClickListener {
                    deleteClickListener?.onDeleteClicked()
                }
            }

            when (it.session_type) {
                TripType.SINCE_CHARGE -> rowIcon.setImageResource(R.drawable.ic_charger_2)
                TripType.MONTH -> rowIcon.setImageResource(R.drawable.ic_month)
                TripType.AUTO -> rowIcon.setImageResource(R.drawable.ic_day)
                TripType.MANUAL -> rowIcon.setImageResource(R.drawable.ic_hand)
                else -> rowIcon.setImageResource(R.drawable.ic_help)
            }

            if ((it.end_epoch_time?:0) <= 0) {
                rowEndButton.setImageResource(R.drawable.ic_reset)
                if (it.session_type != TripType.MANUAL) {
                    // rowEndButton.isEnabled = false
                    rowEndButton.setColorFilter(context.getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)
                }
            }

            topText.text = "${
                StringFormatters.getDateString(
                    Calendar.getInstance().apply { timeInMillis = it.start_epoch_time })
            } ID: ${session?.driving_session_id.toString()}"
            distanceText.text = StringFormatters.getTraveledDistanceString(it.driven_distance.toFloat())
            energyText.text = StringFormatters.getEnergyString(it.used_energy.toFloat())
            consText.text = StringFormatters.getAvgConsumptionString(it.used_energy.toFloat(), it.driven_distance.toFloat())
            timeText.text = StringFormatters.getElapsedTimeString(it.drive_time, minutes = true)
        }
    }
}