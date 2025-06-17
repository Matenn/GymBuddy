package com.kaczmarzykmarcin.GymBuddy.core.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.kaczmarzykmarcin.GymBuddy.core.network.NetworkConnectivityManager
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.AchievementDefinitionDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.AchievementProgressDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserAchievementDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserAuthDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserProfileDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.UserStatsDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.WorkoutCategoryDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.WorkoutDao
import com.kaczmarzykmarcin.GymBuddy.core.data.local.dao.WorkoutTemplateDao
import com.kaczmarzykmarcin.GymBuddy.features.achievements.data.repository.AchievementRepository
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.ExerciseJsonParser
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.dao.ExerciseDao
import com.kaczmarzykmarcin.GymBuddy.features.user.data.mapper.UserMappers
import com.kaczmarzykmarcin.GymBuddy.features.user.data.remote.RemoteUserDataSource
import com.kaczmarzykmarcin.GymBuddy.features.user.data.sync.SyncManager
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.service.AchievementService
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.repository.ExerciseRepository
import com.kaczmarzykmarcin.GymBuddy.features.user.data.repository.UserRepository
import com.kaczmarzykmarcin.GymBuddy.features.workouts.data.repository.WorkoutCategoryRepository
import com.kaczmarzykmarcin.GymBuddy.features.workouts.data.repository.WorkoutRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideExerciseJsonParser(
        @ApplicationContext context: Context,
        gson: Gson
    ): ExerciseJsonParser {
        return ExerciseJsonParser(context, gson)
    }

    @Provides
    @Singleton
    fun provideExerciseRepository(
        @ApplicationContext context: Context,
        exerciseDao: ExerciseDao,
        exerciseJsonParser: ExerciseJsonParser
    ): ExerciseRepository {
        return ExerciseRepository(context, exerciseDao, exerciseJsonParser)
    }

    @Provides
    @Singleton
    fun provideUserMappers(gson: Gson): UserMappers {
        return UserMappers(gson)
    }

    @Provides
    @Singleton
    fun provideRemoteUserDataSource(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): RemoteUserDataSource {
        return RemoteUserDataSource(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context,
        userDao: UserDao,
        userAuthDao: UserAuthDao,
        userProfileDao: UserProfileDao,
        userStatsDao: UserStatsDao,
        userAchievementDao: UserAchievementDao,
        achievementDefinitionDao: AchievementDefinitionDao,
        achievementProgressDao: AchievementProgressDao,
        workoutTemplateDao: WorkoutTemplateDao,
        workoutDao: WorkoutDao,
        workoutCategoryDao: WorkoutCategoryDao,
        remoteDataSource: RemoteUserDataSource,
        mappers: UserMappers,
        networkManager: NetworkConnectivityManager
    ): SyncManager {
        return SyncManager(
            context, userDao, userAuthDao, userProfileDao, userStatsDao,
            userAchievementDao, achievementDefinitionDao, achievementProgressDao,
            workoutTemplateDao, workoutDao, workoutCategoryDao,
            remoteDataSource, mappers, networkManager
        )
    }

    @Provides
    @Singleton
    fun provideWorkoutCategoryRepository(
        workoutCategoryDao: WorkoutCategoryDao,
        remoteDataSource: RemoteUserDataSource,
        syncManager: SyncManager,
        networkManager: NetworkConnectivityManager,
        mappers: UserMappers
    ): WorkoutCategoryRepository {
        return WorkoutCategoryRepository(
            workoutCategoryDao, remoteDataSource, syncManager, networkManager, mappers
        )
    }

    @Provides
    @Singleton
    fun provideAchievementRepository(
        achievementDefinitionDao: AchievementDefinitionDao,
        achievementProgressDao: AchievementProgressDao,
        userAchievementDao: UserAchievementDao,
        remoteDataSource: RemoteUserDataSource,
        syncManager: SyncManager,
        networkManager: NetworkConnectivityManager,
        mappers: UserMappers
    ): AchievementRepository {
        return AchievementRepository(
            achievementDefinitionDao, achievementProgressDao, userAchievementDao,
            remoteDataSource, syncManager, networkManager, mappers
        )
    }

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        exerciseRepository: ExerciseRepository,
        workoutDao: WorkoutDao,
        workoutTemplateDao: WorkoutTemplateDao,
        remoteDataSource: RemoteUserDataSource,
        syncManager: SyncManager,
        networkManager: NetworkConnectivityManager,
        mappers: UserMappers
    ): WorkoutRepository {
        return WorkoutRepository(
            exerciseRepository, workoutDao, workoutTemplateDao,
            remoteDataSource, syncManager, networkManager, mappers
        )
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        auth: FirebaseAuth,
        remoteDataSource: RemoteUserDataSource,
        userDao: UserDao,
        userAuthDao: UserAuthDao,
        userProfileDao: UserProfileDao,
        userStatsDao: UserStatsDao,
        userAchievementDao: UserAchievementDao,
        workoutCategoryDao: WorkoutCategoryDao,
        syncManager: SyncManager,
        mappers: UserMappers,
        workoutRepository: WorkoutRepository,
        workoutCategoryRepository: WorkoutCategoryRepository,
        achievementRepository: AchievementRepository
    ): UserRepository {
        return UserRepository(
            auth, remoteDataSource, userDao, userAuthDao, userProfileDao,
            userStatsDao, userAchievementDao, workoutCategoryDao, syncManager,
            mappers, workoutRepository, workoutCategoryRepository, achievementRepository
        )
    }

    // SERWIS OSIĄGNIĘĆ - ZAKTUALIZOWANY
    @Provides
    @Singleton
    fun provideAchievementService(
        achievementRepository: AchievementRepository,
        workoutRepository: WorkoutRepository,
        userRepository: UserRepository
    ): AchievementService {
        return AchievementService(achievementRepository, workoutRepository, userRepository)
    }
}