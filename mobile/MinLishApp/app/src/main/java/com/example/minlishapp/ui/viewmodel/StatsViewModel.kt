package com.example.minlishapp.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minlishapp.data.repository.StatsRepository
import com.example.minlishapp.ui.screens.StatsUiState
import kotlinx.coroutines.launch

class StatsViewModel : ViewModel() {
    private val statsRepository = StatsRepository.create()

    var uiState by mutableStateOf<StatsUiState>(StatsUiState.Loading)
        private set

    fun fetchStats(userId: String) {
        val activeUserId = if (userId.isBlank()) "b64361ca-719d-4a07-b50f-910d8e05f9da" else userId

        viewModelScope.launch {
            uiState = StatsUiState.Loading
            try {
                val response = statsRepository.getStatsDashboard(activeUserId)
                if (response.success && response.data != null) {
                    uiState = StatsUiState.Success(response.data)
                } else {
                    uiState = StatsUiState.Error(response.message ?: "Không thể tải dữ liệu thống kê")
                }
            } catch (e: Exception) {
                uiState = StatsUiState.Error("Lỗi kết nối: ${e.localizedMessage ?: "Không thể kết nối đến máy chủ"}")
            }
        }
    }
}
