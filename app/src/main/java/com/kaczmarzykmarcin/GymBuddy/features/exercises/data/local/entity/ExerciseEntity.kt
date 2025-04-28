package com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.kaczmarzykmarcin.GymBuddy.features.exercises.data.local.converter.StringListConverter

/**
 * Encja ćwiczenia w lokalnej bazie danych Room, dostosowana do formatu yuhonas/free-exercise-db
 */
@Entity(tableName = "exercises")
@TypeConverters(StringListConverter::class)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val force: String? = null,  // pull, push, static
    val level: String? = null,  // beginner, intermediate, expert
    val mechanic: String? = null, // compound, isolation
    val equipment: String? = null, // barbell, dumbbell, body only, etc.
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val instructions: List<String>,
    val notes: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val sortLetter: String,  // Pierwsza litera do grupowania
    val imageUrl: String? = null, // URL do pierwszego obrazu (dla kompatybilności)
    val images: List<String> = emptyList() // Lista wszystkich obrazów
)