package com.example.minlishapp.ui.screens

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minlishapp.data.Deck
import com.example.minlishapp.data.Word
import com.example.minlishapp.core.utils.Sm2Engine
import com.example.minlishapp.core.utils.translated
import com.example.minlishapp.ui.theme.*
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FlashcardScreen(
    learningViewModel: com.example.minlishapp.ui.viewmodel.LearningViewModel,
    activeDeck: Deck?,
    navController: NavHostController,
    onSubmitReview: (cardId: String, quality: String) -> Unit,
    userProgress: com.example.minlishapp.data.UserProgress
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var forceReview by remember { mutableStateOf(false) }

    // Initialize TextToSpeech engine
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Ready
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    fun speak(text: String, isUk: Boolean = false) {
        tts?.let {
            it.language = if (isUk) java.util.Locale.UK else java.util.Locale.US
            it.setSpeechRate(0.85f)
            it.setPitch(1.0f)
            val params = android.os.Bundle()
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            it.speak(text, TextToSpeech.QUEUE_FLUSH, params, null)
        }
    }

    LaunchedEffect(activeDeck) {
        val words = activeDeck?.words ?: emptyList()
        learningViewModel.initSession(words, userProgress.streak)
    }

    val studyWords by learningViewModel.studyWords.collectAsState()
    val currentIndex by learningViewModel.currentIndex.collectAsState()
    val currentStep by learningViewModel.currentStep.collectAsState()
    val isFlipped by learningViewModel.isFlipped.collectAsState()
    val selectedOptionIndex by learningViewModel.selectedOptionIndex.collectAsState()
    val isMcAnswerChecked by learningViewModel.isMcAnswerChecked.collectAsState()
    val mcAttemptCount by learningViewModel.mcAttemptCount.collectAsState()
    val isMcFirstTimeWrong by learningViewModel.isMcFirstTimeWrong.collectAsState()
    val mcSelectedWrongOptions by learningViewModel.mcSelectedWrongOptions.collectAsState()
    val typingInput by learningViewModel.typingInput.collectAsState()
    val isTypingChecked by learningViewModel.isTypingChecked.collectAsState()
    val isTypingCorrect by learningViewModel.isTypingCorrect.collectAsState()
    val showTypingHint by learningViewModel.showTypingHint.collectAsState()
    val showSm2ForCurrentWord by learningViewModel.showSm2ForCurrentWord.collectAsState()

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "FlashcardFlip"
    )

    if (studyWords.isEmpty() && activeDeck?.words?.isNotEmpty() == true) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (activeDeck != null && activeDeck.id != "daily_plan" && activeDeck.progress >= 1.0f && !forceReview) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar with Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigate(AppRoute.VocabDecks.route) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }

                // Congratulations Card
                val isDark = MaterialTheme.colorScheme.background == Color(0xFF090A0F)
                val cardBg = if (isDark) Color(0xFF064E3B).copy(alpha = 0.2f) else Color(0xFFDCFCE7).copy(alpha = 0.4f)
                val cardBorder = if (isDark) Color(0xFF059669) else Color(0xFF10B981)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 40.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.5.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Green check circle
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF059669) else Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Success",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Hoàn thành! 🎉".translated(userProgress.appLanguage),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF065F46),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Bạn đã học hết các từ trong phần này. Quay lại sau để ôn tập.".translated(userProgress.appLanguage),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Action buttons at the bottom
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Bạn có muốn ôn tập?".translated(userProgress.appLanguage),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = { forceReview = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Ôn tập ngay".translated(userProgress.appLanguage),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { navController.navigate(AppRoute.VocabDecks.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = "Quay lại".translated(userProgress.appLanguage),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        return
    }

    // Khi hoàn thành học tất cả các từ trong Deck
    if (studyWords.isNotEmpty() && currentIndex >= studyWords.size) {
        LaunchedEffect(Unit) {
            navController.navigate(AppRoute.LessonComplete.route)
        }
        return
    }

    val currentWord = studyWords[currentIndex]

    // Sinh 4 đáp án trắc nghiệm ngẫu nhiên cho Bước 2
    val options = remember(currentWord) {
        val correct = currentWord.meaning
        val incorrect = studyWords.filter { it.meaning != correct }.map { it.meaning }.shuffled().take(3)
        val placeholders = listOf("tham gia", "đăng ký", "tham dự", "hủy bỏ", "trì hoãn", "chuẩn bị")
            .filter { it != correct && !incorrect.contains(it) }
        val allOptions = (incorrect + correct + placeholders).take(4).shuffled()
        allOptions
    }

    // Đếm số lượng từ trạng thái học dựa trên tracking sets từ ViewModel
    val learnedWordIds by learningViewModel.learnedWordIds.collectAsState()
    val reviewWordIds by learningViewModel.reviewWordIds.collectAsState()
    val totalUniqueWords = studyWords.map { it.id }.toSet().size
    val learnedCount = learnedWordIds.size
    val reviewCount = reviewWordIds.size
    val newCount = totalUniqueWords - learnedCount - reviewCount

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .systemBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // ==========================================
            // HEADER BAR: Back, Mode Indicator Pill, Progress Info
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigate(AppRoute.VocabDecks.route) }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }

                // Thanh chọn chế độ học 4 bước bằng icon sang trọng
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon 1: Xem / Lật mặt trước
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Flashcard Front",
                        tint = if (currentStep == 1 && !isFlipped) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    // Icon 2: Đọc sách / Xem nghĩa mặt sau
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Flashcard Back",
                        tint = if (currentStep == 1 && isFlipped) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    // Icon 3: Tay cầm game / Trắc nghiệm & Gõ từ
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "Practice Games",
                        tint = if (currentStep == 2 || (currentStep == 3 && !isTypingChecked)) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    // Icon 4: Cài đặt / Đánh giá
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "SM-2 Rating",
                        tint = if (currentStep == 3 && isTypingChecked) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "${currentIndex + 1}/${studyWords.size}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Thanh tiến trình chung
            val globalProgress = (currentIndex.toFloat() / studyWords.size)
            LinearProgressIndicator(
                progress = { globalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // ==========================================
            // VÙNG HIỂN THỊ THẺ CHÍNH (DYNAMIC PER STEP)
            // ==========================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showSm2ForCurrentWord) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxSize(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                        ) {
                            Text(
                                text = currentWord.word,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "(${currentWord.wordType}) ${currentWord.pronunciation}",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = currentWord.meaning,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Đánh giá mức độ dễ nhớ của từ:".translated(userProgress.appLanguage),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            class Sm2OptionData(val score: Int, val color: Color, val label: String)
                            val sm2Options = listOf(
                                Sm2OptionData(0, ColorAgain, "Học lại (<1m)".translated(userProgress.appLanguage)),
                                Sm2OptionData(3, ColorHard, "Khó (10m)".translated(userProgress.appLanguage)),
                                Sm2OptionData(4, ColorGood, "Tốt (1d)".translated(userProgress.appLanguage)),
                                Sm2OptionData(5, ColorEasy, "Dễ (4d)".translated(userProgress.appLanguage))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val optAgain = sm2Options[0]
                                val optHard = sm2Options[1]
                                Card(
                                    onClick = {
                                        learningViewModel.handleSm2Score(optAgain.score, onSubmitReview)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = optAgain.color.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, optAgain.color),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = optAgain.label, color = optAgain.color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                Card(
                                    onClick = {
                                        learningViewModel.handleSm2Score(optHard.score, onSubmitReview)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = optHard.color.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, optHard.color),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = optHard.label, color = optHard.color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val optGood = sm2Options[2]
                                val optEasy = sm2Options[3]
                                Card(
                                    onClick = {
                                        learningViewModel.handleSm2Score(optGood.score, onSubmitReview)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = optGood.color.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, optGood.color),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = optGood.label, color = optGood.color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                Card(
                                    onClick = {
                                        learningViewModel.handleSm2Score(optEasy.score, onSubmitReview)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = optEasy.color.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, optEasy.color),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = optEasy.label, color = optEasy.color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    when (currentStep) {
                    // ------------------------------------------
                    // BƯỚC 1: FLASHCARD (FRONT/BACK FLIP)
                    // ------------------------------------------
                    1 -> {
                        Card(
                            onClick = { learningViewModel.flipCard() },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationY = rotation
                                    cameraDistance = 12f * density
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            if (rotation <= 90f) {
                                // MẶT TRƯỚC: Từ vựng, Loại từ, Phát âm UK/US
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = currentWord.word,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${currentWord.wordType})",
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontStyle = FontStyle.Italic,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Phát âm UK (Xanh) & US (Đỏ)
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // UK
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    speak(currentWord.word, isUk = true)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Speak UK",
                                                tint = Color(0xFF2563EB), // Xanh dương
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "UK ${currentWord.pronunciation}",
                                                fontSize = 15.sp,
                                                color = Color(0xFF2563EB),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        // US
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    speak(currentWord.word, isUk = false)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Speak US",
                                                tint = Color(0xFFDC2626), // Đỏ
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            val usPron = currentWord.pronunciationUs.ifEmpty { currentWord.pronunciation }
                                            Text(
                                                text = "US $usPron",
                                                fontSize = 15.sp,
                                                color = Color(0xFFDC2626),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(48.dp))

                                    Text(
                                        text = "Nhấn để xem nghĩa".translated(userProgress.appLanguage),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                // MẶT SAU: Nghĩa tiếng Việt, Câu ví dụ + Dịch, Collocation, Từ đồng nghĩa
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp)
                                        .graphicsLayer { rotationY = 180f }
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Nghĩa lớn
                                    Text(
                                        text = currentWord.meaning,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Khung ví dụ minh họa xám nhạt cao cấp
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = currentWord.example,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (currentWord.exampleTranslation.isNotEmpty()) {
                                                Text(
                                                    text = currentWord.exampleTranslation,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontStyle = FontStyle.Italic
                                                )
                                            }
                                        }
                                    }

                                    // Collocations
                                    if (currentWord.collocations.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Cụm từ đi kèm (Collocations)".translated(userProgress.appLanguage),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            // Danh sách Collocations dạng viên thuốc xanh lam
                                            currentWord.collocations.forEach { col ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFFE0F2FE))
                                                        .border(0.5.dp, Color(0xFF0284C7), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = col,
                                                        color = Color(0xFF0369A1),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Đồng nghĩa
                                    if (currentWord.synonyms.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Đồng nghĩa:".translated(userProgress.appLanguage),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            currentWord.synonyms.forEach { syn ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFFF3E8FF))
                                                        .border(0.5.dp, Color(0xFF7E22CE), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = syn,
                                                        color = Color(0xFF6B21A8),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ------------------------------------------
                    // BƯỚC 2: TRẮC NGHIỆM (MULTIPLE CHOICE)
                    // ------------------------------------------
                    2 -> {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxSize(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                            ) {
                                // Phần trên: Từ vựng & Phát âm
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = currentWord.word,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "(${currentWord.wordType}) ${currentWord.pronunciation}",
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontStyle = FontStyle.Italic,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                if (isMcFirstTimeWrong && !isMcAnswerChecked) {
                                    Text(
                                        text = "Gợi ý: Từ bắt đầu bằng chữ".translated(userProgress.appLanguage) + " '${currentWord.word.take(1).uppercase()}'",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    )
                                }

                                // Phần giữa: 4 đáp án trắc nghiệm
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    options.forEachIndexed { optIndex, optionText ->
                                        val isCorrectOption = optionText == currentWord.meaning
                                        val isSelected = selectedOptionIndex == optIndex
                                        
                                        // Tính toán màu nền và viền dựa trên kết quả kiểm tra
                                        val cardColor = when {
                                            isMcAnswerChecked && isCorrectOption -> Color(0xFFD1FAE5) // Xanh lục nhạt khi đúng
                                            mcSelectedWrongOptions.contains(optIndex) -> Color(0xFFFEE2E2) // Đỏ nhạt khi chọn sai
                                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }

                                        val borderColor = when {
                                            isMcAnswerChecked && isCorrectOption -> Color(0xFF10B981)
                                            mcSelectedWrongOptions.contains(optIndex) -> Color(0xFFEF4444)
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        }

                                        Card(
                                            onClick = {
                                                learningViewModel.selectMcOption(optIndex, optionText == currentWord.meaning)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = cardColor),
                                            border = BorderStroke(1.dp, borderColor),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.primary 
                                                            else MaterialTheme.colorScheme.surfaceVariant
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${optIndex + 1}",
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(16.dp))

                                                Text(
                                                    text = optionText,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ------------------------------------------
                    // BƯỚC 3: GÕ TỪ (TYPING PRACTICE) & ĐÁNH GIÁ (SM-2)
                    // ------------------------------------------
                    3 -> {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxSize(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Badge loại từ
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = currentWord.wordType,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Từ nghĩa tiếng Việt
                                Text(
                                    text = currentWord.meaning,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Ô Nhập từ
                                OutlinedTextField(
                                    value = typingInput,
                                    onValueChange = { learningViewModel.setTypingInput(it) },
                                    placeholder = { Text("Gõ từ tiếng Anh...".translated(userProgress.appLanguage)) },
                                    singleLine = true,
                                    enabled = !isTypingChecked,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (typingInput.isNotBlank() && !isTypingChecked) {
                                            learningViewModel.checkTyping(currentWord.word)
                                            focusManager.clearFocus()
                                        }
                                    }),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isTypingChecked) (if (isTypingCorrect) Color(0xFF10B981) else Color(0xFFEF4444)) else MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = if (isTypingChecked) (if (isTypingCorrect) Color(0xFF10B981) else Color(0xFFEF4444)) else MaterialTheme.colorScheme.outline
                                    )
                                )

                                // Hiển thị gợi ý nếu bật
                                AnimatedVisibility(visible = showTypingHint && !isTypingChecked) {
                                    Text(
                                        text = "Gợi ý: Từ bắt đầu bằng".translated(userProgress.appLanguage) + " '${currentWord.word.take(2)}...' " + "và có".translated(userProgress.appLanguage) + " ${currentWord.word.length} " + "chữ cái.".translated(userProgress.appLanguage),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Hiển thị kết quả sai
                                if (isTypingChecked && !isTypingCorrect) {
                                    Text(
                                        text = "Đáp án đúng:".translated(userProgress.appLanguage) + " ${currentWord.word}",
                                        fontSize = 15.sp,
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (!isTypingChecked) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Nút gợi ý (💡)
                                        OutlinedIconButton(
                                            onClick = { learningViewModel.toggleTypingHint() },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lightbulb,
                                                contentDescription = "Hint",
                                                tint = if (showTypingHint) Color(0xFFEAB308) else Color.Gray
                                            )
                                        }


                                        // Nút kiểm tra
                                        Button(
                                            onClick = {
                                                if (typingInput.isNotBlank()) {
                                                    learningViewModel.checkTyping(currentWord.word)
                                                    focusManager.clearFocus()
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Text("Kiểm tra".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // ==========================================
            // BOTTOM CONTROL / HELPER / PROGRESS STATUS
            // ==========================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Các nút hành động cho từng bước học
                if (showSm2ForCurrentWord) {
                    // Đang hiển thị bảng chọn SM-2 ở thẻ chính, không cần nút phụ
                } else if (currentStep == 1) {
                    if (!isFlipped) {
                        Button(
                            onClick = { learningViewModel.flipCard() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Lật Thẻ".translated(userProgress.appLanguage), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    learningViewModel.showSm2Rating()
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Mastered", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Đã thuộc".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    learningViewModel.setStep(2) // Chuyển qua Trắc nghiệm
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Text("Học tiếp".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Text(
                            text = "Gợi ý: Nhấn 'Đã thuộc' hoặc 'Học tiếp' để bắt đầu luyện tập".translated(userProgress.appLanguage),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else if (currentStep == 2) {
                    if (isMcAnswerChecked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    learningViewModel.showSm2Rating()
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Mastered", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Đã thuộc".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    learningViewModel.setStep(3) // Chuyển qua Viết từ
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Text("Học tiếp".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Bấm 1, 2, 3, 4 trên bàn phím hoặc chạm đáp án để chọn".translated(userProgress.appLanguage),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (currentStep == 3) {
                    if (isTypingChecked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    learningViewModel.showSm2Rating()
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = "Mastered", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Đã thuộc".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    learningViewModel.resetWordStepStates()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Text("Học lại".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Bấm Enter để kiểm tra nhanh kết quả".translated(userProgress.appLanguage),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Dòng đếm trạng thái từ chuẩn thiết kế (Image 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$newCount " + "Từ mới".translated(userProgress.appLanguage),
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "$learnedCount " + "Đã học".translated(userProgress.appLanguage),
                        color = Color(0xFF7E22CE),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "$reviewCount " + "Ôn tập".translated(userProgress.appLanguage),
                        color = Color(0xFFEA580C),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
