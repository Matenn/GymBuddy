package com.kaczmarzykmarcin.GymBuddy.features.user.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kaczmarzykmarcin.GymBuddy.data.model.*
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.*

/**
 * Mapery do konwersji między modelami danych a encjami Room
 */
class UserMappers(private val gson: Gson) {

    // User
    fun toEntity(model: User, needsSync: Boolean = false): UserEntity {
        return UserEntity(
            id = model.id,
            authId = model.authId,
            profileId = model.profileId,
            statsId = model.statsId,
            needsSync = needsSync,
            lastSyncTime = System.currentTimeMillis()
        )
    }

    fun toModel(entity: UserEntity): User {
        return User(
            id = entity.id,
            authId = entity.authId,
            profileId = entity.profileId,
            statsId = entity.statsId
        )
    }

    // UserAuth
    fun toEntity(model: UserAuth, needsSync: Boolean = false): UserAuthEntity {
        return UserAuthEntity(
            id = model.id,
            email = model.email,
            provider = model.provider.name,
            createdAt = model.createdAt,
            lastLogin = model.lastLogin,
            language = model.language,
            needsSync = needsSync,
            lastSyncTime = System.currentTimeMillis()
        )
    }

    fun toModel(entity: UserAuthEntity): UserAuth {
        return UserAuth(
            id = entity.id,
            email = entity.email,
            provider = try {
                AuthProvider.valueOf(entity.provider)
            } catch (e: Exception) {
                AuthProvider.EMAIL
            },
            createdAt = entity.createdAt,
            lastLogin = entity.lastLogin,
            language = entity.language
        )
    }

    // UserProfile
    fun toEntity(model: UserProfile, needsSync: Boolean = false): UserProfileEntity {
        return UserProfileEntity(
            id = model.id,
            userId = model.userId,
            displayName = model.displayName,
            photoUrl = model.photoUrl,
            favoriteBodyPart = model.favoriteBodyPart,
            needsSync = needsSync,
            lastSyncTime = System.currentTimeMillis()
        )
    }

    fun toModel(entity: UserProfileEntity): UserProfile {
        return UserProfile(
            id = entity.id,
            userId = entity.userId,
            displayName = entity.displayName,
            photoUrl = entity.photoUrl,
            favoriteBodyPart = entity.favoriteBodyPart
        )
    }

    // UserStats
    fun toEntity(model: UserStats, needsSync: Boolean = false): UserStatsEntity {
        return UserStatsEntity(
            id = model.id,
            userId = model.userId,
            level = model.level,
            xp = model.xp,
            totalWorkoutsCompleted = model.totalWorkoutsCompleted,
            longestStreak = model.longestStreak,
            currentStreak = model.currentStreak,
            lastWorkoutDate = model.lastWorkoutDate,
            totalWorkoutTime = model.totalWorkoutTime,
            workoutTypeStats = gson.toJson(model.workoutTypeStats.mapValues { it.value.toMap() }),
            exerciseStats = gson.toJson(model.exerciseStats.mapValues { it.value.toMap() }),
            needsSync = needsSync,
            lastSyncTime = System.currentTimeMillis()
        )
    }

    fun toModel(entity: UserStatsEntity): UserStats {
        val workoutTypeStatsType = object : TypeToken<Map<String, Map<String, Any?>>>() {}.type
        val workoutTypeStatsMap = gson.fromJson<Map<String, Map<String, Any?>>>(entity.workoutTypeStats, workoutTypeStatsType) ?: mapOf()

        val workoutTypeStats = workoutTypeStatsMap.mapValues {
            WorkoutTypeStat.fromMap(it.value)
        }

        val exerciseStatsType = object : TypeToken<Map<String, Map<String, Any?>>>() {}.type
        val exerciseStatsMap = gson.fromJson<Map<String, Map<String, Any?>>>(entity.exerciseStats, exerciseStatsType) ?: mapOf()

        val exerciseStats = exerciseStatsMap.mapValues {
            ExerciseStat.fromMap(it.value)
        }

        return UserStats(
            id = entity.id,
            userId = entity.userId,
            level = entity.level,
            xp = entity.xp,
            totalWorkoutsCompleted = entity.totalWorkoutsCompleted,
            longestStreak = entity.longestStreak,
            currentStreak = entity.currentStreak,
            lastWorkoutDate = entity.lastWorkoutDate,
            totalWorkoutTime = entity.totalWorkoutTime,
            workoutTypeStats = workoutTypeStats,
            exerciseStats = exerciseStats
        )
    }

    // UserAchievement
    fun toEntity(model: UserAchievement, needsSync: Boolean = false): UserAchievementEntity {
        return UserAchievementEntity(
            id = model.id,
            userId = model.userId,
            achievementId = model.achievementId,
            earnedAt = model.earnedAt,
            needsSync = needsSync,
            lastSyncTime = System.currentTimeMillis()
        )
    }

    fun toModel(entity: UserAchievementEntity): UserAchievement {
        return UserAchievement(
            id = entity.id,
            userId = entity.userId,
            achievementId = entity.achievementId,
            earnedAt = entity.earnedAt
        )
    }

