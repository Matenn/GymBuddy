package com.kaczmarzykmarcin.GymBuddy.features.achievements.data.repository

import android.util.Log
import com.kaczmarzykmarcin.GymBuddy.core.network.NetworkConnectivityManager
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model.AchievementDefinition
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model.AchievementProgress
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model.AchievementType
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model.AchievementWithProgress
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserAchievementDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.AchievementDefinitionDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.AchievementProgressDao
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
 * Implementuje pattern local-first z synchronizacją Firebase.
 */
@Singleton
class AchievementRepository @Inject constructor(
    private val achievementDefinitionDao: AchievementDefinitionDao,
    private val achievementProgressDao: AchievementProgressDao,
    private val userAchievementDao: UserAchievementDao, // Zachowane dla kompatybilności
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

            // Zapisz lokalnie
            val entity = mappers.toEntity(definitionWithId)
            achievementDefinitionDao.insertAchievementDefinition(entity)

            // Synchronizuj z Firebase w tle
            if (networkManager.isInternetAvailable()) {
                try {
                    remoteDataSource.saveAchievementDefinition(definitionWithId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync definition to Firebase immediately", e)
                    // Synchronizacja zostanie ponowiona przez SyncManager
                }
            }

            Result.success(definitionWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create/update achievement definition", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera wszystkie definicje osiągnięć z lokalnej bazy
     */
    suspend fun getAllAchievementDefinitions(): Result<List<AchievementDefinition>> {
        return try {
            // Pobierz z lokalnej bazy
            val localDefinitions = achievementDefinitionDao.getAllActiveDefinitions()
                .map { mappers.toModel(it) }

            // Jeśli puste i jest internet, pobierz z Firebase
            if (localDefinitions.isEmpty() && networkManager.isInternetAvailable()) {
                Log.d(TAG, "No local definitions found, syncing from Firebase")
                syncDefinitionsFromFirebase()

                // Pobierz ponownie z lokalnej bazy
                val updatedDefinitions = achievementDefinitionDao.getAllActiveDefinitions()
                    .map { mappers.toModel(it) }
                return Result.success(updatedDefinitions)
            }

            // Jeśli lokalnie mamy dane, ale nie ma internetu, zwróć lokalne
            if (localDefinitions.isNotEmpty()) {
                return Result.success(localDefinitions)
            }

            // Fallback - zwróć podstawowe osiągnięcia
            val defaultDefinitions = getDefaultAchievementDefinitions()
            // Zapisz domyślne definicje lokalnie
            defaultDefinitions.forEach { definition ->
                achievementDefinitionDao.insertAchievementDefinition(mappers.toEntity(definition))
            }

            Result.success(defaultDefinitions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get achievement definitions", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera definicję osiągnięcia po ID z lokalnej bazy
     */
    suspend fun getAchievementDefinition(achievementId: String): Result<AchievementDefinition?> {
        return try {
            // Próba pobrania z lokalnej bazy
            val localDefinition = achievementDefinitionDao.getAchievementDefinitionById(achievementId)

            if (localDefinition != null) {
                return Result.success(mappers.toModel(localDefinition))
            }

            // Jeśli nie ma lokalnie i jest internet, pobierz z Firebase
            if (networkManager.isInternetAvailable()) {
                Log.d(TAG, "Definition not found locally, trying Firebase")
                val remoteResult = remoteDataSource.getAchievementDefinition(achievementId)

                if (remoteResult.isSuccess) {
                    val definition = remoteResult.getOrNull()
                    if (definition != null) {
                        // Zapisz lokalnie
                        achievementDefinitionDao.insertAchievementDefinition(mappers.toEntity(definition))
                        return Result.success(definition)
                    }
                }
            }

            // Fallback - sprawdź w domyślnych definicjach
            val defaultDefinitions = getDefaultAchievementDefinitions()
            val definition = defaultDefinitions.find { it.id == achievementId }
            Result.success(definition)
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

            // Zapisz lokalnie
            val entity = mappers.toEntity(progressWithId)
            achievementProgressDao.insertAchievementProgress(entity)

            // Synchronizuj z Firebase w tle
            if (networkManager.isInternetAvailable()) {
                try {
                    remoteDataSource.saveAchievementProgress(progressWithId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync progress to Firebase immediately", e)
                    // Synchronizacja zostanie ponowiona przez SyncManager
                }
            }

            Result.success(progressWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update achievement progress", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera postęp użytkownika w konkretnym osiągnięciu z lokalnej bazy
     */
    suspend fun getAchievementProgress(userId: String, achievementId: String): Result<AchievementProgress?> {
        return try {
            // Pobierz z lokalnej bazy
            val localProgress = achievementProgressDao.getUserProgressForAchievement(userId, achievementId)

            if (localProgress != null) {
                return Result.success(mappers.toModel(localProgress))
            }

            // Jeśli nie ma lokalnie i jest internet, pobierz z Firebase
            if (networkManager.isInternetAvailable()) {
                Log.d(TAG, "Progress not found locally, trying Firebase")
                val remoteResult = remoteDataSource.getAchievementProgress(userId, achievementId)

                if (remoteResult.isSuccess) {
                    val progress = remoteResult.getOrNull()
                    if (progress != null) {
                        // Zapisz lokalnie
                        achievementProgressDao.insertAchievementProgress(mappers.toEntity(progress))
                        return Result.success(progress)
                    }
                }
            }

            Result.success(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get achievement progress", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera wszystkie postępy użytkownika w osiągnięciach z lokalnej bazy
     */
    suspend fun getUserAchievementProgresses(userId: String): Result<List<AchievementProgress>> {
        return try {
            // Pobierz z lokalnej bazy
            val localProgresses = achievementProgressDao.getUserProgresses(userId)
                .map { mappers.toModel(it) }

            // Jeśli puste i jest internet, zsynchronizuj z Firebase
            if (localProgresses.isEmpty() && networkManager.isInternetAvailable()) {
                Log.d(TAG, "No local progresses found, syncing from Firebase")
                syncProgressesFromFirebase(userId)

                // Pobierz ponownie z lokalnej bazy
                val updatedProgresses = achievementProgressDao.getUserProgresses(userId)
                    .map { mappers.toModel(it) }
                return Result.success(updatedProgresses)
            }

            Result.success(localProgresses)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user achievement progresses", e)
            Result.failure(e)
        }
    }

    // ===== POŁĄCZONE DANE OSIĄGNIĘĆ =====

    /**
     * Pobiera wszystkie osiągnięcia użytkownika (definicje + postęp) z lokalnej bazy
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
     * Pobiera ostatnio zdobyte osiągnięcia użytkownika z lokalnej bazy
     */
    suspend fun getRecentlyCompletedAchievements(userId: String, limit: Int = 10): Result<List<AchievementWithProgress>> {
        return try {
            val recentProgresses = achievementProgressDao.getRecentCompletedProgresses(userId, limit)
                .map { mappers.toModel(it) }

            val definitions = getAllAchievementDefinitions().getOrNull() ?: emptyList()

            val recentlyCompleted = recentProgresses.mapNotNull { progress ->
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
     * Pobiera osiągnięcia w trakcie realizacji z lokalnej bazy
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
     * Obserwuje zmiany w osiągnięciach użytkownika jako Flow z lokalnej bazy
     */
    fun observeUserAchievements(userId: String): Flow<List<AchievementWithProgress>> = flow {
        try {
            // Emit initial data
            val achievements = getUserAchievements(userId).getOrNull() ?: emptyList()
            emit(achievements)

            // Obserwuj zmiany w postępach
            achievementProgressDao.observeUserProgresses(userId).collect { progressEntities ->
                val progresses = progressEntities.map { mappers.toModel(it) }
                val definitions = getAllAchievementDefinitions().getOrNull() ?: emptyList()

                val updatedAchievements = definitions.map { definition ->
                    val progress = progresses.find { it.achievementId == definition.id }
                    AchievementWithProgress(definition, progress)
                }

                emit(updatedAchievements)
            }
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
     * Usuwa postęp w osiągnięciu z lokalnej bazy i Firebase
     */
    suspend fun resetAchievementProgress(userId: String, achievementId: String): Result<Unit> {
        return try {
            // Usuń z lokalnej bazy
            achievementProgressDao.deleteUserProgressForAchievement(userId, achievementId)

            // Usuń z Firebase jeśli jest połączenie
            if (networkManager.isInternetAvailable()) {
                remoteDataSource.deleteAchievementProgress(userId, achievementId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset achievement progress", e)
            Result.failure(e)
        }
    }

    // ===== SYNCHRONIZACJA =====

    /**
     * Synchronizuje definicje osiągnięć z Firebase do lokalnej bazy
     */
    private suspend fun syncDefinitionsFromFirebase(): Result<Unit> {
        return try {
            if (!networkManager.isInternetAvailable()) {
                return Result.failure(Exception("No network connection"))
            }

            val remoteResult = remoteDataSource.getAllAchievementDefinitions()

            if (remoteResult.isSuccess) {
                val definitions = remoteResult.getOrNull() ?: emptyList()

                // Zapisz wszystkie definicje lokalnie
                definitions.forEach { definition ->
                    achievementDefinitionDao.insertAchievementDefinition(mappers.toEntity(definition))
                }

                Log.d(TAG, "Synced ${definitions.size} achievement definitions from Firebase")
                Result.success(Unit)
            } else {
                remoteResult.exceptionOrNull()?.let { Result.failure(it) } ?: Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync definitions from Firebase", e)
            Result.failure(e)
        }
    }

    /**
     * Synchronizuje postępy osiągnięć z Firebase do lokalnej bazy
     */
    private suspend fun syncProgressesFromFirebase(userId: String): Result<Unit> {
        return try {
            if (!networkManager.isInternetAvailable()) {
                return Result.failure(Exception("No network connection"))
            }

            val remoteResult = remoteDataSource.getUserAchievementProgresses(userId)

            if (remoteResult.isSuccess) {
                val progresses = remoteResult.getOrNull() ?: emptyList()

                // Zapisz wszystkie postępy lokalnie
                progresses.forEach { progress ->
                    achievementProgressDao.insertAchievementProgress(mappers.toEntity(progress))
                }

                Log.d(TAG, "Synced ${progresses.size} achievement progresses from Firebase")
                Result.success(Unit)
            } else {
                remoteResult.exceptionOrNull()?.let { Result.failure(it) } ?: Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync progresses from Firebase", e)
            Result.failure(e)
        }
    }

    /**
     * Wymusza pełną synchronizację osiągnięć użytkownika
     */
    suspend fun syncUserAchievements(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Starting full achievement sync for user: $userId")

            // Synchronizuj definicje
            syncDefinitionsFromFirebase()

            // Synchronizuj postępy
            syncProgressesFromFirebase(userId)

            Log.d(TAG, "Full achievement sync completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user achievements", e)
            Result.failure(e)
        }
    }

    /**
     * Czyści wszystkie lokalne dane postępów osiągnięć z bazy danych.
     * UWAGA: Definicje osiągnięć pozostają, bo są globalne.
     */
    suspend fun clearLocalProgressData(): Result<Unit> {
        return try {
            Log.d(TAG, "Clearing all local achievement progress data")

            // Wyczyść postępy osiągnięć
            achievementProgressDao.clearAllAchievementProgresses()

            // Wyczyść stare osiągnięcia (dla kompatybilności)
            userAchievementDao.clearAllUserAchievements()

            // UWAGA: NIE czyścimy achievement_definitions, bo są globalne

            Log.d(TAG, "Successfully cleared all local achievement progress data")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing local achievement progress data", e)
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