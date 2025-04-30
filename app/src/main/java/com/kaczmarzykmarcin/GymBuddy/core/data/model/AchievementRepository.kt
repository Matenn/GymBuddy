package com.kaczmarzykmarcin.GymBuddy.data.repository

import android.util.Log
import com.kaczmarzykmarcin.GymBuddy.common.network.NetworkConnectivityManager
import com.kaczmarzykmarcin.GymBuddy.data.model.Achievement
import com.kaczmarzykmarcin.GymBuddy.data.model.UserAchievement
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
 * Obsługuje zarówno lokalne jak i zdalne źródło danych.
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

    /**
     * Dodaje nowe osiągnięcie dla użytkownika.
     */
    suspend fun addUserAchievement(userAchievement: UserAchievement): Result<UserAchievement> {
        return try {
            // Generuj nowe ID jeśli nie zostało dostarczone
            val achievementWithId = if (userAchievement.id.isEmpty()) {
                userAchievement.copy(id = UUID.randomUUID().toString())
            } else {
                userAchievement
            }

            // Zapisz lokalnie
            val entity = mappers.toEntity(achievementWithId, true)
            userAchievementDao.insertUserAchievement(entity)

            // Uruchom synchronizację w tle
            syncManager.requestSync()

            Result.success(achievementWithId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add user achievement", e)
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

        // Sprawdź, czy użytkownik już ma to osiągnięcie
        if (hasAchievement(userId, achievementId)) {
            val existingAchievement = userAchievementDao.getUserAchievementByIdType(userId, achievementId)
            if (existingAchievement != null) {
                return Result.success(mappers.toModel(existingAchievement))
            }
        }

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
            // Pobierz z lokalnej bazy danych
            val achievements = userAchievementDao.getUserAchievementsByUserId(userId)

            // Jeśli nie ma danych lokalnie i jest sieć, pobierz z Firebase i zapisz lokalnie
            if (achievements.isEmpty() && networkManager.isInternetAvailable()) {
                val remoteResult = remoteDataSource.getUserAchievements(userId)

                if (remoteResult.isSuccess) {
                    val remoteAchievements = remoteResult.getOrNull()!!

                    // Zapisz osiągnięcia lokalnie
                    val entities = remoteAchievements.map { mappers.toEntity(it) }
                    userAchievementDao.insertUserAchievements(entities)

                    return Result.success(remoteAchievements)
                }
            }

            // Zwróć osiągnięcia z lokalnej bazy danych
            Result.success(achievements.map { mappers.toModel(it) })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user achievements", e)
            Result.failure(e)
        }
    }

    /**
     * Sprawdza, czy użytkownik posiada dane osiągnięcie.
     */
    suspend fun hasAchievement(userId: String, achievementId: Int): Boolean {
        return try {
            val achievement = userAchievementDao.getUserAchievementByIdType(userId, achievementId)

            // Jeśli nie znaleziono lokalnie i jest sieć, sprawdź w Firebase
            if (achievement == null && networkManager.isInternetAvailable()) {
                return remoteDataSource.hasAchievement(userId, achievementId)
            }

            achievement != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for achievement", e)
            false
        }
    }

    /**
     * Obserwuje zmiany w osiągnięciach użytkownika jako Flow.
     */
    fun observeUserAchievements(userId: String): Flow<List<UserAchievement>> = flow {
        try {
            // Pobierz z lokalnej bazy danych
            val achievements = userAchievementDao.getUserAchievementsByUserId(userId)
            emit(achievements.map { mappers.toModel(it) })

            // Jeśli jest sieć, pobierz również z Firebase i zaktualizuj lokalną bazę
            if (networkManager.isInternetAvailable()) {
                val remoteResult = remoteDataSource.getUserAchievements(userId)

                if (remoteResult.isSuccess) {
                    val remoteAchievements = remoteResult.getOrNull()!!

                    // Zapisz osiągnięcia lokalnie
                    val entities = remoteAchievements.map { mappers.toEntity(it) }
                    userAchievementDao.insertUserAchievements(entities)

                    // Emituj zaktualizowane dane
                    val updatedAchievements = userAchievementDao.getUserAchievementsByUserId(userId)
                    emit(updatedAchievements.map { mappers.toModel(it) })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error observing user achievements", e)
            // W przypadku błędu, emitujemy pustą listę
            emit(emptyList())
        }
    }
}