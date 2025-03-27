package com.ixam97.carStatsViewer.compose.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.compose.verticalScrollWithScrollbar
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.util.withContext

internal data class DialogLibrary(
    val name: String,
    val content: List<String>,
    val urls: Boolean
)

@Composable
fun Licenses() {

    val libraries = Libs.Builder().withContext(LocalContext.current).build().libraries

    var dialogLibrary by remember { mutableStateOf<DialogLibrary?>(null)}

    val lazyListState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(state = lazyListState){

            items(libraries) { library ->
                LibraryRow(
                    library = library,
                    onLibraryClick = { clickLibrary ->
                        val dialogContent = mutableListOf<String>()
                        val dialogUrls = mutableListOf<String>()
                        clickLibrary.licenses.forEach { license ->
                            license.licenseContent?.let {
                                if (it.isNotBlank()) dialogContent.add(it)
                            }
                            license.url?.let {
                                if (it.isNotBlank()) dialogUrls.add(it)
                            }
                        }
                        if (dialogContent.isNotEmpty()) {
                            dialogLibrary = DialogLibrary(
                                name = clickLibrary.name,
                                content = dialogContent,
                                urls = false
                            )
                        } else {
                            dialogLibrary = DialogLibrary(
                                name = clickLibrary.name,
                                content = dialogUrls,
                                urls = true
                            )
                        }
                    }
                )
            }
        }

        dialogLibrary?.let { LibraryDialog(it) { dialogLibrary = null } }
    }
}

@Composable
internal fun LibraryDialog(
    library: DialogLibrary,
    onCloseClick: () -> Unit
) {
    Box (
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {  }
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(50.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.surface)
        ) {
            Row (
                modifier = Modifier
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = library.name,
                    style = MaterialTheme.typography.h2
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(65.dp)
                        .clip(CircleShape)
                        .clickable { onCloseClick.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(50.dp),
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onSurface
                    )

                }
            }
            Divider(modifier = Modifier.height(2.dp))
            Column(
                modifier = Modifier.verticalScrollWithScrollbar(rememberScrollState())
            ) {
                val context = LocalContext.current
                library.content.forEach { licenseContent ->
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = library.urls
                            ) {
                                if (library.urls) {
                                    context.startActivity(Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(licenseContent)
                                    ))
                                }
                            }
                            .padding(15.dp),
                        text = licenseContent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
internal fun LibraryRow(
    library: Library,
    onLibraryClick: (Library) -> Unit
) {
    Column {
        CarRow(
            modifier = Modifier
                .clickable { onLibraryClick.invoke(library) },
            title = library.name,
            customContent = {
                Column {
                    if (library.author.isNotBlank()) {
                        Text(
                            text = library.author,
                            color = colorResource(id = R.color.secondary_text_color)
                        )
                    }
                    if (library.licenses.isNotEmpty()) {
                        var licensesString = ""
                        library.licenses.forEachIndexed { index, license ->
                            if (index > 0) licensesString += ", "
                            licensesString += license.name
                        }
                        Text(
                            text = licensesString,
                            color = colorResource(id = R.color.secondary_text_color)
                        )
                    }
                }
            }
        )
        Divider(modifier = Modifier.padding(horizontal = 24.dp))
    }
}