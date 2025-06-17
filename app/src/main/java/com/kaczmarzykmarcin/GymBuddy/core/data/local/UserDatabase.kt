package com.kaczmarzykmarcin.GymBuddy.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kaczmarzykmarcin.GymBuddy.core.data.local.converter.TimestampConverter
import com.kaczmarzykmarcin.GymBuddy.core.data.local.converter.MapConverter
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.AchievementDefinitionDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.AchievementProgressDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserAchievementDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserProfileDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserStatsDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserAuthDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.WorkoutCategoryDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.WorkoutDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.WorkoutTemplateDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.UserEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.UserAchievementEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.UserProfileEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.UserStatsEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.UserAuthEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.CompletedWorkoutEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.WorkoutCategoryEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.WorkoutTemplateEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.AchievementDefinitionEntity
import com.kaczmarzykmarcin.GymBuddy.core.data.local.entity.AchievementProgressEntity

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
        WorkoutCategoryEntity::class,
        AchievementDefinitionEntity::class,  // DODANE: Nowa encja dla definicji osiągnięć
        AchievementProgressEntity::class     // DODANE: Nowa encja dla postępu w osiągnięciach
    ],
    version = 5,  // ZWIĘKSZONA WERSJA: z 4 do 5 (dodane nowe tabele)
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
    abstract fun achievementDefinitionDao(): AchievementDefinitionDao
    abstract fun achievementProgressDao(): AchievementProgressDao

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