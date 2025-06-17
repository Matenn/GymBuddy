package com.kaczmarzykmarcin.GymBuddy.features.user.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserAchievementDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserAuthDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserProfileDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserStatsDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.WorkoutCategoryDao
import com.kaczmarzykmarcin.GymBuddy.features.achievements.data.repository.AchievementRepository
import com.kaczmarzykmarcin.GymBuddy.features.workouts.data.repository.WorkoutCategoryRepository
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model.AchievementProgress
import com.kaczmarzykmarcin.GymBuddy.features.profile.domain.model.UserProfile

import com.kaczmarzykmarcin.GymBuddy.features.user.data.mapper.UserMappers
import com.kaczmarzykmarcin.GymBuddy.features.user.data.remote.RemoteUserDataSource
import com.kaczmarzykmarcin.GymBuddy.features.user.data.sync.SyncManager
import com.kaczmarzykmarcin.GymBuddy.features.user.domain.model.ExerciseStat
import com.kaczmarzykmarcin.GymBuddy.features.user.domain.model.User
import com.kaczmarzykmarcin.GymBuddy.features.user.domain.model.UserData
import com.kaczmarzykmarcin.GymBuddy.features.user.domain.model.UserStats
import com.kaczmarzykmarcin.GymBuddy.features.user.domain.model.WeightEntry
import com.kaczmarzykmarcin.GymBuddy.features.user.domain.model.WorkoutTypeStat
import com.kaczmarzykmarcin.GymBuddy.features.workouts.data.repository.WorkoutRepository
import com.kaczmarzykmarcin.GymBuddy.features.workouts.domain.model.CompletedWorkout
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repozytorium do zarządzania danymi użytkownika z obsługą lokalnego i zdalnego źródła danych.
 * Domyślnie operacje odczytują i zapisują do lokalnej bazy danych, a synchronizacja
 * z serwerem Firebase odbywa się w tle poprzez SyncManager.
 */
