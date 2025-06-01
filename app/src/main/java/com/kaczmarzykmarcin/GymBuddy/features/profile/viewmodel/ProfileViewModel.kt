// ProfileViewModel.kt
package com.kaczmarzykmarcin.GymBuddy.features.profile.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kaczmarzykmarcin.GymBuddy.data.model.AchievementWithProgress
import com.kaczmarzykmarcin.GymBuddy.data.model.UserData
import com.kaczmarzykmarcin.GymBuddy.data.repository.AchievementRepository
import com.kaczmarzykmarcin.GymBuddy.data.repository.UserRepository
import com.kaczmarzykmarcin.GymBuddy.features.achievements.domain.AchievementService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val achievementRepository: AchievementRepository,
    private val achievementService: AchievementService
    // USUNIĘTO: FirebaseAuth - wylogowanie jest teraz obsługiwane przez AuthViewModel
) : ViewModel() {

    private val TAG = "ProfileViewModel"

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    private val _inProgressAchievements = MutableStateFlow<List<AchievementWithProgress>>(emptyList())
    val inProgressAchievements: StateFlow<List<AchievementWithProgress>> = _inProgressAchievements.asStateFlow()

    private val _recentAchievements = MutableStateFlow<List<AchievementWithProgress>>(emptyList())
    val recentAchievements: StateFlow<List<AchievementWithProgress>> = _recentAchievements.asStateFlow()

    // Cache dla sprawdzania czy dane już zostały załadowane
    private var currentUserId: String? = null
    private var isDataLoaded = false

    /**
     * Ładuje dane użytkownika i osiągnięcia tylko jeśli nie są już załadowane
     */
    fun loadUserData(userId: String, forceRefresh: Boolean = false) {
        // Sprawdź czy dane już są załadowane dla tego użytkownika
        if (!forceRefresh && currentUserId == userId && isDataLoaded && _userData.value != null) {
            Log.d(TAG, "Data already loaded for user: $userId, skipping")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                currentUserId = userId

                // Załaduj dane użytkownika
                val userDataResult = userRepository.getFullUserData(userId)
                if (userDataResult.isSuccess) {
                    _userData.value = userDataResult.getOrNull()
                    Log.d(TAG, "User data loaded successfully")
                } else {
                    Log.e(TAG, "Failed to load user data: ${userDataResult.exceptionOrNull()?.message}")
                }

                // Załaduj osiągnięcia w trakcie
                loadInProgressAchievements(userId)

                // Załaduj ostatnio zdobyte osiągnięcia
                loadRecentAchievements(userId)

                isDataLoaded = true

            } catch (e: Exception) {
                Log.e(TAG, "Error loading user data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Ładuje osiągnięcia w trakcie realizacji
     */
    private suspend fun loadInProgressAchievements(userId: String) {
        try {
            val result = achievementRepository.getInProgressAchievements(userId)
            if (result.isSuccess) {
                _inProgressAchievements.value = result.getOrNull() ?: emptyList()
                Log.d(TAG, "In progress achievements loaded: ${_inProgressAchievements.value.size}")
            } else {
                Log.e(TAG, "Failed to load in progress achievements: ${result.exceptionOrNull()?.message}")
                _inProgressAchievements.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading in progress achievements", e)
            _inProgressAchievements.value = emptyList()
        }
    }

    /**
     * Ładuje ostatnio zdobyte osiągnięcia
     */
    private suspend fun loadRecentAchievements(userId: String) {
        try {
            val result = achievementRepository.getRecentlyCompletedAchievements(userId, limit = 5)
            if (result.isSuccess) {
                _recentAchievements.value = result.getOrNull() ?: emptyList()
                Log.d(TAG, "Recent achievements loaded: ${_recentAchievements.value.size}")
            } else {
                Log.e(TAG, "Failed to load recent achievements: ${result.exceptionOrNull()?.message}")
                _recentAchievements.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading recent achievements", e)
            _recentAchievements.value = emptyList()
        }
    }

    /**
     * Odświeża dane osiągnięć (force refresh)
     */
    fun refreshAchievements(userId: String) {
        viewModelScope.launch {
            loadInProgressAchievements(userId)
            loadRecentAchievements(userId)
        }
    }

    /**
     * Czyści cache - przydatne przy wylogowaniu
     */
    fun clearCache() {
        currentUserId = null
        isDataLoaded = false
        _userData.value = null
        _inProgressAchievements.value = emptyList()
        _recentAchievements.value = emptyList()
    }

    /**
     * Wymusza pełne odświeżenie danych
     */
    fun forceRefresh(userId: String) {
        loadUserData(userId, forceRefresh = true)
    }

    // USUNIĘTO: signOut() - wylogowanie jest teraz obsługiwane przez AuthViewModel
}