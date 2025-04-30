package com.kaczmarzykmarcin.GymBuddy.di

import android.content.Context
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.UserDatabase
import com.kaczmarzykmarcin.GymBuddy.features.user.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserDatabaseModule {

    @Provides
    @Singleton
    fun provideUserDatabase(
        @ApplicationContext context: Context
    ): UserDatabase {
        return UserDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideUserDao(database: UserDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideUserAuthDao(database: UserDatabase): UserAuthDao {
        return database.userAuthDao()
    }

    @Provides
    @Singleton
    fun provideUserProfileDao(database: UserDatabase): UserProfileDao {
        return database.userProfileDao()
    }

    @Provides
    @Singleton
    fun provideUserStatsDao(database: UserDatabase): UserStatsDao {
        return database.userStatsDao()
    }

    @Provides
    @Singleton
    fun provideUserAchievementDao(database: UserDatabase): UserAchievementDao {
        return database.userAchievementDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutTemplateDao(database: UserDatabase): WorkoutTemplateDao {
        return database.workoutTemplateDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutDao(database: UserDatabase): WorkoutDao {
        return database.workoutDao()
    }
}