package com.ixam97.carStatsViewer.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.theme.CarTheme
import com.ixam97.carStatsViewer.compose.theme.LocalBrushes

@Composable
fun CarHeader(
    title: String,
    onBackClick: (() -> Unit)? = null,
    minimal: Boolean = false,
    headerLineBrush: Brush = CarTheme.brushes.headerLineBrush
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
                // .background(color = if (!minimal) MaterialTheme.colors.background else Color.Transparent),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                CarIconButton(
                    onClick = onBackClick,
                    iconResId = CarTheme.backButtonResId, // R.drawable.ic_arrow_back,
                    tint = if (!minimal) MaterialTheme.colors.secondary else MaterialTheme.colors.onBackground
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else Spacer(modifier = Modifier.width(24.dp))
            Text(text = title, style = typography.h1)
        }
        if (!minimal) Box(
            modifier = Modifier
                .height(3.dp)
                .fillMaxWidth()
                .background(
                    brush = headerLineBrush
                ),
        )
    }
}

@Composable
fun CarHeaderWithContent(
    onBackClick: (() -> Unit)? = null,
    minimal: Boolean = false,
    headerLineBrush: Brush = CarTheme.brushes.headerLineBrush,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            // .background(color = if (!minimal) MaterialTheme.colors.background else Color.Transparent),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                CarIconButton(
                    onClick = onBackClick,
                    iconResId = R.drawable.ic_arrow_back,
                    tint = if (!minimal) MaterialTheme.colors.secondary else MaterialTheme.colors.onBackground
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else Spacer(modifier = Modifier.width(24.dp))
            content()
        }
        if (!minimal) Box(
            modifier = Modifier
                .height(3.dp)
                .fillMaxWidth()
                .background(
                    brush = headerLineBrush
                ),
        )
    }
}