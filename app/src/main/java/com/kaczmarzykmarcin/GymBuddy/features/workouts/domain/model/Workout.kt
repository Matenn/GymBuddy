package com.kaczmarzykmarcin.GymBuddy.features.workouts.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Klasa reprezentująca szablon treningu.
 */
// Aktualizacja w Workout.kt
data class WorkoutTemplate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val exercises: List<CompletedExercise> = emptyList(), // Zmiana z List<String> na List<CompletedExercise>
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    // Przydatne metody konwersji do/z Map dla Firestore
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "name" to name,
        "description" to description,
        "categoryId" to categoryId,
        "exercises" to exercises.map { it.toMap() }, // Konwersja CompletedExercise na Map
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): WorkoutTemplate = WorkoutTemplate(
            id = id,
            userId = map["userId"] as? String ?: "",
            name = map["name"] as? String ?: "",
            description = map["description"] as? String ?: "",
            categoryId = map["categoryId"] as? String,
            exercises = (map["exercises"] as? List<*>)?.mapNotNull {
                (it as? Map<*, *>)?.let { CompletedExercise.fromMap(it) }
            } ?: emptyList(), // Konwersja Map na CompletedExercise
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
            updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now()
        )
    }
}

/**
 * Klasa reprezentująca wykonany trening.
 */
// Aktualizacja w Workout.kt
data class CompletedWorkout(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val templateId: String? = null,
    val categoryId: String? = null, // Dodane pole
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp? = null,
    val duration: Long = 0,
    val exercises: List<CompletedExercise> = emptyList()
) {
    // Przydatne metody konwersji do/z Map dla Firestore
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "name" to name,
        "templateId" to templateId,
        "categoryId" to categoryId, // Nowe pole
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
            categoryId = map["categoryId"] as? String, // Nowe pole
            startTime = map["startTime"] as? Timestamp ?: Timestamp.now(),
            endTime = map["endTime"] as? Timestamp,
            duration = (map["duration"] as? Long) ?: 0L,
            exercises = (map["exercises"] as? List<*>)?.mapNotNull {
                (it as? Map<*, *>)?.let { CompletedExercise.fromMap(it) }
            } ?: emptyList()
        )
    }




    /**
     * Oblicza czas trwania treningu w sekundach.
     */
    fun calculateDuration(): Long {
        if (endTime == null) return 0
        return endTime.seconds - startTime.seconds // bezpośrednio zwraca sekundy
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