package com.kaczmarzykmarcin.GymBuddy.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Klasa reprezentująca osiągnięcie zdobyte przez użytkownika (stary system).
 * DEPRECATED: Używaj AchievementDefinition i AchievementProgress zamiast tego.
 */
@Deprecated("Use AchievementDefinition and AchievementProgress instead")
data class UserAchievement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val achievementId: Int = 0,
    val earnedAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "achievementId" to achievementId,
        "earnedAt" to earnedAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): UserAchievement = UserAchievement(
            id = id,
            userId = map["userId"] as? String ?: "",
            achievementId = (map["achievementId"] as? Long)?.toInt() ?: 0,
            earnedAt = map["earnedAt"] as? Timestamp ?: Timestamp.now()
        )
    }
}