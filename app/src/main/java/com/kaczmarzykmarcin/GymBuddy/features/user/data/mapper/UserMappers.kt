package com.kaczmarzykmarcin.GymBuddy.features.user.data.mapper

import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.AchievementDefinitionEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.AchievementProgressEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.CompletedWorkoutEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.UserAuthEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.UserEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.UserProfileEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.UserStatsEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.WorkoutCategoryEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.WorkoutTemplateEntity
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model.AchievementDefinition
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model.AchievementProgress
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model.AchievementType
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.model.AchievementWithProgress
import com.kaczmarzykmarcin.GymBuddy.features.auth.domain.model.AuthProvider
import com.kaczmarzykmarcin.GymBuddy.features.auth.domain.model.UserAuth
import com.kaczmarzykmarcin.GymBuddy.features.profile.domain.model.UserProfile

import com.kaczmarzykmarcin.GymBuddy.features.user.domain.model.ExerciseStat
import com.kaczmarzykmarcin.GymBuddy.features.user.domain.model.User
import com.kaczmarzykmarcin.GymBuddy.features.user.domain.model.UserData
import com.kaczmarzykmarcin.GymBuddy.features.user.domain.model.UserStats
import com.kaczmarzykmarcin.GymBuddy.features.user.domain.model.WorkoutTypeStat
import com.kaczmarzykmarcin.GymBuddy.features.workouts.domain.model.CompletedExercise
import com.kaczmarzykmarcin.GymBuddy.features.workouts.domain.model.CompletedWorkout
import com.kaczmarzykmarcin.GymBuddy.features.workouts.domain.model.WorkoutCategory
import com.kaczmarzykmarcin.GymBuddy.features.workouts.domain.model.WorkoutTemplate

/**
 * Mapery do konwersji między modelami danych a encjami Room
 * ZAKTUALIZOWANE: Obsługa nowego systemu osiągnięć z AchievementWithProgress
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

    // ===== NOWY SYSTEM OSIĄGNIĘĆ - ZAKTUALIZOWANE MAPERY =====

    // AchievementDefinition
    fun toEntity(model: AchievementDefinition): AchievementDefinitionEntity {
        return AchievementDefinitionEntity(
            id = model.id,
            title = model.title,
            description = model.description,
            type = model.type.name,
            targetValue = model.targetValue,
            xpReward = model.xpReward,
            iconName = model.iconName,
            isActive = model.isActive,
            exerciseId = model.exerciseId,
            categoryId = model.categoryId,
            createdAt = model.createdAt.seconds
        )
    }

    fun toModel(entity: AchievementDefinitionEntity): AchievementDefinition {
        return AchievementDefinition(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            type = try {
                AchievementType.valueOf(entity.type)
            } catch (e: Exception) {
                AchievementType.FIRST_TIME
            },
            targetValue = entity.targetValue,
            xpReward = entity.xpReward,
            iconName = entity.iconName,
            isActive = entity.isActive,
            exerciseId = entity.exerciseId,
            categoryId = entity.categoryId,
            createdAt = Timestamp(entity.createdAt, 0)
        )
    }

    // AchievementProgress
    fun toEntity(model: AchievementProgress): AchievementProgressEntity {
        return AchievementProgressEntity(
            id = model.id,
            userId = model.userId,
            achievementId = model.achievementId,
            currentValue = model.currentValue,
            isCompleted = model.isCompleted,
            completedAt = model.completedAt?.seconds,
            lastUpdated = model.lastUpdated.seconds
        )
    }

    fun toModel(entity: AchievementProgressEntity): AchievementProgress {
        return AchievementProgress(
            id = entity.id,
            userId = entity.userId,
            achievementId = entity.achievementId,
            currentValue = entity.currentValue,
            isCompleted = entity.isCompleted,
            completedAt = entity.completedAt?.let { Timestamp(it, 0) },
            lastUpdated = Timestamp(entity.lastUpdated, 0)
        )
    }

    // WorkoutTemplate
    fun toEntity(model: WorkoutTemplate, needsSync: Boolean = false): WorkoutTemplateEntity {
        return WorkoutTemplateEntity(
            id = model.id,
            userId = model.userId,
            name = model.name,
            description = model.description,
            categoryId = model.categoryId,
            exercises = gson.toJson(model.exercises.map { it.toMap() }),
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
            needsSync = needsSync,
            lastSyncTime = System.currentTimeMillis()
        )
    }

    fun toModel(entity: WorkoutTemplateEntity): WorkoutTemplate {
        val completedExercises = try {
            val exercisesType = object : TypeToken<List<Map<String, Any?>>>() {}.type
            val exercisesMaps = gson.fromJson<List<Map<String, Any?>>>(entity.exercises, exercisesType)
            exercisesMaps?.map { CompletedExercise.fromMap(it) } ?: emptyList()
        } catch (e: Exception) {
            try {
                val exerciseIdsType = object : TypeToken<List<String>>() {}.type
                val exerciseIds = gson.fromJson<List<String>>(entity.exercises, exerciseIdsType)
                exerciseIds?.map {
                    CompletedExercise(exerciseId = it, name = "", category = "", sets = emptyList())
                } ?: emptyList()
            } catch (e2: Exception) {
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
            exercises = completedExercises,
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

    // ===== ZAKTUALIZOWANE METODY UserData dla NOWEGO SYSTEMU OSIĄGNIĘĆ =====

    /**
     * Tworzy UserData z przekazanymi modelami
     * ZAKTUALIZOWANE: Używa AchievementWithProgress zamiast UserAchievement
     */
    fun toUserData(
        user: User,
        userAuth: UserAuth,
        userProfile: UserProfile,
        userStats: UserStats,
        achievements: List<AchievementWithProgress> = emptyList()
    ): UserData {
        return UserData(
            user = user,
            auth = userAuth,
            profile = userProfile,
            stats = userStats,
            achievements = achievements // POPRAWKA: Używaj przekazanych osiągnięć
        )
    }

    /**
     * Tworzy UserData z encji Room
     * ZAKTUALIZOWANE: Dodano parametr achievements z AchievementWithProgress
     */
    fun toUserData(
        userEntity: UserEntity,
        userAuthEntity: UserAuthEntity,
        userProfileEntity: UserProfileEntity,
        userStatsEntity: UserStatsEntity,
        achievements: List<AchievementWithProgress> = emptyList()
    ): UserData {
        return UserData(
            user = toModel(userEntity),
            auth = toModel(userAuthEntity),
            profile = toModel(userProfileEntity),
            stats = toModel(userStatsEntity),
            achievements = achievements // POPRAWKA: Dodano parametr osiągnięć
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