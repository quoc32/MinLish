package com.example.minlishapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.minlishapp.data.*
import com.example.minlishapp.data.repository.DeckRepository
import com.example.minlishapp.data.repository.LearningRepository
import com.example.minlishapp.ui.screens.*
import com.example.minlishapp.ui.theme.MinLishAppTheme
import com.example.minlishapp.ui.viewmodel.VocabViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            // ============================================================
            // CORE UI STATE
            // ============================================================
            var isDarkTheme by remember { mutableStateOf(false) }
            var currentScreen by remember { mutableStateOf(Screen.Splash) }
            var userProgress by remember { mutableStateOf(UserProgress()) }
            var activeDeck by remember { mutableStateOf<Deck?>(null) }

            // ============================================================
            // API REPOSITORIES (lazy-initialized)
            // ============================================================
            val deckRepository = remember { DeckRepository.create(context) }
            val learningRepository = remember { LearningRepository.create(context) }

            // ============================================================
            // API-BACKED STATE
            // ============================================================
            val vocabViewModel: VocabViewModel = viewModel()
            val decks by vocabViewModel.decks.collectAsState()
            val isLoadingDecks by vocabViewModel.isLoading.collectAsState()

            // Daily Learning Plan
            var dailyPlan by remember { mutableStateOf<DailyPlanData?>(null) }
            var isLoadingDailyPlan by remember { mutableStateOf(false) }

            // Lesson result tracking (for LessonComplete screen)
            var sessionXpGained by remember { mutableIntStateOf(0) }
            var sessionStreak by remember { mutableIntStateOf(0) }
            var sessionAccuracy by remember { mutableIntStateOf(100) }
            var sessionWordsReviewed by remember { mutableIntStateOf(0) }
            var sessionCorrectCount by remember { mutableIntStateOf(0) }

            // ============================================================
            // API FETCH FUNCTIONS
            // ============================================================

            // Fetch daily plan from API
            fun fetchDailyPlan() {
                isLoadingDailyPlan = true
                coroutineScope.launch {
                    try {
                        val response = learningRepository.getDailyPlan()
                        if (response.isSuccessful && response.body()?.success == true) {
                            dailyPlan = response.body()?.data
                        } else {
                            // Fallback mock daily plan if API fails
                            dailyPlan = DailyPlanData(
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
                        Log.e("MainActivity", "Failed to fetch daily plan: ${e.message}")
                        dailyPlan = DailyPlanData(
                                wordsPerDay = 20,
                                newCardsCount = 0,
                                reviewCardsCount = 0,
                                inSessionReviewCount = 0,
                                newCards = emptyList(),
                                reviewCards = emptyList(),
                                inSessionReviewCards = emptyList()
                        )
                    } finally {
                        isLoadingDailyPlan = false
                    }
                }
            }
            // Submit review to API
            fun submitReviewToApi(cardId: String, quality: String) {
                coroutineScope.launch {
                    try {
                        val response = learningRepository.submitReview(cardId, quality)
                        if (response.isSuccessful && response.body()?.success == true) {
                            val reviewData = response.body()!!.data
                            if (reviewData != null) {
                                // Update session stats
                                sessionXpGained += reviewData.rewards.xpGained
                                sessionStreak = reviewData.rewards.streak
                                sessionWordsReviewed++
                                if (quality != "again") sessionCorrectCount++
                                sessionAccuracy = if (sessionWordsReviewed > 0)
                                    (sessionCorrectCount * 100 / sessionWordsReviewed) else 100

                                // Update userProgress with new XP/Level/Streak
                                userProgress = userProgress.copy(
                                    xp = reviewData.rewards.xpTotal,
                                    level = reviewData.rewards.level,
                                    streak = reviewData.rewards.streak
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Failed to submit review: ${e.message}")
                    }
                }
            }

            // ============================================================
            // FETCH DATA WHEN ENTERING DASHBOARD
            // ============================================================
            LaunchedEffect(currentScreen, userProgress.userId) {
                if (currentScreen == Screen.Dashboard && userProgress.userId.isNotEmpty()) {
                    vocabViewModel.fetchDecks()
                    fetchDailyPlan()
                }
                // Reset session stats when entering Flashcards
                if (currentScreen == Screen.Flashcards) {
                    sessionXpGained = 0
                    sessionStreak = userProgress.streak
                    sessionAccuracy = 100
                    sessionWordsReviewed = 0
                    sessionCorrectCount = 0
                }
            }

            // ============================================================
            // BACK HANDLER
            // ============================================================
            BackHandler(enabled = currentScreen != Screen.Dashboard && currentScreen != Screen.Splash && currentScreen != Screen.Welcome) {
                currentScreen = when (currentScreen) {
                    Screen.Login -> Screen.Welcome
                    Screen.LanguageSelection -> Screen.Welcome
                    Screen.OnboardingGoals -> Screen.LanguageSelection
                    Screen.OnboardingDailyWords -> Screen.OnboardingGoals
                    Screen.VocabDecks, Screen.Stats, Screen.Profile -> Screen.Dashboard
                    Screen.Flashcards -> Screen.Dashboard
                    Screen.LessonComplete -> Screen.Dashboard
                    else -> Screen.Dashboard
                }
            }

            // ============================================================
            // SCREEN ROUTING
            // ============================================================
            MinLishAppTheme(darkTheme = isDarkTheme) {
                Crossfade(targetState = currentScreen, label = "ScreenTransition") { targetScreen ->
                    when (targetScreen) {
                        Screen.Splash -> SplashScreen(onNavigate = { currentScreen = it })
                        Screen.Welcome -> WelcomeScreen(
                            onLoginSuccess = { userId, email, displayName, targetGoal, xp, level, streak ->
                                userProgress = userProgress.copy(
                                    userId = userId,
                                    email = email,
                                    name = displayName,
                                    targetGoal = targetGoal,
                                    xp = xp,
                                    level = level,
                                    streak = streak
                                )
                            },
                            onNavigate = { currentScreen = it }
                        )
                        Screen.Login -> LoginScreen(
                            onLoginSuccess = { userId, email, displayName, targetGoal, xp, level, streak ->
                                userProgress = userProgress.copy(
                                    userId = userId,
                                    email = email,
                                    name = displayName,
                                    targetGoal = targetGoal,
                                    xp = xp,
                                    level = level,
                                    streak = streak
                                )
                            },
                            onNavigate = { currentScreen = it }
                        )
                        Screen.LanguageSelection -> LanguageSelectionScreen(onNavigate = { currentScreen = it })
                        Screen.OnboardingGoals -> OnboardingGoalsScreen(
                            userProgress = userProgress,
                            onProgressUpdate = { userProgress = it },
                            onNavigate = { currentScreen = it }
                        )
                        Screen.OnboardingDailyWords -> OnboardingDailyWordsScreen(
                            userProgress = userProgress,
                            onProgressUpdate = { userProgress = it },
                            onNavigate = { currentScreen = it }
                        )
                        Screen.Dashboard -> DashboardScreen(
                            userProgress = userProgress,
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { isDarkTheme = !isDarkTheme },
                            onNavigate = { currentScreen = it },
                            activeDeck = activeDeck,
                            onActiveDeckSelect = { deck ->
                                activeDeck = deck
                            },
                            decks = decks,
                            dailyPlan = dailyPlan,
                            isLoading = isLoadingDecks || isLoadingDailyPlan,
                            onStartDailyPlan = {
                                // Build a deck from daily plan cards
                                val planCards = dailyPlan?.let { plan ->
                                    val allCards = plan.inSessionReviewCards + plan.reviewCards + plan.newCards
                                    allCards.map { card ->
                                        Word(
                                            id = card.id,
                                            word = card.word,
                                            pronunciation = card.pronunciation,
                                            meaning = card.meaning,
                                            description = card.descriptionEn ?: "",
                                            example = card.example ?: "",
                                            easeFactor = card.progress?.easeFactor ?: 2.5,
                                            repetitions = card.progress?.repetitions ?: 0,
                                            intervalDays = card.progress?.interval ?: 0
                                        )
                                    }
                                } ?: emptyList()

                                if (planCards.isNotEmpty()) {
                                    activeDeck = Deck(
                                        id = "daily_plan",
                                        name = "Kế hoạch hôm nay",
                                        description = "Từ mới + Ôn tập hàng ngày",
                                        tags = listOf("Daily Plan"),
                                        words = planCards
                                    )
                                    currentScreen = Screen.Flashcards
                                }
                            },
                            onStartStudy = { deck ->
                                activeDeck = deck
                                Toast.makeText(context, "Đang tải bài học...", Toast.LENGTH_SHORT).show()
                                vocabViewModel.fetchDeckCards(deck.id) { loadedWords ->
                                    activeDeck = deck.copy(words = loadedWords)
                                    currentScreen = Screen.Flashcards
                                }
                            }
                        )
                        Screen.VocabDecks -> VocabScreen(
                            vocabViewModel = vocabViewModel,
                            onNavigate = { currentScreen = it },
                            userProgress = userProgress,
                            activeDeck = activeDeck,
                            onActiveDeckSelect = { deck ->
                                activeDeck = deck
                            },
                            onStartStudy = { deck ->
                                activeDeck = deck
                                Toast.makeText(context, "Đang tải bài học...", Toast.LENGTH_SHORT).show()
                                vocabViewModel.fetchDeckCards(deck.id) { loadedWords ->
                                    activeDeck = deck.copy(words = loadedWords)
                                    currentScreen = Screen.Flashcards
                                }
                            }
                        )
                        Screen.Flashcards -> FlashcardScreen(
                            activeDeck = activeDeck ?: decks.firstOrNull(),
                            onNavigate = { currentScreen = it },
                            onSubmitReview = { cardId, quality ->
                                submitReviewToApi(cardId, quality)
                            }
                        )
                        Screen.LessonComplete -> LessonCompleteScreen(
                            onNavigate = { currentScreen = it },
                            xpGained = sessionXpGained,
                            streak = sessionStreak,
                            accuracy = sessionAccuracy
                        )
                        Screen.Stats -> StatsScreen(userId = userProgress.userId, onNavigate = { currentScreen = it })
                        Screen.Profile -> ProfileScreen(
                            userProgress = userProgress,
                            onProgressUpdate = { userProgress = it },
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { isDarkTheme = !isDarkTheme },
                            onNavigate = { currentScreen = it }
                        )
                    }
                }
            }
        }
    }


}
