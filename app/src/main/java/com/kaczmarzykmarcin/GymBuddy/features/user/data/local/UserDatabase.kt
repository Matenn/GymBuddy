package com.kaczmarzykmarcin.GymBuddy.features.user.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.converter.TimestampConverter
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.converter.MapConverter
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.UserDao
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.UserAchievementDao
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.UserProfileDao
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.UserStatsDao
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.UserAuthDao
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.WorkoutCategoryDao
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.WorkoutDao
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.WorkoutTemplateDao
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.UserEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.UserAchievementEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.UserProfileEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.UserStatsEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.UserAuthEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.CompletedWorkoutEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.WorkoutCategoryEntity
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.entity.WorkoutTemplateEntity

/**
 * Konfiguracja lokalnej bazy danych Room dla danych użytkownika
 */
@Database(
    entities = [
        UserEntity::class,
        UserAchievementEntity::class,
        UserProfileEntity::class,
        UserStatsEntity::class,
        UserAuthEntity::class,
        CompletedWorkoutEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutCategoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(TimestampConverter::class, MapConverter::class)
abstract class UserDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun userAchievementDao(): UserAchievementDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun userAuthDao(): UserAuthDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun workoutCategoryDao(): WorkoutCategoryDao

    companion object {
        private const val DATABASE_NAME = "user_database"

        @Volatile
        private var INSTANCE: UserDatabase? = null

        fun getInstance(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}