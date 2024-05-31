package com.ixam97.carStatsViewer.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R

@Composable
fun Polestar2Header(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .height(139.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row (
                modifier = Modifier.height(80.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    modifier = Modifier
                        .padding(start = 13.dp, end = 23.dp)
                        .size(height = 80.dp, width = 80.dp),
                    onClick = onBackClick
                ) {
                    Icon(
                        painterResource(id = R.drawable.ic_arrow_back),
                        tint = MaterialTheme.colors.secondary,
                        contentDescription = null)
                }
                Text(
                    modifier = Modifier
                        .padding(bottom = 11.dp),
                    text = "Polestar Header",
                    style = MaterialTheme.typography.h1
                )
            }
        }
        Box(modifier = Modifier
            .height(2.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.secondary)
        )
    }
}