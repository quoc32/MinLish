package com.example.minlishapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minlishapp.data.DashboardData
import com.example.minlishapp.data.repository.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StatsUiState {
    object Loading : StatsUiState
    data class Success(val data: DashboardData) : StatsUiState
    data class Error(val message: String) : StatsUiState
}

class StatsViewModel : ViewModel() {
    private val statsRepository = StatsRepository.create()

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    fun fetchStats(userId: String) {
        viewModelScope.launch {
            _uiState.value = StatsUiState.Loading
            val activeUserId = if (userId.isBlank()) "b64361ca-719d-4a07-b50f-910d8e05f9da" else userId
            try {
                val response = statsRepository.getStatsDashboard(activeUserId)
                if (response.success && response.data != null) {
                    _uiState.value = StatsUiState.Success(response.data)
                } else {
                    _uiState.value = StatsUiState.Error(response.message ?: "Không thể tải dữ liệu thống kê")
                }
            } catch (e: Exception) {
                Log.e("StatsViewModel", "Failed to fetch stats", e)
                _uiState.value = StatsUiState.Error("Lỗi kết nối: ${e.localizedMessage ?: "Không thể kết nối đến máy chủ"}")
            }
        }
    }
}
