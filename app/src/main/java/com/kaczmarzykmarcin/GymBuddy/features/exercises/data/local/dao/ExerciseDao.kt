package com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object dla ćwiczeń w lokalnej bazie danych Room
 */
@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%'")
    suspend fun searchExercisesByName(query: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE category = :category ORDER BY name ASC")
    suspend fun getExercisesByCategory(category: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE :muscleGroup IN (primaryMuscles) OR :muscleGroup IN (secondaryMuscles) ORDER BY name ASC")
    suspend fun getExercisesByMuscleGroup(muscleGroup: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE equipment = :equipment ORDER BY name ASC")
    suspend fun getExercisesByEquipment(equipment: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE level = :level ORDER BY name ASC")
    suspend fun getExercisesByLevel(level: String): List<ExerciseEntity>

    @Query("SELECT DISTINCT category FROM exercises ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>

    @Query("SELECT DISTINCT equipment FROM exercises WHERE equipment IS NOT NULL ORDER BY equipment ASC")
    suspend fun getAllEquipment(): List<String>

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercisesForMuscleData(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()
}