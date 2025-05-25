package com.kaczmarzykmarcin.GymBuddy.features.statistics.data.model

import com.kaczmarzykmarcin.GymBuddy.data.model.Exercise

// Enums
enum class TimePeriod(val displayName: String) {
    WEEK("Tydzień"),
    MONTH("Miesiąc"),
    THREE_MONTHS("3 mies."),
    YEAR("Rok")
}

enum class StatType(val displayName: String) {
    CATEGORY("Kategoria"),
    EXERCISE("Ćwiczenia")
}

// Data classes for statistics
data class BasicStatistics(
    val totalWorkouts: Int,
    val totalTimeMinutes: Int
) {
    val totalTimeFormatted: String
        get() {
            val hours = totalTimeMinutes / 60
            val minutes = totalTimeMinutes % 60
            return if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes}m"
            }
        }
}

data class ActivityData(
    val label: String,
    val minutes: Int
)

data class CategoryDistribution(
    val categoryName: String,
    val categoryColor: String,
    val count: Int,
    val percentage: Int
)

data class ExerciseDistribution(
    val exerciseId: String,
    val exerciseName: String,
    val setCount: Int,
    val percentage: Int
)

data class ProgressPoint(
    val timestamp: Long,
    val weight: Double,
    val reps: Int,
    val label: String
) {
    val displayText: String
        get() = "${weight.toInt()} kg x $reps"
}

data class ProgressData(
    val exerciseId: String,
    val exerciseName: String,
    val progressPoints: List<ProgressPoint>
)

data class ExerciseStatisticsData(
    val exerciseId: String,
    val exerciseName: String,
    val personalBestWeight: Double,
    val personalBestReps: Int,
    val averageWeight: Double,
    val averageReps: Int,
    val averageSets: Double,
    val progressPoints: List<ProgressPoint>
) {
    val personalBestFormatted: String
        get() = "${personalBestWeight.toInt()} kg"

    val averageWeightFormatted: String
        get() = "${averageWeight.toInt()} kg"

    val averageSetsFormatted: String
        get() = String.format("%.1f", averageSets)
}