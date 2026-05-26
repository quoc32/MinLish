package com.example.minlishapp.ui.screens

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
import com.example.minlishapp.data.Sm2Engine
import com.example.minlishapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FlashcardScreen(
    activeDeck: Deck?,
    onNavigate: (Screen) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // 1. Tải danh sách từ học từ activeDeck hoặc fallback
    val studyWords = remember(activeDeck) {
        mutableStateListOf<Word>().apply {
            addAll(activeDeck?.words ?: emptyList())
        }
    }

    var currentIndex by remember { mutableStateOf(0) }
    
    // Tiến độ học 4 giai đoạn cho từ hiện tại:
    // 1: Flashcard, 2: Trắc nghiệm, 3: Gõ từ, 4: Đánh giá SM-2
    var currentStep by remember { mutableStateOf(1) }
    
    // Trạng thái cho Bước 1: Flashcard
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "FlashcardFlip"
    )

    // Trạng thái cho Bước 2: Trắc nghiệm
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isMcAnswerChecked by remember { mutableStateOf(false) }

    // Trạng thái cho Bước 3: Gõ từ
    var typingInput by remember { mutableStateOf("") }
    var isTypingChecked by remember { mutableStateOf(false) }
    var isTypingCorrect by remember { mutableStateOf(false) }
    var showTypingHint by remember { mutableStateOf(false) }

    // Khi hoàn thành học tất cả các từ trong Deck
    if (studyWords.isEmpty() || currentIndex >= studyWords.size) {
        LaunchedEffect(Unit) {
            onNavigate(Screen.LessonComplete)
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

    // Đếm số lượng từ trạng thái học
    val newCount = studyWords.count { it.repetitions == 0 }
    val learnedCount = studyWords.count { it.repetitions > 0 }
    val reviewCount = studyWords.count { it.repetitions > 0 && it.intervalDays <= 1 }

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
                IconButton(onClick = { onNavigate(Screen.Dashboard) }) {
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
                when (currentStep) {
                    // ------------------------------------------
                    // BƯỚC 1: FLASHCARD (FRONT/BACK FLIP)
                    // ------------------------------------------
                    1 -> {
                        Card(
                            onClick = { isFlipped = !isFlipped },
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
                                                    Toast.makeText(context, "🔊 UK: ${currentWord.pronunciation}", Toast.LENGTH_SHORT).show()
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
                                                    val usPron = currentWord.pronunciationUs.ifEmpty { currentWord.pronunciation }
                                                    Toast.makeText(context, "🔊 US: $usPron", Toast.LENGTH_SHORT).show()
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
                                        text = "Nhấn để xem nghĩa",
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
                                                text = "Cụm từ đi kèm (Collocations)",
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
                                                text = "Đồng nghĩa:",
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
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically)
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

                                Spacer(modifier = Modifier.height(16.dp))

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
                                            isMcAnswerChecked && isSelected && !isCorrectOption -> Color(0xFFFEE2E2) // Đỏ nhạt khi sai
                                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }

                                        val borderColor = when {
                                            isMcAnswerChecked && isCorrectOption -> Color(0xFF10B981)
                                            isMcAnswerChecked && isSelected && !isCorrectOption -> Color(0xFFEF4444)
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        }

                                        Card(
                                            onClick = {
                                                if (!isMcAnswerChecked) {
                                                    selectedOptionIndex = optIndex
                                                    isMcAnswerChecked = true
                                                    
                                                    // Tự động chuyển qua bước tiếp theo sau khi phản hồi
                                                    coroutineScope.launch {
                                                        delay(if (isCorrectOption) 1000 else 2000)
                                                        currentStep = 3 // Gõ từ
                                                        selectedOptionIndex = null
                                                        isMcAnswerChecked = false
                                                    }
                                                }
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
                                    onValueChange = { if (!isTypingChecked) typingInput = it },
                                    placeholder = { Text("Gõ từ tiếng Anh...") },
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
                                            isTypingChecked = true
                                            isTypingCorrect = typingInput.trim().lowercase() == currentWord.word.lowercase()
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
                                        text = "Gợi ý: Từ bắt đầu bằng '${currentWord.word.take(2)}...' và có ${currentWord.word.length} chữ cái.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Hiển thị kết quả sai
                                if (isTypingChecked && !isTypingCorrect) {
                                    Text(
                                        text = "Đáp án đúng: ${currentWord.word}",
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
                                            onClick = { showTypingHint = !showTypingHint },
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
                                                    isTypingChecked = true
                                                    isTypingCorrect = typingInput.trim().lowercase() == currentWord.word.lowercase()
                                                    focusManager.clearFocus()
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Text("Kiểm tra", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Khung đánh giá riêng biệt nổi bật
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Đánh giá mức độ dễ nhớ của từ:",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            class Sm2Option(val score: Int, val color: Color, val label: String)
                                            val sm2Options = listOf(
                                                Sm2Option(0, ColorAgain, "Học lại (<1m)"),
                                                Sm2Option(3, ColorHard, "Khó (10m)"),
                                                Sm2Option(4, ColorGood, "Tốt (1d)"),
                                                Sm2Option(5, ColorEasy, "Dễ (4d)")
                                            )
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Học lại & Khó
                                                val optAgain = sm2Options[0]
                                                val optHard = sm2Options[1]
                                                
                                                Card(
                                                    onClick = {
                                                        val res = Sm2Engine.calculate(currentWord.repetitions, currentWord.easeFactor, currentWord.intervalDays, optAgain.score)
                                                        currentWord.repetitions = res.first
                                                        currentWord.easeFactor = res.second
                                                        currentWord.intervalDays = res.third
                                                        
                                                        // Chuyển từ tiếp theo
                                                        isFlipped = false
                                                        currentStep = 1
                                                        currentIndex++
                                                        typingInput = ""
                                                        isTypingChecked = false
                                                        showTypingHint = false
                                                    },
                                                    colors = CardDefaults.cardColors(containerColor = optAgain.color.copy(alpha = 0.08f)),
                                                    border = BorderStroke(1.dp, optAgain.color),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                ) {
                                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        Text(text = optAgain.label, color = optAgain.color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    }
                                                }
                                                
                                                Card(
                                                    onClick = {
                                                        val res = Sm2Engine.calculate(currentWord.repetitions, currentWord.easeFactor, currentWord.intervalDays, optHard.score)
                                                        currentWord.repetitions = res.first
                                                        currentWord.easeFactor = res.second
                                                        currentWord.intervalDays = res.third
                                                        
                                                        // Chuyển từ tiếp theo
                                                        isFlipped = false
                                                        currentStep = 1
                                                        currentIndex++
                                                        typingInput = ""
                                                        isTypingChecked = false
                                                        showTypingHint = false
                                                    },
                                                    colors = CardDefaults.cardColors(containerColor = optHard.color.copy(alpha = 0.08f)),
                                                    border = BorderStroke(1.dp, optHard.color),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
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
                                                // Tốt & Dễ
                                                val optGood = sm2Options[2]
                                                val optEasy = sm2Options[3]
                                                
                                                Card(
                                                    onClick = {
                                                        val res = Sm2Engine.calculate(currentWord.repetitions, currentWord.easeFactor, currentWord.intervalDays, optGood.score)
                                                        currentWord.repetitions = res.first
                                                        currentWord.easeFactor = res.second
                                                        currentWord.intervalDays = res.third
                                                        
                                                        // Chuyển từ tiếp theo
                                                        isFlipped = false
                                                        currentStep = 1
                                                        currentIndex++
                                                        typingInput = ""
                                                        isTypingChecked = false
                                                        showTypingHint = false
                                                    },
                                                    colors = CardDefaults.cardColors(containerColor = optGood.color.copy(alpha = 0.08f)),
                                                    border = BorderStroke(1.dp, optGood.color),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                ) {
                                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        Text(text = optGood.label, color = optGood.color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    }
                                                }
                                                
                                                Card(
                                                    onClick = {
                                                        val res = Sm2Engine.calculate(currentWord.repetitions, currentWord.easeFactor, currentWord.intervalDays, optEasy.score)
                                                        currentWord.repetitions = res.first
                                                        currentWord.easeFactor = res.second
                                                        currentWord.intervalDays = res.third
                                                        
                                                        // Chuyển từ tiếp theo
                                                        isFlipped = false
                                                        currentStep = 1
                                                        currentIndex++
                                                        typingInput = ""
                                                        isTypingChecked = false
                                                        showTypingHint = false
                                                    },
                                                    colors = CardDefaults.cardColors(containerColor = optEasy.color.copy(alpha = 0.08f)),
                                                    border = BorderStroke(1.dp, optEasy.color),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                ) {
                                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        Text(text = optEasy.label, color = optEasy.color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    }
                                                }
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
                // Các nút hành động cho Bước 1: Flashcard
                if (currentStep == 1) {
                    if (!isFlipped) {
                        Button(
                            onClick = { isFlipped = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Lật Thẻ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Nút mặt sau
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    currentStep = 2 // Chuyển qua Trắc nghiệm
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
                                    Text("Đã thuộc", fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    currentStep = 2 // Chuyển qua Trắc nghiệm
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Text("Học tiếp", fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Text(
                            text = "Gợi ý: Nhấn 'Đã thuộc' hoặc 'Học tiếp' để bắt đầu luyện tập",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    // Trợ giúp phím tắt phím ảo cho các bước game
                    val helperLabel = when {
                        currentStep == 2 -> "Bấm 1, 2, 3, 4 trên bàn phím hoặc chạm đáp án để chọn"
                        currentStep == 3 && !isTypingChecked -> "Bấm Enter để kiểm tra nhanh kết quả"
                        currentStep == 3 && isTypingChecked -> "Đánh giá khả năng nhớ từ của bạn"
                        else -> "Đánh giá khả năng nhớ từ của bạn"
                    }
                    Text(
                        text = helperLabel,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Dòng đếm trạng thái từ chuẩn thiết kế (Image 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$newCount Từ mới",
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "$learnedCount Đã học",
                        color = Color(0xFF7E22CE),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "$reviewCount Ôn tập",
                        color = Color(0xFFEA580C),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
