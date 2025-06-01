// AchievementService.kt - poprawione sprawdzanie osiągnięć
package com.kaczmarzykmarcin.GymBuddy.features.achievements.domain

import android.util.Log
import com.google.firebase.Timestamp
import com.kaczmarzykmarcin.GymBuddy.data.model.*
import com.kaczmarzykmarcin.GymBuddy.data.repository.AchievementRepository
import com.kaczmarzykmarcin.GymBuddy.data.repository.WorkoutRepository
import com.kaczmarzykmarcin.GymBuddy.data.repository.UserRepository
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementService @Inject constructor(
    private val achievementRepository: AchievementRepository,
    private val workoutRepository: WorkoutRepository,
    private val userRepository: UserRepository
) {
    private val TAG = "AchievementService"

    /**
     * Inicjalizuje domyślne osiągnięcia w systemie
     * ZMIANA: Sprawdza czy już istnieją w lokalnej bazie
     */
    suspend fun initializeDefaultAchievements(): Result<Unit> {
        return try {
            // Sprawdź czy już są definicje w lokalnej bazie
            val existing = achievementRepository.getAllAchievementDefinitions().getOrNull() ?: emptyList()
            if (existing.isNotEmpty()) {
                Log.d(TAG, "Default achievements already exist (${existing.size} found)")
                return Result.success(Unit)
            }

            val defaultAchievements = listOf(
                AchievementDefinition(
                    id = "first_workout",
                    title = "Pierwszy trening",
                    description = "Wykonaj swój pierwszy trening",
                    type = AchievementType.WORKOUT_COUNT,
                    targetValue = 1,
                    xpReward = 50,
                    iconName = "🏃"
                ),
                AchievementDefinition(
                    id = "morning_bird",
                    title = "Poranny ptaszek",
                    description = "Wykonaj 10 porannych treningów (przed 10:00)",
                    type = AchievementType.MORNING_WORKOUTS,
                    targetValue = 10,
                    xpReward = 100,
                    iconName = "🌅"
                ),
                AchievementDefinition(
                    id = "workout_streak_3",
                    title = "Mini seria",
                    description = "Trenuj przez 3 dni z rzędu",
                    type = AchievementType.WORKOUT_STREAK,
                    targetValue = 3,
                    xpReward = 100,
                    iconName = "🔥"
                ),
                AchievementDefinition(
                    id = "workout_streak_7",
                    title = "Tygodniowa seria",
                    description = "Trenuj przez 7 dni z rzędu",
                    type = AchievementType.WORKOUT_STREAK,
                    targetValue = 7,
                    xpReward = 250,
                    iconName = "💪"
                ),
                AchievementDefinition(
                    id = "workout_count_10",
                    title = "Regularny bywalec",
                    description = "Ukończ 10 treningów",
                    type = AchievementType.WORKOUT_COUNT,
                    targetValue = 10,
                    xpReward = 200,
                    iconName = "⭐"
                ),
                AchievementDefinition(
                    id = "workout_hour",
                    title = "Godzinna sesja",
                    description = "Zakończ trening trwający ponad godzinę",
                    type = AchievementType.WORKOUT_DURATION,
                    targetValue = 3600, // 1 godzina w sekundach
                    xpReward = 150,
                    iconName = "⏱️"
                )
            )

            // Zapisz domyślne osiągnięcia w repozytorium (local-first)
            defaultAchievements.forEach { achievement ->
                achievementRepository.createOrUpdateAchievementDefinition(achievement)
            }

            Log.d(TAG, "Default achievements initialized successfully (${defaultAchievements.size} created)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize default achievements", e)
            Result.failure(e)
        }
    }

    /**
     * Sprawdza i aktualizuje postęp w osiągnięciach po ukończeniu treningu
     * ZMIANA: Lepsze error handling dla local-first
     */
    suspend fun checkAndUpdateAchievements(userId: String, completedWorkout: CompletedWorkout): List<AchievementWithProgress> {
        val newlyCompleted = mutableListOf<AchievementWithProgress>()

        try {
            // Pobierz wszystkie definicje osiągnięć z lokalnej bazy
            val definitionsResult = achievementRepository.getAllAchievementDefinitions()
            if (definitionsResult.isFailure) {
                Log.e(TAG, "Failed to get achievement definitions")
                return emptyList()
            }

            val definitions = definitionsResult.getOrNull() ?: emptyList()

            for (definition in definitions.filter { it.isActive }) {
                try {
                    val newProgress = when (definition.type) {
                        AchievementType.WORKOUT_COUNT -> checkWorkoutCountAchievement(userId, definition)
                        AchievementType.WORKOUT_STREAK -> checkWorkoutStreakAchievement(userId, definition)
                        AchievementType.MORNING_WORKOUTS -> checkMorningWorkoutAchievement(userId, definition, completedWorkout)
                        AchievementType.WORKOUT_DURATION -> checkWorkoutDurationAchievement(userId, definition, completedWorkout)
                        AchievementType.EXERCISE_WEIGHT -> checkExerciseWeightAchievement(userId, definition, completedWorkout)
                        AchievementType.FIRST_TIME -> null // POPRAWKA: Nie sprawdzamy FIRST_TIME automatycznie
                    }

                    newProgress?.let { progress ->
                        // Zapisz postęp (local-first)
                        val saveResult = achievementRepository.updateAchievementProgress(progress)

                        if (saveResult.isSuccess) {
                            // Jeśli osiągnięcie zostało ukończone, dodaj XP użytkownikowi
                            if (progress.isCompleted && progress.completedAt != null) {
                                addXPToUser(userId, definition.xpReward)
                                newlyCompleted.add(AchievementWithProgress(definition, progress))
                                Log.d(TAG, "Achievement completed: ${definition.title} (+${definition.xpReward} XP)")
                            } else {
                                Log.d(TAG, "Achievement progress updated: ${definition.title}")
                            }
                        } else {
                            Log.w(TAG, "Failed to save progress for achievement: ${definition.id}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking achievement ${definition.id}", e)
                    // Kontynuuj sprawdzanie innych osiągnięć
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking achievements", e)
        }

        return newlyCompleted
    }

    /**
     * NOWA METODA: Dodaje XP użytkownikowi (kompatybilna z local-first)
     */
    private suspend fun addXPToUser(userId: String, xpAmount: Int) {
        try {
            // Pobierz aktualne statystyki z UserRepository
            val userStatsResult = userRepository.getUserStats(userId)
            val currentStats = userStatsResult.getOrNull()

            if (currentStats != null) {
                val newXP = currentStats.xp + xpAmount
                val newLevel = calculateLevelFromXP(newXP)

                val updatedStats = currentStats.copy(
                    xp = newXP,
                    level = newLevel
                )

                // Aktualizuj statystyki (local-first)
                userRepository.updateUserStats(updatedStats)
                Log.d(TAG, "Added $xpAmount XP to user $userId (total: $newXP, level: $newLevel)")
            } else {
                Log.w(TAG, "Could not find user stats for user: $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add XP to user $userId", e)
        }
    }

    /**
     * NOWA METODA: Oblicza poziom na podstawie XP
     */
    private fun calculateLevelFromXP(xp: Int): Int {
        // Prosta formuła: poziom = sqrt(XP / 100) + 1
        return (kotlin.math.sqrt(xp.toDouble() / 100.0) + 1).toInt()
    }

    private suspend fun checkWorkoutCountAchievement(userId: String, definition: AchievementDefinition): AchievementProgress? {
        try {
            val workouts = workoutRepository.getUserWorkoutHistory(userId).first()
            val completedWorkouts = workouts.filter { it.endTime != null }
            val currentCount = completedWorkouts.size

            val existingProgress = achievementRepository.getAchievementProgress(userId, definition.id).getOrNull()

            return if (existingProgress == null || existingProgress.currentValue != currentCount) {
                val isCompleted = currentCount >= definition.targetValue
                AchievementProgress(
                    id = existingProgress?.id ?: "",
                    userId = userId,
                    achievementId = definition.id,
                    currentValue = currentCount,
                    isCompleted = isCompleted,
                    completedAt = if (isCompleted && existingProgress?.isCompleted != true) Timestamp.now() else existingProgress?.completedAt,
                    lastUpdated = Timestamp.now()
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking workout count achievement", e)
            return null
        }
    }

    private suspend fun checkWorkoutStreakAchievement(userId: String, definition: AchievementDefinition): AchievementProgress? {
        try {
            val workouts = workoutRepository.getUserWorkoutHistory(userId).first()
            val completedWorkouts = workouts
                .filter { it.endTime != null }
                .sortedByDescending { it.endTime?.seconds ?: 0 }

            val currentStreak = calculateCurrentStreak(completedWorkouts)

            val existingProgress = achievementRepository.getAchievementProgress(userId, definition.id).getOrNull()

            return if (existingProgress == null || existingProgress.currentValue != currentStreak) {
                val isCompleted = currentStreak >= definition.targetValue
                AchievementProgress(
                    id = existingProgress?.id ?: "",
                    userId = userId,
                    achievementId = definition.id,
                    currentValue = currentStreak,
                    isCompleted = isCompleted,
                    completedAt = if (isCompleted && existingProgress?.isCompleted != true) Timestamp.now() else existingProgress?.completedAt,
                    lastUpdated = Timestamp.now()
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking workout streak achievement", e)
            return null
        }
    }

    private suspend fun checkMorningWorkoutAchievement(
        userId: String,
        definition: AchievementDefinition,
        completedWorkout: CompletedWorkout
    ): AchievementProgress? {
        try {
            // Sprawdź czy trening był poranny (przed 10:00)
            val workoutTime = completedWorkout.endTime?.toDate()
            val isMorningWorkout = workoutTime?.let {
                val calendar = Calendar.getInstance()
                calendar.time = it
                calendar.get(Calendar.HOUR_OF_DAY) < 10
            } ?: false

            if (!isMorningWorkout) return null

            val existingProgress = achievementRepository.getAchievementProgress(userId, definition.id).getOrNull()
            val newCount = (existingProgress?.currentValue ?: 0) + 1

            val isCompleted = newCount >= definition.targetValue

            return AchievementProgress(
                id = existingProgress?.id ?: "",
                userId = userId,
                achievementId = definition.id,
                currentValue = newCount,
                isCompleted = isCompleted,
                completedAt = if (isCompleted && existingProgress?.isCompleted != true) Timestamp.now() else existingProgress?.completedAt,
                lastUpdated = Timestamp.now()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking morning workout achievement", e)
            return null
        }
    }

    private suspend fun checkWorkoutDurationAchievement(
        userId: String,
        definition: AchievementDefinition,
        completedWorkout: CompletedWorkout
    ): AchievementProgress? {
        try {
            if (completedWorkout.duration < definition.targetValue) return null

            val existingProgress = achievementRepository.getAchievementProgress(userId, definition.id).getOrNull()

            return if (existingProgress?.isCompleted != true) {
                AchievementProgress(
                    id = existingProgress?.id ?: "",
                    userId = userId,
                    achievementId = definition.id,
                    currentValue = definition.targetValue,
                    isCompleted = true,
                    completedAt = Timestamp.now(),
                    lastUpdated = Timestamp.now()
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking workout duration achievement", e)
            return null
        }
    }

    private suspend fun checkExerciseWeightAchievement(
        userId: String,
        definition: AchievementDefinition,
        completedWorkout: CompletedWorkout
    ): AchievementProgress? {
        try {
            if (definition.exerciseId == null) return null

            val exercise = completedWorkout.exercises.find { it.exerciseId == definition.exerciseId }
            val maxWeight = exercise?.sets?.maxOfOrNull { it.weight } ?: 0.0

            if (maxWeight < definition.targetValue) return null

            val existingProgress = achievementRepository.getAchievementProgress(userId, definition.id).getOrNull()

            return if (existingProgress?.isCompleted != true) {
                AchievementProgress(
                    id = existingProgress?.id ?: "",
                    userId = userId,
                    achievementId = definition.id,
                    currentValue = maxWeight.toInt(),
                    isCompleted = true,
                    completedAt = Timestamp.now(),
                    lastUpdated = Timestamp.now()
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking exercise weight achievement", e)
            return null
        }
    }

    /**
     * POPRAWKA: Usunięta metoda checkFirstTimeAchievement
     * Osiągnięcia typu FIRST_TIME nie są już sprawdzane automatycznie
     * Zamiast tego osiągnięcie "Pierwszy trening" używa typu WORKOUT_COUNT z targetValue = 1
     */

    private fun calculateCurrentStreak(workouts: List<CompletedWorkout>): Int {
        if (workouts.isEmpty()) return 0

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        var streak = 0
        val checkDate = Calendar.getInstance()
        checkDate.time = today.time

        // Sprawdzamy wstecz dzień po dniu
        for (i in 0 until 365) { // Maksymalnie rok wstecz
            val dayStart = checkDate.timeInMillis
            val dayEnd = dayStart + 24 * 60 * 60 * 1000 // +1 dzień

            val hasWorkoutThisDay = workouts.any { workout ->
                val workoutTime = (workout.endTime?.seconds ?: 0) * 1000
                workoutTime >= dayStart && workoutTime < dayEnd
            }

            if (hasWorkoutThisDay) {
                streak++
                checkDate.add(Calendar.DAY_OF_YEAR, -1)
            } else if (i == 0) {
                // Jeśli dzisiaj nie ma treningu, sprawdź wczoraj
                checkDate.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                // Przerwa w serii
                break
            }
        }

        return streak
    }

    /**
     * Pobiera postęp w osiągnięciach dla użytkownika
     * ZMIANA: Używa nowej metody z AchievementRepository
     */
    suspend fun getUserAchievements(userId: String): Result<List<AchievementWithProgress>> {
        return try {
            // Używa nowej metody która łączy definicje i postępy z lokalnej bazy
            achievementRepository.getUserAchievements(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user achievements", e)
            Result.failure(e)
        }
    }

    /**
     * NOWA METODA: Pobiera ostatnio zdobyte osiągnięcia
     */
    suspend fun getRecentlyCompletedAchievements(userId: String, limit: Int = 5): Result<List<AchievementWithProgress>> {
        return try {
            achievementRepository.getRecentlyCompletedAchievements(userId, limit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recently completed achievements", e)
            Result.failure(e)
        }
    }

    /**
     * NOWA METODA: Pobiera osiągnięcia w trakcie realizacji
     */
    suspend fun getInProgressAchievements(userId: String): Result<List<AchievementWithProgress>> {
        return try {
            achievementRepository.getInProgressAchievements(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get in progress achievements", e)
            Result.failure(e)
        }
    }
}