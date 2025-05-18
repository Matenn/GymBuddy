// WorkoutCategoryRepository.kt
package com.kaczmarzykmarcin.GymBuddy.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.kaczmarzykmarcin.GymBuddy.common.network.NetworkConnectivityManager
import com.kaczmarzykmarcin.GymBuddy.data.model.WorkoutCategory
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.WorkoutCategoryDao
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.WorkoutCategoryEntity
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
            val categories = workoutCategoryDao.getWorkoutCategoriesByUserId(userId).collect { entities ->
                if (entities.none { it.isDefault }) {
                    // Jeśli nie ma domyślnych kategorii, dodaj je
                    val defaultEntities = WorkoutCategory.PREDEFINED_CATEGORIES.map { category ->
                        mappers.toEntity(category.copy(userId = userId))
                    }
                    workoutCategoryDao.insertWorkoutCategories(defaultEntities)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize default categories", e)
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

            // Jeśli nie ma danych lokalnie, zwróć błąd
            // (Zakładamy, że RemoteUserDataSource nie ma jeszcze metody getWorkoutCategory)
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

            // Zapisz lokalnie
            val entity = mappers.toEntity(categoryWithId, true)
            workoutCategoryDao.insertWorkoutCategory(entity)

            // Uruchom synchronizację w tle
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
            // Zapisz lokalnie
            val entity = mappers.toEntity(category, true)
            workoutCategoryDao.updateWorkoutCategory(entity)

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

            // Uruchom synchronizację w tle (jeśli potrzeba)
            syncManager.requestSync()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete workout category", e)
            Result.failure(e)
        }
    }

    /**
     * Synchronizuje kategorie treningowe z serwerem (tymczasowa implementacja)
     */
    suspend fun syncWorkoutCategories(userId: String): Result<List<WorkoutCategory>> {
        return try {
            // Pobierz lokalne kategorie
            val categories = getUserWorkoutCategories(userId).map { it }.first()
            Result.success(categories)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync workout categories", e)
            Result.failure(e)
        }
    }
}