package com.kaczmarzykmarcin.GymBuddy.features.workout.presentation.viewmodel

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.kaczmarzykmarcin.GymBuddy.R
import com.kaczmarzykmarcin.GymBuddy.core.data.model.PreviousSetInfo
import com.kaczmarzykmarcin.GymBuddy.data.model.CompletedExercise
import com.kaczmarzykmarcin.GymBuddy.data.model.CompletedWorkout
import com.kaczmarzykmarcin.GymBuddy.data.model.Exercise
import com.kaczmarzykmarcin.GymBuddy.data.model.ExerciseSet
import com.kaczmarzykmarcin.GymBuddy.data.model.ExerciseStat
import com.kaczmarzykmarcin.GymBuddy.data.model.WorkoutCategory
import com.kaczmarzykmarcin.GymBuddy.data.model.WorkoutTemplate
import com.kaczmarzykmarcin.GymBuddy.data.repository.ExerciseRepository
import com.kaczmarzykmarcin.GymBuddy.data.repository.UserRepository
import com.kaczmarzykmarcin.GymBuddy.data.repository.WorkoutCategoryRepository
import com.kaczmarzykmarcin.GymBuddy.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val userRepository: UserRepository,
    private val workoutCategoryRepository: WorkoutCategoryRepository
) : ViewModel() {

    private val TAG = "WorkoutViewModel"

    // Current user ID
    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId

    // Workout templates
    private val _workoutTemplates = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
    val workoutTemplates = _workoutTemplates.asStateFlow()

    // Filtered workout templates (for search functionality)
    private val _filteredWorkoutTemplates = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
    val filteredWorkoutTemplates = _filteredWorkoutTemplates.asStateFlow()

    // Search query for templates
    private val _templateSearchQuery = MutableStateFlow("")
    val templateSearchQuery: StateFlow<String> = _templateSearchQuery

    // Active workout (if any)
    private val _activeWorkout = MutableStateFlow<CompletedWorkout?>(null)
    val activeWorkout = _activeWorkout.asStateFlow()

    // Available exercises for selection
    private val _exercisesList = MutableStateFlow<List<Exercise>>(emptyList())
    val exercisesList = _exercisesList.asStateFlow()

    // Exercise stats for the current user
    private val _exerciseStats = MutableStateFlow<Map<String, ExerciseStat>>(emptyMap())
    val exerciseStats = _exerciseStats.asStateFlow()

    // Selected category ID for filtering
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId

    // Lista wszystkich kategorii
    private val _categoriesFlow = MutableStateFlow<List<WorkoutCategory>>(emptyList())
    val categories = _categoriesFlow.asStateFlow()

    // Treningi filtrowane po kategorii
    val filteredWorkouts = combine(
        workoutRepository.getUserWorkoutHistory(_currentUserId.value),
        selectedCategoryId
    ) { workoutsList, categoryId ->
        if (categoryId == null) {
            workoutsList
        } else {
            workoutsList.filter { it.categoryId == categoryId }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // Job for time tracking
    private var timeTrackingJob: Job? = null

    /**
     * Przechowuje poprzednie wartości serii dla danego ćwiczenia
     */
    private val _previousSetsMap = MutableStateFlow<Map<String, List<PreviousSetInfo>>>(emptyMap())
    val previousSetsMap = _previousSetsMap.asStateFlow()

    // Flag showing workout recorder
    private val _showWorkoutRecorder = MutableStateFlow(false)
    val showWorkoutRecorder = _showWorkoutRecorder.asStateFlow()

    init {
        // Get current user ID from Firebase Auth
        auth.currentUser?.let { user ->
            _currentUserId.value = user.uid
            checkActiveWorkout(user.uid)
            // Initialize default categories
            initializeCategories(user.uid)
            // Ładuj kategorie przy inicjalizacji ViewModel
            loadCategories()
        }
    }

    /**
     * Updates search query for templates and applies filtering
     */
    fun updateTemplateSearchQuery(query: String) {
        _templateSearchQuery.value = query
        applyTemplateFilters()
    }

    /**
     * Applies search filter to workout templates
     */
    private fun applyTemplateFilters() {
        viewModelScope.launch {
            var filtered = _workoutTemplates.value

            // Apply search query filter if not empty
            val query = _templateSearchQuery.value.trim().lowercase()
            if (query.isNotEmpty()) {
                filtered = filtered.filter { template ->
                    template.name.lowercase().contains(query) ||
                            template.description.lowercase().contains(query)
                }
                Log.d(TAG, "Applied template search filter: '$query', results: ${filtered.size}")
            }

            _filteredWorkoutTemplates.value = filtered
        }
    }

    /**
     * Inicjalizuje kategorie treningowe dla użytkownika
     */
    private fun initializeCategories(userId: String) {
        viewModelScope.launch {
            workoutCategoryRepository.initializeDefaultCategories(userId)
        }
    }

    /**
     * Ładuje kategorie treningowe dla użytkownika
     */
    fun loadCategories() {
        viewModelScope.launch {
            val userId = _currentUserId.value
            if (userId.isNotBlank()) {
                workoutCategoryRepository.getUserWorkoutCategories(userId)
                    .collect { categoriesList ->
                        _categoriesFlow.value = categoriesList
                        Log.d(TAG, "Loaded ${categoriesList.size} categories")
                    }
            }
        }
    }

    /**
     * Dodaje nową kategorię
     */
    fun addCategory(name: String, color: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            val newCategory = WorkoutCategory(
                id = UUID.randomUUID().toString(),
                userId = _currentUserId.value,
                name = name,
                color = color
            )
            val result = workoutCategoryRepository.createWorkoutCategory(newCategory)
            Log.d(TAG, "Category added: ${newCategory.name}")

            // Ręczne odświeżenie listy kategorii po dodaniu
            // To zapewnia, że kategoria pojawi się na liście nawet jeśli Flow w repozytorium
            // nie emituje nowych wartości
            val updatedCategories = _categoriesFlow.value.toMutableList()
            updatedCategories.add(newCategory)
            _categoriesFlow.value = updatedCategories
        }
    }

    /**
     * Aktualizuje kategorię
     */
    fun updateCategory(category: WorkoutCategory) {
        if (category.name.isBlank()) return

        viewModelScope.launch {
            val result = workoutCategoryRepository.updateWorkoutCategory(category)
            Log.d(TAG, "Category updated: ${category.name}")

            // Ręczne odświeżenie listy kategorii po aktualizacji
            val updatedCategories = _categoriesFlow.value.toMutableList()
            val index = updatedCategories.indexOfFirst { it.id == category.id }
            if (index != -1) {
                updatedCategories[index] = category
                _categoriesFlow.value = updatedCategories
            }
        }
    }

    /**
     * Usuwa kategorię
     */
    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            // Sprawdź czy istnieją treningi z tą kategorią
            val workouts = workoutRepository.getUserWorkoutHistory(_currentUserId.value)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Lazily,
                    initialValue = emptyList()
                ).value

            val templates = workoutRepository.getUserWorkoutTemplates(_currentUserId.value)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Lazily,
                    initialValue = emptyList()
                ).value

            // Jeśli istnieją treningi z tą kategorią, zmień ich kategorię na null
            workouts.filter { it.categoryId == categoryId }.forEach { workout ->
                workoutRepository.updateWorkout(workout.copy(categoryId = null))
            }

            // Zmień kategorię szablonów treningów na null
            templates.filter { it.categoryId == categoryId }.forEach { template ->
                workoutRepository.updateWorkoutTemplate(template.copy(categoryId = null))
            }

            // Usuń kategorię
            val result = workoutCategoryRepository.deleteWorkoutCategory(categoryId)
            Log.d(TAG, "Category deleted: $categoryId")

            // Ręczne odświeżenie listy kategorii po usunięciu
            val updatedCategories = _categoriesFlow.value.toMutableList()
            updatedCategories.removeAll { it.id == categoryId }
            _categoriesFlow.value = updatedCategories

            // Jeśli usunięta kategoria była wybrana, zresetuj filtr
            if (_selectedCategoryId.value == categoryId) {
                _selectedCategoryId.value = null
            }
        }
    }

    /**
     * Metoda do zmiany wybranej kategorii dla filtrowania
     */
    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    /**
     * Pobiera kategorię po ID
     */
    suspend fun getCategory(categoryId: String?): WorkoutCategory? {
        if (categoryId == null) return null

        return workoutCategoryRepository.getWorkoutCategory(categoryId).getOrNull()
    }

    /**
     * Loads user's workout templates from the repository
     */
    fun loadWorkoutTemplates(userId: String) {
        viewModelScope.launch {
            try {
                workoutRepository.getUserWorkoutTemplates(userId).collect { templates ->
                    _workoutTemplates.value = templates
                    // Apply current filters to the new data
                    applyTemplateFilters()
                    Log.d(TAG, "Loaded ${templates.size} workout templates")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading workout templates", e)
            }
        }
    }

    /**
     * Loads all available exercises from the repository
     */
    fun loadAllExercises() {
        viewModelScope.launch {
            try {
                // Initialize database if needed
                exerciseRepository.initializeExerciseDatabase()

                // Get all exercises
                exerciseRepository.getAllExercises().collect { exercises ->
                    _exercisesList.value = exercises
                    Log.d(TAG, "Loaded ${exercises.size} exercises")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading exercises", e)
            }
        }
    }

    /**
     * Starts a new workout for the user
     */
    fun startNewWorkout(userId: String, categoryId: String? = null) {
        viewModelScope.launch {
            try {
                // Check if there's already an active workout
                if (_activeWorkout.value != null) {
                    // Don't create a new one, just return
                    return@launch
                }

                // Generate workout name based on time of day
                val workoutName = generateWorkoutNameBasedOnTimeOfDay()

                // Create a new workout
                val newWorkout = CompletedWorkout(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = workoutName,
                    categoryId = categoryId,
                    startTime = Timestamp.now(),
                    exercises = emptyList()
                )

                // Save to repository
                val result = workoutRepository.startWorkout(newWorkout)
                if (result.isSuccess) {
                    val savedWorkout = result.getOrNull()
                    if (savedWorkout != null) {
                        _activeWorkout.value = savedWorkout
                        Log.d(TAG, "Started new workout: ${savedWorkout.id}")

                        // Start time tracking for the new workout
                        startWorkoutTimeTracking()

                        // Show workout recorder
                        showWorkoutRecorder(true)
                    }
                } else {
                    Log.e(TAG, "Failed to start workout: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting new workout", e)
            }
        }
    }

    /**
     * Checks if the user has an active workout
     */
    fun checkActiveWorkout(userId: String) {
        viewModelScope.launch {
            try {
                val result = workoutRepository.getActiveWorkout(userId)
                if (result.isSuccess) {
                    val activeWorkout = result.getOrNull()

                    // Only set as active if it has no endTime (i.e., not finished or canceled)
                    if (activeWorkout != null && activeWorkout.endTime == null) {
                        _activeWorkout.value = activeWorkout
                        Log.d(TAG, "Found active workout: ${activeWorkout.id}")
                        // Start time tracking if there's an active workout
                        startWorkoutTimeTracking()
                    } else {
                        // If workout is finished or canceled, make sure activeWorkout is null
                        _activeWorkout.value = null
                        // Also ensure workout recorder is hidden
                        _showWorkoutRecorder.value = false
                        Log.d(TAG, "No active workout found or workout was completed/canceled")
                    }
                } else {
                    Log.e(TAG, "Error checking active workout: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception checking active workout", e)
            }
        }
    }

    /**
     * Updates the current active workout
     */
    fun updateWorkout(workout: CompletedWorkout) {
        viewModelScope.launch {
            try {
                val result = workoutRepository.updateWorkout(workout)
                if (result.isSuccess) {
                    // Update the activeWorkout state with the latest version
                    _activeWorkout.value = workout
                    Log.d(TAG, "Updated workout: ${workout.id}")
                } else {
                    Log.e(TAG, "Failed to update workout: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating workout", e)
            }
        }
    }

    /**
     * Finishes the current workout and saves it to history
     */
    fun finishWorkout(workout: CompletedWorkout) {
        viewModelScope.launch {
            try {
                // First update the workout with end time and duration if needed
                val workoutToFinish = if (workout.endTime == null) {
                    val endTime = Timestamp.now()
                    val duration = endTime.seconds - workout.startTime.seconds // Teraz w sekundach zamiast minut
                    workout.copy(
                        endTime = endTime,
                        duration = duration
                    )
                } else {
                    workout
                }

                // Save to repository
                val result = workoutRepository.finishWorkout(workoutToFinish.id)
                if (result.isSuccess) {
                    // Update user stats with the completed workout
                    val statsResult = userRepository.updateUserStatsAfterWorkout(
                        userId = workout.userId,
                        completedWorkout = workoutToFinish
                    )

                    if (statsResult.isSuccess) {
                        Log.d(TAG, "Updated user stats after workout")
                    } else {
                        Log.e(TAG, "Failed to update user stats: ${statsResult.exceptionOrNull()?.message}")
                    }

                    // Clear the active workout
                    _activeWorkout.value = null

                    // Hide the workout recorder
                    _showWorkoutRecorder.value = false

                    // Stop time tracking
                    timeTrackingJob?.cancel()
                    timeTrackingJob = null

                    Log.d(TAG, "Finished workout: ${workoutToFinish.id}")
                } else {
                    Log.e(TAG, "Failed to finish workout: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error finishing workout", e)
            }
        }
    }

    /**
     * Cancels the current workout
     */
    fun cancelWorkout(workoutId: String) {
        viewModelScope.launch {
            try {
                // First update the workout with end time
                val currentWorkout = _activeWorkout.value
                if (currentWorkout != null && currentWorkout.id == workoutId) {
                    // Update the workout with an end time to mark it as canceled
                    val canceledWorkout = currentWorkout.copy(
                        endTime = Timestamp.now()
                    )
                    // Update in repository before canceling
                    workoutRepository.updateWorkout(canceledWorkout)
                }

                val result = workoutRepository.cancelWorkout(workoutId)
                if (result.isSuccess) {
                    // Clear the active workout immediately
                    _activeWorkout.value = null

                    // Hide the workout recorder
                    _showWorkoutRecorder.value = false

                    // Stop time tracking
                    timeTrackingJob?.cancel()
                    timeTrackingJob = null

                    Log.d(TAG, "Canceled workout: $workoutId")
                } else {
                    Log.e(TAG, "Failed to cancel workout: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling workout", e)
            }
        }
    }

    /**
     * Loads exercise stats for the current user
     */
    fun loadExerciseStats(userId: String) {
        viewModelScope.launch {
            try {
                val result = userRepository.getUserStats(userId)
                if (result.isSuccess) {
                    val stats = result.getOrNull()
                    if (stats != null) {
                        _exerciseStats.value = stats.exerciseStats
                        Log.d(TAG, "Loaded exercise stats for ${stats.exerciseStats.size} exercises")
                    }
                } else {
                    Log.e(TAG, "Failed to load user stats: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading exercise stats", e)
            }
        }
    }

    /**
     * Gets stats for a specific exercise
     */
    fun getExerciseStatsForId(exerciseId: String): StateFlow<Map<String, Any?>?> {
        val statsFlow = MutableStateFlow<Map<String, Any?>?>(null)

        viewModelScope.launch {
            val stats = _exerciseStats.value[exerciseId]
            if (stats != null) {
                statsFlow.value = stats.toMap()
            }
        }

        return statsFlow
    }

    /**
     * Starts tracking workout time (updates every second)
     */
    fun startWorkoutTimeTracking() {
        // Cancel any existing job
        timeTrackingJob?.cancel()

        // Start a new job
        timeTrackingJob = viewModelScope.launch {
            while (true) {
                // Only update if there's an active workout with no end time
                _activeWorkout.value?.let { workout ->
                    if (workout.endTime == null) {
                        // Trigger recomposition by updating the current workout
                        _activeWorkout.update { it }
                    } else {
                        // If workout has ended, cancel the job
                        timeTrackingJob?.cancel()
                    }
                } ?: run {
                    // If activeWorkout is null, cancel the job
                    timeTrackingJob?.cancel()
                    return@launch
                }

                delay(1000) // Update every second
            }
        }
    }

    /**
     * Zmienia widoczność rejestratora treningów
     */
    fun showWorkoutRecorder(show: Boolean) {
        _showWorkoutRecorder.value = show
    }

    /**
     * Pobiera historię treningów użytkownika i aktualizuje mapę poprzednich serii
     */
    fun loadPreviousSetsData(userId: String) {
        viewModelScope.launch {
            try {
                // Pobierz ostatnio zakończone treningi użytkownika
                workoutRepository.getUserWorkoutHistory(userId).collect { workouts ->
                    // Filtrujemy tylko zakończone treningi, sortujemy według daty (od najnowszego)
                    val completedWorkouts = workouts
                        .filter { it.endTime != null }
                        .sortedByDescending { it.endTime?.seconds }

                    if (completedWorkouts.isNotEmpty()) {
                        // Tworzymy mapę poprzednich serii dla każdego ćwiczenia
                        val previousSetsMap = mutableMapOf<String, List<PreviousSetInfo>>()

                        // Dla każdego ćwiczenia szukamy jego ostatniego wystąpienia
                        completedWorkouts.forEach { workout ->
                            workout.exercises.forEach { exercise ->
                                // Jeśli to ćwiczenie nie jest jeszcze w mapie, dodajemy jego serie
                                if (!previousSetsMap.containsKey(exercise.exerciseId)) {
                                    // Konwertujemy serie na PreviousSetInfo
                                    val normalSetCounter = exercise.sets
                                        .filter { it.setType == "normal" }
                                        .withIndex()
                                        .associate { (index, _) -> index to index + 1 }

                                    val previousSets = exercise.sets.mapIndexed { index, set ->
                                        val normalSetNumber = if (set.setType == "normal") {
                                            normalSetCounter[exercise.sets.take(index).count { it.setType == "normal" }] ?: 0
                                        } else {
                                            0
                                        }

                                        PreviousSetInfo(
                                            setType = set.setType,
                                            normalSetNumber = normalSetNumber,
                                            weight = set.weight,
                                            reps = set.reps
                                        )
                                    }

                                    previousSetsMap[exercise.exerciseId] = previousSets
                                }
                            }
                        }

                        _previousSetsMap.value = previousSetsMap
                        Log.d(TAG, "Loaded previous sets data for ${previousSetsMap.size} exercises")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading previous sets data", e)
            }
        }
    }

    /**
     * Pobiera poprzednią serię dla danego ćwiczenia, typu serii i numeru
     */
    fun getPreviousSetInfo(exerciseId: String, setType: String, normalSetNumber: Int): PreviousSetInfo? {
        val previousSets = _previousSetsMap.value[exerciseId] ?: return null

        // Dla serii normalnych szukamy po typie i numerze
        if (setType == "normal") {
            return previousSets.find {
                it.setType == setType && it.normalSetNumber == normalSetNumber
            }
        }

        // Dla innych typów serii (warmup, dropset, failure) szukamy tylko po typie
        return previousSets.find { it.setType == setType }
    }

    /**
     * Generates a workout name based on the time of day
     */
    private fun generateWorkoutNameBasedOnTimeOfDay(): String {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        return when {
            hourOfDay < 12 -> "Poranny Trening"
            hourOfDay < 17 -> "Popołudniowy Trening"
            else -> "Wieczorny Trening"
        }
    }

    /**
     * Pobiera ćwiczenie po ID
     */
    fun getExerciseById(exerciseId: String): Exercise? {
        return _exercisesList.value.find { it.id == exerciseId }
    }

    /**
     * Tworzy nowy szablon treningu
     */
    fun createWorkoutTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            try {
                val result = workoutRepository.createWorkoutTemplate(template)
                if (result.isSuccess) {
                    Log.d(TAG, "Created workout template: ${template.name}")
                    // Reload templates
                    loadWorkoutTemplates(template.userId)
                } else {
                    Log.e(TAG, "Failed to create workout template: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating workout template", e)
            }
        }
    }

    /**
     * Aktualizuje istniejący szablon treningu
     */
    fun updateWorkoutTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            try {
                val result = workoutRepository.updateWorkoutTemplate(template)
                if (result.isSuccess) {
                    Log.d(TAG, "Updated workout template: ${template.name}")
                    // Reload templates
                    loadWorkoutTemplates(template.userId)
                } else {
                    Log.e(TAG, "Failed to update workout template: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating workout template", e)
            }
        }
    }

    /**
     * Usuwa szablon treningu
     */
    fun deleteWorkoutTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                val result = workoutRepository.deleteWorkoutTemplate(templateId)
                if (result.isSuccess) {
                    Log.d(TAG, "Deleted workout template: $templateId")
                    // Reload templates
                    loadWorkoutTemplates(_currentUserId.value)
                } else {
                    Log.e(TAG, "Failed to delete workout template: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting workout template", e)
            }
        }
    }

    /**
     * Tworzy szablon treningowy na podstawie aktualnego treningu
     */
    fun createTemplateFromWorkout(workout: CompletedWorkout) {
        viewModelScope.launch {
            try {
                // Convert CompletedWorkout to WorkoutTemplate with full exercise information
                val template = WorkoutTemplate(
                    id = "",
                    userId = workout.userId,
                    name = "${workout.name} (Template)",
                    description = "",
                    categoryId = workout.categoryId,
                    exercises = workout.exercises // Zachowujemy pełne informacje o ćwiczeniach
                )

                // Create the template
                val result = workoutRepository.createWorkoutTemplate(template)
                if (result.isSuccess) {
                    Log.d(TAG, "Created template from workout: ${template.name}")
                    // Reload templates
                    loadWorkoutTemplates(template.userId)
                } else {
                    Log.e(TAG, "Failed to create template from workout: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating template from workout", e)
            }
        }
    }

    /**
     * Rozpoczyna trening na podstawie szablonu
     */
    fun startWorkoutFromTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            try {
                // Sprawdź, czy użytkownik nie ma już aktywnego treningu
                if (_activeWorkout.value != null) {
                    // W prostszej wersji możemy po prostu pokazać istniejący trening
                    showWorkoutRecorder(true)
                    return@launch
                }

                // Utwórz nowy trening na podstawie szablonu
                // Używamy bezpośrednio listy ćwiczeń z szablonu, która zawiera wszystkie szczegóły
                val newWorkout = CompletedWorkout(
                    id = UUID.randomUUID().toString(),
                    userId = _currentUserId.value,
                    name = template.name,
                    templateId = template.id,
                    categoryId = template.categoryId,
                    startTime = Timestamp.now(),
                    exercises = template.exercises // Bezpośrednio używamy ćwiczeń z szablonu
                )

                // Zapisz trening w repozytorium
                val result = workoutRepository.startWorkout(newWorkout)
                if (result.isSuccess) {
                    val savedWorkout = result.getOrNull()
                    if (savedWorkout != null) {
                        _activeWorkout.value = savedWorkout
                        Log.d(TAG, "Started workout from template: ${template.name}")

                        // Uruchom śledzenie czasu treningu
                        startWorkoutTimeTracking()

                        // Pokaż rejestrację treningu
                        showWorkoutRecorder(true)
                    }
                } else {
                    Log.e(TAG, "Failed to start workout from template: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting workout from template", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timeTrackingJob?.cancel()
    }
}