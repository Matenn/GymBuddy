package com.kaczmarzykmarcin.GymBuddy.data.model

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