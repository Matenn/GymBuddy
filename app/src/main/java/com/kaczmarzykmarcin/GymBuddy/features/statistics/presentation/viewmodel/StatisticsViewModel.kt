package com.kaczmarzykmarcin.GymBuddy.features.statistics.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kaczmarzykmarcin.GymBuddy.data.model.*
import com.kaczmarzykmarcin.GymBuddy.data.repository.WorkoutRepository
import com.kaczmarzykmarcin.GymBuddy.data.repository.WorkoutCategoryRepository
import com.kaczmarzykmarcin.GymBuddy.data.repository.UserRepository
import com.kaczmarzykmarcin.GymBuddy.features.statistics.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val workoutRepository: WorkoutRepository,
    private val categoryRepository: WorkoutCategoryRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val TAG = "StatisticsViewModel"

    // Current user ID
    private val currentUserId = auth.currentUser?.uid ?: ""

    // UI State
    private val _selectedTimePeriod = MutableStateFlow(TimePeriod.WEEK)
    val selectedTimePeriod = _selectedTimePeriod.asStateFlow()

    private val _selectedStatType = MutableStateFlow(StatType.CATEGORY)
    val selectedStatType = _selectedStatType.asStateFlow()

    private val _selectedCategory = MutableStateFlow<WorkoutCategory?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedExercise = MutableStateFlow<Exercise?>(null)
    val selectedExercise = _selectedExercise.asStateFlow()

    // Data
    private val _allWorkouts = MutableStateFlow<List<CompletedWorkout>>(emptyList())
    val allWorkouts = _allWorkouts.asStateFlow()

    private val _allCategories = MutableStateFlow<List<WorkoutCategory>>(emptyList())
    val allCategories = _allCategories.asStateFlow()

    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats = _userStats.asStateFlow()

    // Progress chart exercise selection
    private val _selectedExercisesForChart = MutableStateFlow<Set<String>>(emptySet())
    val selectedExercisesForChart = _selectedExercisesForChart.asStateFlow()

    private val _showAllExercisesInChart = MutableStateFlow(true)
    val showAllExercisesInChart = _showAllExercisesInChart.asStateFlow()

    // Computed statistics
    val filteredWorkouts = combine(
        allWorkouts,
        selectedTimePeriod,
        selectedCategory
    ) { workouts, timePeriod, category ->
        val timeFilteredWorkouts = filterWorkoutsByTimePeriod(workouts, timePeriod)
        if (category != null) {
            timeFilteredWorkouts.filter { it.categoryId == category.id }
        } else {
            timeFilteredWorkouts
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val basicStats = filteredWorkouts.map { workouts ->
        val totalWorkouts = workouts.size
        val totalTimeMinutes = workouts.sumOf { it.duration / 60 } // Convert seconds to minutes

        BasicStatistics(
            totalWorkouts = totalWorkouts,
            totalTimeMinutes = totalTimeMinutes.toInt()
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, BasicStatistics(0, 0))

    val exerciseStatistics = combine(
        selectedExercise,
        allWorkouts,
        selectedTimePeriod
    ) { exercise, workouts, timePeriod ->
        if (exercise != null) {
            calculateExerciseStatistics(exercise, workouts, timePeriod)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Load workouts
                workoutRepository.getUserWorkoutHistory(currentUserId).collect { workouts ->
                    _allWorkouts.value = workouts.filter { it.endTime != null }
                    Log.d(TAG, "Loaded ${workouts.size} workouts")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading workouts", e)
            }
        }

        viewModelScope.launch {
            try {
                // Load categories
                categoryRepository.getUserWorkoutCategories(currentUserId).collect { categories ->
                    _allCategories.value = categories
                    Log.d(TAG, "Loaded ${categories.size} categories")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading categories", e)
            }
        }

        viewModelScope.launch {
            try {
                // Load user stats
                val result = userRepository.getUserStats(currentUserId)
                if (result.isSuccess) {
                    _userStats.value = result.getOrNull()
                    Log.d(TAG, "Loaded user stats")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user stats", e)
            }
        }
    }

    fun selectTimePeriod(period: TimePeriod) {
        _selectedTimePeriod.value = period
    }

    fun selectStatType(type: StatType) {
        _selectedStatType.value = type
        // Reset selections when changing type
        if (type == StatType.CATEGORY) {
            _selectedExercise.value = null
        } else {
            _selectedCategory.value = null
        }
    }

    fun selectCategory(category: WorkoutCategory?) {
        _selectedCategory.value = category
        // Reset exercise selections for chart
        _selectedExercisesForChart.value = emptySet()
        _showAllExercisesInChart.value = true
    }

    fun selectExercise(exercise: Exercise?) {
        _selectedExercise.value = exercise
    }

    fun toggleExerciseForChart(exerciseId: String) {
        val currentSelected = _selectedExercisesForChart.value.toMutableSet()
        if (currentSelected.contains(exerciseId)) {
            currentSelected.remove(exerciseId)
        } else {
            currentSelected.add(exerciseId)
            // If selecting specific exercises, uncheck "show all"
            _showAllExercisesInChart.value = false
        }
        _selectedExercisesForChart.value = currentSelected
    }

    fun toggleShowAllExercisesInChart() {
        val newValue = !_showAllExercisesInChart.value
        _showAllExercisesInChart.value = newValue
        if (newValue) {
            // If showing all, clear specific selections
            _selectedExercisesForChart.value = emptySet()
        }
    }

    // Calculate workout activity data for chart
    fun calculateWorkoutActivity(): List<ActivityData> {
        val workouts = filteredWorkouts.value
        val timePeriod = selectedTimePeriod.value

        return when (timePeriod) {
            TimePeriod.WEEK -> calculateWeeklyActivity(workouts)
            TimePeriod.MONTH -> calculateMonthlyActivity(workouts)
            TimePeriod.THREE_MONTHS -> calculateThreeMonthsActivity(workouts)
            TimePeriod.YEAR -> calculateYearlyActivity(workouts)
            TimePeriod.ALL -> calculateAllTimeActivity(workouts)
        }
    }

    private fun calculateAllTimeActivity(workouts: List<CompletedWorkout>): List<ActivityData> {
        if (workouts.isEmpty()) return emptyList()

        // Grupuj treningi według roku
        val workoutsByYear = workouts.groupBy { workout ->
            workout.endTime?.let { endTime ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = endTime.seconds * 1000
                calendar.get(Calendar.YEAR)
            } ?: 0
        }.filterKeys { it != 0 } // Usuń treningi bez daty

        return workoutsByYear.map { (year, yearWorkouts) ->
            ActivityData(
                label = year.toString(),
                minutes = yearWorkouts.sumOf { it.duration / 60 }.toInt()
            )
        }.sortedBy { it.label.toIntOrNull() ?: 0 }
    }

    // Calculate category distribution
    fun calculateCategoryDistribution(): List<CategoryDistribution> {
        val workouts = filteredWorkouts.value
        val categories = allCategories.value

        val categoryWorkoutCounts = workouts
            .groupBy { it.categoryId }
            .mapValues { it.value.size }

        val total = workouts.size
        if (total == 0) return emptyList()

        val categoryDistributions = categoryWorkoutCounts
            .mapNotNull { (categoryId, count) ->
                val category = categories.find { it.id == categoryId }
                if (category != null) {
                    CategoryDistribution(
                        categoryName = category.name,
                        categoryColor = category.color,
                        count = count,
                        percentage = (count.toFloat() / total * 100).toInt()
                    )
                } else null
            }
            .sortedByDescending { it.count }

        // Take top 5 and group rest as "Other"
        val top5 = categoryDistributions.take(5)
        val rest = categoryDistributions.drop(5)

        return if (rest.isNotEmpty()) {
            val otherCount = rest.sumOf { it.count }
            val otherPercentage = rest.sumOf { it.percentage }
            top5 + CategoryDistribution(
                categoryName = "Inne",
                categoryColor = "#607D8B",
                count = otherCount,
                percentage = otherPercentage
            )
        } else {
            top5
        }
    }

    // Calculate exercise distribution for category
    fun calculateExerciseDistribution(): List<ExerciseDistribution> {
        val workouts = filteredWorkouts.value
        val exerciseCounts = mutableMapOf<String, Int>()

        workouts.forEach { workout ->
            workout.exercises.forEach { exercise ->
                val currentCount = exerciseCounts[exercise.exerciseId] ?: 0
                exerciseCounts[exercise.exerciseId] = currentCount + exercise.sets.size
            }
        }

        val total = exerciseCounts.values.sum()
        if (total == 0) return emptyList()

        val exerciseDistributions = exerciseCounts
            .map { (exerciseId, count) ->
                val exerciseName = workouts
                    .flatMap { it.exercises }
                    .find { it.exerciseId == exerciseId }
                    ?.name ?: "Unknown"

                ExerciseDistribution(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    setCount = count,
                    percentage = (count.toFloat() / total * 100).toInt()
                )
            }
            .sortedByDescending { it.setCount }

        // Take top 5 and group rest as "Other"
        val top5 = exerciseDistributions.take(5)
        val rest = exerciseDistributions.drop(5)

        return if (rest.isNotEmpty()) {
            val otherCount = rest.sumOf { it.setCount }
            val otherPercentage = rest.sumOf { it.percentage }
            top5 + ExerciseDistribution(
                exerciseId = "other",
                exerciseName = "Inne",
                setCount = otherCount,
                percentage = otherPercentage
            )
        } else {
            top5
        }
    }

    // Calculate progress data for exercises
    fun calculateProgressData(): List<ProgressData> {
        val workouts = filteredWorkouts.value
        val timePeriod = selectedTimePeriod.value

        val exerciseIds = if (showAllExercisesInChart.value) {
            // Show all exercises in the filtered workouts
            workouts.flatMap { it.exercises }.map { it.exerciseId }.distinct()
        } else {
            // Show only selected exercises
            selectedExercisesForChart.value.toList()
        }

        return exerciseIds.mapNotNull { exerciseId ->
            calculateExerciseProgress(exerciseId, workouts, timePeriod)
        }
    }

    private fun calculateExerciseProgress(
        exerciseId: String,
        workouts: List<CompletedWorkout>,
        timePeriod: TimePeriod
    ): ProgressData? {
        val exerciseWorkouts = workouts
            .filter { workout -> workout.exercises.any { it.exerciseId == exerciseId } }
            .sortedBy { it.endTime?.seconds }

        if (exerciseWorkouts.isEmpty()) return null

        val exerciseName = exerciseWorkouts
            .flatMap { it.exercises }
            .find { it.exerciseId == exerciseId }
            ?.name ?: "Unknown"

        val progressPoints = when (timePeriod) {
            TimePeriod.WEEK -> calculateWeeklyProgressPoints(exerciseWorkouts, exerciseId)
            TimePeriod.MONTH -> calculateMonthlyProgressPoints(exerciseWorkouts, exerciseId)
            TimePeriod.THREE_MONTHS -> calculateThreeMonthsProgressPoints(exerciseWorkouts, exerciseId)
            TimePeriod.YEAR -> calculateYearlyProgressPoints(exerciseWorkouts, exerciseId)
            TimePeriod.ALL -> calculateAllTimeProgressPoints(exerciseWorkouts, exerciseId)
        }

        return ProgressData(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            progressPoints = progressPoints
        )
    }

    private fun calculateAllTimeProgressPoints(
        workouts: List<CompletedWorkout>,
        exerciseId: String
    ): List<ProgressPoint> {
        // Grupuj według miesięcy i pobierz najlepszy wynik dla każdego miesiąca
        val monthNames = listOf("Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru")

        return workouts.mapNotNull { workout ->
            val exerciseData = workout.exercises.find { it.exerciseId == exerciseId }
            val bestSet = exerciseData?.sets?.maxByOrNull { it.weight }

            if (bestSet != null && workout.endTime != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = workout.endTime.seconds * 1000
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)

                ProgressPoint(
                    timestamp = workout.endTime.seconds,
                    weight = bestSet.weight,
                    reps = bestSet.reps,
                    label = "${monthNames[month]} $year"
                )
            } else null
        }.groupBy { it.label }
            .mapValues { it.value.maxByOrNull { point -> point.weight } }
            .values
            .filterNotNull()
            .sortedBy { it.timestamp }
    }

    private fun calculateExerciseStatistics(
        exercise: Exercise,
        allWorkouts: List<CompletedWorkout>,
        timePeriod: TimePeriod
    ): ExerciseStatisticsData? {
        val filteredWorkouts = filterWorkoutsByTimePeriod(allWorkouts, timePeriod)
        val exerciseWorkouts = filteredWorkouts
            .filter { workout -> workout.exercises.any { it.exerciseId == exercise.id } }

        if (exerciseWorkouts.isEmpty()) return null

        val allSets = exerciseWorkouts
            .flatMap { workout ->
                workout.exercises
                    .filter { it.exerciseId == exercise.id }
                    .flatMap { it.sets }
            }

        if (allSets.isEmpty()) return null

        val personalBest = allSets.maxByOrNull { it.weight }
        val averageWeight = allSets.map { it.weight }.average()
        val averageReps = allSets.map { it.reps }.average()
        val averageSets = exerciseWorkouts.map { workout ->
            workout.exercises.filter { it.exerciseId == exercise.id }.sumOf { it.sets.size }
        }.average()

        val progressPoints = when (timePeriod) {
            TimePeriod.WEEK -> calculateWeeklyProgressPoints(exerciseWorkouts, exercise.id)
            TimePeriod.MONTH -> calculateMonthlyProgressPoints(exerciseWorkouts, exercise.id)
            TimePeriod.THREE_MONTHS -> calculateThreeMonthsProgressPoints(exerciseWorkouts, exercise.id)
            TimePeriod.YEAR -> calculateYearlyProgressPoints(exerciseWorkouts, exercise.id)
            TimePeriod.ALL -> calculateAllTimeProgressPoints(exerciseWorkouts, exercise.id)
        }

        return ExerciseStatisticsData(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            personalBestWeight = personalBest?.weight ?: 0.0,
            personalBestReps = personalBest?.reps ?: 0,
            averageWeight = averageWeight,
            averageReps = averageReps.toInt(),
            averageSets = averageSets,
            progressPoints = progressPoints
        )
    }

    private fun filterWorkoutsByTimePeriod(
        workouts: List<CompletedWorkout>,
        timePeriod: TimePeriod
    ): List<CompletedWorkout> {
        if (timePeriod == TimePeriod.ALL) {
            return workouts // Zwróć wszystkie treningi bez filtrowania
        }

        val now = Calendar.getInstance()
        val startTime = Calendar.getInstance()

        when (timePeriod) {
            TimePeriod.WEEK -> startTime.add(Calendar.DAY_OF_YEAR, -7)
            TimePeriod.MONTH -> startTime.add(Calendar.MONTH, -1)
            TimePeriod.THREE_MONTHS -> startTime.add(Calendar.MONTH, -3)
            TimePeriod.YEAR -> startTime.add(Calendar.YEAR, -1)
            TimePeriod.ALL -> return workouts // Już obsłużone powyżej
        }

        return workouts.filter { workout ->
            workout.endTime?.let { endTime ->
                endTime.seconds * 1000 >= startTime.timeInMillis
            } ?: false
        }
    }

    // Helper methods for calculating activity data
    private fun calculateWeeklyActivity(workouts: List<CompletedWorkout>): List<ActivityData> {
        val daysOfWeek = listOf("Pon", "Wt", "Śr", "Czw", "Pt", "Sob", "Nd")
        val calendar = Calendar.getInstance()

        return (0..6).map { dayOffset ->
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.add(Calendar.DAY_OF_YEAR, dayOffset)

            val dayWorkouts = workouts.filter { workout ->
                workout.endTime?.let { endTime ->
                    val workoutCalendar = Calendar.getInstance()
                    workoutCalendar.timeInMillis = endTime.seconds * 1000
                    workoutCalendar.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
                } ?: false
            }

            ActivityData(
                label = daysOfWeek[dayOffset],
                minutes = dayWorkouts.sumOf { it.duration / 60 }.toInt()
            )
        }
    }

    private fun calculateMonthlyActivity(workouts: List<CompletedWorkout>): List<ActivityData> {
        // Calculate average for each week of the month
        return (1..4).map { week ->
            val weekWorkouts = workouts.filter { workout ->
                workout.endTime?.let { endTime ->
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = endTime.seconds * 1000
                    val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)
                    weekOfMonth == week
                } ?: false
            }

            ActivityData(
                label = "Tydz. $week",
                minutes = weekWorkouts.sumOf { it.duration / 60 }.toInt()
            )
        }
    }

    private fun calculateThreeMonthsActivity(workouts: List<CompletedWorkout>): List<ActivityData> {
        val monthNames = listOf("Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru")
        val calendar = Calendar.getInstance()

        return (0..2).map { monthOffset ->
            calendar.add(Calendar.MONTH, -monthOffset)
            val month = calendar.get(Calendar.MONTH)

            val monthWorkouts = workouts.filter { workout ->
                workout.endTime?.let { endTime ->
                    val workoutCalendar = Calendar.getInstance()
                    workoutCalendar.timeInMillis = endTime.seconds * 1000
                    workoutCalendar.get(Calendar.MONTH) == month
                } ?: false
            }

            ActivityData(
                label = monthNames[month],
                minutes = monthWorkouts.sumOf { it.duration / 60 }.toInt()
            )
        }.reversed()
    }

    private fun calculateYearlyActivity(workouts: List<CompletedWorkout>): List<ActivityData> {
        val monthNames = listOf("Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru")

        return (0..11).map { month ->
            val monthWorkouts = workouts.filter { workout ->
                workout.endTime?.let { endTime ->
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = endTime.seconds * 1000
                    calendar.get(Calendar.MONTH) == month
                } ?: false
            }

            ActivityData(
                label = monthNames[month],
                minutes = monthWorkouts.sumOf { it.duration / 60 }.toInt()
            )
        }
    }

    // Helper methods for calculating progress points
    private fun calculateWeeklyProgressPoints(
        workouts: List<CompletedWorkout>,
        exerciseId: String
    ): List<ProgressPoint> {
        // Group by day and get best weight for each day
        return workouts.mapNotNull { workout ->
            val exerciseData = workout.exercises.find { it.exerciseId == exerciseId }
            val bestSet = exerciseData?.sets?.maxByOrNull { it.weight }

            if (bestSet != null && workout.endTime != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = workout.endTime.seconds * 1000
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                ProgressPoint(
                    timestamp = workout.endTime.seconds,
                    weight = bestSet.weight,
                    reps = bestSet.reps,
                    label = getDayLabel(dayOfWeek)
                )
            } else null
        }.distinctBy { it.label }.sortedBy { it.timestamp }
    }

    private fun calculateMonthlyProgressPoints(
        workouts: List<CompletedWorkout>,
        exerciseId: String
    ): List<ProgressPoint> {
        // Group by week and get best weight for each week
        return workouts.mapNotNull { workout ->
            val exerciseData = workout.exercises.find { it.exerciseId == exerciseId }
            val bestSet = exerciseData?.sets?.maxByOrNull { it.weight }

            if (bestSet != null && workout.endTime != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = workout.endTime.seconds * 1000
                val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)

                ProgressPoint(
                    timestamp = workout.endTime.seconds,
                    weight = bestSet.weight,
                    reps = bestSet.reps,
                    label = "Tydz. $weekOfMonth"
                )
            } else null
        }.groupBy { it.label }
            .mapValues { it.value.maxByOrNull { point -> point.weight } }
            .values
            .filterNotNull()
            .sortedBy { it.timestamp }
    }

    private fun calculateThreeMonthsProgressPoints(
        workouts: List<CompletedWorkout>,
        exerciseId: String
    ): List<ProgressPoint> {
        val monthNames = listOf("Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru")

        return workouts.mapNotNull { workout ->
            val exerciseData = workout.exercises.find { it.exerciseId == exerciseId }
            val bestSet = exerciseData?.sets?.maxByOrNull { it.weight }

            if (bestSet != null && workout.endTime != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = workout.endTime.seconds * 1000
                val month = calendar.get(Calendar.MONTH)

                ProgressPoint(
                    timestamp = workout.endTime.seconds,
                    weight = bestSet.weight,
                    reps = bestSet.reps,
                    label = monthNames[month]
                )
            } else null
        }.groupBy { it.label }
            .mapValues { it.value.maxByOrNull { point -> point.weight } }
            .values
            .filterNotNull()
            .sortedBy { it.timestamp }
    }

    private fun calculateYearlyProgressPoints(
        workouts: List<CompletedWorkout>,
        exerciseId: String
    ): List<ProgressPoint> {
        val monthNames = listOf("Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru")

        return workouts.mapNotNull { workout ->
            val exerciseData = workout.exercises.find { it.exerciseId == exerciseId }
            val bestSet = exerciseData?.sets?.maxByOrNull { it.weight }

            if (bestSet != null && workout.endTime != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = workout.endTime.seconds * 1000
                val month = calendar.get(Calendar.MONTH)

                ProgressPoint(
                    timestamp = workout.endTime.seconds,
                    weight = bestSet.weight,
                    reps = bestSet.reps,
                    label = monthNames[month]
                )
            } else null
        }.groupBy { it.label }
            .mapValues { it.value.maxByOrNull { point -> point.weight } }
            .values
            .filterNotNull()
            .sortedBy { it.timestamp }
    }

    private fun getDayLabel(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Pon"
            Calendar.TUESDAY -> "Wt"
            Calendar.WEDNESDAY -> "Śr"
            Calendar.THURSDAY -> "Czw"
            Calendar.FRIDAY -> "Pt"
            Calendar.SATURDAY -> "Sob"
            Calendar.SUNDAY -> "Nd"
            else -> "Unknown"
        }
    }
}