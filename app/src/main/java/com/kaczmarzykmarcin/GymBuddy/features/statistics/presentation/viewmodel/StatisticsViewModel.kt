package com.kaczmarzykmarcin.GymBuddy.features.statistics.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kaczmarzykmarcin.GymBuddy.data.model.*
import com.kaczmarzykmarcin.GymBuddy.data.repository.ExerciseRepository
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
    private val userRepository: UserRepository,
    private val exerciseRepository: ExerciseRepository // DODANY DEPENDENCY
) : ViewModel() {

    private val TAG = "StatisticsViewModel"

    // Current user ID
    private val currentUserId = auth.currentUser?.uid ?: ""

    // Cache for exercises to avoid repeated database calls
    private val exerciseCache = mutableMapOf<String, Exercise>()

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
        selectedTimePeriod,
        selectedProgressMetric
    ) { exercise, workouts, timePeriod, metric ->
        if (exercise != null) {
            calculateExerciseStatistics(exercise, workouts, timePeriod, metric)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // ===== NOWE METODY DLA PROFILU =====

    /**
     * Pobiera główną grupę mięśniową dla ćwiczenia z cache lub bazy danych
     */
    private suspend fun getPrimaryMuscleForExercise(exerciseId: String): String {
        return try {
            // Sprawdź cache
            exerciseCache[exerciseId]?.let { exercise ->
                return exercise.primaryMuscles.firstOrNull() ?: ""
            }

            // Pobierz z bazy danych
            val result = exerciseRepository.getExerciseById(exerciseId)
            if (result.isSuccess) {
                val exercise = result.getOrNull()!!
                exerciseCache[exerciseId] = exercise // Dodaj do cache
                exercise.primaryMuscles.firstOrNull() ?: ""
            } else {
                // Fallback do mapowania po nazwie
                getFallbackMuscleGroup(exerciseId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting primary muscle for exercise $exerciseId", e)
            getFallbackMuscleGroup(exerciseId)
        }
    }

    /**
     * Fallback mapowanie gdy nie można pobrać z bazy danych
     */
    private fun getFallbackMuscleGroup(exerciseId: String): String {
        return when {
            exerciseId.contains("squat", ignoreCase = true) -> "quadriceps"
            exerciseId.contains("bench", ignoreCase = true) -> "chest"
            exerciseId.contains("deadlift", ignoreCase = true) -> "hamstrings"
            exerciseId.contains("press", ignoreCase = true) -> "shoulders"
            exerciseId.contains("curl", ignoreCase = true) -> "biceps"
            exerciseId.contains("extension", ignoreCase = true) -> "triceps"
            exerciseId.contains("row", ignoreCase = true) -> "lats"
            exerciseId.contains("pull", ignoreCase = true) -> "lats"
            exerciseId.contains("push", ignoreCase = true) -> "chest"
            exerciseId.contains("leg", ignoreCase = true) -> "quadriceps"
            exerciseId.contains("calf", ignoreCase = true) -> "calves"
            exerciseId.contains("shoulder", ignoreCase = true) -> "shoulders"
            else -> "other"
        }
    }

    /**
     * Oblicza ulubioną partię mięśniową użytkownika synchronicznie
     */
    suspend fun calculateFavoriteMuscleGroup(): String {
        val workouts = allWorkouts.value
        if (workouts.isEmpty()) return "Brak danych"

        val muscleGroupCounts = mutableMapOf<String, Int>()

        workouts.forEach { workout ->
            workout.exercises.forEach { exercise ->
                val primaryMuscle = getPrimaryMuscleForExercise(exercise.exerciseId)
                if (primaryMuscle.isNotEmpty()) {
                    muscleGroupCounts[primaryMuscle] = muscleGroupCounts.getOrDefault(primaryMuscle, 0) + 1
                }
            }
        }

        val favoriteGroup = muscleGroupCounts.maxByOrNull { it.value }?.key
        return translateMuscleGroup(favoriteGroup ?: "other")
    }

    /**
     * Pobiera podstawowe statystyki użytkownika dla profilu
     */
    fun getUserBasicStats(): StateFlow<UserBasicStats> = combine(
        allWorkouts,
        userStats
    ) { workouts, stats ->
        val completedWorkouts = workouts.filter { it.endTime != null }

        // Oblicz ulubioną grupę mięśniową w tle
        var favoriteMuscle = "Ładowanie..."
        viewModelScope.launch {
            try {
                favoriteMuscle = calculateFavoriteMuscleGroup()
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating favorite muscle group", e)
                favoriteMuscle = "Błąd"
            }
        }

        UserBasicStats(
            totalWorkouts = completedWorkouts.size,
            currentLevel = stats?.level ?: 1,
            currentXP = stats?.xp ?: 0,
            favoriteMuscleGroup = favoriteMuscle,
            totalWorkoutTime = completedWorkouts.sumOf { it.duration }
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, UserBasicStats())

    /**
     * Model danych dla podstawowych statystyk użytkownika
     */
    data class UserBasicStats(
        val totalWorkouts: Int = 0,
        val currentLevel: Int = 1,
        val currentXP: Int = 0,
        val favoriteMuscleGroup: String = "Brak danych",
        val totalWorkoutTime: Long = 0
    )

    /**
     * Tłumaczy angielskie nazwy grup mięśniowych na polskie
     */
    private fun translateMuscleGroup(muscle: String): String {
        return when (muscle.lowercase()) {
            "chest" -> "Klatka"
            "back", "lats", "middle back", "lower back" -> "Plecy"
            "legs", "quadriceps", "hamstrings" -> "Nogi"
            "shoulders" -> "Barki"
            "biceps" -> "Biceps"
            "triceps" -> "Triceps"
            "abs", "abdominals" -> "Brzuch"
            "glutes" -> "Pośladki"
            "calves" -> "Łydki"
            "forearms" -> "Przedramiona"
            "traps" -> "Kaptur"
            else -> "Inne"
        }
    }

    /**
     * Metoda do czyszczenia cache (przydatna przy testach)
     */
    fun clearExerciseCache() {
        exerciseCache.clear()
    }

    // ===== POZOSTAŁE METODY (bez zmian) =====

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
                        weight = value, // GŁÓWNA WARTOŚĆ WEDŁUG WYBRANEJ METRYKI
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
                        weight = value, // WARTOŚĆ WEDŁUG WYBRANEJ METRYKI
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
                // Wybierz najlepszy wynik według wybranej metryki
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
        timePeriod: TimePeriod,
        metric: ProgressMetric
    ): ExerciseStatisticsData? {
        val allExerciseWorkouts = allWorkouts
            .filter { workout -> workout.exercises.any { it.exerciseId == exercise.id } }

        val allSetsEver = allExerciseWorkouts
            .flatMap { workout ->
                workout.exercises
                    .filter { it.exerciseId == exercise.id }
                    .flatMap { it.sets }
            }

        if (allSetsEver.isEmpty()) return null

        val filteredWorkouts = filterWorkoutsByTimePeriod(allWorkouts, timePeriod)
        val exerciseWorkouts = filteredWorkouts
            .filter { workout -> workout.exercises.any { it.exerciseId == exercise.id } }

        val allSets = exerciseWorkouts
            .flatMap { workout ->
                workout.exercises
                    .filter { it.exerciseId == exercise.id }
                    .flatMap { it.sets }
            }

        if (allSets.isEmpty()) return null

        val personalBestWeightSet = allSetsEver.maxByOrNull { it.weight }
        val personalBestWeight = personalBestWeightSet?.weight ?: 0.0
        val personalBest1RM = allSetsEver.maxOfOrNull { calculate1RM(it.weight, it.reps) } ?: 0.0

        val totalReps = allSets.sumOf { it.reps }

        val averageWeight = allSets.map { it.weight }.average()
        val averageReps = allSets.map { it.reps }.average()
        val averageSets = exerciseWorkouts.map { workout ->
            workout.exercises.filter { it.exerciseId == exercise.id }.sumOf { it.sets.size }
        }.average()

        val average1RM = allSets.map { calculate1RM(it.weight, it.reps) }.average()
        val personalBestVolume = allSets.maxOfOrNull { calculateVolume(it.weight, it.reps) } ?: 0.0
        val averageVolume = allSets.map { calculateVolume(it.weight, it.reps) }.average()

        val workoutTonnages = exerciseWorkouts.map { workout ->
            val exerciseSets = workout.exercises
                .filter { it.exerciseId == exercise.id }
                .flatMap { it.sets }
            calculateTonnage(exerciseSets)
        }
        val personalBestTonnage = workoutTonnages.maxOrNull() ?: 0.0
        val averageTonnage = if (workoutTonnages.isNotEmpty()) workoutTonnages.average() else 0.0

        val progressPoints = when (timePeriod) {
            TimePeriod.WEEK -> calculateWeeklyProgressPointsForMetric(exerciseWorkouts, exercise.id, metric)
            TimePeriod.MONTH -> calculateMonthlyProgressPointsForMetric(exerciseWorkouts, exercise.id, metric)
            TimePeriod.THREE_MONTHS -> calculateThreeMonthsProgressPointsForMetric(exerciseWorkouts, exercise.id, metric)
            TimePeriod.YEAR -> calculateYearlyProgressPointsForMetric(exerciseWorkouts, exercise.id, metric)
            TimePeriod.ALL -> calculateAllTimeProgressPointsForMetric(exerciseWorkouts, exercise.id, metric)
        }

        return ExerciseStatisticsData(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            personalBestWeight = personalBestWeight,
            totalReps = totalReps,
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
            return workouts
        }

        val now = Calendar.getInstance()
        val startTime = Calendar.getInstance()

        when (timePeriod) {
            TimePeriod.WEEK -> startTime.add(Calendar.DAY_OF_YEAR, -7)
            TimePeriod.MONTH -> startTime.add(Calendar.MONTH, -1)
            TimePeriod.THREE_MONTHS -> startTime.add(Calendar.MONTH, -3)
            TimePeriod.YEAR -> startTime.add(Calendar.YEAR, -1)
            TimePeriod.ALL -> return workouts
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
        val today = Calendar.getInstance()

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
        }.reversed()
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

        val workoutsByYear = workouts.groupBy { workout ->
            workout.endTime?.let { endTime ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = endTime.seconds * 1000
                calendar.get(Calendar.YEAR)
            } ?: 0
        }.filterKeys { it != 0 }

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
            weight / (1.0278 - (0.0278 * 10))
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