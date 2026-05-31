package com.example.minlishapp.ui.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minlishapp.core.utils.ReminderManager
import com.example.minlishapp.data.ProfileUpdateRequest
import com.example.minlishapp.data.UserProgress
import com.example.minlishapp.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.create(application)
    private val sharedPrefs = application.getSharedPreferences("minlish_prefs", Context.MODE_PRIVATE)

    // Form inputs and UI States managed inside ViewModel
    var isEditingName by mutableStateOf(false)
    var nameInput by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    var dailyReminderEnabled by mutableStateOf(sharedPrefs.getBoolean("daily_reminder_enabled", true))
        private set
    var reviewReminderEnabled by mutableStateOf(sharedPrefs.getBoolean("review_reminder_enabled", true))
        private set
    var dailyReminderTime by mutableStateOf(sharedPrefs.getString("daily_reminder_time", "09:00") ?: "09:00")
        private set

    var showTimePickerDialog by mutableStateOf(false)
    var showGoalPicker by mutableStateOf(false)

    fun fetchProfile(userProgress: UserProgress, onProgressUpdate: (UserProgress) -> Unit) {
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
                            xp = profile.xp,
                            level = profile.level,
                            streak = profile.streak
                        ))
                        nameInput = profile.displayName ?: userProgress.name
                    }
                }
            } catch (e: Exception) {
                // Ignore, keep current state
            }
        }
    }

    fun saveProfileName(userProgress: UserProgress, onProgressUpdate: (UserProgress) -> Unit) {
        if (nameInput.isBlank()) {
            Toast.makeText(getApplication(), "Tên không được để trống!", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        viewModelScope.launch {
            try {
                val response = authRepository.updateProfile(ProfileUpdateRequest(displayName = nameInput))
                isLoading = false
                if (response.isSuccessful && response.body()?.success == true) {
                    onProgressUpdate(userProgress.copy(name = nameInput))
                    isEditingName = false
                    Toast.makeText(getApplication(), "Đã cập nhật tên!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(getApplication(), response.body()?.message ?: "Cập nhật thất bại!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isLoading = false
                Toast.makeText(getApplication(), "Lỗi kết nối: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveProfileGoal(goal: String, userProgress: UserProgress, onProgressUpdate: (UserProgress) -> Unit) {
        isLoading = true
        viewModelScope.launch {
            try {
                val response = authRepository.updateProfile(ProfileUpdateRequest(targetGoal = goal))
                isLoading = false
                if (response.isSuccessful && response.body()?.success == true) {
                    onProgressUpdate(userProgress.copy(targetGoal = goal))
                    showGoalPicker = false
                    Toast.makeText(getApplication(), "Đã cập nhật mục tiêu học tập!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(getApplication(), response.body()?.message ?: "Cập nhật mục tiêu thất bại!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isLoading = false
                Toast.makeText(getApplication(), "Lỗi kết nối: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateDailyReminderEnabled(enabled: Boolean) {
        dailyReminderEnabled = enabled
        sharedPrefs.edit().putBoolean("daily_reminder_enabled", enabled).apply()
        if (enabled) {
            ReminderManager.scheduleDailyReminder(getApplication(), dailyReminderTime)
            Toast.makeText(getApplication(), "Đã bật nhắc nhở lúc $dailyReminderTime hàng ngày!", Toast.LENGTH_SHORT).show()
        } else {
            ReminderManager.cancelReminder(getApplication())
            Toast.makeText(getApplication(), "Đã tắt nhắc nhở hàng ngày!", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateReviewReminderEnabled(enabled: Boolean) {
        reviewReminderEnabled = enabled
        sharedPrefs.edit().putBoolean("review_reminder_enabled", enabled).apply()
        if (enabled) {
            Toast.makeText(getApplication(), "Đã bật nhắc nhở ôn tập!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(getApplication(), "Đã tắt nhắc nhở ôn tập!", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateDailyReminderTime(timeStr: String) {
        dailyReminderTime = timeStr
        sharedPrefs.edit().putString("daily_reminder_time", timeStr).apply()
        if (dailyReminderEnabled) {
            ReminderManager.scheduleDailyReminder(getApplication(), timeStr)
        }
        Toast.makeText(getApplication(), "Đã hẹn giờ nhắc học lúc $timeStr hàng ngày!", Toast.LENGTH_SHORT).show()
    }
}
