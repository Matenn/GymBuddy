package com.kaczmarzykmarcin.GymBuddy.features.user.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Klasa reprezentująca statystyki użytkownika związane z treningami.
 */
data class UserStats(
    @DocumentId val id: String = "",
    val userId: String = "",
    val level: Int = 1,
    val xp: Int = 0,
    val totalWorkoutsCompleted: Int = 0,
    val longestStreak: Int = 0,
    val currentStreak: Int = 0,
    val lastWorkoutDate: Timestamp? = null,
    val totalWorkoutTime: Long = 0, // w minutach
    // Statystyki poszczególnych typów treningów i ćwiczeń
    val workoutTypeStats: Map<String, WorkoutTypeStat> = mapOf(),
    val exerciseStats: Map<String, ExerciseStat> = mapOf()
) {
    // Przydatne metody konwersji do/z Map dla Firestore
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "level" to level,
        "xp" to xp,
        "totalWorkoutsCompleted" to totalWorkoutsCompleted,
        "longestStreak" to longestStreak,
        "currentStreak" to currentStreak,
        "lastWorkoutDate" to lastWorkoutDate,
        "totalWorkoutTime" to totalWorkoutTime,
        "workoutTypeStats" to workoutTypeStats.mapValues { it.value.toMap() },
        "exerciseStats" to exerciseStats.mapValues { it.value.toMap() }
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): UserStats = UserStats(
            id = id,
            userId = map["userId"] as? String ?: "",
            level = (map["level"] as? Long)?.toInt() ?: 1,
            xp = (map["xp"] as? Long)?.toInt() ?: 0,
            totalWorkoutsCompleted = (map["totalWorkoutsCompleted"] as? Long)?.toInt() ?: 0,
            longestStreak = (map["longestStreak"] as? Long)?.toInt() ?: 0,
            currentStreak = (map["currentStreak"] as? Long)?.toInt() ?: 0,
            lastWorkoutDate = map["lastWorkoutDate"] as? Timestamp,
            totalWorkoutTime = (map["totalWorkoutTime"] as? Long) ?: 0L,
            workoutTypeStats = (map["workoutTypeStats"] as? Map<*, *>)?.let { statsMap ->
                statsMap.entries.mapNotNull { entry ->
                    val key = entry.key as? String ?: return@mapNotNull null
                    val value = entry.value as? Map<*, *> ?: return@mapNotNull null
                    key to WorkoutTypeStat.fromMap(value)
                }.toMap()
            } ?: mapOf(),
            exerciseStats = (map["exerciseStats"] as? Map<*, *>)?.let { statsMap ->
                statsMap.entries.mapNotNull { entry ->
                    val key = entry.key as? String ?: return@mapNotNull null
                    val value = entry.value as? Map<*, *> ?: return@mapNotNull null
                    key to ExerciseStat.fromMap(value)
                }.toMap()
            } ?: mapOf()
        )
    }

    /**
     * Oblicza poziom na podstawie XP
     */
    fun calculateLevel(): Int {
        // Przykładowy wzór: każdy poziom wymaga więcej XP
        var remainingXp = xp
        var currentLevel = 1
        var xpForNextLevel = 100

        while (remainingXp >= xpForNextLevel) {
            remainingXp -= xpForNextLevel
            currentLevel++
            xpForNextLevel = 100 + (currentLevel - 1) * 50
        }

        return currentLevel
    }
}

/**
 * Klasa reprezentująca statystyki dla konkretnego typu treningu.
 */
data class WorkoutTypeStat(
    val count: Int = 0,
    val totalTime: Long = 0, // w minutach
    val lastPerformedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "count" to count,
        "totalTime" to totalTime,
        "lastPerformedAt" to lastPerformedAt
    )

    companion object {
        fun fromMap(map: Map<*, *>): WorkoutTypeStat {
            return WorkoutTypeStat(
                count = (map["count"] as? Long)?.toInt() ?: 0,
                totalTime = (map["totalTime"] as? Long) ?: 0L,
                lastPerformedAt = map["lastPerformedAt"] as? Timestamp
            )
        }
    }
}

/**
 * Klasa reprezentująca statystyki dla konkretnego ćwiczenia.
 */
data class ExerciseStat(
    val count: Int = 0,                      // ile razy wykonane
    val personalBestWeight: Double = 0.0,    // najlepszy ciężar (kg)
    val personalBestReps: Int = 0,           // najlepsze powtórzenie
    val averageWeight: Double = 0.0,         // średni ciężar
    val averageReps: Int = 0,                // średnia liczba powtórzeń
    val averageSets: Double = 0.0,           // średnia liczba serii
    val lastPerformedAt: Timestamp? = null,  // ostatnie wykonanie
    val progressHistory: List<WeightEntry> = emptyList() // historia postępu wagi
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "count" to count,
        "personalBestWeight" to personalBestWeight,
        "personalBestReps" to personalBestReps,
        "averageWeight" to averageWeight,
        "averageReps" to averageReps,
        "averageSets" to averageSets,
        "lastPerformedAt" to lastPerformedAt,
        "progressHistory" to progressHistory.map { it.toMap() }
    )

    companion object {
        fun fromMap(map: Map<*, *>): ExerciseStat {
            return ExerciseStat(
                count = (map["count"] as? Long)?.toInt() ?: 0,
                personalBestWeight = (map["personalBestWeight"] as? Double) ?: 0.0,
                personalBestReps = (map["personalBestReps"] as? Long)?.toInt() ?: 0,
                averageWeight = (map["averageWeight"] as? Double) ?: 0.0,
                averageReps = (map["averageReps"] as? Long)?.toInt() ?: 0,
                averageSets = (map["averageSets"] as? Double) ?: 0.0,
                lastPerformedAt = map["lastPerformedAt"] as? Timestamp,
                progressHistory = (map["progressHistory"] as? List<*>)?.mapNotNull {
                    (it as? Map<*, *>)?.let { WeightEntry.fromMap(it) }
                } ?: emptyList()
            )
        }
    }
}

/**
 * Klasa przechowująca pojedynczy wpis historii obciążenia dla wykresów postępu.
 */
data class WeightEntry(
    val date: Timestamp = Timestamp.now(),
    val weight: Double = 0.0,
    val reps: Int = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "date" to date,
        "weight" to weight,
        "reps" to reps
    )

    companion object {
        fun fromMap(map: Map<*, *>): WeightEntry {
            return WeightEntry(
                date = map["date"] as? Timestamp ?: Timestamp.now(),
                weight = (map["weight"] as? Double) ?:.0,
                reps = (map["reps"] as? Long)?.toInt() ?: 0
            )
        }
    }
}