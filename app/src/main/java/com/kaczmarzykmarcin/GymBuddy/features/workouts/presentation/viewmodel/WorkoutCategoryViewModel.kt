// WorkoutCategoryViewModel.kt
package com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kaczmarzykmarcin.GymBuddy.data.model.WorkoutCategory
import com.kaczmarzykmarcin.GymBuddy.data.repository.WorkoutCategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WorkoutCategoryViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val workoutCategoryRepository: WorkoutCategoryRepository
) : ViewModel() {

    private val userId = auth.currentUser?.uid ?: ""

    // Inicjalizacja domyślnych kategorii
    init {
        viewModelScope.launch {
            workoutCategoryRepository.initializeDefaultCategories(userId)
        }
    }

    // Lista kategorii jako StateFlow
    val categories: StateFlow<List<WorkoutCategory>> = workoutCategoryRepository
        .getUserWorkoutCategories(userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    // Dodaj nową kategorię
    fun addCategory(name: String, color: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            val newCategory = WorkoutCategory(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                color = color
            )
            workoutCategoryRepository.createWorkoutCategory(newCategory)
        }
    }

    // Aktualizuj kategorię
    fun updateCategory(category: WorkoutCategory) {
        if (category.name.isBlank()) return

        viewModelScope.launch {
            workoutCategoryRepository.updateWorkoutCategory(category)
        }
    }

    // Usuń kategorię
    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            workoutCategoryRepository.deleteWorkoutCategory(categoryId)
        }
    }
}