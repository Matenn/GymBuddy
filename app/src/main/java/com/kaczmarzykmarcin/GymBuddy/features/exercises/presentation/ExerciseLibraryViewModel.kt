package com.kaczmarzykmarcin.GymBuddy.features.exercises.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaczmarzykmarcin.GymBuddy.data.model.Exercise
import com.kaczmarzykmarcin.GymBuddy.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ExerciseLibraryViewModel"

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    // State for exercises grouped by first letter
    private val _exercisesGrouped = MutableStateFlow<Map<String, List<Exercise>>>(emptyMap())
    val exercisesGrouped: StateFlow<Map<String, List<Exercise>>> = _exercisesGrouped

    // State for search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // State for search results
    private val _searchResults = MutableStateFlow<List<Exercise>>(emptyList())
    val searchResults: StateFlow<List<Exercise>> = _searchResults

    // Available categories for filtering
    private val _availableCategories = MutableStateFlow<List<String>>(emptyList())
    val availableCategories: StateFlow<List<String>> = _availableCategories

    // Available muscle groups for filtering
    private val _availableMuscleGroups = MutableStateFlow<List<String>>(emptyList())
    val availableMuscleGroups: StateFlow<List<String>> = _availableMuscleGroups

    // Selected categories for filtering
    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories

    // Selected muscle groups for filtering
    private val _selectedMuscleGroups = MutableStateFlow<Set<String>>(emptySet())
    val selectedMuscleGroups: StateFlow<Set<String>> = _selectedMuscleGroups

    // All exercises cached for filtering
    private val _allExercises = MutableStateFlow<List<Exercise>>(emptyList())

    init {
        Log.d(TAG, "ViewModel initialized")
    }

    /**
     * Loads all exercises and organizes them by first letter
     */
    fun loadExercises() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading exercises...")

                // Initialize exercise database if needed
                exerciseRepository.initializeExerciseDatabase()

                // Get all exercises sorted by first letter
                val result = exerciseRepository.getExercisesGroupedByLetter()

                if (result.isSuccess) {
                    val exercisesMap = result.getOrNull() ?: emptyMap()
                    _exercisesGrouped.value = exercisesMap
                    // Cache all exercises for search and filtering
                    _allExercises.value = exercisesMap.values.flatten()
                    Log.d(TAG, "Successfully loaded ${_allExercises.value.size} exercises")
                } else {
                    Log.e(TAG, "Failed to load exercises: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading exercises", e)
            }
        }
    }

    /**
     * Loads all available exercise categories
     */
    fun loadCategories() {
        viewModelScope.launch {
            try {
                val result = exerciseRepository.getCategories()
                if (result.isSuccess) {
                    // Sort and capitalize the first letter of each category
                    val categories = result.getOrNull()?.map {
                        it.capitalizeFirstLetter()
                    }?.sorted() ?: emptyList()

                    _availableCategories.value = categories
                    Log.d(TAG, "Loaded ${categories.size} categories")
                } else {
                    Log.e(TAG, "Failed to load categories: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading categories", e)
            }
        }
    }

    /**
     * Loads all available muscle groups
     */
    fun loadMuscleGroups() {
        viewModelScope.launch {
            try {
                val result = exerciseRepository.getMuscleGroups()
                if (result.isSuccess) {
                    // Sort and capitalize the first letter of each muscle group
                    val muscleGroups = result.getOrNull()?.map {
                        it.capitalizeFirstLetter()
                    }?.sorted() ?: emptyList()

                    _availableMuscleGroups.value = muscleGroups
                    Log.d(TAG, "Loaded ${muscleGroups.size} muscle groups")
                } else {
                    Log.e(TAG, "Failed to load muscle groups: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading muscle groups", e)
            }
        }
    }

    /**
     * Updates search query and filters exercises
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterExercises()
    }

    /**
     * Handles category selection in filter
     */
    fun onCategorySelected(category: String, isSelected: Boolean) {
        val currentSelection = _selectedCategories.value.toMutableSet()
        if (isSelected) {
            currentSelection.add(category)
        } else {
            currentSelection.remove(category)
        }
        _selectedCategories.value = currentSelection
        filterExercises()
    }

    /**
     * Handles muscle group selection in filter
     */
    fun onMuscleGroupSelected(muscleGroup: String, isSelected: Boolean) {
        val currentSelection = _selectedMuscleGroups.value.toMutableSet()
        if (isSelected) {
            currentSelection.add(muscleGroup)
        } else {
            currentSelection.remove(muscleGroup)
        }
        _selectedMuscleGroups.value = currentSelection
        filterExercises()
    }

    /**
     * Clears all category filters
     */
    fun clearCategoryFilters() {
        _selectedCategories.value = emptySet()
        filterExercises()
    }

    /**
     * Clears all muscle group filters
     */
    fun clearMuscleGroupFilters() {
        _selectedMuscleGroups.value = emptySet()
        filterExercises()
    }

    /**
     * Filters exercises based on search query and selected filters
     */
    private fun filterExercises() {
        viewModelScope.launch {
            // Get current filters
            val query = _searchQuery.value.trim().lowercase()
            val categories = _selectedCategories.value.map { it.lowercase() }.toSet()
            val muscleGroups = _selectedMuscleGroups.value.map { it.lowercase() }.toSet()

            // Start with all exercises
            var filteredExercises = _allExercises.value

            // Apply search filter if query is not empty
            if (query.isNotEmpty()) {
                filteredExercises = filteredExercises.filter {
                    it.name.lowercase().contains(query)
                }
            }

            // Apply category filter if any categories are selected
            if (categories.isNotEmpty()) {
                filteredExercises = filteredExercises.filter {
                    categories.contains(it.category.lowercase())
                }
            }

            // Apply muscle group filter if any muscle groups are selected
            if (muscleGroups.isNotEmpty()) {
                filteredExercises = filteredExercises.filter { exercise ->
                    // Sprawdzamy tylko primaryMuscles - nie bierzemy pod uwagę secondaryMuscles
                    val exercisePrimaryMuscles = exercise.primaryMuscles.map { it.lowercase() }

                    // Ćwiczenie musi mieć przynajmniej jedną główną grupę mięśniową z wybranych
                    muscleGroups.any { it in exercisePrimaryMuscles }
                }
            }

            // Update search results
            _searchResults.value = filteredExercises.sortedBy { it.name }

            Log.d(TAG, "Filtered exercises: ${filteredExercises.size} results")
        }
    }

    // Helper function to capitalize first letter
    private fun String.capitalizeFirstLetter(): String {
        return if (this.isEmpty()) this else this.substring(0, 1).uppercase() + this.substring(1)
    }
}