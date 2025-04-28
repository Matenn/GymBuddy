package com.kaczmarzykmarcin.GymBuddy.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.kaczmarzykmarcin.GymBuddy.data.repository.AchievementRepository
import com.kaczmarzykmarcin.GymBuddy.data.repository.ExerciseRepository
import com.kaczmarzykmarcin.GymBuddy.data.repository.UserRepository
import com.kaczmarzykmarcin.GymBuddy.data.repository.WorkoutRepository
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.ExerciseJsonParser
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.dao.ExerciseDao
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
    fun provideAchievementRepository(firestore: FirebaseFirestore): AchievementRepository {
        return AchievementRepository(firestore)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        achievementRepository: AchievementRepository
    ): UserRepository {
        return UserRepository(firestore, auth, achievementRepository)
    }

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        firestore: FirebaseFirestore,
        exerciseRepository: ExerciseRepository
    ): WorkoutRepository {
        return WorkoutRepository(firestore, exerciseRepository)
    }
}