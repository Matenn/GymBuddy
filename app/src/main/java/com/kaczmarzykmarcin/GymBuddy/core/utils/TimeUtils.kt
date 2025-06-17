package com.kaczmarzykmarcin.GymBuddy.core.utils

import android.annotation.SuppressLint

/**
 * Utility functions for time and date operations
 */
object TimeUtils {

    /**
     * Formats elapsed time in milliseconds to a string in format "h:mm:ss"
     */
    @SuppressLint("DefaultLocale")
    fun formatElapsedTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        // Always return in h:mm:ss format
        return String.format("%d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Formats duration in seconds to a human-readable string format
     * Example: "2h 30min 15s" or "45min 20s" or "30s"
     */
    @SuppressLint("DefaultLocale")
    fun formatDurationSeconds(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> String.format("%dh %dmin %ds", hours, minutes, secs)
            minutes > 0 -> String.format("%dmin %ds", minutes, secs)
            else -> String.format("%ds", secs)
        }
    }
}