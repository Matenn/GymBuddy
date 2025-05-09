package com.kaczmarzykmarcin.GymBuddy.utils

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
}