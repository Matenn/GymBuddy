package com.kaczmarzykmarcin.GymBuddy.features.statistics.data.model

import com.kaczmarzykmarcin.GymBuddy.data.model.Exercise

// Enums
enum class TimePeriod(val displayName: String) {
    WEEK("Tydzień"),
    MONTH("Miesiąc"),
    THREE_MONTHS("3 mies."),
    YEAR("Rok"),
    ALL("Wszystkie")
}
enum class ProgressMetric(val displayName: String, val shortName: String, val unit: String) {
    MAX_WEIGHT("Maksymalna waga", "Max waga", "kg"),
    ONE_RM("Teoretyczne 1RM", "1RM", "kg"),
    VOLUME("Objętość treningowa", "Objętość", "kg×reps"),
    TONNAGE("Tonaż", "Tonaż", "kg")
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
    val weight: Double, // Główna wartość do wyświetlenia
    val reps: Int,
    val label: String,
    val originalWeight: Double = weight, // Oryginalna waga ze setu
    val originalReps: Int = reps, // Oryginalna liczba powtórzeń
    val volume: Double = weight * reps, // Objętość (waga × reps)
    val tonnage: Double = 0.0 // Będzie obliczane jako suma wszystkich setów
) {
    val displayText: String
        get() = "${weight.toInt()} kg x $reps"

    val oneRMFormatted: String
        get() = String.format("%.1f kg", weight)

    val originalSetFormatted: String
        get() = "${originalWeight}kg × ${originalReps} reps"

    val volumeFormatted: String
        get() = String.format("%.0f kg×reps", volume)

    val tonnageFormatted: String
        get() = String.format("%.1f kg", tonnage)
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
    val personalBest1RM: Double, // Najlepszy teoretyczny 1RM
    val personalBestVolume: Double, // Najlepszy pojedynczy set (waga × reps)
    val personalBestTonnage: Double, // Najlepszy tonaż w jednym treningu
    val averageWeight: Double,
    val averageReps: Int,
    val average1RM: Double, // Średni 1RM
    val averageVolume: Double, // Średnia objętość na set
    val averageTonnage: Double, // Średni tonaż na trening
    val averageSets: Double,
    val progressPoints: List<ProgressPoint>
) {
    val personalBestFormatted: String
        get() = "${personalBestWeight.toInt()} kg"

    val averageWeightFormatted: String
        get() = "${averageWeight.toInt()} kg"

    val averageSetsFormatted: String
        get() = String.format("%.1f", averageSets)

    val personalBest1RMFormatted: String
        get() = String.format("%.1f kg", personalBest1RM)

    val personalBestVolumeFormatted: String
        get() = String.format("%.0f kg×reps", personalBestVolume)

    val personalBestTonnageFormatted: String
        get() = String.format("%.1f kg", personalBestTonnage)

    val average1RMFormatted: String
        get() = String.format("%.1f kg", average1RM)

    val averageVolumeFormatted: String
        get() = String.format("%.0f kg×reps", averageVolume)

    val averageTonnageFormatted: String
        get() = String.format("%.1f kg", averageTonnage)
}
