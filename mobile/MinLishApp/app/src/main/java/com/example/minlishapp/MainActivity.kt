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
import kotlinx.coroutines.launch

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
            // Decks: ưu tiên từ API, fallback hardcoded
            var isLoadingDecks by remember { mutableStateOf(false) }
            val decks = remember(userProgress.targetGoal) {
                mutableStateListOf<Deck>().apply {
                    addAll(getDecksForGoal(userProgress.targetGoal))
                }
            }

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

            // Fetch decks from API and merge with local
            fun fetchDecksFromApi() {
                coroutineScope.launch {
                    isLoadingDecks = true
                    try {
                        val response = deckRepository.getAllDecks()
                        if (response.isSuccessful && response.body()?.success == true) {
                            val apiDecks = response.body()!!.data
                            if (!apiDecks.isNullOrEmpty()) {
                                // Convert API decks to local Deck model
                                val convertedDecks = apiDecks.map { apiDeck ->
                                    Deck(
                                        id = apiDeck.id,
                                        name = apiDeck.name,
                                        description = apiDeck.tag ?: "",
                                        tags = listOfNotNull(apiDeck.tag),
                                        apiProgress = apiDeck.progress.toFloat(),
                                        apiWordCount = apiDeck.totalWords
                                    )
                                }
                                decks.clear()
                                decks.addAll(convertedDecks)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Failed to fetch decks from API: ${e.message}")
                        // Keep fallback hardcoded decks
                    }
                    isLoadingDecks = false
                }
            }

            // Fetch daily plan from API
            fun fetchDailyPlan() {
                coroutineScope.launch {
                    isLoadingDailyPlan = true
                    try {
                        val response = learningRepository.getDailyPlan()
                        if (response.isSuccessful && response.body()?.success == true) {
                            dailyPlan = response.body()!!.data
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Failed to fetch daily plan: ${e.message}")
                    }
                    isLoadingDailyPlan = false
                }
            }

            // Fetch cards for a specific deck from API
            fun fetchDeckCards(deckId: String, onComplete: (List<Word>) -> Unit) {
                coroutineScope.launch {
                    try {
                        val response = deckRepository.getDeckCards(deckId)
                        if (response.isSuccessful && response.body()?.success == true) {
                            val apiCards = response.body()!!.data
                            if (!apiCards.isNullOrEmpty()) {
                                val words = apiCards.map { card ->
                                    Word(
                                        id = card.id,
                                        word = card.word,
                                        pronunciation = card.pronunciation,
                                        meaning = card.meaning,
                                        description = card.descriptionEn ?: "",
                                        example = card.example ?: "",
                                        wordType = "noun",
                                        // Map SM-2 progress from API if available
                                        easeFactor = card.progress?.easeFactor ?: 2.5,
                                        repetitions = card.progress?.repetitions ?: 0,
                                        intervalDays = card.progress?.interval ?: 0
                                    )
                                }
                                // Update the deck's words in the local list
                                val idx = decks.indexOfFirst { it.id == deckId }
                                if (idx != -1) {
                                    decks[idx] = decks[idx].copy(words = words)
                                }
                                onComplete(words)
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Failed to fetch deck cards: ${e.message}")
                    }
                    // Fallback to local hardcoded words for this deck if network call fails or is empty
                    val localDeck = decks.firstOrNull { it.id == deckId }
                    onComplete(localDeck?.words ?: emptyList())
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
                    fetchDecksFromApi()
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
                                fetchDeckCards(deck.id) { loadedWords ->
                                    activeDeck = deck.copy(words = loadedWords)
                                    currentScreen = Screen.Flashcards
                                }
                            }
                        )
                        Screen.VocabDecks -> VocabScreen(
                            decks = decks,
                            onAddDeck = { newDeck ->
                                decks.add(newDeck)
                                // Also create on API
                                coroutineScope.launch {
                                    try {
                                        val response = deckRepository.createDeck(
                                            CreateDeckRequest(
                                                name = newDeck.name,
                                                tag = newDeck.tags.firstOrNull()
                                            )
                                        )
                                        if (response.isSuccessful && response.body()?.success == true) {
                                            val createdApiDeck = response.body()!!.data
                                            if (createdApiDeck != null) {
                                                val index = decks.indexOfFirst { it.id == newDeck.id }
                                                if (index != -1) {
                                                    decks[index] = Deck(
                                                        id = createdApiDeck.id,
                                                        name = createdApiDeck.name,
                                                        description = createdApiDeck.tag ?: "",
                                                        tags = listOfNotNull(createdApiDeck.tag),
                                                        apiProgress = createdApiDeck.progress.toFloat(),
                                                        apiWordCount = createdApiDeck.totalWords
                                                    )
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Failed to create deck on API: ${e.message}")
                                    }
                                }
                            },
                            onNavigate = { currentScreen = it },
                            userProgress = userProgress,
                            activeDeck = activeDeck,
                             onActiveDeckSelect = { deck ->
                                 activeDeck = deck
                             },
                             onStartStudy = { deck ->
                                 activeDeck = deck
                                 Toast.makeText(context, "Đang tải bài học...", Toast.LENGTH_SHORT).show()
                                 fetchDeckCards(deck.id) { loadedWords ->
                                     activeDeck = deck.copy(words = loadedWords)
                                     currentScreen = Screen.Flashcards
                                 }
                             },
                            onAddWordToDeck = { deckId, word ->
                                val index = decks.indexOfFirst { it.id == deckId }
                                if (index != -1) {
                                    val oldDeck = decks[index]
                                    decks[index] = oldDeck.copy(words = oldDeck.words + word)
                                }
                                // Also create on API
                                coroutineScope.launch {
                                    try {
                                        val response = deckRepository.createCard(
                                            CreateCardRequest(
                                                deckId = deckId,
                                                word = word.word,
                                                pronunciation = word.pronunciation,
                                                meaning = word.meaning,
                                                descriptionEn = word.description,
                                                example = word.example
                                            )
                                        )
                                        if (response.isSuccessful && response.body()?.success == true) {
                                            val createdApiCard = response.body()!!.data
                                            if (createdApiCard != null) {
                                                val dIndex = decks.indexOfFirst { it.id == deckId }
                                                if (dIndex != -1) {
                                                    val deck = decks[dIndex]
                                                    val updatedWords = deck.words.map { w ->
                                                        if (w.word == word.word && w.id.isEmpty()) {
                                                            w.copy(id = createdApiCard.id)
                                                        } else {
                                                            w
                                                        }
                                                    }
                                                    decks[dIndex] = deck.copy(words = updatedWords)
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Failed to create card on API: ${e.message}")
                                    }
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

    private fun getDecksForGoal(goal: String): List<Deck> {
        return when (goal.uppercase().trim()) {
            "TOEIC" -> listOf(
                Deck(
                    id = "toeic_1",
                    name = "Business & Marketing",
                    description = "Chủ đề tiếp thị và kinh doanh.",
                    tags = listOf("600 TỪ VỰNG TOEIC"),
                    words = listOf(
                        Word("Accommodate", "/əˈkɒm.ə.deɪt/", "Chứa đựng, đáp ứng", "Provide lodging or sufficient space for.", "The conference hall can accommodate up to 500 guests."),
                        Word("Negotiation", "/nəˌɡəʊ.ʃiˈeɪ.ʃən/", "Sự đàm phán, thương lượng", "Discussion aimed at reaching an agreement.", "Negotiations between the two corporations are ongoing.")
                    )
                ),
                Deck(
                    id = "toeic_2",
                    name = "Office Communication",
                    description = "Giao tiếp trong văn phòng.",
                    tags = listOf("600 TỪ VỰNG TOEIC"),
                    words = listOf(
                        Word("Colleague", "/ˈkɒl.iːɡ/", "Đồng nghiệp", "A person with whom one works in a profession or business.", "I discussed the project outline with my colleague."),
                        Word("Memorandum", "/ˌmem.əˈræn.dəm/", "Bản thông báo nội bộ", "A written message in business or diplomacy.", "The manager circulated a memorandum regarding office security.")
                    )
                )
            )
            "IELTS" -> listOf(
                Deck(
                    id = "ielts_1",
                    name = "Academic Essentials",
                    description = "Từ vựng học thuật cốt lõi.",
                    tags = listOf("IELTS 7.5+"),
                    words = listOf(
                        Word("Ubiquitous", "/juːˈbɪk.wɪ.təs/", "Có mặt ở khắp mọi nơi", "Present, appearing, or found everywhere.", "Smartphones are ubiquitous nowadays."),
                        Word("Ephemeral", "/ɪˈfem.ər.əl/", "Chóng vánh, phù du", "Lasting for a very short time.", "Fame in the age of the internet is ephemeral.")
                    )
                ),
                Deck(
                    id = "ielts_2",
                    name = "Science & Technology",
                    description = "Chủ đề khoa học công nghệ.",
                    tags = listOf("IELTS 7.5+"),
                    words = listOf(
                        Word("Catalyst", "/ˈkæt.əl.ɪst/", "Chất xúc tác", "A substance that increases the rate of a chemical reaction without undergoing permanent change.", "The new technology was a catalyst for industrial reform."),
                        Word("Synthesize", "/ˈsɪn.θə.saɪz/", "Tổng hợp", "Combine elements or substances into a coherent whole.", "Plants synthesize chlorophyll from sunlight and carbon.")
                    )
                )
            )
            "GIAO TIẾP" -> listOf(
                Deck(
                    id = "giao_tiep_1",
                    name = "Daily Conversations",
                    description = "Giao tiếp hàng ngày cơ bản.",
                    tags = listOf("General English"),
                    words = listOf(
                        Word("Enthusiastic", "/ɪnˈθjuː.zi.æs.tɪk/", "Hăng hái, nhiệt tình", "Having or showing intense and eager enjoyment, interest, or approval.", "She was very enthusiastic about her new English class."),
                        Word("Sincere", "/sɪnˈsɪər/", "Chân thành", "Free from pretense or deceit; proceeding from genuine feelings.", "Please accept my sincere apologies for the delay.")
                    )
                ),
                Deck(
                    id = "giao_tiep_2",
                    name = "Travel English",
                    description = "Từ vựng giao tiếp du lịch.",
                    tags = listOf("General English"),
                    words = listOf(
                        Word("Destination", "/ˌdes.tɪˈneɪ.ʃən/", "Điểm đến", "The place to which someone or something is going.", "Paris is our final destination for the summer."),
                        Word("Reservation", "/ˌrez.əˈveɪ.ʃən/", "Sự đặt phòng/chỗ trước", "An arrangement to have something kept for one's use.", "We made a hotel reservation online.")
                    )
                )
            )
            "THPT QUỐC GIA" -> listOf(
                Deck(
                    id = "thpt_1",
                    name = "Grammar Mastery",
                    description = "Ngữ pháp THPT trọng điểm.",
                    tags = listOf("THPT Quốc gia"),
                    words = listOf(
                        Word("Relative clause", "/ˈrel.ə.tɪv klɔːz/", "Mệnh đề quan hệ", "A clause that is attached to an antecedent by a relative pronoun.", "The man who came here yesterday is my teacher."),
                        Word("Conditional", "/kənˈdɪʃ.ən.əl/", "Câu điều kiện", "Expressing a condition or state.", "We studied the third conditional structure today.")
                    )
                ),
                Deck(
                    id = "thpt_2",
                    name = "Exam Vocabulary",
                    description = "Từ vựng đề thi THPT.",
                    tags = listOf("THPT Quốc gia"),
                    words = listOf(
                        Word("Predecessor", "/ˈpriː.dɪˌses.ər/", "Người tiền nhiệm", "A person who held a job or office before the current holder.", "The new president praised the work of his predecessor."),
                        Word("Simultaneous", "/ˌsɪm.əlˈteɪ.ni.əs/", "Đồng thời, cùng một lúc", "Occurring, operating, or done at the same time.", "The match was broadcast with simultaneous translation.")
                    )
                )
            )
            else -> listOf(
                Deck(
                    id = "ielts_1",
                    name = "Academic Essentials",
                    description = "Từ vựng học thuật cốt lõi.",
                    tags = listOf("IELTS 7.5+"),
                    words = listOf(
                        Word("Ubiquitous", "/juːˈbɪk.wɪ.təs/", "Có mặt ở khắp mọi nơi", "Present, appearing, or found everywhere.", "Smartphones are ubiquitous nowadays."),
                        Word("Ephemeral", "/ɪˈfem.ər.əl/", "Chóng vánh, phù du", "Lasting for a very short time.", "Fame in the age of the internet is ephemeral.")
                    )
                )
            )
        }
    }
}
