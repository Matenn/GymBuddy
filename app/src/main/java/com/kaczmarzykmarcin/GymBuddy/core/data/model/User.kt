package com.kaczmarzykmarcin.GymBuddy.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Główna klasa User, która służy jako centralna referencja do wszystkich informacji
 * związanych z użytkownikiem. Przechowuje tylko ID oraz odniesienia do powiązanych dokumentów.
 */
data class User(
    @DocumentId val id: String = "",
    val authId: String = "",
    val profileId: String = "",
    val statsId: String = ""
) {
    // Przydatne metody konwersji do/z Map dla Firestore
    fun toMap(): Map<String, Any?> = mapOf(
        "authId" to authId,
        "profileId" to profileId,
        "statsId" to statsId
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): User = User(
            id = id,
            authId = map["authId"] as? String ?: "",
            profileId = map["profileId"] as? String ?: "",
            statsId = map["statsId"] as? String ?: ""
        )
    }
}