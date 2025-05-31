package com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.converter.StringListConverter
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.dao.ExerciseDao
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.entity.ExerciseEntity

/**
 * Konfiguracja lokalnej bazy danych Room dla ćwiczeń
 */
@Database(
    entities = [ExerciseEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class LocalExerciseDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao

    companion object {
        private const val DATABASE_NAME = "exercise_database"

        @Volatile
        private var INSTANCE: LocalExerciseDatabase? = null

        fun getInstance(context: Context): LocalExerciseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalExerciseDatabase::class.java,
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