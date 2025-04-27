package com.ixam97.carStatsViewer.compose.screens.settings

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ixam97.carStatsViewer.compose.DefaultLazyListScrollbar
import com.ixam97.carStatsViewer.compose.SettingsViewModel
import com.ixam97.carStatsViewer.compose.theme.polestarOrange
import com.ixam97.carStatsViewer.database.log.LogEntry
import com.ixam97.carStatsViewer.utils.InAppLogger
import java.text.SimpleDateFormat

@Composable
fun LogScreen(viewModel: SettingsViewModel) {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val logRows = viewModel.log
        if (logRows == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (logRows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No log entries.")
            }
        }
        else {
            DefaultLazyListScrollbar(
                reverseLayout = true
            ) {
                items(logRows.reversed()) { logRow ->
                    LogRow(logRow)
                }
            }
        }
    }

}

@Composable
internal fun LogRow(logEntry: LogEntry) {
    Row(
        modifier = Modifier
            .padding(horizontal = 15.dp, vertical = 3.dp)
    ) {
        var color = when(logEntry.type) {
            Log.ERROR -> Color.Red
            Log.WARN -> polestarOrange
            else -> Color.Gray
        }
        if (logEntry.message.contains("Car Stats Viewer")) {
            color = Color.Green
        }
        Text(
            text = "${SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(logEntry.epochTime)} ${InAppLogger.typeSymbol(logEntry.type)}: ",
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            color = color
        )
        Text(
            text = logEntry.message,
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            color = color
        )
    }
}