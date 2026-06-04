package com.example.minlishapp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minlishapp.data.*
import com.example.minlishapp.data.repository.AuthRepository
import com.example.minlishapp.data.repository.LearningRepository
import com.example.minlishapp.data.repository.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository.create(application)
    private val learningRepository = LearningRepository.create(application)
    private val statsRepository = StatsRepository.create()

    private val _isEditingName = MutableStateFlow(false)
    val isEditingName: StateFlow<Boolean> = _isEditingName.asStateFlow()

    private val _nameInput = MutableStateFlow("")
    val nameInput: StateFlow<String> = _nameInput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _masteredWords = MutableStateFlow(0)
    val masteredWords: StateFlow<Int> = _masteredWords.asStateFlow()

    private val _accuracyRate = MutableStateFlow(0.0)
    val accuracyRate: StateFlow<Double> = _accuracyRate.asStateFlow()

    fun setIsEditingName(editing: Boolean) {
        _isEditingName.value = editing
    }

    fun setNameInput(input: String) {
        _nameInput.value = input
    }

    fun fetchProfileAndStats(
        userId: String,
        userProgress: UserProgress,
        onProgressUpdate: (UserProgress) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = authRepository.getProfile()
                if (response.isSuccessful) {
                    val profile = response.body()?.data
                    if (profile != null) {
                        onProgressUpdate(userProgress.copy(
                            name = profile.displayName ?: userProgress.name,
                            email = profile.email ?: userProgress.email,
                            targetGoal = profile.targetGoal,
                            wordsPerDay = profile.wordsPerDay,
                            xp = profile.xp,
                            level = profile.level,
                            streak = profile.streak
                        ))
                        _nameInput.value = profile.displayName ?: userProgress.name
                        _accuracyRate.value = profile.retentionRate
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error fetching profile", e)
            }

            try {
                val activeUserId = if (userId.isBlank()) "b64361ca-719d-4a07-b50f-910d8e05f9da" else userId
                val statsResponse = statsRepository.getStatsDashboard(activeUserId)
                if (statsResponse.success && statsResponse.data != null) {
                    _masteredWords.value = statsResponse.data.donutChart.proficient
                    _accuracyRate.value = statsResponse.data.profile.retentionRate
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error fetching stats", e)
            }
        }
    }

    fun updateDisplayName(
        name: String,
        userProgress: UserProgress,
        onProgressUpdate: (UserProgress) -> Unit,
        onResult: (Boolean) -> Unit
    ) {
        if (name.isBlank()) return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = authRepository.updateProfile(ProfileUpdateRequest(displayName = name))
                if (response.isSuccessful) {
                    onProgressUpdate(userProgress.copy(name = name))
                    _isEditingName.value = false
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update display name", e)
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTargetGoal(
        goal: String,
        userProgress: UserProgress,
        onProgressUpdate: (UserProgress) -> Unit,
        onResult: (Boolean) -> Unit
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = authRepository.updateProfile(ProfileUpdateRequest(targetGoal = goal))
                if (response.isSuccessful) {
                    onProgressUpdate(userProgress.copy(targetGoal = goal))
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update target goal", e)
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateWordsPerDay(
        count: Int,
        userProgress: UserProgress,
        onProgressUpdate: (UserProgress) -> Unit,
        onResult: (Boolean) -> Unit
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = authRepository.updateProfile(ProfileUpdateRequest(wordsPerDay = count))
                if (response.isSuccessful) {
                    onProgressUpdate(userProgress.copy(wordsPerDay = count))
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update words per day", e)
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetProgress(
        userProgress: UserProgress,
        onProgressUpdate: (UserProgress) -> Unit,
        onResult: (Boolean) -> Unit
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = learningRepository.resetProgress()
                if (response.isSuccessful && response.body()?.success == true) {
                    onProgressUpdate(UserProgress(
                        userId = userProgress.userId,
                        email = userProgress.email,
                        name = userProgress.name,
                        targetGoal = userProgress.targetGoal,
                        wordsPerDay = userProgress.wordsPerDay,
                        appLanguage = userProgress.appLanguage
                    ))
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to reset progress", e)
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
