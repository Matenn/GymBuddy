package com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.history.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaczmarzykmarcin.GymBuddy.features.workouts.domain.model.CompletedWorkout
import com.kaczmarzykmarcin.GymBuddy.features.workouts.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val TAG = "WorkoutHistoryViewModel"

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _workoutHistory = MutableStateFlow<List<CompletedWorkout>>(emptyList())
    val workoutHistory: StateFlow<List<CompletedWorkout>> = _workoutHistory

    private val _filteredWorkouts = MutableStateFlow<List<CompletedWorkout>>(emptyList())
    val filteredWorkouts: StateFlow<List<CompletedWorkout>> = _filteredWorkouts

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _activeDate = MutableStateFlow<Date?>(null)
    val activeDate: StateFlow<Date?> = _activeDate

    // Load workout history for a user
    fun loadWorkoutHistory(userId: String) {
        viewModelScope.launch {
            try {
                val workouts = workoutRepository.getUserWorkoutHistory(userId).firstOrNull() ?: emptyList()

                // Sort workouts by date (newest first)
                val sortedWorkouts = workouts.sortedByDescending {
                    it.endTime?.seconds ?: it.startTime.seconds
                }

                _workoutHistory.value = sortedWorkouts
                _filteredWorkouts.value = sortedWorkouts

                Log.d(TAG, "Loaded ${sortedWorkouts.size} workouts")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading workout history", e)
            }
        }
    }

    // Update search query and filter workouts
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    // Filter workouts by date
    fun filterByDate(date: Date) {
        _activeDate.value = date
        applyFilters()
    }

    // Clear date filter
    fun clearDateFilter() {
        _activeDate.value = null
        applyFilters()
    }

    // Reset all filters
    fun resetFilters() {
        _searchQuery.value = ""
        _activeDate.value = null
        _filteredWorkouts.value = _workoutHistory.value
        Log.d(TAG, "All filters reset")
    }

    // Apply all active filters
    private fun applyFilters() {
        viewModelScope.launch {
            var filtered = _workoutHistory.value

            // Apply search query filter if not empty
            val query = _searchQuery.value.trim().lowercase()
            if (query.isNotEmpty()) {
                filtered = filtered.filter { workout ->
                    workout.name.lowercase().contains(query)
                }
                Log.d(TAG, "Applied search filter: '$query', results: ${filtered.size}")
            }

            // Apply date filter if active
            _activeDate.value?.let { date ->
                // Format the target date to compare strings in the format YYYY-MM-DD
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val targetDateStr = dateFormat.format(date)

                filtered = filtered.filter { workout ->
                    val workoutDate = workout.endTime?.toDate() ?: workout.startTime.toDate()
                    dateFormat.format(workoutDate) == targetDateStr
                }
                Log.d(TAG, "Applied date filter: $targetDateStr, results: ${filtered.size}")
            }

            _filteredWorkouts.value = filtered
        }
    }
}