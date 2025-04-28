package com.kaczmarzykmarcin.GymBuddy.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.LocalExerciseDatabase
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.dao.ExerciseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalDatabaseModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .create()
    }

    @Provides
    @Singleton
    fun provideLocalExerciseDatabase(
        @ApplicationContext context: Context
    ): LocalExerciseDatabase {
        return LocalExerciseDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideExerciseDao(
        database: LocalExerciseDatabase
    ): ExerciseDao {
        return database.exerciseDao()
    }
}