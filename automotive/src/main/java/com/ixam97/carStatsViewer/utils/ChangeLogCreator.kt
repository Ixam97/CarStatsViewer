package com.ixam97.carStatsViewer.utils

object ChangeLogCreator {
    fun createChangelogFromList(changesList: List<String>): Map<String, String> {
        val versionsMap = mutableMapOf<String, String>()

        var currentTitle = ""
        var currentChanges = ""

        changesList.forEachIndexed { index, change ->
            if (change.contains("VERSION")) {
                if (index > 0) {
                    versionsMap[currentTitle] = currentChanges
                }
                currentTitle = change.drop(change.indexOf(" "))
                currentChanges = ""
            } else {
                if (currentChanges.isNotEmpty()) currentChanges += "\n"
                currentChanges += "● $change"
                if (index >= changesList.size -1) {
                    versionsMap[currentTitle] = currentChanges
                }
            }
        }

        return versionsMap
    }
}