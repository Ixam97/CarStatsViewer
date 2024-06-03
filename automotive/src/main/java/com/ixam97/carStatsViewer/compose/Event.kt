package com.ixam97.carStatsViewer.compose

open class Event<out T>(private val content: T) {
    var wasConsumed = false
        private set

    fun peek() = content

    fun consume(): T? {
        return when (wasConsumed) {
            true -> null
            false -> {
                wasConsumed = true
                content
            }
        }
    }
}