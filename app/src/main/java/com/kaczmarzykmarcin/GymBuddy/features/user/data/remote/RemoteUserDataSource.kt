package com.kaczmarzykmarcin.GymBuddy.features.user.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.kaczmarzykmarcin.GymBuddy.data.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Źródło danych zdalnych użytkownika (Firebase)
 */
@Singleton
class RemoteUserDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val usersCollection = firestore.collection("users")
    private val userAuthCollection = firestore.collection("userAuth")
    private val userProfilesCollection = firestore.collection("userProfiles")
    private val userStatsCollection = firestore.collection("userStats")
    private val userAchievementsCollection = firestore.collection("userAchievements")
    private val workoutTemplatesCollection = firestore.collection("workoutTemplates")
    private val completedWorkoutsCollection = firestore.collection("completedWorkouts")

    /**
     * Tworzy nowego użytkownika w Firebase
     */
    suspend fun createUser(firebaseUser: FirebaseUser): Result<User> {
        return try {
            // Check if user already exists to avoid duplication
            val existingUserDoc = usersCollection.document(firebaseUser.uid).get().await()
            if (existingUserDoc.exists()) {
                val existingUser = User.fromMap(existingUserDoc.id, existingUserDoc.data ?: mapOf())
                return Result.success(existingUser)
            }

            // Use transaction to create user documents
            val user = firestore.runTransaction { transaction ->
                // 1. Create UserAuth
                val userAuth = UserAuth(
                    email = firebaseUser.email ?: "",
                    provider = determineProvider(firebaseUser)
                )
                val authDocRef = userAuthCollection.document()
                val userAuthWithId = userAuth.copy(id = authDocRef.id)
                transaction.set(authDocRef, userAuthWithId.toMap())

                // 2. Create UserProfile
                val userProfile = UserProfile(
                    userId = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
                val profileDocRef = userProfilesCollection.document()
                val userProfileWithId = userProfile.copy(id = profileDocRef.id)
                transaction.set(profileDocRef, userProfileWithId.toMap())

                // 3. Create UserStats
                val userStats = UserStats(
                    userId = firebaseUser.uid
                )
                val statsDocRef = userStatsCollection.document()
                val userStatsWithId = userStats.copy(id = statsDocRef.id)
                transaction.set(statsDocRef, userStatsWithId.toMap())

                // 4. Create main User document linking all information
                val user = User(
                    id = firebaseUser.uid,
                    authId = userAuthWithId.id,
                    profileId = userProfileWithId.id,
                    statsId = userStatsWithId.id
                )
                transaction.set(usersCollection.document(firebaseUser.uid), user.toMap())

                // Return the user object so we can access it after the transaction
                user
            }.await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera użytkownika z Firebase
     */
    suspend fun getUser(userId: String): Result<User> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            if (userDoc.exists()) {
                val user = User.fromMap(userDoc.id, userDoc.data ?: mapOf())
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje użytkownika w Firebase
     */
    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera dane uwierzytelniania z Firebase
     */
    suspend fun getUserAuth(authId: String): Result<UserAuth> {
        return try {
            val userAuthDoc = userAuthCollection.document(authId).get().await()
            if (userAuthDoc.exists()) {
                val userAuth = UserAuth.fromMap(userAuthDoc.id, userAuthDoc.data ?: mapOf())
                Result.success(userAuth)
            } else {
                Result.failure(Exception("User auth not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje dane uwierzytelniania w Firebase
     */
    suspend fun updateUserAuth(userAuth: UserAuth): Result<Unit> {
        return try {
            userAuthCollection.document(userAuth.id).set(userAuth.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera profil użytkownika z Firebase
     */
    suspend fun getUserProfile(profileId: String): Result<UserProfile> {
        return try {
            val userProfileDoc = userProfilesCollection.document(profileId).get().await()
            if (userProfileDoc.exists()) {
                val userProfile = UserProfile.fromMap(userProfileDoc.id, userProfileDoc.data ?: mapOf())
                Result.success(userProfile)
            } else {
                Result.failure(Exception("User profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje profil użytkownika w Firebase
     */
    suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit> {
        return try {
            userProfilesCollection.document(userProfile.id).set(userProfile.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera statystyki użytkownika z Firebase
     */
    suspend fun getUserStats(statsId: String): Result<UserStats> {
        return try {
            val userStatsDoc = userStatsCollection.document(statsId).get().await()
            if (userStatsDoc.exists()) {
                val userStats = UserStats.fromMap(userStatsDoc.id, userStatsDoc.data ?: mapOf())
                Result.success(userStats)
            } else {
                Result.failure(Exception("User stats not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje statystyki użytkownika w Firebase
     */
    suspend fun updateUserStats(userStats: UserStats): Result<Unit> {
        return try {
            userStatsCollection.document(userStats.id).set(userStats.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera wszystkie dane użytkownika z Firebase
     */
    suspend fun getFullUserData(userId: String): Result<UserData> {
        return try {
            // 1. Get main User document
            val userResult = getUser(userId)
            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull() ?: Exception("Unknown error"))
            }
            val user = userResult.getOrNull()!!

            // 2. Get UserAuth
            val userAuthResult = getUserAuth(user.authId)
            if (userAuthResult.isFailure) {
                return Result.failure(userAuthResult.exceptionOrNull() ?: Exception("User auth data not found"))
            }
            val userAuth = userAuthResult.getOrNull()!!

            // 3. Get UserProfile
            val userProfileResult = getUserProfile(user.profileId)
            if (userProfileResult.isFailure) {
                return Result.failure(userProfileResult.exceptionOrNull() ?: Exception("User profile not found"))
            }
            val userProfile = userProfileResult.getOrNull()!!

            // 4. Get UserStats
            val userStatsResult = getUserStats(user.statsId)
            if (userStatsResult.isFailure) {
                return Result.failure(userStatsResult.exceptionOrNull() ?: Exception("User stats not found"))
            }
            val userStats = userStatsResult.getOrNull()!!

            // 5. Get user achievements
            val achievementsResult = getUserAchievements(userId)
            val achievements = achievementsResult.getOrDefault(emptyList())

            // 6. Create and return the full UserData object
            val userData = UserData(
                user = user,
                auth = userAuth,
                profile = userProfile,
                stats = userStats,
                achievements = achievements
            )

            Result.success(userData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera osiągnięcia użytkownika z Firebase
     */
    suspend fun getUserAchievements(userId: String): Result<List<UserAchievement>> {
        return try {
            val snapshot = userAchievementsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val achievements = snapshot.documents.map { doc ->
                UserAchievement.fromMap(doc.id, doc.data ?: mapOf())
            }

            Result.success(achievements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dodaje osiągnięcie użytkownika w Firebase
     */
    suspend fun addUserAchievement(userAchievement: UserAchievement): Result<UserAchievement> {
        return try {
            val docRef = userAchievementsCollection.document()
            val achievementWithId = userAchievement.copy(id = docRef.id)
            docRef.set(achievementWithId.toMap()).await()
            Result.success(achievementWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sprawdza, czy użytkownik ma dane osiągnięcie
     */
    suspend fun hasAchievement(userId: String, achievementId: Int): Boolean {
        return try {
            val snapshot = userAchievementsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("achievementId", achievementId)
                .limit(1)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Pobiera szablony treningów użytkownika z Firebase
     */
    suspend fun getUserWorkoutTemplates(userId: String): Result<List<WorkoutTemplate>> {
        return try {
            val snapshot = workoutTemplatesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val templates = snapshot.documents.map { doc ->
                WorkoutTemplate.fromMap(doc.id, doc.data ?: mapOf())
            }

            Result.success(templates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera szablon treningu z Firebase
     */
    suspend fun getWorkoutTemplate(templateId: String): Result<WorkoutTemplate> {
        return try {
            val doc = workoutTemplatesCollection.document(templateId).get().await()

            if (doc.exists()) {
                val template = WorkoutTemplate.fromMap(doc.id, doc.data ?: mapOf())
                Result.success(template)
            } else {
                Result.failure(Exception("Workout template not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tworzy szablon treningu w Firebase
     */
    suspend fun createWorkoutTemplate(template: WorkoutTemplate): Result<WorkoutTemplate> {
        return try {
            val docRef = workoutTemplatesCollection.document()
            val templateWithId = template.copy(id = docRef.id)
            docRef.set(templateWithId.toMap()).await()
            Result.success(templateWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje szablon treningu w Firebase
     */
    suspend fun updateWorkoutTemplate(template: WorkoutTemplate): Result<Unit> {
        return try {
            workoutTemplatesCollection.document(template.id).set(template.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Usuwa szablon treningu z Firebase
     */
    suspend fun deleteWorkoutTemplate(templateId: String): Result<Unit> {
        return try {
            workoutTemplatesCollection.document(templateId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera historię treningów użytkownika z Firebase
     */
    suspend fun getUserWorkoutHistory(userId: String): Result<List<CompletedWorkout>> {
        return try {
            val snapshot = completedWorkoutsCollection
                .whereEqualTo("userId", userId)
                .whereNotEqualTo("endTime", null)
                .get()
                .await()

            val workouts = snapshot.documents.map { doc ->
                CompletedWorkout.fromMap(doc.id, doc.data ?: mapOf())
            }

            Result.success(workouts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera aktywny trening użytkownika z Firebase
     */
    suspend fun getActiveWorkout(userId: String): Result<CompletedWorkout?> {
        return try {
            val snapshot = completedWorkoutsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("endTime", null)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val doc = snapshot.documents.first()
                val workout = CompletedWorkout.fromMap(doc.id, doc.data ?: mapOf())
                Result.success(workout)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tworzy nowy trening w Firebase
     */
    suspend fun createWorkout(workout: CompletedWorkout): Result<CompletedWorkout> {
        return try {
            val docRef = completedWorkoutsCollection.document()
            val workoutWithId = workout.copy(id = docRef.id)
            docRef.set(workoutWithId.toMap()).await()
            Result.success(workoutWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aktualizuje trening w Firebase
     */
    suspend fun updateWorkout(workout: CompletedWorkout): Result<Unit> {
        return try {
            completedWorkoutsCollection.document(workout.id).set(workout.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera szczegóły zakończonego treningu z Firebase.
     */
    suspend fun getCompletedWorkout(workoutId: String): Result<CompletedWorkout> {
        return try {
            val doc = completedWorkoutsCollection.document(workoutId).get().await()

            if (doc.exists()) {
                val workout = CompletedWorkout.fromMap(doc.id, doc.data ?: mapOf())
                Result.success(workout)
            } else {
                Result.failure(Exception("Workout not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Usuwa trening z Firebase
     */
    suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        return try {
            completedWorkoutsCollection.document(workoutId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Określa dostawcę uwierzytelniania na podstawie danych FirebaseUser
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