    // WorkoutTemplate - Zmienione aby obsługiwać List<CompletedExercise> zamiast List<String>
    fun toEntity(model: WorkoutTemplate, needsSync: Boolean = false): WorkoutTemplateEntity {
        return WorkoutTemplateEntity(
            id = model.id,
            userId = model.userId,
            name = model.name,
            description = model.description,
            categoryId = model.categoryId,
            exercises = gson.toJson(model.exercises.map { it.toMap() }), // Serializacja do JSON
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
            needsSync = needsSync,
            lastSyncTime = System.currentTimeMillis()
        )
    }

    fun toModel(entity: WorkoutTemplateEntity): WorkoutTemplate {
        // Add robust error handling for JSON parsing
        val completedExercises = try {
            val exercisesType = object : TypeToken<List<Map<String, Any?>>>() {}.type
            val exercisesMaps = gson.fromJson<List<Map<String, Any?>>>(entity.exercises, exercisesType)
            exercisesMaps?.map { CompletedExercise.fromMap(it) } ?: emptyList()
        } catch (e: Exception) {
            // If parsing fails, try to determine what format we have
            try {
                // Maybe it's a list of strings (exercise IDs) instead of a list of objects?
                val exerciseIdsType = object : TypeToken<List<String>>() {}.type
                val exerciseIds = gson.fromJson<List<String>>(entity.exercises, exerciseIdsType)

                // If so, convert each ID to a minimal CompletedExercise
                exerciseIds?.map {
                    CompletedExercise(exerciseId = it, name = "", category = "", sets = emptyList())
                } ?: emptyList()
            } catch (e2: Exception) {
                // If all parsing attempts fail, log the error and return empty list
                android.util.Log.e("UserMappers", "Failed to parse exercises JSON: ${entity.exercises}", e2)
                emptyList()
            }
        }

        return WorkoutTemplate(
            id = entity.id,
            userId = entity.userId,
            name = entity.name,
            description = entity.description,
            categoryId = entity.categoryId,
            exercises = completedExercises, // The safely parsed list
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    // CompletedWorkout
    fun toEntity(model: CompletedWorkout, needsSync: Boolean = false): CompletedWorkoutEntity {
        return CompletedWorkoutEntity(
            id = model.id,
            userId = model.userId,
            name = model.name,
            templateId = model.templateId,
            categoryId = model.categoryId,
            startTime = model.startTime,
            endTime = model.endTime,
            duration = model.duration,
            exercises = gson.toJson(model.exercises.map { it.toMap() }),
            needsSync = needsSync,
            lastSyncTime = System.currentTimeMillis()
        )
    }

    fun toModel(entity: CompletedWorkoutEntity): CompletedWorkout {
        val exercisesType = object : TypeToken<List<Map<String, Any?>>>() {}.type
        val exercisesMaps = gson.fromJson<List<Map<String, Any?>>>(entity.exercises, exercisesType) ?: emptyList()

        val completedExercises = exercisesMaps.map {
            CompletedExercise.fromMap(it)
        }

        return CompletedWorkout(
            id = entity.id,
            userId = entity.userId,
            name = entity.name,
            templateId = entity.templateId,
            categoryId = entity.categoryId,
            startTime = entity.startTime,
            endTime = entity.endTime,
            duration = entity.duration,
            exercises = completedExercises
        )
    }

    // UserData (composite model)
    fun toUserData(
        user: User,
        userAuth: UserAuth,
        userProfile: UserProfile,
        userStats: UserStats,
        achievements: List<UserAchievement>
    ): UserData {
        return UserData(
            user = user,
            auth = userAuth,
            profile = userProfile,
            stats = userStats,
            achievements = achievements
        )
    }

    fun toUserData(
        userEntity: UserEntity,
        userAuthEntity: UserAuthEntity,
        userProfileEntity: UserProfileEntity,
        userStatsEntity: UserStatsEntity,
        achievementEntities: List<UserAchievementEntity>
    ): UserData {
        return UserData(
            user = toModel(userEntity),
            auth = toModel(userAuthEntity),
            profile = toModel(userProfileEntity),
            stats = toModel(userStatsEntity),
            achievements = achievementEntities.map { toModel(it) }
        )
    }

    // WorkoutCategory
    fun toEntity(model: WorkoutCategory, needsSync: Boolean = false): WorkoutCategoryEntity {
        return WorkoutCategoryEntity(
            id = model.id,
            userId = model.userId,
            name = model.name,
            color = model.color,
            createdAt = model.createdAt,
            isDefault = model.isDefault,
            needsSync = needsSync,
            lastSyncTime = System.currentTimeMillis()
        )
    }

    fun toModel(entity: WorkoutCategoryEntity): WorkoutCategory {
        return WorkoutCategory(
            id = entity.id,
            userId = entity.userId,
            name = entity.name,
            color = entity.color,
            createdAt = entity.createdAt,
            isDefault = entity.isDefault
        )
    }
}