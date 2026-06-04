package com.example.minlishapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.minlishapp.data.Deck
import com.example.minlishapp.data.DailyPlanData
import com.example.minlishapp.data.UserProgress
import com.example.minlishapp.ui.theme.ColorStreakFlame
import com.example.minlishapp.core.utils.LanguageHelper
import com.example.minlishapp.core.utils.translated


@Composable
fun DashboardScreen(
    userProgress: UserProgress,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onNavigate: (Screen) -> Unit,
    activeDeck: Deck?,
    onActiveDeckSelect: (Deck) -> Unit,
    decks: List<Deck>,
    dailyPlan: DailyPlanData? = null,
    isLoading: Boolean = false,
    onStartDailyPlan: () -> Unit = {},
    onStartStudy: (Deck) -> Unit = {}
) {
    val filteredDecks = remember(decks) {
        decks
    }

    var selectedDeck by remember {
        mutableStateOf<Deck?>(null)
    }

    Scaffold(
        bottomBar = { AppBottomBar(currentScreen = Screen.Dashboard, onNavigate = onNavigate, appLanguage = userProgress.appLanguage) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            val isSmallScreen = maxHeight < 640.dp

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ... (Top Header remains same)
                // Top Header: Chỉ số tổng quan & Theme Switcher
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Level Badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${userProgress.level}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = userProgress.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${userProgress.xp} XP",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Streak & Theme
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ColorStreakFlame.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = "🔥", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${userProgress.streak} " + "ngày".translated(userProgress.appLanguage),
                                    color = ColorStreakFlame,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            IconButton(onClick = onThemeToggle) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme"
                                )
                            }
                        }
                    }
                }

                // Central content: Lộ trình dạng Duolingo theo chủ đề
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp)
                ) {
                    // Daily Learning Plan Card
                    if (dailyPlan != null || isLoading) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 0.dp)
                            ) {
                                if (isLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                } else if (dailyPlan != null) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "📋 " + "Bài học hôm nay".translated(userProgress.appLanguage),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )

                                        if (dailyPlan.totalNewCardsAvailable < userProgress.wordsPerDay) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "⚠️ " + "Kho từ vựng mới sắp hết!".translated(userProgress.appLanguage),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                    val msg = if (dailyPlan.totalNewCardsAvailable == 0) {
                                                        "Bạn đã học hết từ mới trong các bộ từ hiện tại. Hãy tạo hoặc tải thêm bộ từ vựng mới!".translated(userProgress.appLanguage)
                                                    } else {
                                                        "Số từ mới còn lại (${dailyPlan.totalNewCardsAvailable} từ) ít hơn mục tiêu hàng ngày (${userProgress.wordsPerDay} từ) của bạn. Nên chuyển mục tiêu học thành ${dailyPlan.totalNewCardsAvailable} từ/ngày hoặc bổ sung thêm từ vựng mới.".translated(userProgress.appLanguage)
                                                    }
                                                    Text(
                                                        text = msg,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                                                        lineHeight = 15.sp
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(text = "🆕", fontSize = 20.sp)
                                                Text(
                                                    text = "${dailyPlan.newCardsCount}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = "Từ mới".translated(userProgress.appLanguage),
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(text = "📝", fontSize = 20.sp)
                                                Text(
                                                    text = "${dailyPlan.reviewCardsCount}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = "Cần ôn".translated(userProgress.appLanguage),
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                )
                                            }
                                            if (dailyPlan.inSessionReviewCount > 0) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(text = "🔄", fontSize = 20.sp)
                                                    Text(
                                                        text = "${dailyPlan.inSessionReviewCount}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 18.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                    Text(
                                                        text = "Học lại".translated(userProgress.appLanguage),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }

                                        val totalCards = dailyPlan.newCardsCount + dailyPlan.reviewCardsCount + dailyPlan.inSessionReviewCount
                                        if (totalCards > 0) {
                                            Button(
                                                onClick = onStartDailyPlan,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Text(
                                                    text = "Bắt đầu học".translated(userProgress.appLanguage) + " ($totalCards " + "Từ vựng".translated(userProgress.appLanguage).lowercase() + ")",
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    item {
                        Text(
                            text = "Lộ trình học".translated(userProgress.appLanguage) + " ${userProgress.targetGoal}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }

                    // Render các chủ đề từ vựng động từ list decks đã lọc
                    filteredDecks.forEachIndexed { index, deck ->
                        item {
                            // Mở khóa nếu là chủ đề đầu tiên hoặc chủ đề trước đạt tiến độ tốt
                            val isLocked = index > 0 && filteredDecks[index - 1].progress < 0.5f
                            val progress = deck.progress
                            val offsetPercent = when (index % 3) {
                                0 -> -0.2f
                                1 -> 0.2f
                                else -> 0.0f
                            }
                            
                            val isSelected = selectedDeck?.id == deck.id

                            DuolingoNode(
                                title = deck.name,
                                icon = when (deck.name.lowercase()) {
                                    "conferences" -> "💼"
                                    "marketing" -> "📈"
                                    "contracts" -> "📜"
                                    "academic core" -> "🎓"
                                    "science & tech" -> "🔬"
                                    "hotels & stays" -> "🏨"
                                    else -> "📚"
                                },
                                progress = progress,
                                isLocked = isLocked,
                                offsetPercent = offsetPercent,
                                onClick = {
                                    selectedDeck = deck
                                    onActiveDeckSelect(deck)
                                }
                            )
                        }

                        if (index < filteredDecks.size - 1) {
                            item {
                                val nextOffsetPercent = when ((index + 1) % 3) {
                                    0 -> -0.2f
                                    1 -> 0.2f
                                    else -> 0.0f
                                }
                                val currentOffsetPercent = when (index % 3) {
                                    0 -> -0.2f
                                    1 -> 0.2f
                                    else -> 0.0f
                                }
                                NodeConnector(
                                    height = 40.dp, 
                                    offsetPercent = (currentOffsetPercent + nextOffsetPercent) / 2
                                )
                            }
                        }
                    }
                }
            }

            // Dialog "Vào học" hiển thị thông tin chủ đề được chọn khi click vào node
            selectedDeck?.let { deck ->
                val contentPadding = if (isSmallScreen) 16.dp else 24.dp
                val buttonHeight = if (isSmallScreen) 42.dp else 48.dp

                Dialog(onDismissRequest = { selectedDeck = null }) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(contentPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Icon chủ đề lớn trong popup
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (deck.name.lowercase()) {
                                        "conferences" -> "💼"
                                        "marketing" -> "📈"
                                        "contracts" -> "📜"
                                        "academic core" -> "🎓"
                                        "science & tech" -> "🔬"
                                        "hotels & stays" -> "🏨"
                                        else -> "📚"
                                    },
                                    fontSize = 40.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Tag
                                val tagText = deck.tags.firstOrNull() ?: "CHỦ ĐỀ"
                                Text(
                                    text = tagText.uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))

                                // Title
                                Text(
                                    text = deck.name,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                // Subtitle
                                Text(
                                    text = "${deck.wordCount} " + "từ vựng".translated(userProgress.appLanguage),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Nút Vào học bo viền xanh lá chuẩn thiết kế
                            val buttonColor = Color(0xFF10B981)
                            Button(
                                onClick = {
                                    onStartStudy(deck)
                                    selectedDeck = null
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = buttonColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(buttonHeight)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "VÀO HỌC".translated(userProgress.appLanguage),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            
                            TextButton(
                                onClick = { selectedDeck = null },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Đóng".translated(userProgress.appLanguage),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DuolingoNode(
    title: String,
    icon: String,
    progress: Float,
    isLocked: Boolean,
    offsetPercent: Float, // -1f (bên trái cực bộ) -> 1f (bên phải cực bộ)
    onClick: () -> Unit
) {
    val alignmentBias = offsetPercent * 100

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(x = alignmentBias.dp)
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            // Draw progress circle ring or dashed ring
            if (!isLocked) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (progress >= 1f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    strokeCap = StrokeCap.Round
                )
            }

            // Central button
            IconButton(
                onClick = onClick,
                enabled = !isLocked,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isLocked -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            progress >= 1f -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
            ) {
                if (isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        text = icon,
                        fontSize = 28.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NodeConnector(height: androidx.compose.ui.unit.Dp, offsetPercent: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val startX = size.width / 2 + (offsetPercent * 100.dp.toPx())
        // Vẽ đường nối nét đứt dọc nhẹ nghệ thuật
        drawLine(
            color = Color.LightGray.copy(alpha = 0.6f),
            start = Offset(startX, 0f),
            end = Offset(startX, size.height),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}
