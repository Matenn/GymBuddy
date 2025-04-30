package com.kaczmarzykmarcin.GymBuddy.features.user.data.sync

import android.content.Context
import android.util.Log
import com.kaczmarzykmarcin.GymBuddy.common.network.NetworkConnectivityManager
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.*
import com.kaczmarzykmarcin.GymBuddy.features.user.data.mapper.UserMappers
import com.kaczmarzykmarcin.GymBuddy.features.user.data.remote.RemoteUserDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menedżer synchronizacji danych między lokalną a zdalną bazą danych
 */
@Singleton
class SyncManager @Inject constructor(
    private val context: Context,
    private val userDao: UserDao,
    private val userAuthDao: UserAuthDao,
    private val userProfileDao: UserProfileDao,
    private val userStatsDao: UserStatsDao,
    private val userAchievementDao: UserAchievementDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val workoutDao: WorkoutDao,
    private val remoteDataSource: RemoteUserDataSource,
    private val mappers: UserMappers,
    private val networkManager: NetworkConnectivityManager
) {
    private val TAG = "SyncManager"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _lastSyncTime = MutableStateFlow<Long>(0)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime

    private var isSyncRunning = false

    /**
     * Inicjalizuje menedżer synchronizacji
     */
    fun initialize() {
        Log.d(TAG, "SyncManager initialized")
        startNetworkMonitoring()
        startPeriodicSync()
    }

    /**
     * Rozpoczyna monitorowanie stanu sieci
     */
    private fun startNetworkMonitoring() {
        coroutineScope.launch {
            networkManager.isNetworkAvailable.collectLatest { isAvailable ->
                if (isAvailable && !isSyncRunning) {
                    Log.d(TAG, "Network became available, starting sync")
                    syncData()
                }
            }
        }
    }

    /**
     * Rozpoczyna okresową synchronizację danych
     */
    private fun startPeriodicSync() {
        coroutineScope.launch {
            while (isActive) {
                if (networkManager.isInternetAvailable() && !isSyncRunning) {
                    syncData()
                }
                delay(SYNC_INTERVAL)
            }
        }
    }

    /**
     * Wymusza synchronizację danych
     */
    fun requestSync() {
        if (!isSyncRunning) {
            coroutineScope.launch {
                syncData()
            }
        }
    }

    /**
     * Synchronizuje dane między lokalną a zdalną bazą danych
     */
    private suspend fun syncData() {
        if (isSyncRunning || !networkManager.isInternetAvailable()) return

        isSyncRunning = true
        _syncState.value = SyncState.Syncing

        try {
            Log.d(TAG, "Starting data synchronization")

            // Synchronizacja danych użytkowników
            syncUsers()

            // Synchronizacja danych uwierzytelniania
            syncUserAuth()

            // Synchronizacja profili użytkowników
            syncUserProfiles()

            // Synchronizacja statystyk użytkowników
            syncUserStats()

            // Synchronizacja osiągnięć użytkowników
            syncUserAchievements()

            // Synchronizacja szablonów treningów
            syncWorkoutTemplates()

            // Synchronizacja historii treningów
            syncCompletedWorkouts()

            _lastSyncTime.value = System.currentTimeMillis()
            _syncState.value = SyncState.Success
            Log.d(TAG, "Data synchronization completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during data synchronization", e)
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
        } finally {
            isSyncRunning = false
        }
    }

    /**
     * Synchronizuje dane użytkowników
     */
    private suspend fun syncUsers() {
        val usersToSync = userDao.getUsersToSync()

        for (userEntity in usersToSync) {
            try {
                val userModel = mappers.toModel(userEntity)
                remoteDataSource.updateUser(userModel)

                // Oznacz jako zsynchronizowane
                userDao.updateUser(userEntity.copy(needsSync = false, lastSyncTime = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing user: ${userEntity.id}", e)
            }
        }
    }

    /**
     * Synchronizuje dane uwierzytelniania
     */
    private suspend fun syncUserAuth() {
        val userAuthToSync = userAuthDao.getUserAuthToSync()

        for (userAuthEntity in userAuthToSync) {
            try {
                val userAuthModel = mappers.toModel(userAuthEntity)
                remoteDataSource.updateUserAuth(userAuthModel)

                // Oznacz jako zsynchronizowane
                userAuthDao.updateUserAuth(userAuthEntity.copy(needsSync = false, lastSyncTime = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing user auth: ${userAuthEntity.id}", e)
            }
        }
    }

    /**
     * Synchronizuje profile użytkowników
     */
    private suspend fun syncUserProfiles() {
        val userProfilesToSync = userProfileDao.getUserProfilesToSync()

        for (userProfileEntity in userProfilesToSync) {
            try {
                val userProfileModel = mappers.toModel(userProfileEntity)
                remoteDataSource.updateUserProfile(userProfileModel)

                // Oznacz jako zsynchronizowane
                userProfileDao.updateUserProfile(userProfileEntity.copy(needsSync = false, lastSyncTime = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing user profile: ${userProfileEntity.id}", e)
            }
        }
    }

    /**
     * Synchronizuje statystyki użytkowników
     */
    private suspend fun syncUserStats() {
        val userStatsToSync = userStatsDao.getUserStatsToSync()

        for (userStatsEntity in userStatsToSync) {
            try {
                val userStatsModel = mappers.toModel(userStatsEntity)
                remoteDataSource.updateUserStats(userStatsModel)

                // Oznacz jako zsynchronizowane
                userStatsDao.updateUserStats(userStatsEntity.copy(needsSync = false, lastSyncTime = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing user stats: ${userStatsEntity.id}", e)
            }
        }
    }

    /**
     * Synchronizuje osiągnięcia użytkowników
     */
    private suspend fun syncUserAchievements() {
        val userAchievementsToSync = userAchievementDao.getUserAchievementsToSync()

        for (userAchievementEntity in userAchievementsToSync) {
            try {
                val userAchievementModel = mappers.toModel(userAchievementEntity)
                remoteDataSource.addUserAchievement(userAchievementModel)

                // Oznacz jako zsynchronizowane
                userAchievementDao.updateUserAchievement(userAchievementEntity.copy(needsSync = false, lastSyncTime = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing user achievement: ${userAchievementEntity.id}", e)
            }
        }
    }

    /**
     * Synchronizuje szablony treningów
     */
    private suspend fun syncWorkoutTemplates() {
        val workoutTemplatesToSync = workoutTemplateDao.getWorkoutTemplatesToSync()

        for (workoutTemplateEntity in workoutTemplatesToSync) {
            try {
                val workoutTemplateModel = mappers.toModel(workoutTemplateEntity)
                remoteDataSource.updateWorkoutTemplate(workoutTemplateModel)

                // Oznacz jako zsynchronizowane
                workoutTemplateDao.updateWorkoutTemplate(workoutTemplateEntity.copy(needsSync = false, lastSyncTime = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing workout template: ${workoutTemplateEntity.id}", e)
            }
        }
    }

    /**
     * Synchronizuje historię treningów
     */
    private suspend fun syncCompletedWorkouts() {
        val completedWorkoutsToSync = workoutDao.getCompletedWorkoutsToSync()

        for (completedWorkoutEntity in completedWorkoutsToSync) {
            try {
                val completedWorkoutModel = mappers.toModel(completedWorkoutEntity)
                remoteDataSource.updateWorkout(completedWorkoutModel)

                // Oznacz jako zsynchronizowane
                workoutDao.updateCompletedWorkout(completedWorkoutEntity.copy(needsSync = false, lastSyncTime = System.currentTimeMillis()))
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing completed workout: ${completedWorkoutEntity.id}", e)
            }
        }
    }

    companion object {
        private const val SYNC_INTERVAL = 15 * 60 * 1000L // 15 minut
    }
}

/**
 * Reprezentuje stan synchronizacji
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}