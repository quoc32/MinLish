package com.example.minlishapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.minlishapp.data.*
import com.example.minlishapp.ui.screens.*
import com.example.minlishapp.ui.theme.MinLishAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Giả lập trạng thái ứng dụng
            var isDarkTheme by remember { mutableStateOf(false) }
            var currentScreen by remember { mutableStateOf(Screen.Splash) }
            var userProgress by remember { mutableStateOf(UserProgress()) }
            var activeDeck by remember { mutableStateOf<Deck?>(null) }
            
            val decks = remember(userProgress.targetGoal) {
                mutableStateListOf<Deck>().apply {
                    addAll(getDecksForGoal(userProgress.targetGoal))
                }
            }

            // Xử lý nút Back hệ thống để điều hướng hợp lý thay vì thoát app
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
                            onActiveDeckSelect = { activeDeck = it },
                            decks = decks
                        )
                        Screen.VocabDecks -> VocabScreen(
                            decks = decks,
                            onAddDeck = { decks.add(it) },
                            onNavigate = { currentScreen = it },
                            userProgress = userProgress,
                            activeDeck = activeDeck,
                            onActiveDeckSelect = { activeDeck = it },
                            onAddWordToDeck = { deckId, word ->
                                val index = decks.indexOfFirst { it.id == deckId }
                                if (index != -1) {
                                    val oldDeck = decks[index]
                                    decks[index] = oldDeck.copy(words = oldDeck.words + word)
                                }
                            }
                        )
                        Screen.Flashcards -> FlashcardScreen(
                            activeDeck = activeDeck ?: decks.firstOrNull(),
                            onNavigate = { currentScreen = it }
                        )
                        Screen.LessonComplete -> LessonCompleteScreen(onNavigate = { currentScreen = it })
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
        return when (goal.uppercase()) {
            "TOEIC" -> listOf(
                Deck(
                    id = "toeic_1",
                    name = "Conferences",
                    description = "Chủ đề hội nghị, hội thảo, giao tiếp sự kiện.",
                    tags = listOf("600 TỪ VỰNG TOEIC"),
                    words = listOf(
                        Word(
                            word = "accommodate",
                            wordType = "verb",
                            pronunciation = "/ə'kɒmədeɪt/",
                            pronunciationUs = "/ə'kɑːmədeɪt/",
                            meaning = "chứa đựng",
                            example = "\"The conference venue can accommodate up to 1,000 participants.\"",
                            exampleTranslation = "(Địa điểm hội nghị có thể chứa tới 1.000 người tham dự.)",
                            collocations = listOf("accommodate guests: tiếp đón khách", "accommodate needs: đáp ứng nhu cầu"),
                            synonyms = listOf("hold")
                        ),
                        Word(
                            word = "attend",
                            wordType = "verb",
                            pronunciation = "/ə'tend/",
                            pronunciationUs = "/ə'attend/",
                            meaning = "tham dự",
                            example = "\"More than 200 people attended the seminar yesterday.\"",
                            exampleTranslation = "(Hơn 200 người đã tham dự hội thảo ngày hôm qua.)",
                            collocations = listOf("attend a meeting: tham dự cuộc họp", "attend a conference: tham dự hội nghị"),
                            synonyms = listOf("present")
                        ),
                        Word(
                            word = "register",
                            wordType = "verb",
                            pronunciation = "/ˈredʒ.ɪ.stər/",
                            pronunciationUs = "/ˈredʒ.ɪ.stər/",
                            meaning = "đăng ký",
                            example = "\"Please register for the conference online before Friday.\"",
                            exampleTranslation = "(Vui lòng đăng ký tham dự hội nghị trực tuyến trước thứ Sáu.)",
                            collocations = listOf("register online: đăng ký trực tuyến", "registration desk: bàn đăng ký"),
                            synonyms = listOf("enroll")
                        ),
                        Word(
                            word = "session",
                            wordType = "noun",
                            pronunciation = "/ˈseʃ.ən/",
                            pronunciationUs = "/ˈseʃ.ən/",
                            meaning = "phiên họp, buổi học",
                            example = "\"The afternoon session will focus on marketing strategies.\"",
                            exampleTranslation = "(Buổi học chiều nay sẽ tập trung vào các chiến lược tiếp thị.)",
                            collocations = listOf("training session: buổi đào tạo", "plenary session: phiên họp toàn thể"),
                            synonyms = listOf("meeting")
                        ),
                        Word(
                            word = "coordinate",
                            wordType = "verb",
                            pronunciation = "/kəʊˈɔː.dɪ.neɪt/",
                            pronunciationUs = "/koʊˈɔːr.dɪ.neɪt/",
                            meaning = "phối hợp, điều phối",
                            example = "\"We need to coordinate our efforts to ensure a successful event.\"",
                            exampleTranslation = "(Chúng ta cần phối hợp các nỗ lực để đảm bảo sự kiện thành công.)",
                            collocations = listOf("coordinate activities: phối hợp hoạt động", "event coordinator: người điều phối sự kiện"),
                            synonyms = listOf("organize")
                        ),
                        Word(
                            word = "association",
                            wordType = "noun",
                            pronunciation = "/əˌsəʊ.siˈeɪ.ʃən/",
                            pronunciationUs = "/əˌsoʊ.siˈeɪ.ʃən/",
                            meaning = "hiệp hội, sự liên kết",
                            example = "\"The local business association organized this international conference.\"",
                            exampleTranslation = "(Hiệp hội doanh nghiệp địa phương đã tổ chức hội nghị quốc tế này.)",
                            collocations = listOf("member association: hiệp hội thành viên", "in association with: liên kết với"),
                            synonyms = listOf("organization")
                        ),
                        Word(
                            word = "hold",
                            wordType = "verb",
                            pronunciation = "/həʊld/",
                            pronunciationUs = "/hoʊld/",
                            meaning = "tổ chức, nắm giữ",
                            example = "\"The annual meeting will be held in the main exhibition center.\"",
                            exampleTranslation = "(Cuộc họp thường niên sẽ được tổ chức tại trung tâm triển lãm chính.)",
                            collocations = listOf("hold a meeting: tổ chức cuộc họp", "hold a press conference: họp báo"),
                            synonyms = listOf("conduct")
                        ),
                        Word(
                            word = "get together",
                            wordType = "phrasal verb",
                            pronunciation = "/ɡet təˈɡeð.ər/",
                            pronunciationUs = "/ɡet təˈɡeð.ər/",
                            meaning = "tụ họp, họp mặt",
                            example = "\"The participants got together for lunch after the morning presentations.\"",
                            exampleTranslation = "(Các học viên đã tụ họp ăn trưa sau buổi thuyết trình sáng.)",
                            collocations = listOf("family get-together: sum họp gia đình", "get together to talk: họp lại trò chuyện"),
                            synonyms = listOf("gather")
                        ),
                        Word(
                            word = "location",
                            wordType = "noun",
                            pronunciation = "/ləʊˈkeɪ.ʃən/",
                            pronunciationUs = "/loʊˈkeɪ.ʃən/",
                            meaning = "địa điểm",
                            example = "\"The location of the conferences is easy to access by public transit.\"",
                            exampleTranslation = "(Địa điểm của hội nghị rất dễ tiếp cận bằng phương tiện công cộng.)",
                            collocations = listOf("prime location: vị trí đắc địa", "venue location: vị trí hội trường"),
                            synonyms = listOf("site")
                        ),
                        Word(
                            word = "overcrowded",
                            wordType = "adjective",
                            pronunciation = "/ˌəʊ.vəˈkraʊ.dɪd/",
                            pronunciationUs = "/ˌoʊ.vɚˈkraʊ.dɪd/",
                            meaning = "quá tải, quá đông đúc",
                            example = "\"The seminar room was overcrowded with extra chairs needed.\"",
                            exampleTranslation = "(Phòng hội thảo quá đông đúc và cần thêm ghế phụ.)",
                            collocations = listOf("overcrowded hall: hội trường quá tải", "overcrowded schedule: lịch trình quá dày"),
                            synonyms = listOf("congested")
                        ),
                        Word(
                            word = "schedule",
                            wordType = "noun",
                            pronunciation = "/ˈʃedj.uːl/",
                            pronunciationUs = "/ˈskedʒ.uːl/",
                            meaning = "lịch trình, thời khóa biểu",
                            example = "\"Please check the conference schedule for exact starting times.\"",
                            exampleTranslation = "(Vui lòng kiểm tra lịch trình hội nghị để biết thời gian bắt đầu chính xác.)",
                            collocations = listOf("ahead of schedule: trước thời hạn", "on schedule: đúng tiến độ"),
                            synonyms = listOf("timetable")
                        ),
                        Word(
                            word = "select",
                            wordType = "verb",
                            pronunciation = "/sɪˈlekt/",
                            pronunciationUs = "/sɪˈlekt/",
                            meaning = "lựa chọn",
                            example = "\"A panel of experts will select the best research paper.\"",
                            exampleTranslation = "(Một ban chuyên gia sẽ lựa chọn bài nghiên cứu tốt nhất.)",
                            collocations = listOf("select options: chọn phương án", "carefully selected: tuyển chọn kỹ lượng"),
                            synonyms = listOf("choose")
                        )
                    )
                ),
                Deck(
                    id = "toeic_2",
                    name = "Marketing",
                    description = "Chủ đề tiếp thị, nghiên cứu thị trường, quảng cáo.",
                    tags = listOf("600 TỪ VỰNG TOEIC"),
                    words = listOf(
                        Word("attract", "/əˈtrækt/", "thu hút", "convince customers", "Attract new clients", listOf("attract customers")),
                        Word("consume", "/kənˈsjuːm/", "tiêu thụ", "eat or use", "Consume resources", listOf("consume time"))
                    )
                ),
                Deck(
                    id = "toeic_3",
                    name = "Contracts",
                    description = "Chủ đề hợp đồng, đàm phán thương lượng.",
                    tags = listOf("600 TỪ VỰNG TOEIC"),
                    words = listOf(
                        Word("agreement", "/əˈbɪl.ə.ti/", "thỏa thuận", "contractual deal", "Reach an agreement", listOf("sign agreement")),
                        Word("commit", "/kəˈmɪt/", "cam kết", "promise to do", "Commit to quality", listOf("commit crimes"))
                    )
                )
            )
            "IELTS" -> listOf(
                Deck(
                    id = "ielts_1",
                    name = "Academic Core",
                    description = "Bộ từ vựng học thuật cốt lõi.",
                    tags = listOf("IELTS 7.5+"),
                    words = listOf(
                        Word("Ephemeral", "/ə'fem(ə)rəl/", "Kéo dài trong thời gian ngắn.", "Lasting for a very short time.", "The beauty of the sunset was ephemeral.", listOf("Ephemeral joy", "Ephemeral beauty")),
                        Word("Ubiquitous", "/juːˈbɪk.wɪ.təs/", "Có mặt ở khắp mọi nơi.", "Present everywhere.", "Smartphones are ubiquitous nowadays.", listOf("Ubiquitous presence")),
                        Word("Cacophony", "/kəˈkɒf.ə.ni/", "Âm thanh hỗn loạn.", "A harsh mixture of sounds.", "A cacophony of car horns filled the street.", listOf("Urban cacophony"))
                    )
                ),
                Deck(
                    id = "ielts_2",
                    name = "Science & Tech",
                    description = "Chủ đề Khoa học & Công nghệ.",
                    tags = listOf("IELTS 7.5+"),
                    words = listOf(
                        Word("Synergy", "/ˈsɪn.ə.dʒi/", "Sự cộng hưởng, hợp lực.", "Combined action or cooperation.", "The synergy between the two companies created great success.", listOf("Team synergy"))
                    )
                )
            )
            "TRAVEL" -> listOf(
                Deck(
                    id = "travel_1",
                    name = "Hotels & Stays",
                    description = "Từ vựng về khách sạn, dịch vụ lưu trú.",
                    tags = listOf("Travel English"),
                    words = listOf(
                        Word("Itinerary", "/aɪˈtɪn.ər.ər.i/", "Lịch trình chuyến đi.", "A planned route or journey.", "Here is our itinerary for the trip to Japan.", listOf("Travel itinerary")),
                        Word("reservation", "/ˌrez.əˈveɪ.ʃən/", "đặt chỗ", "booking a room", "Make a reservation", listOf("hotel reservation"))
                    )
                )
            )
            else -> listOf(
                Deck(
                    id = "general_1",
                    name = "Daily Communication",
                    description = "Giao tiếp hàng ngày cơ bản.",
                    tags = listOf("General English"),
                    words = listOf(
                        Word("Leverage", "/ˈliː.vər.ɪdʒ/", "Tận dụng tối đa tài nguyên.", "Use to maximum advantage.", "We can leverage our network to find clients.", listOf("Leverage assets")),
                        Word("Synergy", "/ˈsɪn.ə.dʒi/", "Sự cộng hưởng, hợp lực.", "Combined action or cooperation.", "The synergy between the two companies created great success.", listOf("Team synergy"))
                    )
                )
            )
        }
    }
}




