// BadgesViewModel.kt
package com.kaczmarzykmarcin.GymBuddy.features.badges.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaczmarzykmarcin.GymBuddy.data.model.AchievementType
import com.kaczmarzykmarcin.GymBuddy.data.model.AchievementWithProgress
import com.kaczmarzykmarcin.GymBuddy.data.repository.AchievementRepository
import com.kaczmarzykmarcin.GymBuddy.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow


@HiltViewModel
class BadgesViewModel @Inject constructor(
    private val achievementRepository: AchievementRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val TAG = "BadgesViewModel"

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _recentlyCompleted = MutableStateFlow<List<AchievementWithProgress>>(emptyList())
    val recentlyCompleted: StateFlow<List<AchievementWithProgress>> = _recentlyCompleted.asStateFlow()

    private val _inProgress = MutableStateFlow<List<AchievementWithProgress>>(emptyList())
    val inProgress: StateFlow<List<AchievementWithProgress>> = _inProgress.asStateFlow()

    private val _available = MutableStateFlow<List<AchievementWithProgress>>(emptyList())
    val available: StateFlow<List<AchievementWithProgress>> = _available.asStateFlow()

    private val _userStats = MutableStateFlow(UserProgressStats())
    val userStats: StateFlow<UserProgressStats> = _userStats.asStateFlow()

    // Detail screen state
    private val _filteredBadges = MutableStateFlow<List<AchievementWithProgress>>(emptyList())
    val filteredBadges: StateFlow<List<AchievementWithProgress>> = _filteredBadges.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("all")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    // Cache for all badges
    private var allBadges: List<AchievementWithProgress> = emptyList()
    private var currentDetailCategory: String = ""

    data class UserProgressStats(
        val currentLevel: Int = 1,
        val currentXP: Int = 0,
        val progressToNextLevel: Float = 0f,
        val completedBadges: Int = 0,
        val totalBadges: Int = 0
    )


    // Bottom Sheet State
    private val _showBadgeDetails = MutableSharedFlow<AchievementWithProgress>()
    val showBadgeDetails: SharedFlow<AchievementWithProgress> = _showBadgeDetails.asSharedFlow()

    /**
     * Pokazuje szczegóły odznaki w bottom sheet
     */
    fun showBadgeDetails(badge: AchievementWithProgress) {
        viewModelScope.launch {
            _showBadgeDetails.emit(badge)
        }
    }

    /**
     * Ładuje odznaki użytkownika dla głównego ekranu
     */
    fun loadUserBadges(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Pobierz wszystkie osiągnięcia użytkownika
                val achievementsResult = achievementRepository.getUserAchievements(userId)
                if (achievementsResult.isSuccess) {
                    allBadges = achievementsResult.getOrNull() ?: emptyList()

                    // Podziel na kategorie
                    val completed = allBadges.filter { it.isCompleted }
                        .sortedByDescending { it.progress?.completedAt?.seconds ?: 0 }

                    val inProgressList = allBadges.filter {
                        !it.isCompleted && it.currentValue > 0
                    }.sortedByDescending { it.progressPercentage }

                    val availableList = allBadges.filter {
                        !it.isCompleted && it.currentValue == 0
                    }.sortedBy { it.definition.targetValue }

                    _recentlyCompleted.value = completed
                    _inProgress.value = inProgressList
                    _available.value = availableList

                    Log.d(TAG, "Loaded badges - Completed: ${completed.size}, In Progress: ${inProgressList.size}, Available: ${availableList.size}")
                }

                // Pobierz statystyki użytkownika
                loadUserStats(userId)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading user badges", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Ładuje odznaki dla ekranu szczegółowego
     */
    fun loadDetailBadges(userId: String, category: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                currentDetailCategory = category

                // Jeśli nie mamy jeszcze wszystkich odznak, pobierz je
                if (allBadges.isEmpty()) {
                    val achievementsResult = achievementRepository.getUserAchievements(userId)
                    if (achievementsResult.isSuccess) {
                        allBadges = achievementsResult.getOrNull() ?: emptyList()
                    }
                }

                // Filtruj według kategorii
                val filteredByCategory = when (category) {
                    "completed" -> allBadges.filter { it.isCompleted }
                        .sortedByDescending { it.progress?.completedAt?.seconds ?: 0 }
                    "in_progress" -> allBadges.filter {
                        !it.isCompleted && it.currentValue > 0
                    }.sortedByDescending { it.progressPercentage }
                    "available" -> allBadges.filter {
                        !it.isCompleted && it.currentValue == 0
                    }.sortedBy { it.definition.targetValue }
                    else -> allBadges
                }

                // Zastosuj filtr kategorii
                applyFilterToBadges(filteredByCategory)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading detail badges", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Ustawia filtr kategorii dla ekranu szczegółowego
     */
    fun setCategoryFilter(filter: String) {
        _selectedCategoryFilter.value = filter

        // Ponownie zastosuj filtr do obecnych danych
        val currentBadges = when (currentDetailCategory) {
            "completed" -> allBadges.filter { it.isCompleted }
            "in_progress" -> allBadges.filter { !it.isCompleted && it.currentValue > 0 }
            "available" -> allBadges.filter { !it.isCompleted && it.currentValue == 0 }
            else -> allBadges
        }

        applyFilterToBadges(currentBadges)
    }

    /**
     * Stosuje filtr kategorii do listy odznak
     */
    private fun applyFilterToBadges(badges: List<AchievementWithProgress>) {
        val filtered = if (_selectedCategoryFilter.value == "all") {
            badges
        } else {
            badges.filter { badge ->
                when (_selectedCategoryFilter.value) {
                    "workout_count" -> badge.definition.type == AchievementType.WORKOUT_COUNT
                    "workout_streak" -> badge.definition.type == AchievementType.WORKOUT_STREAK
                    "exercise_weight" -> badge.definition.type == AchievementType.EXERCISE_WEIGHT
                    "workout_duration" -> badge.definition.type == AchievementType.WORKOUT_DURATION
                    "morning_workouts" -> badge.definition.type == AchievementType.MORNING_WORKOUTS
                    else -> true
                }
            }
        }

        _filteredBadges.value = filtered
    }

    /**
     * Ładuje statystyki użytkownika
     */
    private suspend fun loadUserStats(userId: String) {
        try {
            val userStatsResult = userRepository.getUserStats(userId)
            if (userStatsResult.isSuccess) {
                val stats = userStatsResult.getOrNull()!!

                // Oblicz postęp do następnego poziomu
                val xpForCurrentLevel = calculateXPForLevel(stats.level)
                val xpForNextLevel = calculateXPForLevel(stats.level + 1)
                val progressInCurrentLevel = stats.xp - xpForCurrentLevel
                val xpNeededForNext = xpForNextLevel - xpForCurrentLevel
                val progressPercentage = if (xpNeededForNext > 0) {
                    (progressInCurrentLevel.toFloat() / xpNeededForNext.toFloat()).coerceIn(0f, 1f)
                } else {
                    1f
                }

                // Policz zdobyte odznaki
                val completedCount = allBadges.count { it.isCompleted }
                val totalCount = allBadges.size

                _userStats.value = UserProgressStats(
                    currentLevel = stats.level,
                    currentXP = stats.xp,
                    progressToNextLevel = progressPercentage,
                    completedBadges = completedCount,
                    totalBadges = totalCount
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user stats", e)
        }
    }

    /**
     * Oblicza wymagane XP dla danego poziomu
     */
    private fun calculateXPForLevel(level: Int): Int {
        var totalXP = 0
        for (i in 1 until level) {
            totalXP += 100 + (i - 1) * 50
        }
        return totalXP
    }



    /**
     * Odświeża dane odznak
     */
    fun refreshBadges(userId: String) {
        loadUserBadges(userId)
    }
}