package com.ixam97.carStatsViewer.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.log.LogEntry
import com.ixam97.carStatsViewer.utils.InAppLogger
import java.text.SimpleDateFormat

class LogAdapter(var log: List<LogEntry>): RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    inner class LogViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val logRow = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_log_row, parent, false)
        return LogViewHolder(logRow)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.itemView.findViewById<TextView>(R.id.log_row).apply {
            val logEntry = log[position]
            val logStringBuilder = StringBuilder()
            if (logEntry.message.contains("Car Stats Viewer"))
                logStringBuilder.append("------------------------------------------------------------\n")
            logStringBuilder.append("${SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(logEntry.epochTime)} ${InAppLogger.typeSymbol(logEntry.type)}: ${logEntry.message}")
            text = logStringBuilder.toString()
            when (logEntry.type) {
                Log.ERROR -> setTextColor(context.getColor(R.color.bad_red))
                Log.WARN -> setTextColor(context.getColor(R.color.polestar_orange))
                else -> setTextColor(Color.GRAY)
            }
        }
    }

    override fun getItemCount() = log.size
}