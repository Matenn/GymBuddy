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

@Dao
interface UserAchievementDao {
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
}