package com.example.minlishapp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minlishapp.data.DailyPlanData
import com.example.minlishapp.data.repository.LearningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val learningRepository = LearningRepository.create(application)

    private val _dailyPlan = MutableStateFlow<DailyPlanData?>(null)
    val dailyPlan: StateFlow<DailyPlanData?> = _dailyPlan.asStateFlow()

    private val _isLoadingDailyPlan = MutableStateFlow(false)
    val isLoadingDailyPlan: StateFlow<Boolean> = _isLoadingDailyPlan.asStateFlow()

    fun fetchDailyPlan() {
        _isLoadingDailyPlan.value = true
        viewModelScope.launch {
            try {
                val response = learningRepository.getDailyPlan()
                if (response.isSuccessful && response.body()?.success == true) {
                    _dailyPlan.value = response.body()?.data
                } else {
                    // Fallback mock daily plan if API fails
                    _dailyPlan.value = DailyPlanData(
                        wordsPerDay = 20,
                        newCardsCount = 0,
                        reviewCardsCount = 0,
                        inSessionReviewCount = 0,
                        newCards = emptyList(),
                        reviewCards = emptyList(),
                        inSessionReviewCards = emptyList()
                    )
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Failed to fetch daily plan: ${e.message}")
                _dailyPlan.value = DailyPlanData(
                    wordsPerDay = 20,
                    newCardsCount = 0,
                    reviewCardsCount = 0,
                    inSessionReviewCount = 0,
                    newCards = emptyList(),
                    reviewCards = emptyList(),
                    inSessionReviewCards = emptyList()
                )
            } finally {
                _isLoadingDailyPlan.value = false
            }
        }
    }
}
