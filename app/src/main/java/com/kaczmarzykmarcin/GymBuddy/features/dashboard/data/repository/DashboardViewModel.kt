package com.kaczmarzykmarcin.GymBuddy.features.dashboard.data.repository

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaczmarzykmarcin.GymBuddy.data.model.CompletedWorkout
import com.kaczmarzykmarcin.GymBuddy.data.model.UserData
import com.kaczmarzykmarcin.GymBuddy.data.repository.UserRepository
import com.kaczmarzykmarcin.GymBuddy.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

private const val TAG = "DashboardViewModel"

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    private val _lastWorkout = MutableStateFlow<CompletedWorkout?>(null)
    val lastWorkout: StateFlow<CompletedWorkout?> = _lastWorkout

    private val _weeklyActivity = MutableStateFlow<Map<String, Boolean>>(mapOf())
    val weeklyActivity: StateFlow<Map<String, Boolean>> = _weeklyActivity

    // Load user data including profile, stats, and achievements
    fun loadUserData(userId: String) {
        viewModelScope.launch {
            try {
                val result = userRepository.getFullUserData(userId)
                if (result.isSuccess) {
                    _userData.value = result.getOrNull()
                    Log.d(TAG, "User data loaded successfully: ${_userData.value?.user?.id}")
                } else {
                    Log.e(TAG, "Failed to load user data: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading user data", e)
            }
        }
    }

    // Load user's last completed workout
    fun loadLastWorkout(userId: String) {
        viewModelScope.launch {
            try {
                // Pobierz pierwszy element z Flow
                val workouts = workoutRepository.getUserWorkoutHistory(userId).firstOrNull() ?: emptyList()
                _lastWorkout.value = workouts.firstOrNull()
                Log.d(TAG, "Last workout loaded: ${_lastWorkout.value?.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading workout history", e)
            }
        }
    }

    // Load weekly workout activity (which days the user worked out)
    fun loadWeeklyActivity(userId: String) {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val today = calendar.time

                // Get the start of the current week (Monday)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val startOfWeek = calendar.time

                // Initialize activity map with all days set to false
                val activityMap = mutableMapOf<String, Boolean>(
                    "Pn" to false,
                    "Wt" to false,
                    "Śr" to false,
                    "Cz" to false,
                    "Pt" to false,
                    "Sb" to false,
                    "Nd" to false
                )

                try {
                    // Pobierz pierwszy element z Flow
                    val workouts = workoutRepository.getUserWorkoutHistory(userId).firstOrNull() ?: emptyList()

                    // Mark days with workouts
                    for (workout in workouts) {
                        workout.endTime?.let { endTime ->
                            val workoutDate = endTime.toDate()

                            // Only check workouts from current week
                            if (workoutDate.after(startOfWeek) || workoutDate == startOfWeek) {
                                val cal = Calendar.getInstance()
                                cal.time = workoutDate

                                // Get day of week
                                val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                                    Calendar.MONDAY -> "Pn"
                                    Calendar.TUESDAY -> "Wt"
                                    Calendar.WEDNESDAY -> "Śr"
                                    Calendar.THURSDAY -> "Cz"
                                    Calendar.FRIDAY -> "Pt"
                                    Calendar.SATURDAY -> "Sb"
                                    Calendar.SUNDAY -> "Nd"
                                    else -> null
                                }

                                dayOfWeek?.let {
                                    activityMap[it] = true
                                }
                            }
                        }
                    }

                    _weeklyActivity.value = activityMap
                    Log.d(TAG, "Weekly activity loaded: ${activityMap.filter { it.value }.size} active days")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load workout history for activity", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading weekly activity", e)
            }
        }
    }

    // Alternatywna wersja ładowania danych aktywności tygodniowej, jeśli potrzebujemy ciągłej aktualizacji
    fun observeWeeklyActivity(userId: String) {
        viewModelScope.launch {
            workoutRepository.getUserWorkoutHistory(userId).collectLatest { workouts ->
                val calendar = Calendar.getInstance()

                // Get the start of the current week (Monday)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val startOfWeek = calendar.time

                // Initialize activity map with all days set to false
                val activityMap = mutableMapOf<String, Boolean>(
                    "Pn" to false,
                    "Wt" to false,
                    "Śr" to false,
                    "Cz" to false,
                    "Pt" to false,
                    "Sb" to false,
                    "Nd" to false
                )

                // Mark days with workouts
                for (workout in workouts) {
                    workout.endTime?.let { endTime ->
                        val workoutDate = endTime.toDate()

                        // Only check workouts from current week
                        if (workoutDate.after(startOfWeek) || workoutDate == startOfWeek) {
                            val cal = Calendar.getInstance()
                            cal.time = workoutDate

                            // Get day of week
                            val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                                Calendar.MONDAY -> "Pn"
                                Calendar.TUESDAY -> "Wt"
                                Calendar.WEDNESDAY -> "Śr"
                                Calendar.THURSDAY -> "Cz"
                                Calendar.FRIDAY -> "Pt"
                                Calendar.SATURDAY -> "Sb"
                                Calendar.SUNDAY -> "Nd"
                                else -> null
                            }

                            dayOfWeek?.let {
                                activityMap[it] = true
                            }
                        }
                    }
                }

                _weeklyActivity.value = activityMap
                Log.d(TAG, "Weekly activity updated: ${activityMap.filter { it.value }.size} active days")
            }
        }
    }
}