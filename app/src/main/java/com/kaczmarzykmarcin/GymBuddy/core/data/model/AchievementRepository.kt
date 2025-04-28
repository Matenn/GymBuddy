package com.kaczmarzykmarcin.GymBuddy.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kaczmarzykmarcin.GymBuddy.data.model.Achievement
import com.kaczmarzykmarcin.GymBuddy.data.model.UserAchievement
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repozytorium do zarządzania osiągnięciami użytkowników.
 */
@Singleton
class AchievementRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val userAchievementsCollection = firestore.collection("userAchievements")

    /**
     * Dodaje nowe osiągnięcie dla użytkownika.
     */
    suspend fun addUserAchievement(userAchievement: UserAchievement): Result<UserAchievement> {
        return try {
            val docRef = userAchievementsCollection.document()
            val achievementWithId = userAchievement.copy(id = docRef.id)
            docRef.set(achievementWithId.toMap()).await()
            Result.success(achievementWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dodaje osiągnięcie po ID dla użytkownika.
     */
    suspend fun addAchievementForUser(userId: String, achievementId: Int): Result<UserAchievement> {
        val achievement = Achievement.getById(achievementId) ?: return Result.failure(
            IllegalArgumentException("Achievement with ID $achievementId does not exist")
        )

        val userAchievement = UserAchievement(
            userId = userId,
            achievementId = achievementId
        )

        return addUserAchievement(userAchievement)
    }

    /**
     * Pobiera wszystkie osiągnięcia danego użytkownika.
     */
    suspend fun getUserAchievements(userId: String): Result<List<UserAchievement>> {
        return try {
            val snapshot = userAchievementsCollection
                .whereEqualTo("userId", userId)
                .orderBy("earnedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val achievements = snapshot.documents.map { doc ->
                UserAchievement.fromMap(doc.id, doc.data ?: mapOf())
            }

            Result.success(achievements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sprawdza, czy użytkownik posiada dane osiągnięcie.
     */
    suspend fun hasAchievement(userId: String, achievementId: Int): Boolean {
        return try {
            val snapshot = userAchievementsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("achievementId", achievementId)
                .limit(1)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Obserwuje zmiany w osiągnięciach użytkownika jako Flow.
     */
    fun observeUserAchievements(userId: String): Flow<List<UserAchievement>> = callbackFlow {
        val listenerRegistration = userAchievementsCollection
            .whereEqualTo("userId", userId)
            .orderBy("earnedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val achievements = snapshot.documents.map { doc ->
                    UserAchievement.fromMap(doc.id, doc.data ?: mapOf())
                }

                // Emituj wynik
                trySend(achievements)
            }

        // Zamknij listener po zakończeniu flow
        awaitClose {
            listenerRegistration.remove()
        }
    }
}