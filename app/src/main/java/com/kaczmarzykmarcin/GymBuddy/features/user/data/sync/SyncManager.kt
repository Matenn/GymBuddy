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
    private val achievementDefinitionDao: AchievementDefinitionDao,
    private val achievementProgressDao: AchievementProgressDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val workoutDao: WorkoutDao,
    private val workoutCategoryDao: WorkoutCategoryDao,
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

            // Synchronizacja szablonów treningów
            syncWorkoutTemplates()

            // Synchronizacja historii treningów
            syncCompletedWorkouts()

            // Synchronizacja kategorii - dwukierunkowa
            syncWorkoutCategories()

            // NOWA: Synchronizacja osiągnięć
            syncAchievements()

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

    /**
     * Synchronizuje kategorie treningowe - dwukierunkowa synchronizacja
     */
    private suspend fun syncWorkoutCategories() {
        try {
            // 1. Wyślij lokalne zmiany do serwera
            val categoriesToSync = workoutCategoryDao.getWorkoutCategoriesToSync()

            for (categoryEntity in categoriesToSync) {
                try {
                    val categoryModel = mappers.toModel(categoryEntity)

                    // Sprawdź czy kategoria istnieje na serwerze
                    val existingResult = remoteDataSource.getWorkoutCategory(categoryModel.id)

                    val result = if (existingResult.isSuccess) {
                        // Aktualizuj istniejącą
                        remoteDataSource.updateWorkoutCategory(categoryModel)
                    } else {
                        // Utwórz nową
                        remoteDataSource.createWorkoutCategory(categoryModel)
                    }

                    if (result.isSuccess) {
                        // Oznacz jako zsynchronizowane
                        workoutCategoryDao.updateWorkoutCategory(
                            categoryEntity.copy(needsSync = false, lastSyncTime = System.currentTimeMillis())
                        )
                        Log.d(TAG, "Successfully synced category: ${categoryModel.name}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing workout category: ${categoryEntity.id}", e)
                }
            }

            // 2. Pobierz kategorie z serwera
            // Pobierz ID aktualnie zalogowanego użytkownika
            val currentUserId = getCurrentUserId()
            if (currentUserId != null) {
                val remoteCategories = remoteDataSource.getUserWorkoutCategories(currentUserId)

                if (remoteCategories.isSuccess) {
                    remoteCategories.getOrNull()?.forEach { remoteCategory ->
                        try {
                            // Sprawdź czy kategoria istnieje lokalnie
                            val localCategory = workoutCategoryDao.getWorkoutCategoryById(remoteCategory.id)

                            if (localCategory == null) {
                                // Dodaj nową kategorię z serwera
                                val entity = mappers.toEntity(remoteCategory, needsSync = false)
                                workoutCategoryDao.insertWorkoutCategory(entity)
                                Log.d(TAG, "Added new category from server: ${remoteCategory.name}")
                            } else if (!localCategory.needsSync &&
                                localCategory.lastSyncTime < remoteCategory.createdAt.seconds * 1000) {
                                // Aktualizuj lokalną kategorię jeśli zdalna jest nowsza i lokalna nie ma zmian
                                val entity = mappers.toEntity(remoteCategory, needsSync = false)
                                workoutCategoryDao.updateWorkoutCategory(entity)
                                Log.d(TAG, "Updated category from server: ${remoteCategory.name}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing remote category: ${remoteCategory.id}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during workout categories sync", e)
        }
    }

    /**
     * NOWA: Synchronizuje osiągnięcia - definicje i postępy
     */
    private suspend fun syncAchievements() {
        try {
            Log.d(TAG, "Starting achievements synchronization")

            // 1. Synchronizacja definicji osiągnięć (tylko pobieranie z serwera)
            syncAchievementDefinitions()

            // 2. Synchronizacja postępów osiągnięć (dwukierunkowa)
            syncAchievementProgresses()

            Log.d(TAG, "Achievements synchronization completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during achievements sync", e)
        }
    }

    /**
     * Synchronizuje definicje osiągnięć z Firebase do lokalnej bazy
     */
    private suspend fun syncAchievementDefinitions() {
        try {
            Log.d(TAG, "Syncing achievement definitions from Firebase")

            val remoteDefinitions = remoteDataSource.getAllAchievementDefinitions()

            if (remoteDefinitions.isSuccess) {
                val definitions = remoteDefinitions.getOrNull() ?: emptyList()

                definitions.forEach { definition ->
                    try {
                        // Sprawdź czy definicja istnieje lokalnie
                        val localDefinition = achievementDefinitionDao.getAchievementDefinitionById(definition.id)

                        val entity = mappers.toEntity(definition)

                        if (localDefinition == null) {
                            // Dodaj nową definicję
                            achievementDefinitionDao.insertAchievementDefinition(entity)
                            Log.d(TAG, "Added new achievement definition: ${definition.title}")
                        } else if (localDefinition.createdAt < definition.createdAt.seconds) {
                            // Aktualizuj jeśli zdalna jest nowsza
                            achievementDefinitionDao.updateAchievementDefinition(entity)
                            Log.d(TAG, "Updated achievement definition: ${definition.title}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing achievement definition: ${definition.id}", e)
                    }
                }

                Log.d(TAG, "Successfully synced ${definitions.size} achievement definitions")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing achievement definitions", e)
        }
    }

    /**
     * Synchronizuje postępy osiągnięć - dwukierunkowa synchronizacja
     */
    private suspend fun syncAchievementProgresses() {
        try {
            Log.d(TAG, "Syncing achievement progresses")

            val currentUserId = getCurrentUserId()
            if (currentUserId == null) {
                Log.w(TAG, "No current user ID found for achievement progress sync")
                return
            }

            // 1. Wyślij lokalne postępy do serwera (jeśli zostały zmodyfikowane)
            val localProgresses = achievementProgressDao.getUserProgresses(currentUserId)

            for (progressEntity in localProgresses) {
                try {
                    val progressModel = mappers.toModel(progressEntity)

                    // Sprawdź czy postęp istnieje na serwerze
                    val remoteProgress = remoteDataSource.getAchievementProgress(currentUserId, progressModel.achievementId)

                    val shouldSync = if (remoteProgress.isSuccess) {
                        val remote = remoteProgress.getOrNull()
                        // Synchronizuj jeśli lokalny jest nowszy lub ma większą wartość
                        remote == null ||
                                progressModel.lastUpdated.seconds > remote.lastUpdated.seconds ||
                                (progressModel.currentValue > remote.currentValue && !remote.isCompleted)
                    } else {
                        true // Nie ma na serwerze, wyślij
                    }

                    if (shouldSync) {
                        val result = remoteDataSource.saveAchievementProgress(progressModel)
                        if (result.isSuccess) {
                            Log.d(TAG, "Successfully synced progress to server: ${progressModel.achievementId}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing progress to server: ${progressEntity.achievementId}", e)
                }
            }

            // 2. Pobierz postępy z serwera
            val remoteProgresses = remoteDataSource.getUserAchievementProgresses(currentUserId)

            if (remoteProgresses.isSuccess) {
                val progresses = remoteProgresses.getOrNull() ?: emptyList()

                progresses.forEach { remoteProgress ->
                    try {
                        // Sprawdź czy postęp istnieje lokalnie
                        val localProgress = achievementProgressDao.getUserProgressForAchievement(
                            currentUserId, remoteProgress.achievementId
                        )

                        val entity = mappers.toEntity(remoteProgress)

                        if (localProgress == null) {
                            // Dodaj nowy postęp z serwera
                            achievementProgressDao.insertAchievementProgress(entity)
                            Log.d(TAG, "Added new progress from server: ${remoteProgress.achievementId}")
                        } else {
                            // Porównaj który jest nowszy/lepszy
                            val localModel = mappers.toModel(localProgress)

                            val shouldUpdate = remoteProgress.lastUpdated.seconds > localModel.lastUpdated.seconds ||
                                    (remoteProgress.isCompleted && !localModel.isCompleted) ||
                                    (remoteProgress.currentValue > localModel.currentValue && !localModel.isCompleted)

                            if (shouldUpdate) {
                                achievementProgressDao.updateAchievementProgress(entity)
                                Log.d(TAG, "Updated progress from server: ${remoteProgress.achievementId}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing remote progress: ${remoteProgress.achievementId}", e)
                    }
                }

                Log.d(TAG, "Successfully processed ${progresses.size} achievement progresses from server")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing achievement progresses", e)
        }
    }

    /**
     * Pobiera ID aktualnie zalogowanego użytkownika
     */
    private suspend fun getCurrentUserId(): String? {
        return try {
            // Najpierw spróbuj pobrać z lokalnej bazy
            val users = userDao.getUsersToSync()
            if (users.isNotEmpty()) {
                return users.first().id
            }

            // Jeśli nie ma w lokalnej bazie, pobierz pierwszy użytkownik
            // W rzeczywistej aplikacji powinieneś mieć lepszy sposób na przechowywanie aktualnego użytkownika
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user ID", e)
            null
        }
    }

    /**
     * Wymusza pełną synchronizację danych użytkownika
     */
    suspend fun forceFullSync(userId: String) {
        if (!networkManager.isInternetAvailable()) {
            Log.w(TAG, "No internet connection for full sync")
            return
        }

        try {
            Log.d(TAG, "Starting forced full sync for user: $userId")

            // Pobierz wszystkie dane z serwera
            val userDataResult = remoteDataSource.getFullUserData(userId)

            if (userDataResult.isSuccess) {
                val userData = userDataResult.getOrNull()!!

                // Zapisz dane lokalnie
                userDao.insertUser(mappers.toEntity(userData.user))
                userAuthDao.insertUserAuth(mappers.toEntity(userData.auth))
                userProfileDao.insertUserProfile(mappers.toEntity(userData.profile))
                userStatsDao.insertUserStats(mappers.toEntity(userData.stats))

                Log.d(TAG, "Saved user data to local database")
            }

            // Pobierz i zapisz szablony treningów
            val templatesResult = remoteDataSource.getUserWorkoutTemplates(userId)
            if (templatesResult.isSuccess) {
                templatesResult.getOrNull()?.forEach { template ->
                    workoutTemplateDao.insertWorkoutTemplate(mappers.toEntity(template))
                }
            }

            // Pobierz i zapisz historię treningów
            val workoutsResult = remoteDataSource.getUserWorkoutHistory(userId)
            if (workoutsResult.isSuccess) {
                workoutsResult.getOrNull()?.forEach { workout ->
                    workoutDao.insertCompletedWorkout(mappers.toEntity(workout))
                }
            }

            // Pobierz i zapisz kategorie treningowe
            val categoriesResult = remoteDataSource.getUserWorkoutCategories(userId)
            if (categoriesResult.isSuccess) {
                categoriesResult.getOrNull()?.forEach { category ->
                    // Sprawdź czy kategoria już istnieje lokalnie
                    val existingCategory = workoutCategoryDao.getWorkoutCategoryById(category.id)
                    val entity = mappers.toEntity(category, needsSync = false)

                    if (existingCategory == null) {
                        workoutCategoryDao.insertWorkoutCategory(entity)
                    } else {
                        workoutCategoryDao.updateWorkoutCategory(entity)
                    }
                }
            }

            // NOWE: Pobierz i zapisz osiągnięcia
            syncAchievementDefinitions()
            val achievementProgressesResult = remoteDataSource.getUserAchievementProgresses(userId)
            if (achievementProgressesResult.isSuccess) {
                achievementProgressesResult.getOrNull()?.forEach { progress ->
                    achievementProgressDao.insertAchievementProgress(mappers.toEntity(progress))
                }
            }

            Log.d(TAG, "Forced full sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during forced full sync", e)
        }
    }

    /**
     * Czyści wszystkie dane synchronizacji (przydatne po wylogowaniu)
     * ZAKTUALIZOWANE: dodano więcej szczegółów i lepszą obsługę błędów
     */
    suspend fun clearAllData() {
        try {
            Log.d(TAG, "Clearing all sync data")

            // 1. Zatrzymaj synchronizację
            isSyncRunning = false
            Log.d(TAG, "Stopped sync process")

            // 2. Wyczyść wszystkie dane z lokalnych tabel
            Log.d(TAG, "Clearing user data tables")
            userDao.clearAllUsers()
            userAuthDao.clearAllUserAuth()
            userProfileDao.clearAllUserProfiles()
            userStatsDao.clearAllUserStats()
            userAchievementDao.clearAllUserAchievements()

            Log.d(TAG, "Clearing workout data tables")
            workoutTemplateDao.clearAllWorkoutTemplates()
            workoutDao.clearAllCompletedWorkouts()
            workoutCategoryDao.clearAllUserWorkoutCategories() // Zachowaj domyślne kategorie

            Log.d(TAG, "Clearing achievement progress data")
            achievementProgressDao.clearAllAchievementProgresses()
            // UWAGA: definicje osiągnięć pozostawiamy, bo są globalne

            // 3. Zresetuj stany synchronizacji
            _syncState.value = SyncState.Idle
            _lastSyncTime.value = 0

            Log.d(TAG, "All sync data cleared successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error clearing sync data", e)
            // Mimo błędu, spróbuj zresetować stany
            try {
                isSyncRunning = false
                _syncState.value = SyncState.Idle
                _lastSyncTime.value = 0
            } catch (resetError: Exception) {
                Log.e(TAG, "Error resetting sync states", resetError)
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