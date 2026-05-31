package com.example.minlishapp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minlishapp.data.Deck
import com.example.minlishapp.data.Word
import com.example.minlishapp.data.repository.LearningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class FlashcardViewModel(application: Application) : AndroidViewModel(application) {

    private val learningRepository = LearningRepository.create(application)

    private val _studyWords = MutableStateFlow<List<Word>>(emptyList())
    val studyWords: StateFlow<List<Word>> = _studyWords.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _isFlipped = MutableStateFlow(false)
    val isFlipped: StateFlow<Boolean> = _isFlipped.asStateFlow()

    private val _selectedOptionIndex = MutableStateFlow<Int?>(null)
    val selectedOptionIndex: StateFlow<Int?> = _selectedOptionIndex.asStateFlow()

    private val _isMcAnswerChecked = MutableStateFlow(false)
    val isMcAnswerChecked: StateFlow<Boolean> = _isMcAnswerChecked.asStateFlow()

    private val _typingInput = MutableStateFlow("")
    val typingInput: StateFlow<String> = _typingInput.asStateFlow()

    private val _isTypingChecked = MutableStateFlow(false)
    val isTypingChecked: StateFlow<Boolean> = _isTypingChecked.asStateFlow()

    private val _isTypingCorrect = MutableStateFlow(false)
    val isTypingCorrect: StateFlow<Boolean> = _isTypingCorrect.asStateFlow()

    private val _showTypingHint = MutableStateFlow(false)
    val showTypingHint: StateFlow<Boolean> = _showTypingHint.asStateFlow()

    // Session Stats
    private val _sessionXpGained = MutableStateFlow(0)
    val sessionXpGained: StateFlow<Int> = _sessionXpGained.asStateFlow()

    private val _sessionStreak = MutableStateFlow(0)
    val sessionStreak: StateFlow<Int> = _sessionStreak.asStateFlow()

    private val _sessionAccuracy = MutableStateFlow(100)
    val sessionAccuracy: StateFlow<Int> = _sessionAccuracy.asStateFlow()

    private var sessionWordsReviewed = 0
    private var sessionCorrectCount = 0

    // Random options for current word
    private val _options = MutableStateFlow<List<String>>(emptyList())
    val options: StateFlow<List<String>> = _options.asStateFlow()

    fun startSession(deck: Deck?, initialStreak: Int) {
        _studyWords.value = deck?.words ?: emptyList()
        _currentIndex.value = 0
        _currentStep.value = 1
        _isFlipped.value = false
        _selectedOptionIndex.value = null
        _isMcAnswerChecked.value = false
        _typingInput.value = ""
        _isTypingChecked.value = false
        _isTypingCorrect.value = false
        _showTypingHint.value = false

        _sessionXpGained.value = 0
        _sessionStreak.value = initialStreak
        _sessionAccuracy.value = 100
        sessionWordsReviewed = 0
        sessionCorrectCount = 0

        generateOptions()
    }

    private fun generateOptions() {
        if (_studyWords.value.isEmpty() || _currentIndex.value >= _studyWords.value.size) return
        val currentWord = _studyWords.value[_currentIndex.value]
        val correct = currentWord.meaning
        val incorrect = _studyWords.value.filter { it.meaning != correct }.map { it.meaning }.shuffled().take(3)
        val placeholders = listOf("tham gia", "đăng ký", "tham dự", "hủy bỏ", "trì hoãn", "chuẩn bị")
            .filter { it != correct && !incorrect.contains(it) }
        val allOptions = (incorrect + listOf(correct) + placeholders).take(4).shuffled()
        _options.value = allOptions
    }

    fun flipCard() {
        _isFlipped.value = !_isFlipped.value
    }

    fun setStep(step: Int) {
        _currentStep.value = step
    }

    fun selectMcOption(index: Int, isCorrect: Boolean) {
        if (_isMcAnswerChecked.value) return
        _selectedOptionIndex.value = index
        _isMcAnswerChecked.value = true

        viewModelScope.launch {
            delay(if (isCorrect) 1000 else 2000)
            _currentStep.value = 3
            _selectedOptionIndex.value = null
            _isMcAnswerChecked.value = false
        }
    }

    fun updateTypingInput(input: String) {
        if (!_isTypingChecked.value) {
            _typingInput.value = input
        }
    }

    fun checkTyping() {
        if (_typingInput.value.isNotBlank() && !_isTypingChecked.value) {
            _isTypingChecked.value = true
            val currentWord = _studyWords.value[_currentIndex.value].word
            _isTypingCorrect.value = _typingInput.value.trim().equals(currentWord, ignoreCase = true)
        }
    }

    fun skipTyping() {
        _isTypingChecked.value = true
        _isTypingCorrect.value = false
        _typingInput.value = ""
    }

    fun toggleTypingHint() {
        _showTypingHint.value = !_showTypingHint.value
    }

    fun submitReview(cardId: String, quality: String, onProgressUpdated: (xpTotal: Int, level: Int, streak: Int) -> Unit) {
        viewModelScope.launch {
            try {
                val response = learningRepository.submitReview(cardId, quality)
                if (response.isSuccessful && response.body()?.success == true) {
                    val reviewData = response.body()!!.data
                    if (reviewData != null) {
                        _sessionXpGained.value += reviewData.rewards.xpGained
                        _sessionStreak.value = reviewData.rewards.streak
                        sessionWordsReviewed++
                        if (quality != "again") sessionCorrectCount++
                        _sessionAccuracy.value = if (sessionWordsReviewed > 0)
                            (sessionCorrectCount * 100 / sessionWordsReviewed) else 100

                        onProgressUpdated(
                            reviewData.rewards.xpTotal,
                            reviewData.rewards.level,
                            reviewData.rewards.streak
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("FlashcardViewModel", "Failed to submit review: ${e.message}")
            }
            
            // Move to next card
            nextCard()
        }
    }

    private fun nextCard() {
        if (_currentIndex.value < _studyWords.value.size - 1) {
            _currentIndex.value += 1
            _currentStep.value = 1
            _isFlipped.value = false
            _selectedOptionIndex.value = null
            _isMcAnswerChecked.value = false
            _typingInput.value = ""
            _isTypingChecked.value = false
            _isTypingCorrect.value = false
            _showTypingHint.value = false
            generateOptions()
        } else {
            // Force finish (currentIndex >= size will trigger navigation in UI)
            _currentIndex.value += 1
        }
    }
}
