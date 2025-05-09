package com.kaczmarzykmarcin.GymBuddy.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kaczmarzykmarcin.GymBuddy.data.model.*
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.*
import com.kaczmarzykmarcin.GymBuddy.features.user.data.mapper.UserMappers
import com.kaczmarzykmarcin.GymBuddy.features.user.data.remote.RemoteUserDataSource
import com.kaczmarzykmarcin.GymBuddy.features.user.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val syncManager: SyncManager,
    private val mappers: UserMappers,
    private val workoutRepository: WorkoutRepository
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

            // Dodaj pierwsze osiągnięcie lokalnie i zsynchronizuj
            addAchievementForUser(firebaseUser.uid, Achievement.FIRST_WORKOUT.id)

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
        val user = userDao.getUserById(userId) ?: return null
        val userAuth = userAuthDao.getUserAuthById(user.authId) ?: return null
        val userProfile = userProfileDao.getUserProfileById(user.profileId) ?: return null
        val userStats = userStatsDao.getUserStatsById(user.statsId) ?: return null
        val achievements = userAchievementDao.getUserAchievementsByUserId(userId)

        return mappers.toUserData(user, userAuth, userProfile, userStats, achievements)
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

                val achievementEntities = userData.achievements.map {
                    mappers.toEntity(it)
                }
                userAchievementDao.insertUserAchievements(achievementEntities)
                Log.d(TAG, "Saved basic user data to local database")

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
     * Dodaje XP użytkownikowi i aktualizuje poziom jeśli to konieczne.
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

            // Oblicz nowy poziom
            val currentLevel = userStats.level
            val calculatedLevel = userStats.calculateLevel()

            // Aktualizuj model
            val updatedStats = userStats.copy(
                xp = updatedXp,
                level = if (calculatedLevel > currentLevel) calculatedLevel else currentLevel
            )

            // Zapisz lokalnie
            val updatedEntity = mappers.toEntity(updatedStats, true)
            userStatsDao.updateUserStats(updatedEntity)

            // Uruchom synchronizację w tle
            syncManager.requestSync()

            Result.success(updatedStats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add user XP", e)
            Result.failure(e)
        }
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

    /**
     * Dodaje osiągnięcie użytkownikowi.
     */
    suspend fun addAchievementForUser(userId: String, achievementId: Int): Result<UserAchievement> {
        return try {
            // Sprawdź, czy użytkownik już ma to osiągnięcie
            val existingAchievement = userAchievementDao.getUserAchievementByIdType(userId, achievementId)
            if (existingAchievement != null) {
                Log.d(TAG, "User already has achievement $achievementId")
                return Result.success(mappers.toModel(existingAchievement))
            }

            // Sprawdź, czy osiągnięcie istnieje
            val achievement = Achievement.getById(achievementId)
            if (achievement == null) {
                Log.e(TAG, "Achievement with ID $achievementId does not exist")
                return Result.failure(IllegalArgumentException("Achievement with ID $achievementId does not exist"))
            }

            // Utwórz nowe osiągnięcie
            val userAchievement = UserAchievement(
                userId = userId,
                achievementId = achievementId
            )

            // Zapisz lokalnie
            val entity = mappers.toEntity(userAchievement, true)
            val generatedId = java.util.UUID.randomUUID().toString()
            val entityWithId = entity.copy(id = generatedId)
            userAchievementDao.insertUserAchievement(entityWithId)

            // Uruchom synchronizację w tle
            syncManager.requestSync()

            // Przyznaj XP za osiągnięcie
            addUserXp(userId, achievement.xpReward)

            Result.success(mappers.toModel(entityWithId))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add achievement", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera wszystkie osiągnięcia użytkownika.
     */
    suspend fun getUserAchievements(userId: String): Result<List<UserAchievement>> {
        return try {
            val achievements = userAchievementDao.getUserAchievementsByUserId(userId)
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
            achievement != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for achievement", e)
            false
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
            val updatedEntity = mappers.toEntity(updatedStats, true)
            userStatsDao.updateUserStats(updatedEntity)

            // Uruchom synchronizację w tle
            syncManager.requestSync()

            // Dodaj XP za ukończony trening
            addUserXp(userId, 50)

            // Sprawdź osiągnięcia
            checkWorkoutAchievements(userId, stats, currentStreak)

            Result.success(updatedStats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user stats after workout", e)
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje statystyki ćwiczeń na podstawie zakończonego treningu.
     */
    private fun updateExerciseStats(
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
        }

        return result
    }

    /**
     * Sprawdza i przyznaje osiągnięcia związane z treningami.
     */
    private suspend fun checkWorkoutAchievements(
        userId: String,
        stats: UserStats,
        currentStreak: Int
    ) {
        // Sprawdź osiągnięcie za serię treningów (3 dni)
        if (currentStreak >= 3) {
            if (!hasAchievement(userId, Achievement.WORKOUT_STREAK_3.id)) {
                addAchievementForUser(userId, Achievement.WORKOUT_STREAK_3.id)
            }
        }

        // Sprawdź osiągnięcie za serię treningów (7 dni)
        if (currentStreak >= 7) {
            if (!hasAchievement(userId, Achievement.WORKOUT_STREAK_7.id)) {
                addAchievementForUser(userId, Achievement.WORKOUT_STREAK_7.id)
            }
        }

        // Sprawdź osiągnięcie za 10 ukończonych treningów
        if (stats.totalWorkoutsCompleted + 1 >= 10) {
            if (!hasAchievement(userId, Achievement.WORKOUT_COUNT_10.id)) {
                addAchievementForUser(userId, Achievement.WORKOUT_COUNT_10.id)
            }
        }

        // Sprawdź osiągnięcie za poranne treningi
        if (!hasAchievement(userId, Achievement.MORNING_BIRD.id)) {
            // Logika sprawdzania porannych treningów mogłaby być bardziej złożona
            // Na potrzeby przykładu, przyznamy to osiągnięcie po 10 treningach
            if (stats.totalWorkoutsCompleted + 1 >= 10) {
                addAchievementForUser(userId, Achievement.MORNING_BIRD.id)
            }
        }
    }
}