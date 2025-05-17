package com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaczmarzykmarcin.GymBuddy.data.model.CompletedWorkout
import com.kaczmarzykmarcin.GymBuddy.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WorkoutDetailsViewModel"

@HiltViewModel
class WorkoutDetailsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _workout = MutableStateFlow<CompletedWorkout?>(null)
    val workout: StateFlow<CompletedWorkout?> = _workout

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Ładuje szczegóły zakończonego treningu
    fun loadWorkoutDetails(workoutId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val result = workoutRepository.getCompletedWorkout(workoutId)

                if (result.isSuccess) {
                    val workout = result.getOrNull()
                    if (workout != null) {
                        _workout.value = workout
                        Log.d(TAG, "Loaded workout details for ${workout.id}")
                    } else {
                        _error.value = "Nie znaleziono treningu"
                        Log.e(TAG, "Workout not found")
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = exception?.message ?: "Wystąpił nieznany błąd"
                    Log.e(TAG, "Error loading workout details", exception)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Wystąpił nieznany błąd"
                Log.e(TAG, "Exception loading workout details", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Aktualizuje trening
    fun updateWorkout(workout: CompletedWorkout) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val result = workoutRepository.updateWorkout(workout)

                if (result.isSuccess) {
                    // Aktualizuj lokalny stan
                    _workout.value = workout
                    Log.d(TAG, "Updated workout ${workout.id}")
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = exception?.message ?: "Nie udało się zaktualizować treningu"
                    Log.e(TAG, "Error updating workout", exception)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Wystąpił nieznany błąd"
                Log.e(TAG, "Exception updating workout", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}