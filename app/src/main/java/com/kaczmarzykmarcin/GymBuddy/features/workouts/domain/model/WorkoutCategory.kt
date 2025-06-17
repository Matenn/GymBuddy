// WorkoutCategory.kt
package com.kaczmarzykmarcin.GymBuddy.features.workouts.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class WorkoutCategory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",    // Np. "Push", "Pull", "Legs", "Upper", "Lower" itp.
    val color: String = "#000000", // Kolor w formacie HEX dla wizualnego rozróżnienia
    val createdAt: Timestamp = Timestamp.now(),
    val isDefault: Boolean = false // Czy jest domyślną kategorią
) {
    // Przydatne metody konwersji do/z Map dla Firestore
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "name" to name,
        "color" to color,
        "createdAt" to createdAt,
        "isDefault" to isDefault
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): WorkoutCategory = WorkoutCategory(
            id = id,
            userId = map["userId"] as? String ?: "",
            name = map["name"] as? String ?: "",
            color = map["color"] as? String ?: "#000000",
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
            isDefault = map["isDefault"] as? Boolean ?: false
        )

        // Predefiniowane kategorie treningowe
        val PREDEFINED_CATEGORIES = listOf(
            WorkoutCategory(id = "push", name = "Push", color = "#4285F4", isDefault = true),
            WorkoutCategory(id = "pull", name = "Pull", color = "#34A853", isDefault = true),
            WorkoutCategory(id = "legs", name = "Legs", color = "#FBBC05", isDefault = true),
            WorkoutCategory(id = "upper", name = "Upper", color = "#EA4335", isDefault = true),
            WorkoutCategory(id = "lower", name = "Lower", color = "#9C27B0", isDefault = true),
            WorkoutCategory(id = "full_body", name = "Full Body", color = "#FF9800", isDefault = true),
            WorkoutCategory(id = "cardio", name = "Cardio", color = "#795548", isDefault = true),
            WorkoutCategory(id = "other", name = "Other", color = "#607D8B", isDefault = true)
        )
    }
}