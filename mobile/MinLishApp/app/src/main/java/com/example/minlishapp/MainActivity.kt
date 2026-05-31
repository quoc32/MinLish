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
import com.example.minlishapp.ui.viewmodel.DashboardViewModel
import com.example.minlishapp.ui.viewmodel.FlashcardViewModel
import com.example.minlishapp.ui.viewmodel.LoginViewModel

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

            val dashboardViewModel: DashboardViewModel = viewModel()
            val flashcardViewModel: FlashcardViewModel = viewModel()
            val loginViewModel: LoginViewModel = viewModel()
            // ============================================================
            // FETCH DATA WHEN ENTERING DASHBOARD
            // ============================================================
            LaunchedEffect(currentScreen, userProgress.userId) {
                if (currentScreen == Screen.Dashboard && userProgress.userId.isNotEmpty()) {
                    vocabViewModel.fetchDecks()
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
                            loginViewModel = loginViewModel,
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
                                currentScreen = Screen.Dashboard
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
                            dashboardViewModel = dashboardViewModel,
                            isLoadingDecks = isLoadingDecks,
                            onStartDailyPlan = { dailyPlan ->
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
                            flashcardViewModel = flashcardViewModel,
                            userStreak = userProgress.streak,
                            onProgressUpdated = { xp, level, streak ->
                                userProgress = userProgress.copy(
                                    xp = xp,
                                    level = level,
                                    streak = streak
                                )
                            }
                        )
                        Screen.LessonComplete -> LessonCompleteScreen(
                            onNavigate = { currentScreen = it },
                            xpGained = flashcardViewModel.sessionXpGained.collectAsState().value,
                            streak = flashcardViewModel.sessionStreak.collectAsState().value,
                            accuracy = flashcardViewModel.sessionAccuracy.collectAsState().value
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
