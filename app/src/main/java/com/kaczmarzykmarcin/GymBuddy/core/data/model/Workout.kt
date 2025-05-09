package com.kaczmarzykmarcin.GymBuddy.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Klasa reprezentująca szablon treningu.
 */
data class WorkoutTemplate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",  // np. "Klata i triceps"
    val description: String = "", // np. "Bench press, csodaodaosa, doasdoa"
    val exercises: List<String> = emptyList(), // Lista ID ćwiczeń
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    // Przydatne metody konwersji do/z Map dla Firestore
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "name" to name,
        "description" to description,
        "exercises" to exercises,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): WorkoutTemplate = WorkoutTemplate(
            id = id,
            userId = map["userId"] as? String ?: "",
            name = map["name"] as? String ?: "",
            description = map["description"] as? String ?: "",
            exercises = (map["exercises"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
            updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now()
        )
    }
}

/**
 * Klasa reprezentująca wykonany trening.
 */
data class CompletedWorkout(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",  // np. "Poranny Trening"
    val templateId: String? = null, // ID szablonu (jeśli był użyty)
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp? = null,
    val duration: Long = 0, // w minutach
    val exercises: List<CompletedExercise> = emptyList()
) {
    // Przydatne metody konwersji do/z Map dla Firestore
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "name" to name,
        "templateId" to templateId,
        "startTime" to startTime,
        "endTime" to endTime,
        "duration" to duration,
        "exercises" to exercises.map { it.toMap() }
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): CompletedWorkout = CompletedWorkout(
            id = id,
            userId = map["userId"] as? String ?: "",
            name = map["name"] as? String ?: "",
            templateId = map["templateId"] as? String,
            startTime = map["startTime"] as? Timestamp ?: Timestamp.now(),
            endTime = map["endTime"] as? Timestamp,
            duration = (map["duration"] as? Long) ?: 0L,
            exercises = (map["exercises"] as? List<*>)?.mapNotNull {
                (it as? Map<*, *>)?.let { CompletedExercise.fromMap(it) }
            } ?: emptyList()
        )
    }

    /**
     * Oblicza czas trwania treningu w minutach.
     */
    fun calculateDuration(): Long {
        if (endTime == null) return 0
        val diffSeconds = endTime.seconds - startTime.seconds
        return diffSeconds / 60
    }
}

/**
 * Klasa reprezentująca wykonane ćwiczenie w ramach treningu.
 */
data class CompletedExercise(
    val exerciseId: String = "",
    val name: String = "",
    val category: String = "",
    val sets: List<ExerciseSet> = emptyList()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "exerciseId" to exerciseId,
        "name" to name,
        "category" to category,
        "sets" to sets.map { it.toMap() }
    )

    companion object {
        fun fromMap(map: Map<*, *>): CompletedExercise = CompletedExercise(
            exerciseId = map["exerciseId"] as? String ?: "",
            name = map["name"] as? String ?: "",
            category = map["category"] as? String ?: "",
            sets = (map["sets"] as? List<*>)?.mapNotNull {
                (it as? Map<*, *>)?.let { ExerciseSet.fromMap(it) }
            } ?: emptyList()
        )
    }
}

/**
 * Klasa reprezentująca pojedynczą serię ćwiczenia.
 */
data class ExerciseSet(
    val setType: String = "normal", // "normal", "warmup", "dropset", "failure"
    val weight: Double = 0.0,       // ciężar w kg
    val reps: Int = 0               // liczba powtórzeń
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "setType" to setType,
        "weight" to weight,
        "reps" to reps
    )

    companion object {
        fun fromMap(map: Map<*, *>): ExerciseSet = ExerciseSet(
            setType = map["setType"] as? String ?: "normal",
            weight = when (val w = map["weight"]) {
                is Double -> w
                is Float -> w.toDouble()
                is Int -> w.toDouble()
                is Long -> w.toDouble()
                else -> 0.0
            },
            reps = when (val r = map["reps"]) {
                is Int -> r
                is Long -> r.toInt()
                is Double -> r.toInt()
                is Float -> r.toInt()
                else -> 0
            }
        )
    }}