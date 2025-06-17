// WorkoutCategoryRepository.kt
package com.kaczmarzykmarcin.GymBuddy.features.workouts.data.repository

import android.util.Log
import com.kaczmarzykmarcin.GymBuddy.core.network.NetworkConnectivityManager
import com.kaczmarzykmarcin.GymBuddy.features.workouts.domain.model.WorkoutCategory
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.WorkoutCategoryDao
import com.kaczmarzykmarcin.GymBuddy.features.user.data.mapper.UserMappers
import com.kaczmarzykmarcin.GymBuddy.features.user.data.remote.RemoteUserDataSource
import com.kaczmarzykmarcin.GymBuddy.features.user.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutCategoryRepository @Inject constructor(
    private val workoutCategoryDao: WorkoutCategoryDao,
    private val remoteDataSource: RemoteUserDataSource,
    private val syncManager: SyncManager,
    private val networkManager: NetworkConnectivityManager,
    private val mappers: UserMappers
) {
    private val TAG = "WorkoutCategoryRepository"

    // Inicjalizacja - sprawdza czy istnieją domyślne kategorie, jeśli nie - tworzy je
    suspend fun initializeDefaultCategories(userId: String) {
        try {
            // Sprawdź czy istnieją już domyślne kategorie
            val categories = workoutCategoryDao.getWorkoutCategoriesByUserId(userId).first()

            if (categories.none { it.isDefault }) {
                // Jeśli nie ma domyślnych kategorii, dodaj je
                val defaultEntities = WorkoutCategory.PREDEFINED_CATEGORIES.map { category ->
                    mappers.toEntity(category.copy(userId = userId), needsSync = true)
                }
                workoutCategoryDao.insertWorkoutCategories(defaultEntities)

                // Uruchom synchronizację w tle
                syncManager.requestSync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize default categories", e)
        }
    }

    /**
     * Czyści wszystkie lokalne kategorie treningowe użytkownika z bazy danych.
     * Zachowuje kategorie domyślne.
     */
    suspend fun clearLocalData(): Result<Unit> {
        return try {
            Log.d(TAG, "Clearing all local user workout categories")

            // Wyczyść tylko kategorie użytkownika (zachowaj domyślne)
            workoutCategoryDao.clearAllUserWorkoutCategories()

            Log.d(TAG, "Successfully cleared all local user workout categories")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing local workout categories", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera wszystkie kategorie treningowe użytkownika jako Flow z lokalnej bazy danych.
     */
    fun getUserWorkoutCategories(userId: String): Flow<List<WorkoutCategory>> {
        return workoutCategoryDao.getWorkoutCategoriesByUserId(userId)
            .map { entities -> entities.map { mappers.toModel(it) } }
    }

    /**
     * Pobiera kategorię treningową po ID.
     */
    suspend fun getWorkoutCategory(categoryId: String): Result<WorkoutCategory> {
        return try {
            // Próba pobrania z lokalnej bazy danych
            val categoryEntity = workoutCategoryDao.getWorkoutCategoryById(categoryId)

            if (categoryEntity != null) {
                Log.d(TAG, "Retrieved workout category from local database")
                return Result.success(mappers.toModel(categoryEntity))
            }

            // Jeśli nie ma danych lokalnie i jest połączenie, spróbuj pobrać z serwera
            if (networkManager.isInternetAvailable()) {
                val remoteResult = remoteDataSource.getWorkoutCategory(categoryId)
                if (remoteResult.isSuccess) {
                    val remoteCategory = remoteResult.getOrNull()!!
                    // Zapisz lokalnie
                    val entity = mappers.toEntity(remoteCategory, needsSync = false)
                    workoutCategoryDao.insertWorkoutCategory(entity)
                    return Result.success(remoteCategory)
                }
            }

            Result.failure(Exception("Category not found: $categoryId"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get workout category", e)
            Result.failure(e)
        }
    }

    /**
     * Tworzy nową kategorię treningową.
     */
    suspend fun createWorkoutCategory(category: WorkoutCategory): Result<WorkoutCategory> {
        return try {
            // Generuj nowe ID jeśli nie zostało dostarczone
            val categoryWithId = if (category.id.isEmpty()) {
                category.copy(id = UUID.randomUUID().toString())
            } else {
                category
            }

            // Zapisz lokalnie z flagą needsSync = true
            val entity = mappers.toEntity(categoryWithId, needsSync = true)
            workoutCategoryDao.insertWorkoutCategory(entity)

            // Jeśli jest połączenie internetowe, wyślij od razu na serwer
            if (networkManager.isInternetAvailable()) {
                val remoteResult = remoteDataSource.createWorkoutCategory(categoryWithId)
                if (remoteResult.isSuccess) {
                    // Zaktualizuj lokalną encję - usuń flagę needsSync
                    workoutCategoryDao.updateWorkoutCategory(entity.copy(needsSync = false))
                } else {
                    // Jeśli nie udało się wysłać, zachowaj flagę needsSync
                    Log.w(TAG, "Failed to sync category to remote, will retry later")
                }
            }

            // Uruchom synchronizację w tle (na wypadek gdyby nie było połączenia)
            syncManager.requestSync()

            Result.success(categoryWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create workout category", e)
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje istniejącą kategorię treningową.
     */
    suspend fun updateWorkoutCategory(category: WorkoutCategory): Result<WorkoutCategory> {
        return try {
            // Zapisz lokalnie z flagą needsSync = true
            val entity = mappers.toEntity(category, needsSync = true)
            workoutCategoryDao.updateWorkoutCategory(entity)

            // Jeśli jest połączenie internetowe, wyślij od razu na serwer
            if (networkManager.isInternetAvailable()) {
                val remoteResult = remoteDataSource.updateWorkoutCategory(category)
                if (remoteResult.isSuccess) {
                    // Zaktualizuj lokalną encję - usuń flagę needsSync
                    workoutCategoryDao.updateWorkoutCategory(entity.copy(needsSync = false))
                } else {
                    Log.w(TAG, "Failed to sync category update to remote, will retry later")
                }
            }

            // Uruchom synchronizację w tle
            syncManager.requestSync()

            Result.success(category)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update workout category", e)
            Result.failure(e)
        }
    }

    /**
     * Usuwa kategorię treningową.
     */
    suspend fun deleteWorkoutCategory(categoryId: String): Result<Unit> {
        return try {
            // Usuń lokalnie
            workoutCategoryDao.deleteWorkoutCategory(categoryId)

            // Jeśli jest połączenie internetowe, usuń też z serwera
            if (networkManager.isInternetAvailable()) {
                val remoteResult = remoteDataSource.deleteWorkoutCategory(categoryId)
                if (remoteResult.isFailure) {
                    Log.w(TAG, "Failed to delete category from remote")
                }
            }

            // Uruchom synchronizację w tle
            syncManager.requestSync()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete workout category", e)
            Result.failure(e)
        }
    }

    /**
     * Synchronizuje kategorie treningowe z serwerem
     */
    suspend fun syncWorkoutCategories(userId: String): Result<List<WorkoutCategory>> {
        return try {
            if (!networkManager.isInternetAvailable()) {
                // Jeśli nie ma połączenia, zwróć lokalne dane
                val categories = getUserWorkoutCategories(userId).first()
                return Result.success(categories)
            }

            // Pobierz kategorie z serwera
            val remoteResult = remoteDataSource.getUserWorkoutCategories(userId)

            if (remoteResult.isSuccess) {
                val remoteCategories = remoteResult.getOrNull()!!

                // Pobierz lokalne kategorie
                val localCategories = workoutCategoryDao.getWorkoutCategoriesByUserId(userId).first()

                // Synchronizuj kategorie
                remoteCategories.forEach { remoteCategory ->
                    val localCategory = localCategories.find { it.id == remoteCategory.id }

                    if (localCategory == null) {
                        // Dodaj nową kategorię z serwera
                        val entity = mappers.toEntity(remoteCategory, needsSync = false)
                        workoutCategoryDao.insertWorkoutCategory(entity)
                    } else if (localCategory.lastSyncTime < remoteCategory.createdAt.seconds * 1000) {
                        // Aktualizuj lokalną kategorię jeśli zdalna jest nowsza
                        val entity = mappers.toEntity(remoteCategory, needsSync = false)
                        workoutCategoryDao.updateWorkoutCategory(entity)
                    }
                }

                // Zwróć zaktualizowaną listę kategorii
                val updatedCategories = getUserWorkoutCategories(userId).first()
                Result.success(updatedCategories)
            } else {
                // Jeśli nie udało się pobrać z serwera, zwróć lokalne dane
                val categories = getUserWorkoutCategories(userId).first()
                Result.success(categories)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync workout categories", e)
            Result.failure(e)
        }
    }
}