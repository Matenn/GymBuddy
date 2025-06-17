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
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model.DefaultAchievements
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

            // Pobierz domyślne definicje do porównania
            val defaultDefinitions = getDefaultAchievementDefinitions()

            // 1. ZNAJDŹ BRAKUJĄCE DEFINICJE
            val missingDefinitions = defaultDefinitions.filter { defaultDef ->
                localDefinitions.none { localDef -> localDef.id == defaultDef.id }
            }

            // 2. ZNAJDŹ PRZESTARZAŁE DEFINICJE (które są w bazie, ale nie w kodzie)
            val obsoleteDefinitions = localDefinitions.filter { localDef ->
                defaultDefinitions.none { defaultDef -> defaultDef.id == localDef.id }
            }

            // 3. ZNAJDŹ DEFINICJE DO AKTUALIZACJI
            val definitionsToUpdate = defaultDefinitions.filter { defaultDef ->
                val localDef = localDefinitions.find { it.id == defaultDef.id }
                localDef != null && needsUpdate(localDef, defaultDef)
            }

            var hasChanges = false

            // USUŃ PRZESTARZAŁE DEFINICJE
            if (obsoleteDefinitions.isNotEmpty()) {
                Log.d(TAG, "🗑️ Found ${obsoleteDefinitions.size} obsolete achievement definitions, removing them:")

                obsoleteDefinitions.forEach { definition ->
                    try {
                        // Usuń całkowicie z lokalnej bazy
                        achievementDefinitionDao.deleteAchievementDefinition(definition.id)
                        Log.d(TAG, "🗑️ Removed obsolete achievement: ${definition.title} (${definition.id})")
                        hasChanges = true

                        // Opcjonalnie: usuń z Firebase (jeśli taka metoda istnieje)
                        if (networkManager.isInternetAvailable()) {
                            try {
                                // remoteDataSource.deleteAchievementDefinition(definition.id)
                                Log.d(TAG, "🔄 Marked ${definition.title} for Firebase cleanup")
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to remove ${definition.title} from Firebase", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to remove achievement: ${definition.title}", e)
                    }
                }
            }

            // DODAJ BRAKUJĄCE DEFINICJE
            if (missingDefinitions.isNotEmpty()) {
                Log.d(TAG, "✅ Found ${missingDefinitions.size} missing achievement definitions, adding them:")

                missingDefinitions.forEach { definition ->
                    try {
                        achievementDefinitionDao.insertAchievementDefinition(mappers.toEntity(definition))
                        Log.d(TAG, "✅ Added missing achievement: ${definition.title} (${definition.id})")
                        hasChanges = true

                        // Synchronizuj z Firebase
                        if (networkManager.isInternetAvailable()) {
                            try {
                                remoteDataSource.saveAchievementDefinition(definition)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to sync ${definition.title} to Firebase", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to add achievement: ${definition.title}", e)
                    }
                }
            }

            // AKTUALIZUJ ZMIENIONE DEFINICJE
            if (definitionsToUpdate.isNotEmpty()) {
                Log.d(TAG, "🔄 Found ${definitionsToUpdate.size} achievement definitions to update:")

                definitionsToUpdate.forEach { definition ->
                    try {
                        val localDef = localDefinitions.find { it.id == definition.id }

                        // Pokaż co się zmieniło
                        logChanges(localDef!!, definition)

                        achievementDefinitionDao.updateAchievementDefinition(mappers.toEntity(definition))
                        Log.d(TAG, "🔄 Updated achievement: ${definition.title}")
                        hasChanges = true

                        // Synchronizuj z Firebase
                        if (networkManager.isInternetAvailable()) {
                            try {
                                remoteDataSource.saveAchievementDefinition(definition)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to sync updated ${definition.title} to Firebase", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to update achievement: ${definition.title}", e)
                    }
                }
            }

            // POBIERZ ZAKTUALIZOWANĄ LISTĘ jeśli były zmiany
            if (hasChanges) {
                val updatedLocalDefinitions = achievementDefinitionDao.getAllActiveDefinitions()
                    .map { mappers.toModel(it) }

                Log.d(TAG, "🎯 Successfully synchronized achievement definitions. Total count: ${updatedLocalDefinitions.size}")
                Log.d(TAG, "📋 Current achievements:")
                updatedLocalDefinitions.forEach { def ->
                    Log.d(TAG, "  ✅ ${def.title} (${def.id})")
                }

                return Result.success(updatedLocalDefinitions)
            }

            // BEZ ZMIAN - zwróć lokalne (ale tylko te które są w defaultDefinitions)
            val validLocalDefinitions = localDefinitions.filter { localDef ->
                defaultDefinitions.any { defaultDef -> defaultDef.id == localDef.id }
            }

            if (validLocalDefinitions.isNotEmpty()) {
                Log.d(TAG, "ℹ️ All ${validLocalDefinitions.size} achievement definitions are up to date")
                return Result.success(validLocalDefinitions)
            }

            // FALLBACK - pierwsze uruchomienie (pusta baza)
            Log.d(TAG, "🚀 No local definitions found, initializing ${defaultDefinitions.size} default achievements")

            defaultDefinitions.forEach { definition ->
                try {
                    achievementDefinitionDao.insertAchievementDefinition(mappers.toEntity(definition))

                    if (networkManager.isInternetAvailable()) {
                        try {
                            remoteDataSource.saveAchievementDefinition(definition)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to sync ${definition.title} to Firebase", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize achievement: ${definition.title}", e)
                }
            }

            Log.d(TAG, "✅ Successfully initialized all default achievement definitions")
            Result.success(defaultDefinitions)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get achievement definitions", e)
            Result.failure(e)
        }
    }

    /**
     * Sprawdza czy definicja potrzebuje aktualizacji
     */
    private fun needsUpdate(local: AchievementDefinition, default: AchievementDefinition): Boolean {
        return local.title != default.title ||
                local.description != default.description ||
                local.type != default.type ||
                local.targetValue != default.targetValue ||
                local.xpReward != default.xpReward ||
                local.iconName != default.iconName ||
                local.exerciseId != default.exerciseId ||
                local.categoryId != default.categoryId ||
                local.isActive != default.isActive
    }

    /**
     * Loguje jakie zmiany zostały wykryte
     */
    private fun logChanges(local: AchievementDefinition, new: AchievementDefinition) {
        val changes = mutableListOf<String>()

        if (local.title != new.title) changes.add("title: '${local.title}' → '${new.title}'")
        if (local.description != new.description) changes.add("description: '${local.description}' → '${new.description}'")
        if (local.targetValue != new.targetValue) changes.add("targetValue: ${local.targetValue} → ${new.targetValue}")
        if (local.xpReward != new.xpReward) changes.add("xpReward: ${local.xpReward} → ${new.xpReward}")
        if (local.exerciseId != new.exerciseId) changes.add("exerciseId: '${local.exerciseId}' → '${new.exerciseId}'")
        if (local.iconName != new.iconName) changes.add("iconName: '${local.iconName}' → '${new.iconName}'")
        if (local.categoryId != new.categoryId) changes.add("categoryId: '${local.categoryId}' → '${new.categoryId}'")
        if (local.isActive != new.isActive) changes.add("isActive: ${local.isActive} → ${new.isActive}")

        if (changes.isNotEmpty()) {
            Log.d(TAG, "  📝 Changes in '${new.title}':")
            changes.forEach { change ->
                Log.d(TAG, "    - $change")
            }
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
        return DefaultAchievements.ALL
    }
}