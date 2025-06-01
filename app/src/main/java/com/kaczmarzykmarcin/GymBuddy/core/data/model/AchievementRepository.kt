package com.kaczmarzykmarcin.GymBuddy.data.repository

import android.util.Log
import com.kaczmarzykmarcin.GymBuddy.common.network.NetworkConnectivityManager
import com.kaczmarzykmarcin.GymBuddy.data.model.*
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.UserAchievementDao
import com.kaczmarzykmarcin.GymBuddy.features.user.data.mapper.UserMappers
import com.kaczmarzykmarcin.GymBuddy.features.user.data.remote.RemoteUserDataSource
import com.kaczmarzykmarcin.GymBuddy.features.user.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repozytorium do zarządzania osiągnięciami użytkowników.
 * Obsługuje nowy system osiągnięć z AchievementDefinition i AchievementProgress.
 */
@Singleton
class AchievementRepository @Inject constructor(
    private val userAchievementDao: UserAchievementDao,
    private val remoteDataSource: RemoteUserDataSource,
    private val syncManager: SyncManager,
    private val networkManager: NetworkConnectivityManager,
    private val mappers: UserMappers
) {
    private val TAG = "AchievementRepository"

    // ===== DEFINICJE OSIĄGNIĘĆ =====

    /**
     * Tworzy lub aktualizuje definicję osiągnięcia
     */
    suspend fun createOrUpdateAchievementDefinition(definition: AchievementDefinition): Result<AchievementDefinition> {
        return try {
            val definitionWithId = if (definition.id.isEmpty()) {
                definition.copy(id = UUID.randomUUID().toString())
            } else {
                definition
            }

            // Zapisz w Firebase (jeśli jest połączenie)
            if (networkManager.isInternetAvailable()) {
                remoteDataSource.saveAchievementDefinition(definitionWithId)
            }

            // TODO: Dodaj do lokalnej bazy danych jeśli potrzebne (opcjonalne)

            Result.success(definitionWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create/update achievement definition", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera wszystkie definicje osiągnięć
     */
    suspend fun getAllAchievementDefinitions(): Result<List<AchievementDefinition>> {
        return try {
            if (networkManager.isInternetAvailable()) {
                remoteDataSource.getAllAchievementDefinitions()
            } else {
                // Fallback - zwróć podstawowe osiągnięcia
                Result.success(getDefaultAchievementDefinitions())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get achievement definitions", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera definicję osiągnięcia po ID
     */
    suspend fun getAchievementDefinition(achievementId: String): Result<AchievementDefinition?> {
        return try {
            if (networkManager.isInternetAvailable()) {
                remoteDataSource.getAchievementDefinition(achievementId)
            } else {
                val defaultDefinitions = getDefaultAchievementDefinitions()
                val definition = defaultDefinitions.find { it.id == achievementId }
                Result.success(definition)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get achievement definition", e)
            Result.failure(e)
        }
    }

    // ===== POSTĘP W OSIĄGNIĘCIACH =====

    /**
     * Aktualizuje postęp użytkownika w osiągnięciu
     */
    suspend fun updateAchievementProgress(progress: AchievementProgress): Result<AchievementProgress> {
        return try {
            val progressWithId = if (progress.id.isEmpty()) {
                progress.copy(id = UUID.randomUUID().toString())
            } else {
                progress
            }

            // Zapisz w Firebase (jeśli jest połączenie)
            if (networkManager.isInternetAvailable()) {
                remoteDataSource.saveAchievementProgress(progressWithId)
            }

            // TODO: Dodaj do lokalnej bazy danych jeśli potrzebne (opcjonalne)

            Result.success(progressWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update achievement progress", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera postęp użytkownika w konkretnym osiągnięciu
     */
    suspend fun getAchievementProgress(userId: String, achievementId: String): Result<AchievementProgress?> {
        return try {
            if (networkManager.isInternetAvailable()) {
                remoteDataSource.getAchievementProgress(userId, achievementId)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get achievement progress", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera wszystkie postępy użytkownika w osiągnięciach
     */
    suspend fun getUserAchievementProgresses(userId: String): Result<List<AchievementProgress>> {
        return try {
            if (networkManager.isInternetAvailable()) {
                remoteDataSource.getUserAchievementProgresses(userId)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user achievement progresses", e)
            Result.failure(e)
        }
    }

    // ===== POŁĄCZONE DANE OSIĄGNIĘĆ =====

    /**
     * Pobiera wszystkie osiągnięcia użytkownika (definicje + postęp)
     */
    suspend fun getUserAchievements(userId: String): Result<List<AchievementWithProgress>> {
        return try {
            val definitions = getAllAchievementDefinitions().getOrNull() ?: emptyList()
            val progresses = getUserAchievementProgresses(userId).getOrNull() ?: emptyList()

            val achievements = definitions.map { definition ->
                val progress = progresses.find { it.achievementId == definition.id }
                AchievementWithProgress(definition, progress)
            }

            Result.success(achievements)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user achievements", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera ostatnio zdobyte osiągnięcia użytkownika
     */
    suspend fun getRecentlyCompletedAchievements(userId: String, limit: Int = 10): Result<List<AchievementWithProgress>> {
        return try {
            val definitions = getAllAchievementDefinitions().getOrNull() ?: emptyList()
            val progresses = getUserAchievementProgresses(userId).getOrNull() ?: emptyList()

            val recentlyCompleted = progresses
                .filter { it.isCompleted && it.completedAt != null }
                .sortedByDescending { it.completedAt?.seconds ?: 0 }
                .take(limit)
                .mapNotNull { progress ->
                    val definition = definitions.find { it.id == progress.achievementId }
                    if (definition != null) {
                        AchievementWithProgress(definition, progress)
                    } else null
                }

            Result.success(recentlyCompleted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recently completed achievements", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera osiągnięcia w trakcie realizacji
     */
    suspend fun getInProgressAchievements(userId: String): Result<List<AchievementWithProgress>> {
        return try {
            val definitions = getAllAchievementDefinitions().getOrNull() ?: emptyList()
            val progresses = getUserAchievementProgresses(userId).getOrNull() ?: emptyList()

            val inProgress = definitions.mapNotNull { definition ->
                val progress = progresses.find { it.achievementId == definition.id }

                when {
                    // Jeśli nie ma postępu, ale osiągnięcie nie jest typu FIRST_TIME, pokaż
                    progress == null && definition.type != AchievementType.FIRST_TIME -> {
                        AchievementWithProgress(definition, null)
                    }
                    // Jeśli jest postęp ale nie ukończone
                    progress != null && !progress.isCompleted -> {
                        AchievementWithProgress(definition, progress)
                    }
                    else -> null
                }
            }.filter { it.currentValue < it.definition.targetValue }

            Result.success(inProgress)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get in progress achievements", e)
            Result.failure(e)
        }
    }

    /**
     * Obserwuje zmiany w osiągnięciach użytkownika jako Flow
     */
    fun observeUserAchievements(userId: String): Flow<List<AchievementWithProgress>> = flow {
        try {
            // Emit initial data
            val achievements = getUserAchievements(userId).getOrNull() ?: emptyList()
            emit(achievements)

            // TODO: Implement real-time updates if needed
            // For now, this is a simple implementation that emits once
        } catch (e: Exception) {
            Log.e(TAG, "Error observing user achievements", e)
            emit(emptyList())
        }
    }

    /**
     * Sprawdza czy użytkownik ma konkretne osiągnięcie ukończone
     */
    suspend fun hasCompletedAchievement(userId: String, achievementId: String): Boolean {
        return try {
            val progress = getAchievementProgress(userId, achievementId).getOrNull()
            progress?.isCompleted == true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking completed achievement", e)
            false
        }
    }

    /**
     * Usuwa postęp w osiągnięciu (przydatne do testów lub resetowania)
     */
    suspend fun resetAchievementProgress(userId: String, achievementId: String): Result<Unit> {
        return try {
            if (networkManager.isInternetAvailable()) {
                remoteDataSource.deleteAchievementProgress(userId, achievementId)
            } else {
                Result.failure(Exception("No network connection"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset achievement progress", e)
            Result.failure(e)
        }
    }

    /**
     * Zwraca domyślne definicje osiągnięć jako fallback
     */
    private fun getDefaultAchievementDefinitions(): List<AchievementDefinition> {
        return listOf(
            AchievementDefinition(
                id = "first_workout",
                title = "Pierwszy trening",
                description = "Wykonaj swój pierwszy trening",
                type = AchievementType.WORKOUT_COUNT,
                targetValue = 1,
                xpReward = 50,
                iconName = "🏃"
            ),
            AchievementDefinition(
                id = "morning_bird",
                title = "Poranny ptaszek",
                description = "Wykonaj 10 porannych treningów (przed 10:00)",
                type = AchievementType.MORNING_WORKOUTS,
                targetValue = 10,
                xpReward = 100,
                iconName = "🌅"
            ),
            AchievementDefinition(
                id = "workout_streak_3",
                title = "Mini seria",
                description = "Trenuj przez 3 dni z rzędu",
                type = AchievementType.WORKOUT_STREAK,
                targetValue = 3,
                xpReward = 100,
                iconName = "🔥"
            ),
            AchievementDefinition(
                id = "workout_streak_7",
                title = "Tygodniowa seria",
                description = "Trenuj przez 7 dni z rzędu",
                type = AchievementType.WORKOUT_STREAK,
                targetValue = 7,
                xpReward = 250,
                iconName = "💪"
            ),
            AchievementDefinition(
                id = "workout_count_10",
                title = "Regularny bywalec",
                description = "Ukończ 10 treningów",
                type = AchievementType.WORKOUT_COUNT,
                targetValue = 10,
                xpReward = 200,
                iconName = "⭐"
            ),
            AchievementDefinition(
                id = "workout_count_25",
                title = "Zaawansowany",
                description = "Ukończ 25 treningów",
                type = AchievementType.WORKOUT_COUNT,
                targetValue = 25,
                xpReward = 500,
                iconName = "🏆"
            ),
            AchievementDefinition(
                id = "workout_count_50",
                title = "Ekspert",
                description = "Ukończ 50 treningów",
                type = AchievementType.WORKOUT_COUNT,
                targetValue = 50,
                xpReward = 1000,
                iconName = "👑"
            ),
            AchievementDefinition(
                id = "workout_hour",
                title = "Godzinna sesja",
                description = "Zakończ trening trwający ponad godzinę",
                type = AchievementType.WORKOUT_DURATION,
                targetValue = 3600, // 1 godzina w sekundach
                xpReward = 150,
                iconName = "⏱️"
            ),
            AchievementDefinition(
                id = "workout_2_hours",
                title = "Maraton treningowy",
                description = "Zakończ trening trwający ponad 2 godziny",
                type = AchievementType.WORKOUT_DURATION,
                targetValue = 7200, // 2 godziny w sekundach
                xpReward = 300,
                iconName = "🏃‍♂️"
            ),
            AchievementDefinition(
                id = "bench_press_100kg",
                title = "Setka na ławce",
                description = "Wykonaj wyciskanie sztangi leżąc z obciążeniem 100kg",
                type = AchievementType.EXERCISE_WEIGHT,
                targetValue = 100,
                xpReward = 500,
                iconName = "💪",
                exerciseId = "bench-press" // ID ćwiczenia z bazy
            )
        )
    }
}