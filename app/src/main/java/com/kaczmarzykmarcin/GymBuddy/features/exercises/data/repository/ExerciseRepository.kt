package com.kaczmarzykmarcin.GymBuddy.features.exercises.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.kaczmarzykmarcin.GymBuddy.features.exercises.domain.model.Exercise
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.ExerciseJsonParser
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.dao.ExerciseDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repozytorium do zarządzania ćwiczeniami, wykorzystuje lokalną bazę danych Room
 */
@Singleton
class ExerciseRepository @Inject constructor(
    private val context: Context,
    private val exerciseDao: ExerciseDao,
    private val exerciseJsonParser: ExerciseJsonParser
) {
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("exercise_prefs", Context.MODE_PRIVATE)
    }

    private companion object {
        private const val TAG = "ExerciseRepository"
        private const val KEY_DB_INITIALIZED = "db_initialized"
    }

    /**
     * Inicjalizuje bazę danych ćwiczeń z zasobów, jeśli jeszcze nie była zainicjalizowana
     */
    suspend fun initializeExerciseDatabase(): Result<Int> {
        try {
            // Sprawdź, czy baza danych już była inicjalizowana
            if (isDbInitialized()) {
                val exerciseCount = exerciseDao.getExerciseCount()
                if (exerciseCount > 0) {
                    Log.d(TAG, "Exercise database already initialized with $exerciseCount exercises")
                    return Result.success(exerciseCount)
                } else {
                    // Jeśli flaga jest ustawiona, ale baza jest pusta, resetujemy flagę
                    setDbInitialized(false)
                }
            }

            // Wyczyść bazę na wszelki wypadek
            exerciseDao.deleteAllExercises()

            // Baza jest pusta - zaimportuj ćwiczenia
            Log.d(TAG, "Initializing exercise database from assets")
            val exercisesJson = exerciseJsonParser.loadExercisesFromAssets()

            if (exercisesJson.isEmpty()) {
                return Result.failure(Exception("Failed to load exercises from assets"))
            }

            // Konwertuj dane JSON na encje Room i zapisz w bazie
            val entities = exercisesJson.map { exerciseJsonParser.convertToEntity(it) }
            exerciseDao.insertExercises(entities)

            Log.d(TAG, "Successfully imported ${entities.size} exercises to local database")

            // Oznacz bazę jako zainicjalizowaną
            setDbInitialized(true)

            return Result.success(entities.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize exercise database", e)
            return Result.failure(e)
        }
    }

    /**
     * Pobiera wszystkie ćwiczenia jako Flow z lokalnej bazy danych
     */
    fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercises().map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Pobiera ćwiczenie po ID
     */
    suspend fun getExerciseById(id: String): Result<Exercise> {
        return try {
            val entity = exerciseDao.getExerciseById(id)
            if (entity != null) {
                Result.success(entity.toModel())
            } else {
                Result.failure(Exception("Exercise not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera ćwiczenia po liście ID
     */
    suspend fun getExercisesByIds(ids: List<String>): Result<List<Exercise>> {
        return try {
            val exercises = mutableListOf<Exercise>()

            for (id in ids) {
                val exercise = exerciseDao.getExerciseById(id)
                if (exercise != null) {
                    exercises.add(exercise.toModel())
                }
            }

            Result.success(exercises)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera ćwiczenia według kategorii
     */
    suspend fun getExercisesByCategory(category: String): Result<List<Exercise>> {
        return try {
            val entities = exerciseDao.getExercisesByCategory(category)
            Result.success(entities.map { it.toModel() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera ćwiczenia według grupy mięśniowej
     */
    suspend fun getExercisesByMuscleGroup(muscleGroup: String): Result<List<Exercise>> {
        return try {
            val entities = exerciseDao.getExercisesByMuscleGroup(muscleGroup)
            Result.success(entities.map { it.toModel() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera ćwiczenia według sprzętu
     */
    suspend fun getExercisesByEquipment(equipment: String): Result<List<Exercise>> {
        return try {
            val entities = exerciseDao.getExercisesByEquipment(equipment)
            Result.success(entities.map { it.toModel() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera ćwiczenia pogrupowane według pierwszej litery (dla widoku alfabetycznego)
     */
    suspend fun getExercisesGroupedByLetter(): Result<Map<String, List<Exercise>>> {
        return try {
            val allExercises = exerciseDao.getAllExercises().map { entities ->
                entities.map { it.toModel() }
            }.first() // Użyj first() aby pobrać jedną wartość z Flow

            val grouped = allExercises.groupBy { it.sortLetter }
            Result.success(grouped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Wyszukuje ćwiczenia według nazwy
     */
    suspend fun searchExercises(query: String): Result<List<Exercise>> {
        return try {
            val entities = exerciseDao.searchExercisesByName(query)
            Result.success(entities.map { it.toModel() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera dostępne kategorie ćwiczeń
     */
    suspend fun getCategories(): Result<List<String>> {
        return try {
            val categories = exerciseDao.getAllCategories()
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera dostępne grupy mięśniowe
     */
    suspend fun getMuscleGroups(): Result<List<String>> {
        return try {
            val allExercises = exerciseDao.getAllExercisesForMuscleData()

            val primaryMuscles = allExercises.flatMap { it.primaryMuscles }
            val secondaryMuscles = allExercises.flatMap { it.secondaryMuscles }

            val allMuscles = (primaryMuscles + secondaryMuscles).distinct().sorted()
            Result.success(allMuscles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pobiera dostępne rodzaje sprzętu
     */
    suspend fun getEquipmentTypes(): Result<List<String>> {
        return try {
            val equipment = exerciseDao.getAllEquipment()
            Result.success(equipment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sprawdza czy baza danych została już zainicjalizowana
     */
    private fun isDbInitialized(): Boolean {
        return sharedPreferences.getBoolean(KEY_DB_INITIALIZED, false)
    }

    /**
     * Ustawia flagę inicjalizacji bazy danych
     */
    private fun setDbInitialized(initialized: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DB_INITIALIZED, initialized).apply()
    }

    /**
     * Resetuje flagę inicjalizacji, wymuszając ponowną inicjalizację przy następnym sprawdzeniu
     */
    fun resetInitializationFlag() {
        setDbInitialized(false)
    }

    /**
     * Konwertuje encję Room na model danych
     */
    private fun com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.entity.ExerciseEntity.toModel(): Exercise {
        return Exercise(
            id = id,
            name = name,
            category = category,
            imageUrl = imageUrl,
            sortLetter = sortLetter,
            force = force,
            level = level,
            mechanic = mechanic,
            equipment = equipment,
            primaryMuscles = primaryMuscles,
            secondaryMuscles = secondaryMuscles,
            instructions = instructions,
            notes = notes,
            tips = tips,
            images = images
        )
    }
}