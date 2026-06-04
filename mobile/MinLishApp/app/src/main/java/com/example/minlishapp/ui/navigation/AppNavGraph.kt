package com.example.minlishapp.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.minlishapp.data.*
import com.example.minlishapp.ui.screens.*
import com.example.minlishapp.ui.viewmodel.*

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    userProgress: UserProgress,
    onProgressUpdate: (UserProgress) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    activeDeck: Deck?,
    onActiveDeckSelect: (Deck?) -> Unit,
    vocabViewModel: VocabViewModel,
    authViewModel: AuthViewModel,
    statsViewModel: StatsViewModel,
    learningViewModel: LearningViewModel,
    profileViewModel: ProfileViewModel
) {
    val context = LocalContext.current

    // State collections from ViewModels
    val decks by vocabViewModel.decks.collectAsState()
    val isLoadingDecks by vocabViewModel.isLoading.collectAsState()

    val dailyPlan by learningViewModel.dailyPlan.collectAsState()
    val isLoadingDailyPlan by learningViewModel.isLoadingDailyPlan.collectAsState()

    val sessionXpGained by learningViewModel.sessionXpGained.collectAsState()
    val sessionStreak by learningViewModel.sessionStreak.collectAsState()
    val sessionAccuracy by learningViewModel.sessionAccuracy.collectAsState()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash.route,
        modifier = modifier
    ) {
        composable(AppRoute.Splash.route) {
            SplashScreen(navController = navController)
        }

        composable(AppRoute.Welcome.route) {
            WelcomeScreen(
                onLoginSuccess = { userId, email, displayName, targetGoal, xp, level, streak ->
                    onProgressUpdate(
                        userProgress.copy(
                            userId = userId,
                            email = email,
                            name = displayName,
                            targetGoal = targetGoal,
                            xp = xp,
                            level = level,
                            streak = streak
                        )
                    )
                },
                navController = navController,
                appLanguage = userProgress.appLanguage
            )
        }

        composable(AppRoute.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { userId, email, displayName, targetGoal, xp, level, streak ->
                    onProgressUpdate(
                        userProgress.copy(
                            userId = userId,
                            email = email,
                            name = displayName,
                            targetGoal = targetGoal,
                            xp = xp,
                            level = level,
                            streak = streak
                        )
                    )
                },
                navController = navController,
                appLanguage = userProgress.appLanguage
            )
        }

        composable(AppRoute.LanguageSelection.route) {
            LanguageSelectionScreen(
                userProgress = userProgress,
                onProgressUpdate = onProgressUpdate,
                navController = navController
            )
        }

        composable(AppRoute.OnboardingGoals.route) {
            OnboardingGoalsScreen(
                userProgress = userProgress,
                onProgressUpdate = onProgressUpdate,
                navController = navController
            )
        }

        composable(AppRoute.OnboardingDailyWords.route) {
            OnboardingDailyWordsScreen(
                userProgress = userProgress,
                onProgressUpdate = onProgressUpdate,
                navController = navController
            )
        }

        composable(AppRoute.Dashboard.route) {
            // Fetch data when entering Dashboard
            LaunchedEffect(userProgress.userId) {
                if (userProgress.userId.isNotEmpty()) {
                    vocabViewModel.fetchDecks()
                    learningViewModel.fetchDailyPlan()
                }
            }

            DashboardScreen(
                userProgress = userProgress,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                navController = navController,
                activeDeck = activeDeck,
                onActiveDeckSelect = { deck ->
                    onActiveDeckSelect(deck)
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
                        onActiveDeckSelect(
                            Deck(
                                id = "daily_plan",
                                name = "Kế hoạch hôm nay",
                                description = "Từ mới + Ôn tập hàng ngày",
                                tags = listOf("Daily Plan"),
                                words = planCards
                            )
                        )
                        navController.navigate(AppRoute.Flashcards.route)
                    }
                },
                onStartStudy = { deck ->
                    onActiveDeckSelect(deck)
                    Toast.makeText(context, "Đang tải bài học...", Toast.LENGTH_SHORT).show()
                    vocabViewModel.fetchDeckCards(deck.id) { loadedWords ->
                        onActiveDeckSelect(deck.copy(words = loadedWords))
                        navController.navigate(AppRoute.Flashcards.route)
                    }
                }
            )
        }

        composable(AppRoute.VocabDecks.route) {
            VocabScreen(
                vocabViewModel = vocabViewModel,
                navController = navController,
                userProgress = userProgress,
                activeDeck = activeDeck,
                onActiveDeckSelect = { deck ->
                    onActiveDeckSelect(deck)
                },
                onStartStudy = { deck ->
                    onActiveDeckSelect(deck)
                    Toast.makeText(context, "Đang tải bài học...", Toast.LENGTH_SHORT).show()
                    vocabViewModel.fetchDeckCards(deck.id) { loadedWords ->
                        onActiveDeckSelect(deck.copy(words = loadedWords))
                        navController.navigate(AppRoute.Flashcards.route)
                    }
                }
            )
        }

        composable(AppRoute.Flashcards.route) {
            FlashcardScreen(
                learningViewModel = learningViewModel,
                activeDeck = activeDeck ?: decks.firstOrNull(),
                navController = navController,
                onSubmitReview = { cardId, quality ->
                    learningViewModel.submitReview(cardId, quality, userProgress) { updatedProgress ->
                        onProgressUpdate(updatedProgress)
                    }
                },
                userProgress = userProgress
            )
        }

        composable(AppRoute.LessonComplete.route) {
            LessonCompleteScreen(
                navController = navController,
                xpGained = sessionXpGained,
                streak = sessionStreak,
                accuracy = sessionAccuracy,
                appLanguage = userProgress.appLanguage
            )
        }

        composable(AppRoute.Stats.route) {
            StatsScreen(
                statsViewModel = statsViewModel,
                userId = userProgress.userId,
                appLanguage = userProgress.appLanguage,
                navController = navController
            )
        }

        composable(AppRoute.Profile.route) {
            ProfileScreen(
                profileViewModel = profileViewModel,
                userProgress = userProgress,
                onProgressUpdate = onProgressUpdate,
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                navController = navController
            )
        }

        composable(AppRoute.ResetPassword.route) {
            ResetPasswordScreen(
                authViewModel = authViewModel,
                navController = navController,
                appLanguage = userProgress.appLanguage
            )
        }
    }
}
