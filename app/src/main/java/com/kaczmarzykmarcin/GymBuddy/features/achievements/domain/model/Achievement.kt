// Achievement.kt - nowy model osiągnięć
package com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Typ osiągnięcia określający jak jest sprawdzane
 */
enum class AchievementType {
    WORKOUT_COUNT,      // Liczba ukończonych treningów
    WORKOUT_STREAK,     // Dni z rzędu z treningiem
    MORNING_WORKOUTS,   // Poranne treningi
    EXERCISE_WEIGHT,    // Ciężar w konkretnym ćwiczeniu
    WORKOUT_DURATION,   // Czas treningu
    FIRST_TIME         // Jednorazowe osiągnięcia
}

/**
 * Definicja osiągnięcia
 */
data class AchievementDefinition(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: AchievementType = AchievementType.FIRST_TIME,
    val targetValue: Int = 1,           // Wartość docelowa (np. 10 treningów)
    val xpReward: Int = 0,
    val iconName: String = "trophy",    // Nazwa ikony lub emoji
    val isActive: Boolean = true,       // Czy osiągnięcie jest aktywne
    val exerciseId: String? = null,     // Dla osiągnięć związanych z konkretnym ćwiczeniem
    val categoryId: String? = null,     // Dla osiągnięć związanych z kategorią
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "description" to description,
        "type" to type.name,
        "targetValue" to targetValue,
        "xpReward" to xpReward,
        "iconName" to iconName,
        "isActive" to isActive,
        "exerciseId" to exerciseId,
        "categoryId" to categoryId,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AchievementDefinition = AchievementDefinition(
            id = id,
            title = map["title"] as? String ?: "",
            description = map["description"] as? String ?: "",
            type = try {
                AchievementType.valueOf(map["type"] as? String ?: "FIRST_TIME")
            } catch (e: Exception) { AchievementType.FIRST_TIME },
            targetValue = (map["targetValue"] as? Long)?.toInt() ?: 1,
            xpReward = (map["xpReward"] as? Long)?.toInt() ?: 0,
            iconName = map["iconName"] as? String ?: "trophy",
            isActive = map["isActive"] as? Boolean ?: true,
            exerciseId = map["exerciseId"] as? String,
            categoryId = map["categoryId"] as? String,
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
        )
    }
}

/**
 * Postęp użytkownika w osiągnięciu
 */
data class AchievementProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val achievementId: String = "",
    val currentValue: Int = 0,          // Obecna wartość (np. 7 z 10 treningów)
    val isCompleted: Boolean = false,
    val completedAt: Timestamp? = null,
    val lastUpdated: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "achievementId" to achievementId,
        "currentValue" to currentValue,
        "isCompleted" to isCompleted,
        "completedAt" to completedAt,
        "lastUpdated" to lastUpdated
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AchievementProgress = AchievementProgress(
            id = id,
            userId = map["userId"] as? String ?: "",
            achievementId = map["achievementId"] as? String ?: "",
            currentValue = (map["currentValue"] as? Long)?.toInt() ?: 0,
            isCompleted = map["isCompleted"] as? Boolean ?: false,
            completedAt = map["completedAt"] as? Timestamp,
            lastUpdated = map["lastUpdated"] as? Timestamp ?: Timestamp.now()
        )
    }

    /**
     * Oblicza procent ukończenia względem definicji osiągnięcia
     */
    fun getProgressPercentage(targetValue: Int): Float {
        return if (targetValue > 0) {
            (currentValue.toFloat() / targetValue.toFloat() * 100f).coerceAtMost(100f)
        } else {
            if (isCompleted) 100f else 0f
        }
    }
}

/**
 * Połączone dane osiągnięcia z postępem
 */
data class AchievementWithProgress(
    val definition: AchievementDefinition,
    val progress: AchievementProgress?
) {
    val isCompleted: Boolean
        get() = progress?.isCompleted == true

    val currentValue: Int
        get() = progress?.currentValue ?: 0

    val progressPercentage: Float
        get() = progress?.getProgressPercentage(definition.targetValue) ?: 0f

    val remainingToComplete: Int
        get() = (definition.targetValue - currentValue).coerceAtLeast(0)
}