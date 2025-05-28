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

    private val _selectedProgressMetric = MutableStateFlow(ProgressMetric.MAX_WEIGHT)
    val selectedProgressMetric = _selectedProgressMetric.asStateFlow()

    fun selectProgressMetric(metric: ProgressMetric) {
        _selectedProgressMetric.value = metric
    }


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

    val progressData = combine(
        filteredWorkouts,
        selectedTimePeriod,
        selectedProgressMetric,
        selectedExercisesForChart,
        showAllExercisesInChart
    ) { workouts, timePeriod, metric, selectedExercises, showAll ->
        Log.d(TAG, "Recalculating progress data: ${workouts.size} workouts, period: $timePeriod, metric: $metric")

        val exerciseIds = if (showAll) {
            workouts.flatMap { it.exercises }.map { it.exerciseId }.distinct()
        } else {
            selectedExercises.toList()
        }

        exerciseIds.mapNotNull { exerciseId ->
            calculateExerciseProgressWithMetric(exerciseId, workouts, timePeriod, metric)
        }.also { result ->
            Log.d(TAG, "Progress data result: ${result.size} exercises")
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

    // REAKTYWNE WERSJE KALKULACJI
    val workoutActivity = combine(
        filteredWorkouts,
        selectedTimePeriod
    ) { workouts, timePeriod ->
        Log.d(TAG, "Recalculating activity: ${workouts.size} workouts, period: $timePeriod")

        when (timePeriod) {
            TimePeriod.WEEK -> calculateWeeklyActivity(workouts)
            TimePeriod.MONTH -> calculateMonthlyActivity(workouts)
            TimePeriod.THREE_MONTHS -> calculateThreeMonthsActivity(workouts)
            TimePeriod.YEAR -> calculateYearlyActivity(workouts)
            TimePeriod.ALL -> calculateAllTimeActivity(workouts)
        }.also { result ->
            Log.d(TAG, "Activity result: ${result.size} data points")
            result.forEach { Log.d(TAG, "Activity: ${it.label} = ${it.minutes}") }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val categoryDistribution = combine(
        filteredWorkouts,
        allCategories
    ) { workouts, categories ->
        Log.d(TAG, "Recalculating category distribution: ${workouts.size} workouts")

        val categoryWorkoutCounts = workouts
            .groupBy { it.categoryId }
            .mapValues { it.value.size }

        val total = workouts.size
        if (total == 0) return@combine emptyList<CategoryDistribution>()

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

        if (rest.isNotEmpty()) {
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
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val exerciseDistribution = filteredWorkouts.map { workouts ->
        Log.d(TAG, "Recalculating exercise distribution: ${workouts.size} workouts")

        val exerciseCounts = mutableMapOf<String, Int>()

        workouts.forEach { workout ->
            workout.exercises.forEach { exercise ->
                val currentCount = exerciseCounts[exercise.exerciseId] ?: 0
                exerciseCounts[exercise.exerciseId] = currentCount + exercise.sets.size
            }
        }

        val total = exerciseCounts.values.sum()
        if (total == 0) return@map emptyList<ExerciseDistribution>()

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

        if (rest.isNotEmpty()) {
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
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
        Log.d(TAG, "Selecting time period: $period")
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
            // Remove the exercise
            currentSelected.remove(exerciseId)

            // If no exercises are selected, automatically select "All"
            if (currentSelected.isEmpty()) {
                _showAllExercisesInChart.value = true
            }
        } else {
            // Add the exercise
            currentSelected.add(exerciseId)

            // If selecting specific exercises, uncheck "All"
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
        // Note: If user unchecks "All", we don't automatically select anything
        // Let them manually select exercises they want to see
    }

    // STARE FUNKCJE - DEPRECATED (zachowane dla kompatybilności)
    @Deprecated("Use reactive workoutActivity StateFlow instead")
    fun calculateWorkoutActivity(): List<ActivityData> {
        return workoutActivity.value
    }



    @Deprecated("Use reactive categoryDistribution StateFlow instead")
    fun calculateCategoryDistribution(): List<CategoryDistribution> {
        return categoryDistribution.value
    }

    @Deprecated("Use reactive exerciseDistribution StateFlow instead")
    fun calculateExerciseDistribution(): List<ExerciseDistribution> {
        return exerciseDistribution.value
    }

    // Calculate progress data for exercises

    @Deprecated("Use reactive progressData StateFlow instead")
    fun calculateProgressData(): List<ProgressData> {
        return progressData.value
    }

    private fun calculateExerciseProgressWithMetric(
        exerciseId: String,
        workouts: List<CompletedWorkout>,
        timePeriod: TimePeriod,
        metric: ProgressMetric
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
            TimePeriod.WEEK -> calculateWeeklyProgressPointsForMetric(exerciseWorkouts, exerciseId, metric)
            TimePeriod.MONTH -> calculateMonthlyProgressPointsForMetric(exerciseWorkouts, exerciseId, metric)
            TimePeriod.THREE_MONTHS -> calculateThreeMonthsProgressPointsForMetric(exerciseWorkouts, exerciseId, metric)
            TimePeriod.YEAR -> calculateYearlyProgressPointsForMetric(exerciseWorkouts, exerciseId, metric)
            TimePeriod.ALL -> calculateAllTimeProgressPointsForMetric(exerciseWorkouts, exerciseId, metric)
        }

        return ProgressData(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            progressPoints = progressPoints
        )
    }

    private fun calculateWeeklyProgressPointsForMetric(
        workouts: List<CompletedWorkout>,
        exerciseId: String,
        metric: ProgressMetric
    ): List<ProgressPoint> {
        return workouts.mapNotNull { workout ->
            val exerciseData = workout.exercises.find { it.exerciseId == exerciseId }

            if (exerciseData != null && workout.endTime != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = workout.endTime.seconds * 1000
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                val (value, bestSet, tonnage) = when (metric) {
                    ProgressMetric.MAX_WEIGHT -> {
                        val bestSet = exerciseData.sets.maxByOrNull { it.weight }
                        Triple(bestSet?.weight ?: 0.0, bestSet, 0.0)
                    }
                    ProgressMetric.ONE_RM -> {
                        val bestSet = exerciseData.sets.maxByOrNull { calculate1RM(it.weight, it.reps) }
                        val oneRM = bestSet?.let { calculate1RM(it.weight, it.reps) } ?: 0.0
                        Triple(oneRM, bestSet, 0.0)
                    }
                    ProgressMetric.VOLUME -> {
                        val bestSet = exerciseData.sets.maxByOrNull { calculateVolume(it.weight, it.reps) }
                        val volume = bestSet?.let { calculateVolume(it.weight, it.reps) } ?: 0.0
                        Triple(volume, bestSet, 0.0)
                    }
                    ProgressMetric.TONNAGE -> {
                        val tonnage = calculateTonnage(exerciseData.sets)
                        val bestSet = exerciseData.sets.maxByOrNull { it.weight }
                        Triple(tonnage, bestSet, tonnage)
                    }
                }

                if (bestSet != null) {
                    ProgressPoint(
                        timestamp = workout.endTime.seconds,
                        weight = value,
                        reps = if (metric == ProgressMetric.ONE_RM) 1 else bestSet.reps,
                        label = getDayLabel(dayOfWeek),
                        originalWeight = bestSet.weight,
                        originalReps = bestSet.reps,
                        volume = calculateVolume(bestSet.weight, bestSet.reps),
                        tonnage = tonnage
                    )
                } else null
            } else null
        }.distinctBy { it.label }.sortedBy { it.timestamp }
    }


    private fun calculateAllTimeProgressPointsForMetric(
        workouts: List<CompletedWorkout>,
        exerciseId: String,
        metric: ProgressMetric
    ): List<ProgressPoint> {
        val monthNames = listOf("Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru")

        return workouts.mapNotNull { workout ->
            val exerciseData = workout.exercises.find { it.exerciseId == exerciseId }

            if (exerciseData != null && workout.endTime != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = workout.endTime.seconds * 1000
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)

                val (value, bestSet, tonnage) = when (metric) {
                    ProgressMetric.MAX_WEIGHT -> {
                        val bestSet = exerciseData.sets.maxByOrNull { it.weight }
                        Triple(bestSet?.weight ?: 0.0, bestSet, 0.0)
                    }
                    ProgressMetric.ONE_RM -> {
                        val bestSet = exerciseData.sets.maxByOrNull { calculate1RM(it.weight, it.reps) }
                        val oneRM = bestSet?.let { calculate1RM(it.weight, it.reps) } ?: 0.0
                        Triple(oneRM, bestSet, 0.0)
                    }
                    ProgressMetric.VOLUME -> {
                        val bestSet = exerciseData.sets.maxByOrNull { calculateVolume(it.weight, it.reps) }
                        val volume = bestSet?.let { calculateVolume(it.weight, it.reps) } ?: 0.0
                        Triple(volume, bestSet, 0.0)
                    }
                    ProgressMetric.TONNAGE -> {
                        val tonnage = calculateTonnage(exerciseData.sets)
                        val bestSet = exerciseData.sets.maxByOrNull { it.weight }
                        Triple(tonnage, bestSet, tonnage)
                    }
                }

                if (bestSet != null) {
                    ProgressPoint(
                        timestamp = workout.endTime.seconds,
                        weight = value,
                        reps = if (metric == ProgressMetric.ONE_RM) 1 else bestSet.reps,
                        label = "${monthNames[month]} $year",
                        originalWeight = bestSet.weight,
                        originalReps = bestSet.reps,
                        volume = calculateVolume(bestSet.weight, bestSet.reps),
                        tonnage = tonnage
                    )
                } else null
            } else null
        }.groupBy { it.label }
            .mapValues { entry ->
                entry.value.maxByOrNull { point -> point.weight }
            }
            .values
            .filterNotNull()
            .sortedBy { it.timestamp }
    }

    private fun calculateMonthlyProgressPointsForMetric(
        workouts: List<CompletedWorkout>,
        exerciseId: String,
        metric: ProgressMetric
    ): List<ProgressPoint> {
        return workouts.mapNotNull { workout ->
            val exerciseData = workout.exercises.find { it.exerciseId == exerciseId }

            if (exerciseData != null && workout.endTime != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = workout.endTime.seconds * 1000
                val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)

                val (value, bestSet, tonnage) = when (metric) {
                    ProgressMetric.MAX_WEIGHT -> {
                        val bestSet = exerciseData.sets.maxByOrNull { it.weight }
                        Triple(bestSet?.weight ?: 0.0, bestSet, 0.0)
                    }
                    ProgressMetric.ONE_RM -> {
                        val bestSet = exerciseData.sets.maxByOrNull { calculate1RM(it.weight, it.reps) }
                        val oneRM = bestSet?.let { calculate1RM(it.weight, it.reps) } ?: 0.0
                        Triple(oneRM, bestSet, 0.0)
                    }
                    ProgressMetric.VOLUME -> {
                        val bestSet = exerciseData.sets.maxByOrNull { calculateVolume(it.weight, it.reps) }
                        val volume = bestSet?.let { calculateVolume(it.weight, it.reps) } ?: 0.0
                        Triple(volume, bestSet, 0.0)
                    }
                    ProgressMetric.TONNAGE -> {
                        val tonnage = calculateTonnage(exerciseData.sets)
                        val bestSet = exerciseData.sets.maxByOrNull { it.weight }
                        Triple(tonnage, bestSet, tonnage)
                    }
                }

                if (bestSet != null) {
                    ProgressPoint(
                        timestamp = workout.endTime.seconds,
                        weight = value,
                        reps = if (metric == ProgressMetric.ONE_RM) 1 else bestSet.reps,
                        label = "Tydz. $weekOfMonth",
                        originalWeight = bestSet.weight,
                        originalReps = bestSet.reps,
                        volume = calculateVolume(bestSet.weight, bestSet.reps),
                        tonnage = tonnage
                    )
                } else null
            } else null
        }.groupBy { it.label }
            .mapValues { entry ->
                // Dla każdego tygodnia, wybierz najlepszy wynik według wybranej metryki
                entry.value.maxByOrNull { point -> point.weight }
            }
            .values
            .filterNotNull()
            .sortedBy { it.timestamp }
    }

    private fun calculateThreeMonthsProgressPointsForMetric(
        workouts: List<CompletedWorkout>,
        exerciseId: String,
        metric: ProgressMetric
    ): List<ProgressPoint> {
        val monthNames = listOf("Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru")

        return workouts.mapNotNull { workout ->
            val exerciseData = workout.exercises.find { it.exerciseId == exerciseId }

            if (exerciseData != null && workout.endTime != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = workout.endTime.seconds * 1000
                val month = calendar.get(Calendar.MONTH)

                val (value, bestSet, tonnage) = when (metric) {
                    ProgressMetric.MAX_WEIGHT -> {
                        val bestSet = exerciseData.sets.maxByOrNull { it.weight }
                        Triple(bestSet?.weight ?: 0.0, bestSet, 0.0)
                    }
                    ProgressMetric.ONE_RM -> {
                        val bestSet = exerciseData.sets.maxByOrNull { calculate1RM(it.weight, it.reps) }
                        val oneRM = bestSet?.let { calculate1RM(it.weight, it.reps) } ?: 0.0
                        Triple(oneRM, bestSet, 0.0)
                    }
                    ProgressMetric.VOLUME -> {
                        val bestSet = exerciseData.sets.maxByOrNull { calculateVolume(it.weight, it.reps) }
                        val volume = bestSet?.let { calculateVolume(it.weight, it.reps) } ?: 0.0
                        Triple(volume, bestSet, 0.0)
                    }
                    ProgressMetric.TONNAGE -> {
                        val tonnage = calculateTonnage(exerciseData.sets)
                        val bestSet = exerciseData.sets.maxByOrNull { it.weight }
                        Triple(tonnage, bestSet, tonnage)
                    }
                }

                if (bestSet != null) {
                    ProgressPoint(
                        timestamp = workout.endTime.seconds,
                        weight = value,
                        reps = if (metric == ProgressMetric.ONE_RM) 1 else bestSet.reps,
                        label = monthNames[month],
                        originalWeight = bestSet.weight,
                        originalReps = bestSet.reps,
                        volume = calculateVolume(bestSet.weight, bestSet.reps),
                        tonnage = tonnage
                    )
                } else null
            } else null
        }.groupBy { it.label }
            .mapValues { entry ->
                // Dla każdego miesiąca, wybierz najlepszy wynik według wybranej metryki
                entry.value.maxByOrNull { point -> point.weight }
            }
            .values
            .filterNotNull()
            .sortedBy { it.timestamp }
    }

    private fun calculateYearlyProgressPointsForMetric(
        workouts: List<CompletedWorkout>,
        exerciseId: String,
        metric: ProgressMetric
    ): List<ProgressPoint> {
        val monthNames = listOf("Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru")

        return workouts.mapNotNull { workout ->
            val exerciseData = workout.exercises.find { it.exerciseId == exerciseId }

            if (exerciseData != null && workout.endTime != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = workout.endTime.seconds * 1000
                val month = calendar.get(Calendar.MONTH)

                val (value, bestSet, tonnage) = when (metric) {
                    ProgressMetric.MAX_WEIGHT -> {
                        val bestSet = exerciseData.sets.maxByOrNull { it.weight }
                        Triple(bestSet?.weight ?: 0.0, bestSet, 0.0)
                    }
                    ProgressMetric.ONE_RM -> {
                        val bestSet = exerciseData.sets.maxByOrNull { calculate1RM(it.weight, it.reps) }
                        val oneRM = bestSet?.let { calculate1RM(it.weight, it.reps) } ?: 0.0
                        Triple(oneRM, bestSet, 0.0)
                    }
                    ProgressMetric.VOLUME -> {
                        val bestSet = exerciseData.sets.maxByOrNull { calculateVolume(it.weight, it.reps) }
                        val volume = bestSet?.let { calculateVolume(it.weight, it.reps) } ?: 0.0
                        Triple(volume, bestSet, 0.0)
                    }
                    ProgressMetric.TONNAGE -> {
                        val tonnage = calculateTonnage(exerciseData.sets)
                        val bestSet = exerciseData.sets.maxByOrNull { it.weight }
                        Triple(tonnage, bestSet, tonnage)
                    }
                }

                if (bestSet != null) {
                    ProgressPoint(
                        timestamp = workout.endTime.seconds,
                        weight = value,
                        reps = if (metric == ProgressMetric.ONE_RM) 1 else bestSet.reps,
                        label = monthNames[month],
                        originalWeight = bestSet.weight,
                        originalReps = bestSet.reps,
                        volume = calculateVolume(bestSet.weight, bestSet.reps),
                        tonnage = tonnage
                    )
                } else null
            } else null
        }.groupBy { it.label }
            .mapValues { entry ->
                // Dla każdego miesiąca w roku, wybierz najlepszy wynik według wybranej metryki
                entry.value.maxByOrNull { point -> point.weight }
            }
            .values
            .filterNotNull()
            .sortedBy { it.timestamp }
    }

    private fun calculateExerciseProgress(
        exerciseId: String,
        workouts: List<CompletedWorkout>,
        timePeriod: TimePeriod,
        metric: ProgressMetric = selectedProgressMetric.value
    ): ProgressData? {
        return calculateExerciseProgressWithMetric(exerciseId, workouts, timePeriod, metric)
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

        // Podstawowe statystyki
        val personalBest = allSets.maxByOrNull { it.weight }
        val averageWeight = allSets.map { it.weight }.average()
        val averageReps = allSets.map { it.reps }.average()
        val averageSets = exerciseWorkouts.map { workout ->
            workout.exercises.filter { it.exerciseId == exercise.id }.sumOf { it.sets.size }
        }.average()

        // Oblicz statystyki dla różnych metryk
        val personalBest1RM = allSets.maxOfOrNull { calculate1RM(it.weight, it.reps) } ?: 0.0
        val average1RM = allSets.map { calculate1RM(it.weight, it.reps) }.average()

        val personalBestVolume = allSets.maxOfOrNull { calculateVolume(it.weight, it.reps) } ?: 0.0
        val averageVolume = allSets.map { calculateVolume(it.weight, it.reps) }.average()

        // Oblicz tonaż na trening
        val workoutTonnages = exerciseWorkouts.map { workout ->
            val exerciseSets = workout.exercises
                .filter { it.exerciseId == exercise.id }
                .flatMap { it.sets }
            calculateTonnage(exerciseSets)
        }
        val personalBestTonnage = workoutTonnages.maxOrNull() ?: 0.0
        val averageTonnage = if (workoutTonnages.isNotEmpty()) workoutTonnages.average() else 0.0

        // Oblicz progress points dla wybranej metryki
        val progressPoints = when (timePeriod) {
            TimePeriod.WEEK -> calculateWeeklyProgressPointsForMetric(exerciseWorkouts, exercise.id, selectedProgressMetric.value)
            TimePeriod.MONTH -> calculateMonthlyProgressPointsForMetric(exerciseWorkouts, exercise.id, selectedProgressMetric.value)
            TimePeriod.THREE_MONTHS -> calculateThreeMonthsProgressPointsForMetric(exerciseWorkouts, exercise.id, selectedProgressMetric.value)
            TimePeriod.YEAR -> calculateYearlyProgressPointsForMetric(exerciseWorkouts, exercise.id, selectedProgressMetric.value)
            TimePeriod.ALL -> calculateAllTimeProgressPointsForMetric(exerciseWorkouts, exercise.id, selectedProgressMetric.value)
        }

        return ExerciseStatisticsData(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            personalBestWeight = personalBest?.weight ?: 0.0,
            personalBestReps = personalBest?.reps ?: 0,
            personalBest1RM = personalBest1RM,
            personalBestVolume = personalBestVolume,
            personalBestTonnage = personalBestTonnage,
            averageWeight = averageWeight,
            averageReps = averageReps.toInt(),
            average1RM = average1RM,
            averageVolume = averageVolume,
            averageTonnage = averageTonnage,
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

    // Helper methods for calculating activity data - POPRAWIONE
    private fun calculateWeeklyActivity(workouts: List<CompletedWorkout>): List<ActivityData> {
        val daysOfWeek = listOf("Pon", "Wt", "Śr", "Czw", "Pt", "Sob", "Nd")
        val today = Calendar.getInstance()

        // Znajdź poniedziałek bieżącego tygodnia
        val startOfWeek = Calendar.getInstance().apply {
            time = today.time
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
            add(Calendar.DAY_OF_YEAR, -daysFromMonday)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return (0..6).map { dayOffset ->
            val currentDay = Calendar.getInstance().apply {
                time = startOfWeek.time
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }

            val nextDay = Calendar.getInstance().apply {
                time = currentDay.time
                add(Calendar.DAY_OF_YEAR, 1)
            }

            val dayWorkouts = workouts.filter { workout ->
                workout.endTime?.let { endTime ->
                    val workoutTimeMillis = endTime.seconds * 1000
                    workoutTimeMillis >= currentDay.timeInMillis && workoutTimeMillis < nextDay.timeInMillis
                } ?: false
            }

            ActivityData(
                label = daysOfWeek[dayOffset],
                minutes = dayWorkouts.sumOf { it.duration }.toInt()
            )
        }
    }

    private fun calculateMonthlyActivity(workouts: List<CompletedWorkout>): List<ActivityData> {
        val currentCalendar = Calendar.getInstance()
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val currentYear = currentCalendar.get(Calendar.YEAR)

        return (1..4).map { week ->
            val weekWorkouts = workouts.filter { workout ->
                workout.endTime?.let { endTime ->
                    val workoutCalendar = Calendar.getInstance()
                    workoutCalendar.timeInMillis = endTime.seconds * 1000

                    // Sprawdzamy czy trening jest z bieżącego miesiąca i roku
                    val workoutMonth = workoutCalendar.get(Calendar.MONTH)
                    val workoutYear = workoutCalendar.get(Calendar.YEAR)
                    val weekOfMonth = workoutCalendar.get(Calendar.WEEK_OF_MONTH)

                    workoutMonth == currentMonth &&
                            workoutYear == currentYear &&
                            weekOfMonth == week
                } ?: false
            }

            ActivityData(
                label = "Tydz. $week",
                minutes = weekWorkouts.sumOf { it.duration }.toInt()
            )
        }
    }

    private fun calculateThreeMonthsActivity(workouts: List<CompletedWorkout>): List<ActivityData> {
        val monthNames = listOf("Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru")
        val currentCalendar = Calendar.getInstance()

        return (0..2).map { monthOffset ->
            // Tworzymy nowy Calendar dla każdej iteracji
            val targetCalendar = Calendar.getInstance().apply {
                time = currentCalendar.time
                add(Calendar.MONTH, -monthOffset)
            }

            val targetMonth = targetCalendar.get(Calendar.MONTH)
            val targetYear = targetCalendar.get(Calendar.YEAR)

            val monthWorkouts = workouts.filter { workout ->
                workout.endTime?.let { endTime ->
                    val workoutCalendar = Calendar.getInstance()
                    workoutCalendar.timeInMillis = endTime.seconds * 1000
                    workoutCalendar.get(Calendar.MONTH) == targetMonth &&
                            workoutCalendar.get(Calendar.YEAR) == targetYear
                } ?: false
            }

            ActivityData(
                label = monthNames[targetMonth],
                minutes = monthWorkouts.sumOf { it.duration }.toInt()
            )
        }.reversed() // Odwracamy żeby mieć chronologiczny porządek
    }

    private fun calculateYearlyActivity(workouts: List<CompletedWorkout>): List<ActivityData> {
        val monthNames = listOf("Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru")
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        return (0..11).map { month ->
            val monthWorkouts = workouts.filter { workout ->
                workout.endTime?.let { endTime ->
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = endTime.seconds * 1000
                    calendar.get(Calendar.MONTH) == month &&
                            calendar.get(Calendar.YEAR) == currentYear
                } ?: false
            }

            ActivityData(
                label = monthNames[month],
                minutes = monthWorkouts.sumOf { it.duration }.toInt()
            )
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
                minutes = yearWorkouts.sumOf { it.duration }.toInt()
            )
        }.sortedBy { it.label.toIntOrNull() ?: 0 }
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

    /**
     * Oblicza teoretyczne 1RM używając wzoru Brzycki
     */
    private fun calculate1RM(weight: Double, reps: Int): Double {
        return if (reps == 1) {
            weight
        } else if (reps > 10) {
            weight / (1.0278 - (0.0278 * 10)) // Limitujemy do 10 reps
        } else {
            weight / (1.0278 - (0.0278 * reps))
        }
    }

    /**
     * Oblicza objętość (waga × reps)
     */
    private fun calculateVolume(weight: Double, reps: Int): Double {
        return weight * reps
    }

    /**
     * Oblicza tonaż dla całego treningu dla danego ćwiczenia
     */
    private fun calculateTonnage(exerciseSets: List<ExerciseSet>): Double {
        return exerciseSets.sumOf { set ->
            (set.weight * set.reps).toDouble()
        }
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