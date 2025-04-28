package com.kaczmarzykmarcin.GymBuddy.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Klasa reprezentująca profil użytkownika z podstawowymi informacjami.
 */
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val favoriteBodyPart: String = "" // ulubiona partia np. "Klatka" (widoczne na profilu)
) {
    // Przydatne metody konwersji do/z Map dla Firestore
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "photoUrl" to photoUrl,
        "favoriteBodyPart" to favoriteBodyPart
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): UserProfile = UserProfile(
            id = id,
            userId = map["userId"] as? String ?: "",
            displayName = map["displayName"] as? String ?: "",
            photoUrl = map["photoUrl"] as? String,
            favoriteBodyPart = map["favoriteBodyPart"] as? String ?: ""
        )
    }
}