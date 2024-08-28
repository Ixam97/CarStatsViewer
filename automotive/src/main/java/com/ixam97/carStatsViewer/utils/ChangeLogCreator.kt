package com.ixam97.carStatsViewer.utils

import android.content.Context
import com.ixam97.carStatsViewer.R

object ChangeLogCreator {

    private val versionRegex = Regex("\\[V]\\((.*)\\)")

    fun createChangelog(context: Context): Map<String, String> {

        val changesList = context.resources.getStringArray(R.array.changes).toMutableList()
        val pendingChangesList = context.resources.getStringArray(R.array.pending_changes).toMutableList()

        pendingChangesList.toList().forEachIndexed { index, change ->
            if (change.contains(versionRegex)) {
                val match = versionRegex.find(change)!!.destructured.toList()[0]
                pendingChangesList[index] = "[V]($match (Pre-Release))"
            }
        }

        val versionsMap = mutableMapOf<String, String>()

        var currentTitle = ""
        var currentChanges = ""

        changesList.addAll(pendingChangesList)

        changesList.reversed().forEachIndexed { index, change ->
            if (change.contains(versionRegex)) {
                if (index > 0) {
                    versionsMap[currentTitle] = currentChanges
                }
                versionRegex.find(change)?.let {
                    val destructedChange = it.destructured.toList()
                    currentTitle = if (destructedChange.isNotEmpty()) it.destructured.toList()[0] else "UNKNOWN"
                }
                currentChanges = ""
            } else {
                if (currentChanges.isNotEmpty()) currentChanges += "\n"
                currentChanges += "●  $change"
                if (index >= changesList.size -1) {
                    versionsMap[currentTitle] = currentChanges
                }
            }
        }

        return versionsMap
    }

    private fun String.toPreReleaseVersion(): String {
        val original = this

        var preReleaseVersion: String = ""

        return preReleaseVersion
    }
}