@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val remoteDataSource: RemoteUserDataSource,
    private val userDao: UserDao,
    private val userAuthDao: UserAuthDao,
    private val userProfileDao: UserProfileDao,
    private val userStatsDao: UserStatsDao,
    private val userAchievementDao: UserAchievementDao,
    private val workoutCategoryDao: WorkoutCategoryDao,
    private val syncManager: SyncManager,
    private val mappers: UserMappers,
    private val workoutRepository: WorkoutRepository,
    private val workoutCategoryRepository: WorkoutCategoryRepository,
    private val achievementRepository: AchievementRepository
) {
    private val TAG = "UserRepository"

    /**
     * Tworzy nowego użytkownika we wszystkich niezbędnych bazach danych.
     * Zapisuje dane lokalnie i uruchamia ich synchronizację z serwerem.
     */
    suspend fun createUser(firebaseUser: FirebaseUser): Result<User> {
        return try {
            Log.d(TAG, "Creating new user: ${firebaseUser.uid}")

            // Sprawdź, czy użytkownik istnieje lokalnie
            val existingUser = userDao.getUserById(firebaseUser.uid)
            if (existingUser != null) {
                Log.d(TAG, "User already exists locally, returning existing user")
                val user = mappers.toModel(existingUser)
                return Result.success(user)
            }

            // Jeśli nie istnieje lokalnie, spróbuj pobrać z Firebase
            val remoteUserResult = remoteDataSource.getUser(firebaseUser.uid)

            if (remoteUserResult.isSuccess) {
                Log.d(TAG, "User exists remotely, downloading user data")
                // Użytkownik istnieje w Firebase, pobierz pełne dane
                val remoteUser = remoteUserResult.getOrNull()!!
                downloadFullUserData(firebaseUser.uid)
                return Result.success(remoteUser)
            }

            // Użytkownik nie istnieje ani lokalnie, ani zdalnie - utwórz nowego
            Log.d(TAG, "Creating new user in Firebase")
            val remoteCreateResult = remoteDataSource.createUser(firebaseUser)

            if (remoteCreateResult.isFailure) {
                return remoteCreateResult
            }

            val newUser = remoteCreateResult.getOrNull()!!

            // Pobierz pełne dane użytkownika z Firebase
            downloadFullUserData(firebaseUser.uid)

            // Inicjalizuj domyślne kategorie treningowe
            workoutCategoryRepository.initializeDefaultCategories(firebaseUser.uid)


            Result.success(newUser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create user", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera pełne dane użytkownika, najpierw z lokalnej bazy danych,
     * a jeśli nie są dostępne, to z Firebase.
     */
    suspend fun getFullUserData(userId: String): Result<UserData> {
        Log.d(TAG, "Getting full user data for: $userId")

        try {
            // Próba pobrania z lokalnej bazy danych
            val localUserData = getLocalUserData(userId)

            if (localUserData != null) {
                Log.d(TAG, "Retrieved user data from local database")
                return Result.success(localUserData)
            }

            // Jeśli nie ma danych lokalnie, pobierz z Firebase i zapisz lokalnie
            Log.d(TAG, "Local user data not found, downloading from Firebase")
            return downloadFullUserData(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get full user data", e)
            return Result.failure(e)
        }
    }

    /**
     * Pobiera dane użytkownika z lokalnej bazy danych.
     */
    private suspend fun getLocalUserData(userId: String): UserData? {
        val userEntity = userDao.getUserById(userId) ?: return null
        val userAuthEntity = userAuthDao.getUserAuthById(userEntity.authId) ?: return null
        val userProfileEntity = userProfileDao.getUserProfileById(userEntity.profileId) ?: return null
        val userStatsEntity = userStatsDao.getUserStatsById(userEntity.statsId) ?: return null

        // Użyj prostszej metody mappers.toUserData która bierze entity bezpośrednio
        return mappers.toUserData(userEntity, userAuthEntity, userProfileEntity, userStatsEntity)
    }

    /**
     * Pobiera pełne dane użytkownika z Firebase i zapisuje lokalnie.
     */
    private suspend fun downloadFullUserData(userId: String): Result<UserData> {
        return try {
            Log.d(TAG, "Starting to download full user data for user: $userId")
            val remoteResult = remoteDataSource.getFullUserData(userId)

            if (remoteResult.isSuccess) {
                val userData = remoteResult.getOrNull()!!
                Log.d(TAG, "Successfully fetched user data from Firebase")

                // Zapisz dane lokalnie
                userDao.insertUser(mappers.toEntity(userData.user))
                userAuthDao.insertUserAuth(mappers.toEntity(userData.auth))
                userProfileDao.insertUserProfile(mappers.toEntity(userData.profile))
                userStatsDao.insertUserStats(mappers.toEntity(userData.stats))

                // Stare osiągnięcia już nie są obsługiwane
                Log.d(TAG, "Saved basic user data to local database")

                // Synchronizuj kategorie treningowe
                try {
                    Log.d(TAG, "Synchronizing workout categories for user: $userId")
                    val categoriesResult = remoteDataSource.getUserWorkoutCategories(userId)

                    if (categoriesResult.isSuccess) {
                        val categories = categoriesResult.getOrNull()!!
                        Log.d(TAG, "Found ${categories.size} categories to sync")

                        categories.forEach { category ->
                            // Sprawdź czy kategoria już istnieje lokalnie
                            val existingCategory = workoutCategoryDao.getWorkoutCategoryById(category.id)
                            if (existingCategory == null) {
                                // Dodaj nową kategorię
                                val entity = mappers.toEntity(category, needsSync = false)
                                workoutCategoryDao.insertWorkoutCategory(entity)
                                Log.d(TAG, "Added category: ${category.name}")
                            } else {
                                // Aktualizuj istniejącą kategorię
                                val entity = mappers.toEntity(category, needsSync = false)
                                workoutCategoryDao.updateWorkoutCategory(entity)
                                Log.d(TAG, "Updated category: ${category.name}")
                            }
                        }

                        // Inicjalizuj domyślne kategorie jeśli ich nie ma
                        workoutCategoryRepository.initializeDefaultCategories(userId)

                        Log.d(TAG, "Successfully synchronized ${categories.size} workout categories")
                    } else {
                        Log.e(TAG, "Failed to sync workout categories: ${categoriesResult.exceptionOrNull()?.message}")
                        // Mimo błędu, spróbuj zainicjalizować domyślne kategorie lokalnie
                        workoutCategoryRepository.initializeDefaultCategories(userId)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error synchronizing workout categories", e)
                    // Mimo błędu, spróbuj zainicjalizować domyślne kategorie lokalnie
                    workoutCategoryRepository.initializeDefaultCategories(userId)
                }

                // Synchronizuj również szablony treningów i historię treningów
                try {
                    Log.d(TAG, "Synchronizing workout templates and history for user: $userId")
                    val templatesResult = workoutRepository.syncWorkoutTemplates(userId)
                    val historyResult = workoutRepository.syncWorkoutHistory(userId)

                    if (templatesResult.isSuccess) {
                        Log.d(TAG, "Successfully synchronized workout templates: ${templatesResult.getOrNull()?.size} templates")
                    } else {
                        Log.e(TAG, "Failed to sync workout templates: ${templatesResult.exceptionOrNull()?.message}")
                    }

                    if (historyResult.isSuccess) {
                        Log.d(TAG, "Successfully synchronized workout history: ${historyResult.getOrNull()?.size} workouts")
                    } else {
                        Log.e(TAG, "Failed to sync workout history: ${historyResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error synchronizing workouts", e)
                    // Kontynuuj mimo błędu, gdyż najważniejsze są dane użytkownika
                }

                Log.d(TAG, "Successfully downloaded and saved user data locally")
                Result.success(userData)
            } else {
                Log.e(TAG, "Failed to get user data from Firebase", remoteResult.exceptionOrNull())
                remoteResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading full user data", e)
            Result.failure(e)
        }
    }

    /**
     * Loguje użytkownika i synchronizuje jego dane
     */
    suspend fun signInAndSyncData(firebaseUser: FirebaseUser): Result<UserData> {
        return try {
            Log.d(TAG, "Signing in and syncing data for user: ${firebaseUser.uid}")

            // Sprawdź czy użytkownik istnieje w Firebase
            val userResult = remoteDataSource.getUser(firebaseUser.uid)

            if (userResult.isFailure) {
                // Jeśli użytkownik nie istnieje, utwórz go
                Log.d(TAG, "User doesn't exist in Firebase, creating new user")
                val createResult = createUser(firebaseUser)
                if (createResult.isFailure) {
                    return Result.failure(createResult.exceptionOrNull() ?: Exception("Failed to create user"))
                }
            }

            // Pobierz pełne dane użytkownika i zsynchronizuj
            Log.d(TAG, "Downloading and syncing full user data")
            val fullDataResult = downloadFullUserData(firebaseUser.uid)

            if (fullDataResult.isSuccess) {
                // Aktualizuj czas ostatniego logowania
                updateLastLogin(firebaseUser.uid)

                // Uruchom pełną synchronizację w tle
                syncManager.forceFullSync(firebaseUser.uid)
            }

            fullDataResult
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sign in and sync data", e)
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje czas ostatniego logowania.
     */
    suspend fun updateLastLogin(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Updating last login for user: $userId")

            // Pobierz użytkownika
            val user = userDao.getUserById(userId)
            if (user == null) {
                Log.e(TAG, "User not found for updating last login")
                return Result.failure(Exception("User not found"))
            }

            // Pobierz dane auth
            val userAuth = userAuthDao.getUserAuthById(user.authId)
            if (userAuth == null) {
                Log.e(TAG, "User auth not found for ID: ${user.authId}")
                return Result.failure(Exception("User auth data not found"))
            }

            // Aktualizuj czas logowania
            val updatedUserAuth = mappers.toModel(userAuth).updateLastLogin()
            val updatedEntity = mappers.toEntity(updatedUserAuth, true)

            // Zapisz lokalnie
            userAuthDao.updateUserAuth(updatedEntity)

            // Uruchom synchronizację w tle
            syncManager.requestSync()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update last login", e)
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje profil użytkownika.
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any?>): Result<UserProfile> {
        return try {
            // Pobierz użytkownika
            val user = userDao.getUserById(userId)
            if (user == null) {
                Log.e(TAG, "User not found for updating profile")
                return Result.failure(Exception("User not found"))
            }

            // Pobierz profil
            val userProfileEntity = userProfileDao.getUserProfileById(user.profileId)
            if (userProfileEntity == null) {
                Log.e(TAG, "User profile not found for ID: ${user.profileId}")
                return Result.failure(Exception("User profile not found"))
            }

            // Konwertuj na model
            val userProfile = mappers.toModel(userProfileEntity)

            // Utwórz nowy profil z aktualizacjami
            val updatedProfile = UserProfile(
                id = userProfile.id,
                userId = userProfile.userId,
                displayName = updates["displayName"] as? String ?: userProfile.displayName,
                photoUrl = updates["photoUrl"] as? String ?: userProfile.photoUrl,
                favoriteBodyPart = updates["favoriteBodyPart"] as? String ?: userProfile.favoriteBodyPart
            )

            // Zapisz lokalnie
            val updatedEntity = mappers.toEntity(updatedProfile, true)
            userProfileDao.updateUserProfile(updatedEntity)

            // Uruchom synchronizację w tle
            syncManager.requestSync()

            Result.success(updatedProfile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user profile", e)
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje statystyki użytkownika.
     */
    suspend fun updateUserStats(userStats: UserStats): Result<UserStats> {
        return try {
            Log.d(TAG, "Updating user stats for user: ${userStats.userId}")

            // Zapisz lokalnie z flagą synchronizacji
            val updatedEntity = mappers.toEntity(userStats, true)
            userStatsDao.updateUserStats(updatedEntity)

            // Uruchom synchronizację w tle
            syncManager.requestSync()

            Result.success(userStats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user stats", e)
            Result.failure(e)
        }
    }

    /**
     * Dodaje XP użytkownikowi i aktualizuje poziom jeśli to konieczne.
     * STARA METODA - używa wewnętrznej logiki UserStats.calculateLevel()
     */
    suspend fun addUserXp(userId: String, xpAmount: Int): Result<UserStats> {
        return try {
            // Pobierz użytkownika
            val user = userDao.getUserById(userId)
            if (user == null) {
                Log.e(TAG, "User not found for adding XP")
                return Result.failure(Exception("User not found"))
            }

            // Pobierz statystyki
            val userStatsEntity = userStatsDao.getUserStatsById(user.statsId)
            if (userStatsEntity == null) {
                Log.e(TAG, "User stats not found for ID: ${user.statsId}")
                return Result.failure(Exception("User stats not found"))
            }

            // Konwertuj na model
            val userStats = mappers.toModel(userStatsEntity)

            // Aktualizuj XP
            val updatedXp = userStats.xp + xpAmount

            // Oblicz nowy poziom używając istniejącej metody z UserStats
            val calculatedLevel = userStats.calculateLevel()

            // Aktualizuj model
            val updatedStats = userStats.copy(
                xp = updatedXp,
                level = if (calculatedLevel > userStats.level) calculatedLevel else userStats.level
            )

            // Zapisz i zwróć
            return updateUserStats(updatedStats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add user XP", e)
            Result.failure(e)
        }
    }

    /**
     * NOWA METODA - dodaje XP z nowym algorytmem obliczania poziomu
     */
    suspend fun addXP(userId: String, xpAmount: Int): Result<UserStats> {
        return try {
            val statsResult = getUserStats(userId)
            if (statsResult.isSuccess) {
                val currentStats = statsResult.getOrNull()!!
                val newXP = currentStats.xp + xpAmount
                val newLevel = calculateLevel(newXP)

                val updatedStats = currentStats.copy(
                    xp = newXP,
                    level = newLevel
                )

                updateUserStats(updatedStats)
            } else {
                statsResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add XP", e)
            Result.failure(e)
        }
    }

    /**
     * Oblicza poziom na podstawie XP - nowy algorytm
     */
    private fun calculateLevel(xp: Int): Int {
        var remainingXp = xp
        var currentLevel = 1
        var xpForNextLevel = 100
        while (remainingXp >= xpForNextLevel) {
            remainingXp -= xpForNextLevel
            currentLevel++
            xpForNextLevel = 100 + (currentLevel - 1) * 50
        }
        return currentLevel
    }

    /**
     * Pobiera statystyki użytkownika.
     */
    suspend fun getUserStats(userId: String): Result<UserStats> {
        return try {
            // Pobierz użytkownika
            val user = userDao.getUserById(userId)
            if (user == null) {
                Log.e(TAG, "User not found for getting stats")
                return Result.failure(Exception("User not found"))
            }

            // Pobierz statystyki
            val userStatsEntity = userStatsDao.getUserStatsById(user.statsId)
            if (userStatsEntity == null) {
                Log.e(TAG, "User stats not found for ID: ${user.statsId}")
                return Result.failure(Exception("User stats not found"))
            }

            // Konwertuj na model
            val userStats = mappers.toModel(userStatsEntity)

            Result.success(userStats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user stats", e)
            Result.failure(e)
        }
    }

    // ===== NOWY SYSTEM OSIĄGNIĘĆ =====

    /**
     * Tworzy lub aktualizuje postęp w osiągnięciu (nowy system)
     */
    suspend fun createOrUpdateAchievementProgress(
        userId: String,
        achievementId: String,
        currentValue: Int,
        isCompleted: Boolean = false
    ): Result<AchievementProgress> {
        return try {
            Log.d(TAG, "Creating/updating achievement progress: $achievementId for user: $userId")

            // Pobierz istniejący postęp
            val existingProgress = achievementRepository.getAchievementProgress(userId, achievementId).getOrNull()

            val progress = if (existingProgress != null) {
                // Aktualizuj istniejący
                existingProgress.copy(
                    currentValue = currentValue,
                    isCompleted = isCompleted || existingProgress.isCompleted,
                    completedAt = if (isCompleted && existingProgress.completedAt == null) {
                        com.google.firebase.Timestamp.now()
                    } else {
                        existingProgress.completedAt
                    },
                    lastUpdated = com.google.firebase.Timestamp.now()
                )
            } else {
                // Utwórz nowy
                AchievementProgress(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    achievementId = achievementId,
                    currentValue = currentValue,
                    isCompleted = isCompleted,
                    completedAt = if (isCompleted) Timestamp.now() else null,
                    lastUpdated = Timestamp.now()
                )
            }

            // Zapisz przez AchievementRepository
            val result = achievementRepository.updateAchievementProgress(progress)

            // Jeśli osiągnięcie zostało ukończone, dodaj XP
            if (isCompleted && (existingProgress == null || !existingProgress.isCompleted)) {
                val definition = achievementRepository.getAchievementDefinition(achievementId).getOrNull()
                if (definition != null && definition.xpReward > 0) {
                    addXP(userId, definition.xpReward)
                    Log.d(TAG, "Added ${definition.xpReward} XP for completing achievement: ${definition.title}")
                }
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create/update achievement progress", e)
            Result.failure(e)
        }
    }

    /**
     * Sprawdza i aktualizuje postęp w osiągnięciach na podstawie aktualnych statystyk użytkownika
     */
    suspend fun checkAndUpdateAchievements(userId: String): Result<List<AchievementProgress>> {
        return try {
            Log.d(TAG, "Checking and updating achievements for user: $userId")

            val updatedProgresses = mutableListOf<AchievementProgress>()

            // Pobierz aktualne statystyki
            val statsResult = getUserStats(userId)
            if (statsResult.isFailure) {
                return Result.failure(statsResult.exceptionOrNull() ?: Exception("Failed to get user stats"))
            }
            val stats = statsResult.getOrNull()!!

            // Sprawdź osiągnięcia związane z liczbą treningów
            val workoutCountAchievements = listOf(
                "first_workout" to 1,
                "workout_count_10" to 10,
                "workout_count_25" to 25,
                "workout_count_50" to 50
            )

            workoutCountAchievements.forEach { (achievementId, targetValue) ->
                if (stats.totalWorkoutsCompleted >= targetValue) {
                    val result = createOrUpdateAchievementProgress(
                        userId,
                        achievementId,
                        stats.totalWorkoutsCompleted,
                        isCompleted = true
                    )
                    if (result.isSuccess) {
                        updatedProgresses.add(result.getOrNull()!!)
                    }
                } else {
                    val result = createOrUpdateAchievementProgress(
                        userId,
                        achievementId,
                        stats.totalWorkoutsCompleted,
                        isCompleted = false
                    )
                    if (result.isSuccess) {
                        updatedProgresses.add(result.getOrNull()!!)
                    }
                }
            }

            // Sprawdź osiągnięcia związane z serią treningów
            val streakAchievements = listOf(
                "workout_streak_3" to 3,
                "workout_streak_7" to 7
            )

            streakAchievements.forEach { (achievementId, targetValue) ->
                if (stats.currentStreak >= targetValue) {
                    val result = createOrUpdateAchievementProgress(
                        userId,
                        achievementId,
                        stats.currentStreak,
                        isCompleted = true
                    )
                    if (result.isSuccess) {
                        updatedProgresses.add(result.getOrNull()!!)
                    }
                } else {
                    val result = createOrUpdateAchievementProgress(
                        userId,
                        achievementId,
                        stats.currentStreak,
                        isCompleted = false
                    )
                    if (result.isSuccess) {
                        updatedProgresses.add(result.getOrNull()!!)
                    }
                }
            }

            Log.d(TAG, "Updated ${updatedProgresses.size} achievement progresses")
            Result.success(updatedProgresses)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check and update achievements", e)
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje statystyki użytkownika po ukończeniu treningu.
     */
    suspend fun updateUserStatsAfterWorkout(
        userId: String,
        completedWorkout: CompletedWorkout
    ): Result<UserStats> {
        return try {
            // Pobierz użytkownika
            val user = userDao.getUserById(userId)
            if (user == null) {
                Log.e(TAG, "User not found for updating stats after workout")
                return Result.failure(Exception("User not found"))
            }

            // Pobierz statystyki
            val userStatsEntity = userStatsDao.getUserStatsById(user.statsId)
            if (userStatsEntity == null) {
                Log.e(TAG, "User stats not found for ID: ${user.statsId}")
                return Result.failure(Exception("User stats not found"))
            }

            // Konwertuj na model
            val stats = mappers.toModel(userStatsEntity)

            if (completedWorkout.endTime == null) {
                return Result.failure(Exception("Workout not completed"))
            }

            // Oblicz serię treningów
            var currentStreak = stats.currentStreak
            var longestStreak = stats.longestStreak

            // Jeśli to pierwszy trening lub ostatni trening był wczoraj, zwiększ serię
            if (stats.lastWorkoutDate == null) {
                currentStreak = 1
            } else {
                val lastWorkoutDay = stats.lastWorkoutDate.toDate().time / (24 * 60 * 60 * 1000)
                val today = completedWorkout.endTime.toDate().time / (24 * 60 * 60 * 1000)

                if (today - lastWorkoutDay == 1L) {
                    // Trening był wczoraj, zwiększ serię
                    currentStreak++
                } else if (today - lastWorkoutDay > 1L) {
                    // Seria przerwana, zacznij od 1
                    currentStreak = 1
                }
                // W przeciwnym przypadku (trening tego samego dnia) nie zmieniaj serii
            }

            // Aktualizuj najdłuższą serię jeśli potrzeba
            if (currentStreak > longestStreak) {
                longestStreak = currentStreak
            }

            // Aktualizuj statystyki ćwiczeń
            val updatedExerciseStats = updateExerciseStats(stats.exerciseStats, completedWorkout)

            // Aktualizuj statystyki kategorii treningów
            val workoutCategories = completedWorkout.exercises.map { it.category }.distinct()
            val updatedWorkoutTypeStats = mutableMapOf<String, WorkoutTypeStat>()
            workoutCategories.forEach { category ->
                val existingStat = stats.workoutTypeStats[category] ?: WorkoutTypeStat()
                updatedWorkoutTypeStats[category] = existingStat.copy(
                    count = existingStat.count + 1,
                    totalTime = existingStat.totalTime + completedWorkout.duration,
                    lastPerformedAt = completedWorkout.endTime
                )
            }

            // Aktualizuj model
            val updatedStats = stats.copy(
                totalWorkoutsCompleted = stats.totalWorkoutsCompleted + 1,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                lastWorkoutDate = completedWorkout.endTime,
                totalWorkoutTime = stats.totalWorkoutTime + completedWorkout.duration,
                workoutTypeStats = stats.workoutTypeStats + updatedWorkoutTypeStats,
                exerciseStats = updatedExerciseStats
            )

            // Zapisz lokalnie
            val updateResult = updateUserStats(updatedStats)
            if (updateResult.isFailure) {
                return updateResult
            }

            // Dodaj XP za ukończony trening
            addXP(userId, 50)

            // Sprawdź i zaktualizuj osiągnięcia (nowy system)
            checkAndUpdateAchievements(userId)

            // Sprawdź też osiągnięcia związane z długością treningu
            checkWorkoutDurationAchievements(userId, completedWorkout)

            Result.success(updatedStats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user stats after workout", e)
            Result.failure(e)
        }
    }

    /**
     * Sprawdza osiągnięcia związane z długością treningu
     */
    private suspend fun checkWorkoutDurationAchievements(userId: String, completedWorkout: CompletedWorkout) {
        try {
            val durationInSeconds = completedWorkout.duration / 1000 // Konwertuj z milisekund na sekundy

            // Osiągnięcie za godzinną sesję (3600 sekund)
            if (durationInSeconds >= 3600) {
                createOrUpdateAchievementProgress(
                    userId,
                    "workout_hour",
                    durationInSeconds.toInt(),
                    isCompleted = true
                )
            }

            // Osiągnięcie za dwugodzinną sesję (7200 sekund)
            if (durationInSeconds >= 7200) {
                createOrUpdateAchievementProgress(
                    userId,
                    "workout_2_hours",
                    durationInSeconds.toInt(),
                    isCompleted = true
                )
            }

            // Sprawdź czy to poranny trening (przed 10:00)
            val workoutHour = java.util.Calendar.getInstance().apply {
                time = completedWorkout.startTime.toDate()
            }.get(java.util.Calendar.HOUR_OF_DAY)

            if (workoutHour < 10) {
                // Pobierz aktualny postęp w porannych treningach
                val currentProgress = achievementRepository.getAchievementProgress(userId, "morning_bird").getOrNull()
                val currentCount = currentProgress?.currentValue ?: 0

                createOrUpdateAchievementProgress(
                    userId,
                    "morning_bird",
                    currentCount + 1,
                    isCompleted = currentCount + 1 >= 10
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking workout duration achievements", e)
        }
    }

    /**
     * Aktualizuje statystyki ćwiczeń na podstawie zakończonego treningu.
     */
    private suspend fun updateExerciseStats(
        currentStats: Map<String, ExerciseStat>,
        completedWorkout: CompletedWorkout
    ): Map<String, ExerciseStat> {
        val result = currentStats.toMutableMap()

        // Dla każdego ćwiczenia w treningu
        completedWorkout.exercises.forEach { exercise ->
            val existingStat = result[exercise.exerciseId] ?: ExerciseStat()

            // Oblicz nowe wartości
            val newCount = existingStat.count + 1
            val setCount = exercise.sets.size

            // Znajdź najlepszy wynik dla ciężaru i powtórzeń
            var bestWeight = existingStat.personalBestWeight
            var bestReps = existingStat.personalBestReps

            exercise.sets.forEach { set ->
                if (set.weight > bestWeight) {
                    bestWeight = set.weight
                }
                if (set.reps > bestReps) {
                    bestReps = set.reps
                }
            }

            // Oblicz średnie wartości
            val totalWeight = exercise.sets.sumOf { it.weight }
            val totalReps = exercise.sets.sumOf { it.reps }
            val avgWeight = if (exercise.sets.isNotEmpty()) totalWeight / exercise.sets.size else 0.0
            val avgReps = if (exercise.sets.isNotEmpty()) totalReps / exercise.sets.size else 0

            // Aktualizuj historię postępu (dodaj tylko najlepszy wynik z tego treningu)
            val progressHistory = existingStat.progressHistory.toMutableList()
            val bestSetInWorkout = exercise.sets.maxByOrNull { it.weight * it.reps }

            if (bestSetInWorkout != null && completedWorkout.endTime != null) {
                progressHistory.add(
                    WeightEntry(
                        date = completedWorkout.endTime,
                        weight = bestSetInWorkout.weight,
                        reps = bestSetInWorkout.reps
                    )
                )
            }

            // Ogranicz historię do ostatnich 50 wpisów
            val limitedHistory = progressHistory.sortedByDescending { it.date }.take(50)

            // Aktualizuj statystyki
            result[exercise.exerciseId] = existingStat.copy(
                count = newCount,
                personalBestWeight = bestWeight,
                personalBestReps = bestReps,
                averageWeight = (existingStat.averageWeight * existingStat.count + avgWeight) / newCount,
                averageReps = (existingStat.averageReps * existingStat.count + avgReps) / newCount,
                averageSets = (existingStat.averageSets * existingStat.count + setCount) / newCount,
                lastPerformedAt = completedWorkout.endTime,
                progressHistory = limitedHistory
            )

            // Sprawdź osiągnięcia związane z ciężarem w konkretnym ćwiczeniu
            checkExerciseWeightAchievements(completedWorkout.userId, exercise.exerciseId, bestWeight)
        }

        return result
    }

    /**
     * Sprawdza osiągnięcia związane z ciężarem w konkretnych ćwiczeniach
     */
    private suspend fun checkExerciseWeightAchievements(userId: String, exerciseId: String, weight: Double) {
        try {
            // Przykład: osiągnięcie za 100kg na wyciskaniu sztangi leżąc
            if (exerciseId == "bench-press" && weight >= 100.0) {
                createOrUpdateAchievementProgress(
                    userId,
                    "bench_press_100kg",
                    weight.toInt(),
                    isCompleted = true
                )
            }

            // Tu można dodać więcej osiągnięć dla różnych ćwiczeń
            // np. squat 200kg, deadlift 250kg, itp.

        } catch (e: Exception) {
            Log.e(TAG, "Error checking exercise weight achievements", e)
        }
    }

    /**
     * Czyści wszystkie dane użytkownika z lokalnej bazy danych po wylogowaniu.
     * Ta metoda powinna być wywołana przed zalogowaniem nowego użytkownika,
     * aby uniknąć problemów z danymi poprzedniego użytkownika.
     */
    suspend fun clearAllUserData(): Result<Unit> {
        return try {
            Log.d(TAG, "Starting to clear all user data from local database")

            // 1. Zatrzymaj synchronizację
            Log.d(TAG, "Clearing sync data")
            syncManager.clearAllData()

            // 2. Wyczyść dane podstawowe użytkownika
            Log.d(TAG, "Clearing basic user data")
            userDao.clearAllUsers()
            userAuthDao.clearAllUserAuth()
            userProfileDao.clearAllUserProfiles()
            userStatsDao.clearAllUserStats()
            userAchievementDao.clearAllUserAchievements()

            // 3. Wyczyść dane treningowe
            Log.d(TAG, "Clearing workout data")
            val workoutClearResult = workoutRepository.clearLocalData()
            if (workoutClearResult.isFailure) {
                Log.w(TAG, "Failed to clear workout data")
            }

            // 4. Wyczyść kategorie treningowe (tylko użytkownika, zachowaj domyślne)
            Log.d(TAG, "Clearing user workout categories")
            val categoryClearResult = workoutCategoryRepository.clearLocalData()
            if (categoryClearResult.isFailure) {
                Log.w(TAG, "Failed to clear workout categories")
            }

            // 5. Wyczyść postępy osiągnięć (definicje pozostają)
            Log.d(TAG, "Clearing achievement progress data")
            val achievementClearResult = achievementRepository.clearLocalProgressData()
            if (achievementClearResult.isFailure) {
                Log.w(TAG, "Failed to clear achievement data")
            }

            Log.d(TAG, "Successfully cleared all user data from local database")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error clearing user data from local database", e)
            Result.failure(e)
        }
    }

    /**
     * Przygotowuje aplikację dla nowego użytkownika po zalogowaniu.
     * Czyści stare dane i inicjalizuje podstawowe dane dla nowego użytkownika.
     */
    suspend fun prepareForNewUser(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Preparing application for new user: $userId")

            // 1. Wyczyść stare dane
            val clearResult = clearAllUserData()
            if (clearResult.isFailure) {
                Log.w(TAG, "Failed to clear old data, but continuing...")
            }

            // 2. Zainicjalizuj podstawowe dane dla nowego użytkownika
            // Przywróć domyślne kategorie treningowe (używając istniejącej metody z userId)
            try {
                workoutCategoryRepository.initializeDefaultCategories(userId)
                Log.d(TAG, "Successfully initialized default categories")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize default categories", e)
            }

            // Opcjonalnie: zainicjalizuj podstawowe definicje osiągnięć jeśli nie istnieją
            try {
                achievementRepository.getAllAchievementDefinitions()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize achievement definitions: ${e.message}")
            }

            Log.d(TAG, "Application prepared for new user: $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error preparing application for new user", e)
            Result.failure(e)
        }
    }

    /**
     * Alias dla createUser - zgodność z AuthViewModel
     */
    suspend fun createOrGetUser(firebaseUser: FirebaseUser): Result<User> {
        return createUser(firebaseUser)
    }
}