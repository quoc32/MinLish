package com.example.minlishapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minlishapp.data.*
import com.example.minlishapp.data.repository.TutorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class TutorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TutorRepository.create(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun sendMessage(userId: String, message: String, targetGoal: String = "IELTS") {
        viewModelScope.launch {
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = message,
                isFromUser = true
            )
            _messages.value = _messages.value + userMessage
            _isLoading.value = true
            _error.value = null

            try {
                val response = repository.sendMessage(
                    TutorChatRequest(user_id = userId, message = message, target_goal = targetGoal)
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val aiMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = response.body()!!.response,
                        isFromUser = false
                    )
                    _messages.value = _messages.value + aiMessage
                } else {
                    _error.value = "Không nhận được phản hồi từ AI"
                }
            } catch (e: Exception) {
                _error.value = "Lỗi kết nối: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun resetChat(userId: String) {
        viewModelScope.launch {
            try {
                repository.resetChat(TutorResetRequest(user_id = userId))
            } catch (_: Exception) { }
            _messages.value = emptyList()
            _error.value = null
        }
    }

    fun clearError() { _error.value = null }
}
