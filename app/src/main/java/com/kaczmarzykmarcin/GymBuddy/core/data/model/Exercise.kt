package com.kaczmarzykmarcin.GymBuddy.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Model danych reprezentujący ćwiczenie w aplikacji
 * Dostosowany do formatu yuhonas/free-exercise-db
 */
data class Exercise(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "",  // np. "strength", "stretching"
    val imageUrl: String? = null,
    val sortLetter: String = "", // Pierwsza litera do grupowania

    // Pola z bazy yuhonas/free-exercise-db
    val force: String? = null,  // "pull" lub "push"
    val level: String? = null,  // "beginner", "intermediate", "expert"
    val mechanic: String? = null, // "compound" lub "isolation"
    val equipment: String? = null, // np. "barbell", "dumbbell", "body only"
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val images: List<String> = emptyList(),

    // Flaga wskazująca, czy obraz jest lokalny
    val isLocalImage: Boolean = imageUrl?.startsWith("/") == true || imageUrl?.startsWith("file:") == true
) {
    // Przydatne metody konwersji do/z Map dla Firestore
    fun toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "category" to category,
        "imageUrl" to imageUrl,
        "sortLetter" to sortLetter,
        "force" to force,
        "level" to level,
        "mechanic" to mechanic,
        "equipment" to equipment,
        "primaryMuscles" to primaryMuscles,
        "secondaryMuscles" to secondaryMuscles,
        "instructions" to instructions,
        "notes" to notes,
        "tips" to tips,
        "images" to images
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Exercise = Exercise(
            id = id,
            name = map["name"] as? String ?: "",
            category = map["category"] as? String ?: "",
            imageUrl = map["imageUrl"] as? String,
            sortLetter = map["sortLetter"] as? String ?: "",
            force = map["force"] as? String,
            level = map["level"] as? String,
            mechanic = map["mechanic"] as? String,
            equipment = map["equipment"] as? String,
            primaryMuscles = (map["primaryMuscles"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            secondaryMuscles = (map["secondaryMuscles"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            instructions = (map["instructions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            notes = (map["notes"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            tips = (map["tips"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            images = (map["images"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    }
}