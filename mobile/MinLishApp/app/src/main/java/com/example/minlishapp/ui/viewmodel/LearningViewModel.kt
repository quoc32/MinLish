package com.example.minlishapp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minlishapp.core.utils.Sm2Engine
import com.example.minlishapp.data.*
import com.example.minlishapp.data.repository.LearningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val learningRepository = LearningRepository.create(application)

    // Daily Learning Plan states
    private val _dailyPlan = MutableStateFlow<DailyPlanData?>(null)
    val dailyPlan: StateFlow<DailyPlanData?> = _dailyPlan.asStateFlow()

    private val _isLoadingDailyPlan = MutableStateFlow(false)
    val isLoadingDailyPlan: StateFlow<Boolean> = _isLoadingDailyPlan.asStateFlow()

    // Session States
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

    private val _mcAttemptCount = MutableStateFlow(0)
    val mcAttemptCount: StateFlow<Int> = _mcAttemptCount.asStateFlow()

    private val _isMcFirstTimeWrong = MutableStateFlow(false)
    val isMcFirstTimeWrong: StateFlow<Boolean> = _isMcFirstTimeWrong.asStateFlow()

    private val _mcSelectedWrongOptions = MutableStateFlow<List<Int>>(emptyList())
    val mcSelectedWrongOptions: StateFlow<List<Int>> = _mcSelectedWrongOptions.asStateFlow()

    private val _typingInput = MutableStateFlow("")
    val typingInput: StateFlow<String> = _typingInput.asStateFlow()

    private val _isTypingChecked = MutableStateFlow(false)
    val isTypingChecked: StateFlow<Boolean> = _isTypingChecked.asStateFlow()

    private val _isTypingCorrect = MutableStateFlow(false)
    val isTypingCorrect: StateFlow<Boolean> = _isTypingCorrect.asStateFlow()

    private val _showTypingHint = MutableStateFlow(false)
    val showTypingHint: StateFlow<Boolean> = _showTypingHint.asStateFlow()

    private val _showSm2ForCurrentWord = MutableStateFlow(false)
    val showSm2ForCurrentWord: StateFlow<Boolean> = _showSm2ForCurrentWord.asStateFlow()

    // Session Stats
    private val _sessionXpGained = MutableStateFlow(0)
    val sessionXpGained: StateFlow<Int> = _sessionXpGained.asStateFlow()

    private val _sessionStreak = MutableStateFlow(0)
    val sessionStreak: StateFlow<Int> = _sessionStreak.asStateFlow()

    private val _sessionAccuracy = MutableStateFlow(100)
    val sessionAccuracy: StateFlow<Int> = _sessionAccuracy.asStateFlow()

    private val _sessionWordsReviewed = MutableStateFlow(0)
    val sessionWordsReviewed: StateFlow<Int> = _sessionWordsReviewed.asStateFlow()

    private val _sessionCorrectCount = MutableStateFlow(0)
    val sessionCorrectCount: StateFlow<Int> = _sessionCorrectCount.asStateFlow()

    // Session word status tracking
    private val _learnedWordIds = MutableStateFlow<Set<String>>(emptySet())
    val learnedWordIds: StateFlow<Set<String>> = _learnedWordIds.asStateFlow()

    private val _reviewWordIds = MutableStateFlow<Set<String>>(emptySet())
    val reviewWordIds: StateFlow<Set<String>> = _reviewWordIds.asStateFlow()

    fun fetchDailyPlan() {
        _isLoadingDailyPlan.value = true
        viewModelScope.launch {
            try {
                val response = learningRepository.getDailyPlan()
                if (response.isSuccessful && response.body()?.success == true) {
                    _dailyPlan.value = response.body()?.data
                } else {
                    _dailyPlan.value = getFallbackDailyPlan()
                }
            } catch (e: Exception) {
                Log.e("LearningViewModel", "Failed to fetch daily plan: ${e.message}")
                _dailyPlan.value = getFallbackDailyPlan()
            } finally {
                _isLoadingDailyPlan.value = false
            }
        }
    }

    private fun getFallbackDailyPlan() = DailyPlanData(
        wordsPerDay = 20,
        newCardsCount = 0,
        reviewCardsCount = 0,
        inSessionReviewCount = 0,
        newCards = emptyList(),
        reviewCards = emptyList(),
        inSessionReviewCards = emptyList()
    )

    fun resetSessionStats(initialStreak: Int) {
        _sessionXpGained.value = 0
        _sessionStreak.value = initialStreak
        _sessionAccuracy.value = 100
        _sessionWordsReviewed.value = 0
        _sessionCorrectCount.value = 0
    }

    fun initSession(words: List<Word>, initialStreak: Int) {
        _studyWords.value = words
        _currentIndex.value = 0
        _learnedWordIds.value = emptySet()
        _reviewWordIds.value = emptySet()
        resetSessionStats(initialStreak)
        resetWordStepStates()
    }

    fun resetWordStepStates() {
        _currentStep.value = 1
        _isFlipped.value = false
        _selectedOptionIndex.value = null
        _isMcAnswerChecked.value = false
        _mcAttemptCount.value = 0
        _isMcFirstTimeWrong.value = false
        _mcSelectedWrongOptions.value = emptyList()
        _typingInput.value = ""
        _isTypingChecked.value = false
        _isTypingCorrect.value = false
        _showTypingHint.value = false
        _showSm2ForCurrentWord.value = false
    }

    fun flipCard() {
        _isFlipped.value = !_isFlipped.value
    }

    fun selectMcOption(optIndex: Int, isCorrect: Boolean) {
        if (_isMcAnswerChecked.value) return
        _selectedOptionIndex.value = optIndex
        if (isCorrect) {
            _isMcAnswerChecked.value = true
        } else {
            _mcAttemptCount.value++
            val wrongOpts = _mcSelectedWrongOptions.value.toMutableList()
            wrongOpts.add(optIndex)
            _mcSelectedWrongOptions.value = wrongOpts
            if (_mcAttemptCount.value == 1) {
                _isMcFirstTimeWrong.value = true
            } else {
                _isMcAnswerChecked.value = true
            }
        }
    }

    fun setTypingInput(input: String) {
        if (!_isTypingChecked.value) {
            _typingInput.value = input
        }
    }

    fun checkTyping(correctWord: String) {
        if (_typingInput.value.isBlank() || _isTypingChecked.value) return
        _isTypingChecked.value = true
        _isTypingCorrect.value = _typingInput.value.trim().lowercase() == correctWord.lowercase()
    }

    fun toggleTypingHint() {
        _showTypingHint.value = !_showTypingHint.value
    }

    fun showSm2Rating() {
        _showSm2ForCurrentWord.value = true
    }

    fun setStep(step: Int) {
        _currentStep.value = step
    }

    fun submitReview(
        cardId: String,
        quality: String,
        userProgress: UserProgress,
        onProgressUpdate: (UserProgress) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = learningRepository.submitReview(cardId, quality)
                if (response.isSuccessful && response.body()?.success == true) {
                    val reviewData = response.body()!!.data
                    if (reviewData != null) {
                        // Update session stats
                        _sessionXpGained.value += reviewData.rewards.xpGained
                        _sessionStreak.value = reviewData.rewards.streak
                        _sessionWordsReviewed.value++
                        if (quality != "again") {
                            _sessionCorrectCount.value++
                        }
                        val reviewed = _sessionWordsReviewed.value
                        val correct = _sessionCorrectCount.value
                        _sessionAccuracy.value = if (reviewed > 0) (correct * 100 / reviewed) else 100

                        // Update shared userProgress
                        onProgressUpdate(userProgress.copy(
                            xp = reviewData.rewards.xpTotal,
                            level = reviewData.rewards.level,
                            streak = reviewData.rewards.streak
                        ))
                    }
                }
            } catch (e: Exception) {
                Log.e("LearningViewModel", "Failed to submit review: ${e.message}")
            }
        }
    }

    fun handleSm2Score(
        score: Int,
        onSubmitReview: (String, String) -> Unit
    ) {
        val wordIndex = _currentIndex.value
        val wordsList = _studyWords.value.toMutableList()
        if (wordIndex < wordsList.size) {
            val currentWord = wordsList[wordIndex].copy()
            
            // Calculate SM-2 locally
            val res = Sm2Engine.calculate(
                currentWord.repetitions,
                currentWord.easeFactor,
                currentWord.intervalDays,
                score
            )
            currentWord.repetitions = res.first
            currentWord.easeFactor = res.second
            currentWord.intervalDays = res.third

            // Cập nhật lại từ tại vị trí hiện tại
            wordsList[wordIndex] = currentWord

            // Submit review API call
            val quality = when (score) {
                0 -> "again"
                3 -> "hard"
                4 -> "good"
                else -> "easy"
            }
            onSubmitReview(currentWord.id, quality)

            if (score == 0) {
                // Again: đánh dấu là cần ôn tập, bỏ khỏi đã học
                _reviewWordIds.value = _reviewWordIds.value + currentWord.id
                _learnedWordIds.value = _learnedWordIds.value - currentWord.id
                // Append copy to end for re-learning
                wordsList.add(currentWord.copy())
            } else {
                // Hard/Good/Easy: đánh dấu đã học, bỏ khỏi ôn tập
                _learnedWordIds.value = _learnedWordIds.value + currentWord.id
                _reviewWordIds.value = _reviewWordIds.value - currentWord.id
            }

            // Gán lại để kích hoạt StateFlow
            _studyWords.value = wordsList

            // Move to next word
            _currentIndex.value = wordIndex + 1
            resetWordStepStates()
        }
    }
}
