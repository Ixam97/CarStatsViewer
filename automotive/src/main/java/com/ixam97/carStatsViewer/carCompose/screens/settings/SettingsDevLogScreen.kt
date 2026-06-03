package com.ixam97.carStatsViewer.carCompose.screens.settings

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.theme.polestar4ContentPadding
import com.ixam97.carStatsViewer.compose.theme.polestarOrange
import com.ixam97.carStatsViewer.database.log.LogEntry
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.logLength
import com.ixam97.carStatsViewer.utils.logLevel
import de.ixam97.carcompose.components.layout.CarPaneLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat

@Serializable
data object SettingsDevLogScreenNavKey: NavKey

@Composable
fun SettingsDevLogScreen(
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    CarPaneLayout(
        headerTitle = "Debug Log",
        isLoading = isLoading,
        onBackAction = onBack
    ) {
        SettingsDevLogContent(
            modifier = Modifier.padding(start = if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp),
            onLoadingStateChanged = { isLoading = it }
        )
    }
}

@Composable
fun SettingsDevLogContent(
    modifier: Modifier = Modifier,
    onLoadingStateChanged: (Boolean) -> Unit
) {

    var logEntries by remember { mutableStateOf(listOf<LogEntry>()) }

    LaunchedEffect(null) {
        onLoadingStateChanged(true)
        withContext(Dispatchers.IO) {
            logEntries = InAppLogger.getLogEntries(
                logLevel = CarStatsViewer.appPreferences.logLevel + 2,
                logLength = LogLengthKey.entries[CarStatsViewer.appPreferences.logLength].length
            ).reversed()
        }
        onLoadingStateChanged(false)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        reverseLayout = true
    ) {
        items(items = logEntries) { logEntry ->
            LogRow(logEntry)
        }
    }
}

@Composable
private fun LogRow(logEntry: LogEntry) {
    Row(
        modifier = Modifier
            .padding(horizontal = 15.dp, vertical = 3.dp)
    ) {
        var color = when(logEntry.type) {
            Log.ERROR -> Color.Red
            Log.WARN -> polestarOrange
            else -> Color.Gray
        }
        if (logEntry.message.contains("Car Stats Viewer") && logEntry.type == Log.INFO) {
            color = Color.Green
        }
        Text(
            text = "${SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(logEntry.epochTime)} ${InAppLogger.typeSymbol(logEntry.type)}: ",
            fontFamily = FontFamily.Monospace,
            fontSize = 17.sp,
            color = color
        )
        Text(
            text = logEntry.message,
            fontFamily = FontFamily.Monospace,
            fontSize = 17.sp,
            color = color
        )
    }
}