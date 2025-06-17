package com.kaczmarzykmarcin.GymBuddy.features.exercises.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kaczmarzykmarcin.GymBuddy.features.exercises.domain.model.Exercise
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.entity.ExerciseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser plików JSON z bazy ćwiczeń yuhonas/free-exercise-db
 */
@Singleton
class ExerciseJsonParser @Inject constructor(
    private val context: Context,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "ExerciseJsonParser"
        private const val EXERCISES_DIR = "exercises"
    }

    /**
     * Klasa pomocnicza do parsowania formatu z yuhonas/free-exercise-db
     */
    data class ExerciseJson(
        @SerializedName("id") val id: String,
        @SerializedName("name") val name: String,
        @SerializedName("force") val force: String?,
        @SerializedName("level") val level: String?,
        @SerializedName("mechanic") val mechanic: String?,
        @SerializedName("equipment") val equipment: String?,
        @SerializedName("primaryMuscles") val primaryMuscles: List<String>,
        @SerializedName("secondaryMuscles") val secondaryMuscles: List<String>,
        @SerializedName("instructions") val instructions: List<String>,
        @SerializedName("category") val category: String?,
        @SerializedName("images") val images: List<String>?,
        @SerializedName("notes") val notes: List<String>?,
        @SerializedName("tips") val tips: List<String>?
    )

    /**
     * Wczytuje wszystkie ćwiczenia z katalogów w assets
     */
    suspend fun loadExercisesFromAssets(): List<ExerciseJson> = withContext(Dispatchers.IO) {
        try {
            val exercises = mutableListOf<ExerciseJson>()

            // Pobierz listę plików w katalogu exercises
            val exerciseFiles = context.assets.list(EXERCISES_DIR) ?: emptyArray()
            Log.d(TAG, "Found ${exerciseFiles.size} exercise files in assets")

            // Wczytaj każdy plik JSON
            exerciseFiles.forEach { fileName ->
                if (fileName.endsWith(".json")) {
                    try {
                        val jsonString = context.assets.open("$EXERCISES_DIR/$fileName").bufferedReader().use { it.readText() }
                        val exercise: ExerciseJson = gson.fromJson(jsonString, ExerciseJson::class.java)
                        exercises.add(exercise)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing exercise file $fileName", e)
                    }
                }
            }

            Log.d(TAG, "Successfully loaded ${exercises.size} exercises from JSON files")
            exercises
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load exercises from assets", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing exercise JSON", e)
            emptyList()
        }
    }

    /**
     * Konwertuje ExerciseJson na model Exercise
     */
    fun convertToModel(exerciseJson: ExerciseJson): Exercise {
        val sortLetter = when {
            exerciseJson.name.firstOrNull()?.isDigit() == true -> "#"
            else -> exerciseJson.name.firstOrNull()?.uppercase() ?: "A"
        }

        val category = exerciseJson.category ?: determineCategory(exerciseJson.primaryMuscles)

        // Usuwanie duplikacji w ścieżkach obrazów
        val imagesList = exerciseJson.images ?: emptyList()
        val firstImageUrl = if (imagesList.isNotEmpty()) {
            val firstImage = imagesList.first()
            // Usuń duplikację w ścieżce - jeśli zaczyna się od id, usuń pierwszy segment
            val cleanImagePath = if (firstImage.startsWith("${exerciseJson.id}/")) {
                firstImage.removePrefix("${exerciseJson.id}/")
            } else {
                firstImage
            }
            "file:///android_asset/exercise_images/${exerciseJson.id}/$cleanImagePath"
        } else {
            null
        }

        return Exercise(
            id = exerciseJson.id,
            name = exerciseJson.name,
            category = category,
            imageUrl = firstImageUrl,
            sortLetter = sortLetter,
            force = exerciseJson.force,
            level = exerciseJson.level,
            mechanic = exerciseJson.mechanic,
            equipment = exerciseJson.equipment,
            primaryMuscles = exerciseJson.primaryMuscles,
            secondaryMuscles = exerciseJson.secondaryMuscles,
            instructions = exerciseJson.instructions,
            notes = exerciseJson.notes ?: emptyList(),
            tips = exerciseJson.tips ?: emptyList(),
            // Usuwanie duplikacji - mapuj każdy obraz i usuń duplikację
            images = exerciseJson.images?.map { imageName ->
                val cleanImagePath = if (imageName.startsWith("${exerciseJson.id}/")) {
                    imageName.removePrefix("${exerciseJson.id}/")
                } else {
                    imageName
                }
                "file:///android_asset/exercise_images/${exerciseJson.id}/$cleanImagePath"
            } ?: emptyList()
        )
    }

    /**
     * Konwertuje ExerciseJson na encję Room
     */
    fun convertToEntity(exerciseJson: ExerciseJson): ExerciseEntity {
        val sortLetter = when {
            exerciseJson.name.firstOrNull()?.isDigit() == true -> "#"
            else -> exerciseJson.name.firstOrNull()?.uppercase() ?: "A"
        }

        val category = exerciseJson.category ?: determineCategory(exerciseJson.primaryMuscles)

        // Usuwanie duplikacji w ścieżkach obrazów
        val imagesList = exerciseJson.images ?: emptyList()
        val firstImageUrl = if (imagesList.isNotEmpty()) {
            val firstImage = imagesList.first()
            // Usuń duplikację w ścieżce - jeśli zaczyna się od id, usuń pierwszy segment
            val cleanImagePath = if (firstImage.startsWith("${exerciseJson.id}/")) {
                firstImage.removePrefix("${exerciseJson.id}/")
            } else {
                firstImage
            }
            "file:///android_asset/exercise_images/${exerciseJson.id}/$cleanImagePath"
        } else {
            null
        }

        return ExerciseEntity(
            id = exerciseJson.id,
            name = exerciseJson.name,
            category = category,
            force = exerciseJson.force,
            level = exerciseJson.level,
            mechanic = exerciseJson.mechanic,
            equipment = exerciseJson.equipment,
            primaryMuscles = exerciseJson.primaryMuscles,
            secondaryMuscles = exerciseJson.secondaryMuscles,
            instructions = exerciseJson.instructions,
            notes = exerciseJson.notes ?: emptyList(),
            tips = exerciseJson.tips ?: emptyList(),
            sortLetter = sortLetter,
            imageUrl = firstImageUrl,
            // Usuwanie duplikacji - mapuj każdy obraz i usuń duplikację
            images = exerciseJson.images?.map { imageName ->
                val cleanImagePath = if (imageName.startsWith("${exerciseJson.id}/")) {
                    imageName.removePrefix("${exerciseJson.id}/")
                } else {
                    imageName
                }
                "file:///android_asset/exercise_images/${exerciseJson.id}/$cleanImagePath"
            } ?: emptyList()
        )
    }

    /**
     * Określa kategorię na podstawie primary muscle, gdy kategoria nie jest podana
     */
    private fun determineCategory(primaryMuscles: List<String>): String {
        if (primaryMuscles.isEmpty()) return "Other"

        return when (primaryMuscles.first()) {
            "chest" -> "Chest"
            "lats", "middle back", "lower back", "traps" -> "Back"
            "abdominals" -> "Core"
            "quadriceps", "hamstrings", "calves", "glutes" -> "Legs"
            "biceps", "triceps", "forearms" -> "Arms"
            "shoulders", "traps" -> "Shoulders"
            "adductors", "abductors" -> "Hips"
            "neck" -> "Neck"
            else -> "Other"
        }
    }
}