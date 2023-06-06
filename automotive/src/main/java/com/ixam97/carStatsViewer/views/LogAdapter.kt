package com.ixam97.carStatsViewer.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ixam97.carStatsViewer.R
import kotlinx.android.synthetic.main.recyclerview_log_row.view.*

class LogAdapter(var log: List<String>): RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    inner class LogViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val logRow = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_log_row, parent, false)
        return LogViewHolder(logRow)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.itemView.apply {
            log_row.text = log[position]
        }
    }

    override fun getItemCount() = log.size
}