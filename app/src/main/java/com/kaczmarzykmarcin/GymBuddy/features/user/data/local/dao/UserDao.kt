package com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.UserEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.UserAuthEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.UserProfileEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.UserStatsEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.UserAchievementEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.WorkoutTemplateEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.CompletedWorkoutEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.WorkoutCategoryEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.AchievementDefinitionEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.AchievementProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE needsSync = 1")
    suspend fun getUsersToSync(): List<UserEntity>

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)
}

@Dao
interface UserAuthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAuth(userAuth: UserAuthEntity)

    @Update
    suspend fun updateUserAuth(userAuth: UserAuthEntity)

    @Query("SELECT * FROM user_auth WHERE id = :authId")
    suspend fun getUserAuthById(authId: String): UserAuthEntity?

    @Query("SELECT * FROM user_auth WHERE needsSync = 1")
    suspend fun getUserAuthToSync(): List<UserAuthEntity>
}

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfileEntity)

    @Update
    suspend fun updateUserProfile(userProfile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles WHERE id = :profileId")
    suspend fun getUserProfileById(profileId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getUserProfileByUserId(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE needsSync = 1")
    suspend fun getUserProfilesToSync(): List<UserProfileEntity>
}

@Dao
interface UserStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(userStats: UserStatsEntity)

    @Update
    suspend fun updateUserStats(userStats: UserStatsEntity)

    @Query("SELECT * FROM user_stats WHERE id = :statsId")
    suspend fun getUserStatsById(statsId: String): UserStatsEntity?

    @Query("SELECT * FROM user_stats WHERE userId = :userId")
    suspend fun getUserStatsByUserId(userId: String): UserStatsEntity?

    @Query("SELECT * FROM user_stats WHERE needsSync = 1")
    suspend fun getUserStatsToSync(): List<UserStatsEntity>
}

// ZAKTUALIZOWANE DAO - zachowuje stary format dla kompatybilności
@Dao
interface UserAchievementDao {
    // STARE METODY (zachowane dla wstecznej kompatybilności z istniejącym kodem)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAchievement(userAchievement: UserAchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAchievements(userAchievements: List<UserAchievementEntity>)

    @Update
    suspend fun updateUserAchievement(userAchievement: UserAchievementEntity)

    @Query("SELECT * FROM user_achievements WHERE id = :achievementId")
    suspend fun getUserAchievementById(achievementId: String): UserAchievementEntity?

    @Query("SELECT * FROM user_achievements WHERE userId = :userId ORDER BY earnedAt DESC")
    suspend fun getUserAchievementsByUserId(userId: String): List<UserAchievementEntity>

    @Query("SELECT * FROM user_achievements WHERE userId = :userId AND achievementId = :achievementId LIMIT 1")
    suspend fun getUserAchievementByIdType(userId: String, achievementId: Int): UserAchievementEntity?

    @Query("SELECT * FROM user_achievements WHERE needsSync = 1")
    suspend fun getUserAchievementsToSync(): List<UserAchievementEntity>
}

@Dao
interface WorkoutTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutTemplate(workoutTemplate: WorkoutTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutTemplates(workoutTemplates: List<WorkoutTemplateEntity>)

    @Update
    suspend fun updateWorkoutTemplate(workoutTemplate: WorkoutTemplateEntity)

    @Query("SELECT * FROM workout_templates WHERE id = :templateId")
    suspend fun getWorkoutTemplateById(templateId: String): WorkoutTemplateEntity?

    @Query("SELECT * FROM workout_templates WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getWorkoutTemplatesByUserId(userId: String): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates WHERE needsSync = 1")
    suspend fun getWorkoutTemplatesToSync(): List<WorkoutTemplateEntity>

    @Query("DELETE FROM workout_templates WHERE id = :templateId")
    suspend fun deleteWorkoutTemplate(templateId: String)

    @Query("SELECT * FROM workout_templates WHERE userId = :userId AND categoryId = :categoryId ORDER BY updatedAt DESC")
    fun getWorkoutTemplatesByCategory(userId: String, categoryId: String): Flow<List<WorkoutTemplateEntity>>
}

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletedWorkout(completedWorkout: CompletedWorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletedWorkouts(completedWorkouts: List<CompletedWorkoutEntity>)

    @Update
    suspend fun updateCompletedWorkout(completedWorkout: CompletedWorkoutEntity)

    @Query("SELECT * FROM completed_workouts WHERE id = :workoutId")
    suspend fun getCompletedWorkoutById(workoutId: String): CompletedWorkoutEntity?

    @Query("SELECT * FROM completed_workouts WHERE userId = :userId AND endTime IS NOT NULL ORDER BY endTime DESC")
    fun getCompletedWorkoutsByUserId(userId: String): Flow<List<CompletedWorkoutEntity>>

    @Query("SELECT * FROM completed_workouts WHERE userId = :userId AND endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveWorkout(userId: String): CompletedWorkoutEntity?

    @Query("SELECT * FROM completed_workouts WHERE needsSync = 1")
    suspend fun getCompletedWorkoutsToSync(): List<CompletedWorkoutEntity>

    @Query("DELETE FROM completed_workouts WHERE id = :workoutId")
    suspend fun deleteCompletedWorkout(workoutId: String)

    @Query("SELECT * FROM completed_workouts WHERE userId = :userId AND categoryId = :categoryId AND endTime IS NOT NULL ORDER BY endTime DESC")
    fun getCompletedWorkoutsByCategory(userId: String, categoryId: String): Flow<List<CompletedWorkoutEntity>>
}

@Dao
interface WorkoutCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutCategory(workoutCategory: WorkoutCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutCategories(workoutCategories: List<WorkoutCategoryEntity>)

    @Update
    suspend fun updateWorkoutCategory(workoutCategory: WorkoutCategoryEntity)

    @Query("SELECT * FROM workout_categories WHERE id = :categoryId")
    suspend fun getWorkoutCategoryById(categoryId: String): WorkoutCategoryEntity?

    @Query("SELECT * FROM workout_categories WHERE userId = :userId OR isDefault = 1 ORDER BY name ASC")
    fun getWorkoutCategoriesByUserId(userId: String): Flow<List<WorkoutCategoryEntity>>

    @Query("SELECT * FROM workout_categories WHERE needsSync = 1")
    suspend fun getWorkoutCategoriesToSync(): List<WorkoutCategoryEntity>

    @Query("DELETE FROM workout_categories WHERE id = :categoryId AND isDefault = 0")
    suspend fun deleteWorkoutCategory(categoryId: String)
}

// NOWE DAO dla systemu osiągnięć

@Dao
interface AchievementDefinitionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievementDefinition(definition: AchievementDefinitionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievementDefinitions(definitions: List<AchievementDefinitionEntity>)

    @Update
    suspend fun updateAchievementDefinition(definition: AchievementDefinitionEntity)

    @Query("SELECT * FROM achievement_definitions WHERE id = :definitionId")
    suspend fun getAchievementDefinitionById(definitionId: String): AchievementDefinitionEntity?

    @Query("SELECT * FROM achievement_definitions WHERE isActive = 1 ORDER BY createdAt ASC")
    suspend fun getAllActiveDefinitions(): List<AchievementDefinitionEntity>

    @Query("SELECT * FROM achievement_definitions ORDER BY createdAt ASC")
    fun getAllDefinitions(): Flow<List<AchievementDefinitionEntity>>

    @Query("SELECT * FROM achievement_definitions WHERE type = :type AND isActive = 1 ORDER BY targetValue ASC")
    suspend fun getDefinitionsByType(type: String): List<AchievementDefinitionEntity>

    @Query("SELECT * FROM achievement_definitions WHERE exerciseId = :exerciseId AND isActive = 1")
    suspend fun getDefinitionsByExercise(exerciseId: String): List<AchievementDefinitionEntity>

    @Query("SELECT * FROM achievement_definitions WHERE categoryId = :categoryId AND isActive = 1")
    suspend fun getDefinitionsByCategory(categoryId: String): List<AchievementDefinitionEntity>

    @Query("DELETE FROM achievement_definitions WHERE id = :definitionId")
    suspend fun deleteAchievementDefinition(definitionId: String)

    @Query("UPDATE achievement_definitions SET isActive = 0 WHERE id = :definitionId")
    suspend fun deactivateAchievementDefinition(definitionId: String)
}

@Dao
interface AchievementProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievementProgress(progress: AchievementProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievementProgresses(progresses: List<AchievementProgressEntity>)

    @Update
    suspend fun updateAchievementProgress(progress: AchievementProgressEntity)

    @Query("SELECT * FROM achievement_progresses WHERE id = :progressId")
    suspend fun getAchievementProgressById(progressId: String): AchievementProgressEntity?

    @Query("SELECT * FROM achievement_progresses WHERE userId = :userId ORDER BY lastUpdated DESC")
    suspend fun getUserProgresses(userId: String): List<AchievementProgressEntity>

    @Query("SELECT * FROM achievement_progresses WHERE userId = :userId ORDER BY lastUpdated DESC")
    fun observeUserProgresses(userId: String): Flow<List<AchievementProgressEntity>>

    @Query("SELECT * FROM achievement_progresses WHERE userId = :userId AND achievementId = :achievementId LIMIT 1")
    suspend fun getUserProgressForAchievement(userId: String, achievementId: String): AchievementProgressEntity?

    @Query("SELECT * FROM achievement_progresses WHERE userId = :userId AND isCompleted = 1 ORDER BY completedAt DESC")
    suspend fun getUserCompletedProgresses(userId: String): List<AchievementProgressEntity>

    @Query("SELECT * FROM achievement_progresses WHERE userId = :userId AND isCompleted = 1 ORDER BY completedAt DESC LIMIT :limit")
    suspend fun getRecentCompletedProgresses(userId: String, limit: Int): List<AchievementProgressEntity>

    @Query("SELECT * FROM achievement_progresses WHERE userId = :userId AND isCompleted = 0 ORDER BY lastUpdated DESC")
    suspend fun getUserInProgressProgresses(userId: String): List<AchievementProgressEntity>

    @Query("DELETE FROM achievement_progresses WHERE id = :progressId")
    suspend fun deleteAchievementProgress(progressId: String)

    @Query("DELETE FROM achievement_progresses WHERE userId = :userId AND achievementId = :achievementId")
    suspend fun deleteUserProgressForAchievement(userId: String, achievementId: String)

    @Query("UPDATE achievement_progresses SET currentValue = :newValue, lastUpdated = :timestamp WHERE userId = :userId AND achievementId = :achievementId")
    suspend fun updateProgressValue(userId: String, achievementId: String, newValue: Int, timestamp: Long)

    @Query("UPDATE achievement_progresses SET isCompleted = 1, completedAt = :completedAt, lastUpdated = :timestamp WHERE userId = :userId AND achievementId = :achievementId")
    suspend fun markProgressAsCompleted(userId: String, achievementId: String, completedAt: Long, timestamp: Long)

    // Metody pomocnicze dla statystyk
    @Query("SELECT COUNT(*) FROM achievement_progresses WHERE userId = :userId AND isCompleted = 1")
    suspend fun getUserCompletedAchievementsCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM achievement_progresses WHERE userId = :userId AND isCompleted = 0 AND currentValue > 0")
    suspend fun getUserInProgressAchievementsCount(userId: String): Int

    @Query("SELECT SUM(currentValue) FROM achievement_progresses WHERE userId = :userId AND achievementId IN (SELECT id FROM achievement_definitions WHERE xpReward > 0)")
    suspend fun getUserTotalXPFromAchievements(userId: String): Int?
}