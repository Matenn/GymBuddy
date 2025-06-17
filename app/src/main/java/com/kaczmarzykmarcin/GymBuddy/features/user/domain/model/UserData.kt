package com.kaczmarzykmarcin.GymBuddy.features.user.domain.model

import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model.UserAchievement
import com.kaczmarzykmarcin.GymBuddy.features.profile.domain.model.UserProfile
import com.kaczmarzykmarcin.GymBuddy.features.auth.domain.model.UserAuth

/**
 * Klasa zbierająca wszystkie dane użytkownika w jednym miejscu.
 * Służy jako wygodna struktura do przekazywania pełnych danych użytkownika w aplikacji.
 */
data class UserData(
    val user: User,
    val auth: UserAuth,
    val profile: UserProfile,
    val stats: UserStats,
    val achievements: List<UserAchievement> = emptyList()
)