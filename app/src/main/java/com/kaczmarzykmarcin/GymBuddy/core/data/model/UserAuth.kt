package com.kaczmarzykmarcin.GymBuddy.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Klasa reprezentująca dane uwierzytelniania użytkownika.
 * Zawiera podstawowe informacje potrzebne do uwierzytelnienia.
 */
data class UserAuth(
    @DocumentId val id: String = "",
    val email: String = "",
    val provider: AuthProvider = AuthProvider.EMAIL,
    val createdAt: Timestamp = Timestamp.now(),
    val lastLogin: Timestamp = Timestamp.now(),
    val language: String = "pl"
) {
    // Przydatne metody konwersji do/z Map dla Firestore
    fun toMap(): Map<String, Any?> = mapOf(
        "email" to email,
        "provider" to provider.name,
        "createdAt" to createdAt,
        "lastLogin" to lastLogin,
        "language" to language
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): UserAuth = UserAuth(
            id = id,
            email = map["email"] as? String ?: "",
            provider = try {
                AuthProvider.valueOf(map["provider"] as? String ?: AuthProvider.EMAIL.name)
            } catch (e: Exception) {
                AuthProvider.EMAIL
            },
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
            lastLogin = map["lastLogin"] as? Timestamp ?: Timestamp.now(),
            language = map["language"] as? String ?: "pl"
        )
    }

    /**
     * Aktualizuje czas ostatniego logowania.
     */
    fun updateLastLogin(): UserAuth {
        return this.copy(lastLogin = Timestamp.now())
    }
}

/**
 * Enum reprezentujący dostawcę uwierzytelniania.
 */
enum class AuthProvider {
    EMAIL,
    GOOGLE,
    FACEBOOK
}