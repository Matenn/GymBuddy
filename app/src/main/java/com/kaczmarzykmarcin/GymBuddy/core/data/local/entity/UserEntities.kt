package com.kaczmarzykmarcin.GymBuddy.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.firebase.Timestamp
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.converter.StringListConverter

import com.kaczmarzykmarcin.GymBuddy.core.data.local.converter.TimestampConverter
import com.kaczmarzykmarcin.GymBuddy.core.data.local.converter.MapConverter

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val authId: String,
    val profileId: String,
    val statsId: String,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)

@Entity(tableName = "user_auth")
@TypeConverters(TimestampConverter::class)
data class UserAuthEntity(
    @PrimaryKey val id: String,
    val email: String,
    val provider: String, // Przechowywane jako String z enum AuthProvider
    val createdAt: Timestamp,
    val lastLogin: Timestamp,
    val language: String = "pl",
    val lastSyncTime: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val favoriteBodyPart: String = "",
    val lastSyncTime: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)

@Entity(tableName = "user_stats")
@TypeConverters(TimestampConverter::class, MapConverter::class, StringListConverter::class)
data class UserStatsEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val level: Int = 1,
    val xp: Int = 0,
    val totalWorkoutsCompleted: Int = 0,
    val longestStreak: Int = 0,
    val currentStreak: Int = 0,
    val lastWorkoutDate: Timestamp? = null,
    val totalWorkoutTime: Long = 0, // w minutach
    val workoutTypeStats: String, // Serializowany JSON Map<String, WorkoutTypeStat>
    val exerciseStats: String, // Serializowany JSON Map<String, ExerciseStat>
    val lastSyncTime: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)

@Entity(tableName = "user_achievements")
@TypeConverters(TimestampConverter::class)
data class UserAchievementEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val achievementId: Int,
    val earnedAt: Timestamp,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)

@Entity(tableName = "workout_templates")
@TypeConverters(TimestampConverter::class)
data class WorkoutTemplateEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val categoryId: String?, // Nowe pole
    val exercises: String, // Zmiana z List<String> na List<CompletedExercise>
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)

@Entity(tableName = "completed_workouts")
@TypeConverters(TimestampConverter::class, MapConverter::class)
data class CompletedWorkoutEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val templateId: String?,
    val categoryId: String?, // Nowe pole
    val startTime: Timestamp,
    val endTime: Timestamp?,
    val duration: Long,
    val exercises: String, // Serializowany JSON List<CompletedExercise>
    val lastSyncTime: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)

// Dodaj do UserEntities.kt
@Entity(tableName = "workout_categories")
@TypeConverters(TimestampConverter::class)
data class WorkoutCategoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val color: String,
    val createdAt: Timestamp,
    val isDefault: Boolean,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)

// AchievementDefinitionEntity.kt
@Entity(tableName = "achievement_definitions")
data class AchievementDefinitionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val type: String,
    val targetValue: Int,
    val xpReward: Int,
    val iconName: String,
    val isActive: Boolean,
    val exerciseId: String?,
    val categoryId: String?,
    val createdAt: Long
)

// AchievementProgressEntity.kt
@Entity(tableName = "achievement_progresses")
data class AchievementProgressEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val achievementId: String,
    val currentValue: Int,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val lastUpdated: Long
)