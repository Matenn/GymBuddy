package com.kaczmarzykmarcin.GymBuddy.data.repository


import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.kaczmarzykmarcin.GymBuddy.data.model.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repozytorium do zarządzania danymi użytkownika, które integruje wszystkie podkomponenty.
 */
@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val achievementRepository: AchievementRepository
) {
    private val usersCollection = firestore.collection("users")
    private val userAuthCollection = firestore.collection("userAuth")
    private val userProfilesCollection = firestore.collection("userProfiles")
    private val userStatsCollection = firestore.collection("userStats")
    private val TAG = "UserRepository"

    /**
     * Tworzy nowego użytkownika z wszystkimi powiązanymi dokumentami.
     */
    suspend fun createUser(firebaseUser: FirebaseUser): Result<User> {
        return try {
            // Log to track the process
            Log.d(TAG, "Creating new user: ${firebaseUser.uid}")

            // Check if user already exists to avoid duplication
            val existingUserDoc = usersCollection.document(firebaseUser.uid).get().await()
            if (existingUserDoc.exists()) {
                Log.d(TAG, "User already exists, returning existing user")
                val existingUser = User.fromMap(existingUserDoc.id, existingUserDoc.data ?: mapOf())
                return Result.success(existingUser)
            }

            // Use transaction to create user documents
            val user = firestore.runTransaction { transaction ->
                // 1. Utwórz UserAuth
                val userAuth = UserAuth(
                    email = firebaseUser.email ?: "",
                    provider = determineProvider(firebaseUser),
                    createdAt = Timestamp.now(),
                    lastLogin = Timestamp.now()
                )
                val authDocRef = userAuthCollection.document()
                val userAuthWithId = userAuth.copy(id = authDocRef.id)
                transaction.set(authDocRef, userAuthWithId.toMap())
                Log.d(TAG, "Created UserAuth with ID: ${authDocRef.id}")

                // 2. Utwórz UserProfile
                val userProfile = UserProfile(
                    userId = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
                val profileDocRef = userProfilesCollection.document()
                val userProfileWithId = userProfile.copy(id = profileDocRef.id)
                transaction.set(profileDocRef, userProfileWithId.toMap())
                Log.d(TAG, "Created UserProfile with ID: ${profileDocRef.id}")

                // 3. Utwórz UserStats
                val userStats = UserStats(
                    userId = firebaseUser.uid
                )
                val statsDocRef = userStatsCollection.document()
                val userStatsWithId = userStats.copy(id = statsDocRef.id)
                transaction.set(statsDocRef, userStatsWithId.toMap())
                Log.d(TAG, "Created UserStats with ID: ${statsDocRef.id}")

                // 4. Utwórz główny dokument User łączący wszystkie informacje
                val user = User(
                    id = firebaseUser.uid,
                    authId = userAuthWithId.id,
                    profileId = userProfileWithId.id,
                    statsId = userStatsWithId.id
                )
                transaction.set(usersCollection.document(firebaseUser.uid), user.toMap())
                Log.d(TAG, "Created main User document with ID: ${firebaseUser.uid}")

                // Return the user object so we can access it after the transaction
                user
            }.await()

            // 5. Przyznaj osiągnięcie za pierwszy trening (outside the transaction)
            try {
                achievementRepository.addAchievementForUser(firebaseUser.uid, Achievement.FIRST_WORKOUT.id)
                Log.d(TAG, "Added first workout achievement")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add achievement but user was created", e)
                // Don't fail the whole operation if just the achievement fails
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create user", e)
            Result.failure(e)
        }
    }

    /**
     * Pobiera główny dokument User z wszystkimi powiązanymi dokumentami.
     */
    suspend fun getFullUserData(userId: String): Result<UserData> {
        return try {
            Log.d(TAG, "Getting full user data for: $userId")

            // 1. Pobierz główny dokument User
            val userDoc = usersCollection.document(userId).get().await()
            if (!userDoc.exists()) {
                Log.e(TAG, "User not found: $userId")
                return Result.failure(Exception("User not found"))
            }

            val user = User.fromMap(userDoc.id, userDoc.data ?: mapOf())
            Log.d(TAG, "Retrieved User: ${user.id}, Auth: ${user.authId}, Profile: ${user.profileId}, Stats: ${user.statsId}")

            // 2. Pobierz UserAuth
            val userAuthDoc = userAuthCollection.document(user.authId).get().await()
            if (!userAuthDoc.exists()) {
                Log.e(TAG, "User auth data not found: ${user.authId}")
                return Result.failure(Exception("User auth data not found"))
            }

            val userAuth = UserAuth.fromMap(userAuthDoc.id, userAuthDoc.data ?: mapOf())

            // 3. Pobierz UserProfile
            val userProfileDoc = userProfilesCollection.document(user.profileId).get().await()
            if (!userProfileDoc.exists()) {
                Log.e(TAG, "User profile not found: ${user.profileId}")
                return Result.failure(Exception("User profile not found"))
            }

            val userProfile = UserProfile.fromMap(userProfileDoc.id, userProfileDoc.data ?: mapOf())

            // 4. Pobierz UserStats
            val userStatsDoc = userStatsCollection.document(user.statsId).get().await()
            if (!userStatsDoc.exists()) {
                Log.e(TAG, "User stats not found: ${user.statsId}")
                return Result.failure(Exception("User stats not found"))
            }

            val userStats = UserStats.fromMap(userStatsDoc.id, userStatsDoc.data ?: mapOf())

            // 5. Pobierz osiągnięcia użytkownika
            val achievementsResult = achievementRepository.getUserAchievements(userId)
            val achievements = achievementsResult.getOrDefault(emptyList())
            Log.d(TAG, "Retrieved ${achievements.size} achievements for user")

            // 6. Utwórz i zwróć pełny obiekt UserData
            val userData = UserData(
                user = user,
                auth = userAuth,
                profile = userProfile,
                stats = userStats,
                achievements = achievements
            )

            Log.d(TAG, "Successfully retrieved full user data")
            Result.success(userData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get full user data", e)
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje czas ostatniego logowania.
     */
    suspend fun updateLastLogin(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Updating last login for user: $userId")
            val userResult = getUser(userId)
            if (userResult.isFailure) {
                Log.e(TAG, "Failed to get user for updating last login", userResult.exceptionOrNull())
                return Result.failure(userResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val user = userResult.getOrNull()!!
            val userAuthDoc = userAuthCollection.document(user.authId)
            val userAuthData = userAuthDoc.get().await().data

            if (userAuthData != null) {
                val userAuth = UserAuth.fromMap(user.authId, userAuthData)
                val updatedUserAuth = userAuth.updateLastLogin()
                userAuthDoc.update("lastLogin", updatedUserAuth.lastLogin).await()
                Log.d(TAG, "Last login updated successfully")
            } else {
                Log.e(TAG, "User auth data is null for ID: ${user.authId}")
                return Result.failure(Exception("User auth data not found"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update last login", e)
            Result.failure(e)
        }
    }


    /**
     * Pobiera podstawowy dokument User.
     */
    suspend fun getUser(userId: String): Result<User> {
        return try {
            Log.d(TAG, "Getting user document for: $userId")
            val userDoc = usersCollection.document(userId).get().await()
            if (userDoc.exists()) {
                val user = User.fromMap(userDoc.id, userDoc.data ?: mapOf())
                Log.d(TAG, "User document retrieved successfully")
                Result.success(user)
            } else {
                Log.e(TAG, "User not found: $userId")
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user", e)
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje profil użytkownika.
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any?>): Result<UserProfile> {
        return try {
            val userResult = getUser(userId)
            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val user = userResult.getOrNull()!!
            val profileDoc = userProfilesCollection.document(user.profileId)
            profileDoc.update(updates).await()

            val updatedProfileDoc = profileDoc.get().await()
            val updatedProfile = UserProfile.fromMap(
                updatedProfileDoc.id,
                updatedProfileDoc.data ?: mapOf()
            )

            Result.success(updatedProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dodaje XP użytkownikowi i aktualizuje poziom jeśli to konieczne.
     */
    suspend fun addUserXp(userId: String, xpAmount: Int): Result<UserStats> {
        return try {
            val userResult = getUser(userId)
            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val user = userResult.getOrNull()!!
            val statsDoc = userStatsCollection.document(user.statsId)
            val statsData = statsDoc.get().await().data

            if (statsData != null) {
                val stats = UserStats.fromMap(user.statsId, statsData)
                val updatedXp = stats.xp + xpAmount

                // Oblicz nowy poziom
                val currentLevel = stats.level
                val calculatedLevel = stats.calculateLevel()

                // Aktualizuj statystyki użytkownika
                val updates = mutableMapOf<String, Any>(
                    "xp" to updatedXp
                )

                // Jeśli poziom się zmienił, zaktualizuj go
                if (calculatedLevel > currentLevel) {
                    updates["level"] = calculatedLevel
                }

                statsDoc.update(updates).await()

                // Pobierz i zwróć zaktualizowane statystyki
                val updatedStatsDoc = statsDoc.get().await()
                val updatedStats = UserStats.fromMap(
                    updatedStatsDoc.id,
                    updatedStatsDoc.data ?: mapOf()
                )

                Result.success(updatedStats)
            } else {
                Result.failure(Exception("User stats not found"))
            }
        } catch (e: Exception) {
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
            val userResult = getUser(userId)
            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val user = userResult.getOrNull()!!
            val statsDoc = userStatsCollection.document(user.statsId)
            val statsData = statsDoc.get().await().data

            if (statsData != null && completedWorkout.endTime != null) {
                val stats = UserStats.fromMap(user.statsId, statsData)

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

                // Przygotuj aktualizacje
                val updates = mapOf(
                    "totalWorkoutsCompleted" to stats.totalWorkoutsCompleted + 1,
                    "currentStreak" to currentStreak,
                    "longestStreak" to longestStreak,
                    "lastWorkoutDate" to completedWorkout.endTime,
                    "totalWorkoutTime" to stats.totalWorkoutTime + completedWorkout.duration,
                    "workoutTypeStats" to (stats.workoutTypeStats + updatedWorkoutTypeStats).mapValues { it.value.toMap() },
                    "exerciseStats" to updatedExerciseStats.mapValues { it.value.toMap() }
                )

                // Aktualizuj dokument
                statsDoc.update(updates).await()

                // Dodaj XP za ukończony trening
                addUserXp(userId, 50)

                // Sprawdź osiągnięcia
                checkWorkoutAchievements(userId, stats, currentStreak)

                // Pobierz i zwróć zaktualizowane statystyki
                val updatedStatsDoc = statsDoc.get().await()
                val updatedStats = UserStats.fromMap(
                    updatedStatsDoc.id,
                    updatedStatsDoc.data ?: mapOf()
                )

                Result.success(updatedStats)
            } else {
                Result.failure(Exception("User stats not found or workout not completed"))
            }
        } catch (e: Exception) {
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
            if (!achievementRepository.hasAchievement(userId, Achievement.WORKOUT_STREAK_3.id)) {
                achievementRepository.addAchievementForUser(userId, Achievement.WORKOUT_STREAK_3.id)
                addUserXp(userId, Achievement.WORKOUT_STREAK_3.xpReward)
            }
        }

        // Sprawdź osiągnięcie za serię treningów (7 dni)
        if (currentStreak >= 7) {
            if (!achievementRepository.hasAchievement(userId, Achievement.WORKOUT_STREAK_7.id)) {
                achievementRepository.addAchievementForUser(userId, Achievement.WORKOUT_STREAK_7.id)
                addUserXp(userId, Achievement.WORKOUT_STREAK_7.xpReward)
            }
        }

        // Sprawdź osiągnięcie za 10 ukończonych treningów
        if (stats.totalWorkoutsCompleted + 1 >= 10) {
            if (!achievementRepository.hasAchievement(userId, Achievement.WORKOUT_COUNT_10.id)) {
                achievementRepository.addAchievementForUser(userId, Achievement.WORKOUT_COUNT_10.id)
                addUserXp(userId, Achievement.WORKOUT_COUNT_10.xpReward)
            }
        }

        // Sprawdź osiągnięcie za poranne treningi
        // (uproszczone - w rzeczywistości trzeba by sprawdzić godzinę dla każdego treningu)
        if (!achievementRepository.hasAchievement(userId, Achievement.MORNING_BIRD.id)) {
            // Logika sprawdzania porannych treningów mogłaby być bardziej złożona
            // Na potrzeby przykładu, przyznamy to osiągnięcie po 10 treningach
            if (stats.totalWorkoutsCompleted + 1 >= 10) {
                achievementRepository.addAchievementForUser(userId, Achievement.MORNING_BIRD.id)
                addUserXp(userId, Achievement.MORNING_BIRD.xpReward)
            }
        }
    }

    /**
     * Obserwuje zmiany w danych użytkownika jako Flow.
     */
    fun observeUserData(userId: String): Flow<UserData> = flow {
        try {
            val userDoc = usersCollection.document(userId).get().await()
            if (!userDoc.exists()) {
                throw Exception("User not found")
            }

            val user = User.fromMap(userDoc.id, userDoc.data ?: mapOf())

            val scope = CoroutineScope(Dispatchers.IO)

            // Ustaw nasłuchiwanie na wszystkie dokumenty
            val authListenerRegistration = userAuthCollection.document(user.authId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    scope.launch {
                        updateUserDataFlow(userId)
                    }
                }

            val profileListenerRegistration = userProfilesCollection.document(user.profileId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    scope.launch {
                        updateUserDataFlow(userId)
                    }
                }

            val statsListenerRegistration = userStatsCollection.document(user.statsId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    scope.launch {
                        updateUserDataFlow(userId)
                    }
                }

            // Czekaj na zakończenie flow
            kotlinx.coroutines.currentCoroutineContext().job.invokeOnCompletion {
                authListenerRegistration.remove()
                profileListenerRegistration.remove()
                statsListenerRegistration.remove()
            }

        } catch (e: Exception) {
            // Obsługa błędów
        }
    }

    /**
     * Aktualizuje flow danych użytkownika po zmianie w Firestore.
     */
    private suspend fun updateUserDataFlow(userId: String) {
        val userDataResult = getFullUserData(userId)
        if (userDataResult.isSuccess) {
            // trySend(userDataResult.getOrNull()!!)
            // Użyłbyś trySend w rzeczywistej implementacji, tutaj pominięte dla uproszczenia
        }
    }

    /**
     * Określa dostawcę uwierzytelniania na podstawie danych FirebaseUser.
     */
    private fun determineProvider(firebaseUser: FirebaseUser): AuthProvider {
        val providers = firebaseUser.providerData.mapNotNull {
            when (it.providerId) {
                "google.com" -> AuthProvider.GOOGLE
                "facebook.com" -> AuthProvider.FACEBOOK
                "password" -> AuthProvider.EMAIL
                else -> null
            }
        }

        return providers.firstOrNull() ?: AuthProvider.EMAIL
    }
}