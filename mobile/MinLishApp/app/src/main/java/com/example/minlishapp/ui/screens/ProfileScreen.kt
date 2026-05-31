package com.example.minlishapp.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import com.example.minlishapp.core.utils.ReminderManager
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minlishapp.data.UserProgress
import com.example.minlishapp.data.ProfileUpdateRequest
import com.example.minlishapp.data.SimpleResponse
import com.example.minlishapp.core.network.TokenManager
import com.example.minlishapp.data.repository.AuthRepository
import com.example.minlishapp.data.repository.LearningRepository
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import com.example.minlishapp.core.utils.translated

@Composable

fun ProfileScreen(
    userProgress: UserProgress,
    onProgressUpdate: (UserProgress) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(userProgress.name) }
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("minlish_prefs", Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository.create(context) }
    val learningRepository = remember { LearningRepository.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    var masteredWords by remember { mutableStateOf(0) }
    var accuracyRate by remember { mutableStateOf(0.0) }

    var showPromotionDialog by remember { mutableStateOf(false) }
    var oldTierForPromotion by remember { mutableStateOf("") }
    var newTierForPromotion by remember { mutableStateOf("") }

    // Tự động tải profile khi vào màn hình
    LaunchedEffect(Unit) {
        try {
            val response = authRepository.getProfile()
            if (response.isSuccessful) {
                val profile = response.body()?.data
                if (profile != null) {
                    onProgressUpdate(userProgress.copy(
                        name = profile.displayName ?: userProgress.name,
                        email = profile.email ?: userProgress.email,
                        targetGoal = profile.targetGoal,
                        wordsPerDay = profile.wordsPerDay,
                        xp = profile.xp,
                        level = profile.level,
                        streak = profile.streak
                    ))
                    nameInput = profile.displayName ?: userProgress.name
                    accuracyRate = profile.retentionRate
                }
            }
        } catch (e: Exception) {
            // Ignore for now, keep current state
        }

        try {
            val statsRepo = com.example.minlishapp.data.repository.StatsRepository.create()
            val statsResponse = statsRepo.getStatsDashboard(userProgress.userId)
            if (statsResponse.success && statsResponse.data != null) {
                masteredWords = statsResponse.data.donutChart.proficient
                accuracyRate = statsResponse.data.profile.retentionRate
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    var dailyReminderEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("daily_reminder_enabled", true)) }
    var reviewReminderEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("review_reminder_enabled", true)) }
    var dailyReminderTime by remember { mutableStateOf(sharedPrefs.getString("daily_reminder_time", "09:00") ?: "09:00") }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showGoalPicker by remember { mutableStateOf(false) }
    var showWordsPerDayPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    
    val goalOptions = listOf("IELTS", "TOEIC", "Giao tiếp", "THPT Quốc gia")
    val wordsPerDayOptions = listOf(5, 10, 15, 20, 25)

    // Tính toán Progress Score và Level Title theo công thức real-time
    val masteredScore = minOf(100f, (masteredWords.toFloat() / 750f) * 100f)
    val accuracyPercent = accuracyRate.toFloat() // accuracyRate từ backend đã là % (0 - 100)
    val accuracyScore = (accuracyPercent / 10f).toInt() * 10f
    val streakScore = minOf(100f, (userProgress.streak.toFloat() / 30f) * 100f)
    
    val progressScore = (masteredScore * 0.5f) + (accuracyScore * 0.3f) + (streakScore * 0.2f)

    val isBeginner = masteredWords < 200 || progressScore < 40
    val isIntermediate = !isBeginner && (masteredWords < 800 || progressScore < 70)
    val levelTitle = when {
        isBeginner -> "Beginner"
        isIntermediate -> "Intermediate"
        else -> "Advanced"
    }
    val displayScore = (progressScore * 10).toInt() / 10.0

    val currentTier = levelTitle

    LaunchedEffect(currentTier) {
        if (masteredWords > 0 || accuracyRate > 0) {
            val lastTier = sharedPrefs.getString("last_known_tier", "") ?: ""
            if (lastTier.isNotEmpty() && lastTier != currentTier) {
                val getTierPriority = { tier: String ->
                    when (tier) {
                        "Advanced" -> 3
                        "Intermediate" -> 2
                        "Beginner" -> 1
                        else -> 0
                    }
                }
                val lastPriority = getTierPriority(lastTier)
                val currentPriority = getTierPriority(currentTier)
                if (currentPriority > lastPriority) {
                    oldTierForPromotion = lastTier
                    newTierForPromotion = currentTier
                    showPromotionDialog = true
                }
            }
            sharedPrefs.edit().putString("last_known_tier", currentTier).apply()
        }
    }

    Scaffold(
        bottomBar = { AppBottomBar(currentScreen = Screen.Profile, onNavigate = onNavigate, appLanguage = userProgress.appLanguage) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
            Text(
                text = "Hồ Sơ".translated(userProgress.appLanguage),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Khung Avatar & Tên
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userProgress.name.isNotEmpty()) userProgress.name.first().toString().uppercase() else "L",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isEditingName) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.width(180.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = {
                            if (nameInput.isNotBlank()) {
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val response = authRepository.updateProfile(ProfileUpdateRequest(displayName = nameInput))
                                        isLoading = false
                                        if (response.isSuccessful) {
                                            onProgressUpdate(userProgress.copy(name = nameInput))
                                            isEditingName = false
                                            Toast.makeText(context, "Đã đổi tên thành công!".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Đổi tên thất bại!".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        Toast.makeText(context, "Lỗi kết nối".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }, enabled = !isLoading) {
                            Text("Lưu".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = userProgress.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            IconButton(
                                onClick = { isEditingName = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (userProgress.email.isNotEmpty()) {
                            Text(
                                text = userProgress.email,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            )
                        }
                        val tierIcon = when (levelTitle) {
                            "Advanced" -> "🏆 Advanced"
                            "Intermediate" -> "📘 Intermediate"
                            else -> "🌱 Beginner"
                        }
                        
                        val tierColor = when (levelTitle) {
                            "Advanced" -> Color(0xFFFFD700) // Gold
                            "Intermediate" -> Color(0xFF1E88E5) // Blue
                            else -> Color(0xFF4CAF50) // Green
                        }

                        val tierBgColor = tierColor.copy(alpha = 0.08f)
                        val tierBorderColor = tierColor.copy(alpha = 0.25f)

                        val totalBlocks = 20
                        val activeBlocks = (progressScore.toInt() / 5).coerceIn(0, totalBlocks)
                        val inactiveBlocks = totalBlocks - activeBlocks
                        val progressText = "█".repeat(activeBlocks) + "░".repeat(inactiveBlocks)

                        val textRemaining = if (userProgress.appLanguage == "English") {
                            if (progressScore < 40) {
                                "${40 - progressScore.toInt()} points remaining to reach Intermediate."
                            } else if (progressScore < 70) {
                                "${70 - progressScore.toInt()} points remaining to reach Advanced."
                            } else {
                                "You have reached the maximum Advanced level!"
                            }
                        } else {
                            if (progressScore < 40) {
                                "Còn ${40 - progressScore.toInt()} điểm để đạt Intermediate."
                            } else if (progressScore < 70) {
                                "Còn ${70 - progressScore.toInt()} điểm để đạt Advanced."
                            } else {
                                "Bạn đã đạt cấp độ tối đa Advanced!"
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = tierBgColor),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, tierBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                 Text(
                                     text = tierIcon,
                                     fontWeight = FontWeight.Bold,
                                     fontSize = 15.sp,
                                     color = tierColor
                                 )

                                // Score
                                Text(
                                    text = "Score: ${progressScore.toInt()} / 100",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Progress bar kí tự
                                Text(
                                    text = progressText,
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = tierColor,
                                    letterSpacing = 1.sp
                                )

                                // Text gợi ý thăng hạng
                                Text(
                                    text = textRemaining,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cài đặt chung
            Text(
                text = "Cài đặt học tập".translated(userProgress.appLanguage),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Dòng cài đặt 1: Toggle Theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (isDarkTheme) "🌙" else "☀️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Chế độ giao diện Tối".translated(userProgress.appLanguage), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onThemeToggle() }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Dòng cài đặt 2: Mục tiêu học
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showGoalPicker = true }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎯", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Thay đổi mục tiêu học".translated(userProgress.appLanguage), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userProgress.targetGoal,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Detail",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Dòng cài đặt 2b: Số từ học mỗi ngày
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showWordsPerDayPicker = true }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📖", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Số từ học mỗi ngày".translated(userProgress.appLanguage), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val wordsStr = if (userProgress.appLanguage == "English") "${userProgress.wordsPerDay} words" else "${userProgress.wordsPerDay} từ"
                            Text(
                                text = wordsStr,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Detail",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Dòng cài đặt 2c: Ngôn ngữ hiển thị
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguagePicker = true }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🌐", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Ngôn ngữ hiển thị".translated(userProgress.appLanguage),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (userProgress.appLanguage == "English") "English" else "Tiếng Việt",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Detail",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Dòng cài đặt 3: Reset dữ liệu học tập
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showResetConfirmDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔄", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Đặt lại toàn bộ tiến độ".translated(userProgress.appLanguage),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Reset",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cài đặt thông báo học tập
            Text(
                text = "Thông báo học tập".translated(userProgress.appLanguage),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Dòng 1: Switch Nhắc học hàng ngày
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔔", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Nhắc nhở học hàng ngày".translated(userProgress.appLanguage), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "Nhận thông báo để duy trì chuỗi học tập".translated(userProgress.appLanguage),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = dailyReminderEnabled,
                            onCheckedChange = { isEnabled ->
                                dailyReminderEnabled = isEnabled
                                sharedPrefs.edit().putBoolean("daily_reminder_enabled", isEnabled).apply()
                                if (isEnabled) {
                                    ReminderManager.scheduleDailyReminder(context, dailyReminderTime)
                                    val msg = if (userProgress.appLanguage == "English") "Daily reminder enabled at $dailyReminderTime!" else "Đã bật nhắc nhở lúc $dailyReminderTime hàng ngày!"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                } else {
                                    ReminderManager.cancelReminder(context)
                                    Toast.makeText(context, "Đã tắt nhắc nhở hàng ngày!".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    if (dailyReminderEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                        // Dòng 1b: Chọn giờ nhắc học
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePickerDialog = true }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⏰", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "Giờ nhắc học hàng ngày".translated(userProgress.appLanguage), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dailyReminderTime,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Edit Time",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Dòng 2: Switch Nhắc từ vựng đến hạn ôn
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🧠", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Ôn tập theo thuật toán SM-2".translated(userProgress.appLanguage), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "Nhắc ôn các từ đến hạn (Spaced Repetition)".translated(userProgress.appLanguage),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = reviewReminderEnabled,
                            onCheckedChange = { isEnabled ->
                                reviewReminderEnabled = isEnabled
                                sharedPrefs.edit().putBoolean("review_reminder_enabled", isEnabled).apply()
                                if (isEnabled) {
                                    Toast.makeText(context, "Đã bật nhắc nhở ôn tập SM-2!".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Đã tắt nhắc nhở ôn tập!".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // Logout Button
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .clickable {
                        TokenManager.getInstance(context).clearToken()
                        onProgressUpdate(UserProgress(appLanguage = userProgress.appLanguage))
                        Toast.makeText(context, "Đăng xuất thành công!".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                        onNavigate(Screen.Welcome)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout Icon",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Đăng xuất tài khoản".translated(userProgress.appLanguage),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Phiên bản ứng dụng dưới chân trang
            Text(
                text = "MinLish App v1.0.0 • Spaced Repetition",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

        // Time Picker Dialog Mock
        if (showTimePickerDialog) {
            Dialog(onDismissRequest = { showTimePickerDialog = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Cài Đặt Giờ Nhắc Học".translated(userProgress.appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Chọn khung giờ nhắc học phù hợp:".translated(userProgress.appLanguage),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Grid of presets
                        val presets = listOf(
                            "01:30" to (if (userProgress.appLanguage == "English") "Late night 🦉" else "Học đêm khuya 🦉"),
                            "07:00" to (if (userProgress.appLanguage == "English") "Early morning 🌅" else "Sáng sớm 🌅"),
                            "09:00" to (if (userProgress.appLanguage == "English") "Start of day ☀️" else "Bắt đầu ngày ☀️"),
                            "12:15" to (if (userProgress.appLanguage == "English") "Lunch break 🍱" else "Nghỉ trưa 🍱"),
                            "18:00" to (if (userProgress.appLanguage == "English") "Evening 🌆" else "Chiều tối 🌆"),
                            "20:30" to (if (userProgress.appLanguage == "English") "Night 🌃" else "Buổi tối 🌃"),
                            "22:30" to (if (userProgress.appLanguage == "English") "Before bed 🛌" else "Trước ngủ 🛌")
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presets.chunked(2).forEach { rowPresets ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowPresets.forEach { (timeStr, label) ->
                                        val isSelected = dailyReminderTime == timeStr
                                        Card(
                                            onClick = {
                                                dailyReminderTime = timeStr
                                                sharedPrefs.edit().putString("daily_reminder_time", timeStr).apply()
                                                if (dailyReminderEnabled) {
                                                    ReminderManager.scheduleDailyReminder(context, timeStr)
                                                }
                                                val msg = if (userProgress.appLanguage == "English") "Reminder set for $timeStr daily!" else "Đã hẹn giờ nhắc học lúc $timeStr hàng ngày!"
                                                Toast.makeText(
                                                    context,
                                                    msg,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                showTimePickerDialog = false
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = timeStr,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    if (rowPresets.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        // Button for Custom Time Picker
                        Button(
                            onClick = {
                                val hour = dailyReminderTime.split(":").getOrNull(0)?.toIntOrNull() ?: 9
                                val minute = dailyReminderTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0
                                TimePickerDialog(
                                    context,
                                    { _, selectedHour, selectedMinute ->
                                        val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                                        dailyReminderTime = formattedTime
                                        sharedPrefs.edit().putString("daily_reminder_time", formattedTime).apply()
                                        if (dailyReminderEnabled) {
                                            ReminderManager.scheduleDailyReminder(context, formattedTime)
                                        }
                                        val msg = if (userProgress.appLanguage == "English") "Reminder set for $formattedTime daily!" else "Đã hẹn giờ nhắc học lúc $formattedTime hàng ngày!"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        showTimePickerDialog = false
                                    },
                                    hour,
                                    minute,
                                    true
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val customTimeStr = if (userProgress.appLanguage == "English") "Custom Time ⚙️" else "Tùy chỉnh chọn giờ ⚙️"
                            Text(customTimeStr, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showTimePickerDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Đóng".translated(userProgress.appLanguage))
                            }
                        }
                    }
                }
            }
        }
        // Goal Picker Dialog
        if (showGoalPicker) {
            Dialog(onDismissRequest = { showGoalPicker = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Thay đổi mục tiêu học".translated(userProgress.appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            goalOptions.forEach { goal ->
                                val isSelected = userProgress.targetGoal == goal
                                Card(
                                    onClick = {
                                        showGoalPicker = false
                                        isLoading = true
                                        coroutineScope.launch {
                                            try {
                                                val response = authRepository.updateProfile(ProfileUpdateRequest(targetGoal = goal))
                                                isLoading = false
                                                if (response.isSuccessful) {
                                                    onProgressUpdate(userProgress.copy(targetGoal = goal))
                                                    Toast.makeText(context, "Đã cập nhật mục tiêu học!".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Cập nhật thất bại!".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                isLoading = false
                                                Toast.makeText(context, "Lỗi kết nối".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = goal.translated(userProgress.appLanguage),
                                        modifier = Modifier.padding(16.dp),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        
                        TextButton(
                            onClick = { showGoalPicker = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Đóng".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Words Per Day Picker Dialog
        if (showWordsPerDayPicker) {
            Dialog(onDismissRequest = { showWordsPerDayPicker = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Số từ học mỗi ngày".translated(userProgress.appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            wordsPerDayOptions.forEach { count ->
                                val isSelected = userProgress.wordsPerDay == count
                                Card(
                                    onClick = {
                                        showWordsPerDayPicker = false
                                        isLoading = true
                                        coroutineScope.launch {
                                            try {
                                                val response = authRepository.updateProfile(ProfileUpdateRequest(wordsPerDay = count))
                                                isLoading = false
                                                if (response.isSuccessful) {
                                                    onProgressUpdate(userProgress.copy(wordsPerDay = count))
                                                    Toast.makeText(context, "Đã cập nhật mục tiêu từ vựng hàng ngày!".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Cập nhật thất bại!".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                isLoading = false
                                                Toast.makeText(context, "Lỗi kết nối".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val countText = if (userProgress.appLanguage == "English") "$count words / day" else "$count từ / ngày"
                                    Text(
                                        text = countText,
                                        modifier = Modifier.padding(16.dp),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        
                        TextButton(
                            onClick = { showWordsPerDayPicker = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Đóng".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Language Picker Dialog
        if (showLanguagePicker) {
            Dialog(onDismissRequest = { showLanguagePicker = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Ngôn ngữ hiển thị".translated(userProgress.appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        val languages = listOf("Vietnamese" to "Tiếng Việt", "English" to "English")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            languages.forEach { (langKey, label) ->
                                val isSelected = userProgress.appLanguage == langKey
                                Card(
                                    onClick = {
                                        showLanguagePicker = false
                                        onProgressUpdate(userProgress.copy(appLanguage = langKey))
                                        Toast.makeText(context, "Đã đổi ngôn ngữ hiển thị thành công!".translated(langKey), Toast.LENGTH_SHORT).show()
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(16.dp),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        
                        TextButton(
                            onClick = { showLanguagePicker = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Đóng".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Reset Confirm Dialog
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = { Text("Đặt lại tiến độ?".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold) },
                text = { Text("Toàn bộ tiến độ học tập, lịch sử ôn tập và điểm XP tích lũy sẽ bị xóa vĩnh viễn trên cơ sở dữ liệu. Bạn có chắc chắn muốn thực hiện?".translated(userProgress.appLanguage)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetConfirmDialog = false
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val response = learningRepository.resetProgress()
                                    isLoading = false
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        onProgressUpdate(UserProgress(
                                            userId = userProgress.userId,
                                            email = userProgress.email,
                                            name = userProgress.name,
                                            targetGoal = userProgress.targetGoal,
                                            wordsPerDay = userProgress.wordsPerDay,
                                            appLanguage = userProgress.appLanguage
                                        ))
                                        Toast.makeText(context, "Đã khôi phục cài đặt tiến độ ban đầu thành công!".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, response.body()?.message ?: "Đặt lại tiến độ thất bại!".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    isLoading = false
                                    Toast.makeText(context, "Lỗi kết nối".translated(userProgress.appLanguage) + ": ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Xác nhận xóa".translated(userProgress.appLanguage), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text("Hủy".translated(userProgress.appLanguage))
                    }
                }
            )
        }

        // Promotion Dialog
        if (showPromotionDialog) {
            val tierIcon = when (newTierForPromotion) {
                "Advanced" -> "🏆"
                "Intermediate" -> "📘"
                else -> "🌱"
            }
            val congratsTitle = if (userProgress.appLanguage == "English") "🎉 Congratulations! 🎉" else "🎉 Chúc Mừng Thăng Hạng! 🎉"
            val congratsMessage = if (userProgress.appLanguage == "English") {
                "You have advanced from $oldTierForPromotion to $newTierForPromotion! Keep up the great work and maintain your learning habit! 💪"
            } else {
                "Bạn đã thăng hạng trình độ từ $oldTierForPromotion lên $newTierForPromotion! Hãy tiếp tục phát huy và duy trì thói quen học tập nhé! 💪"
            }
            val closeBtnText = if (userProgress.appLanguage == "English") "Close" else "Đóng"

            Dialog(onDismissRequest = { showPromotionDialog = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = congratsTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Hiển thị huy hiệu lớn ở giữa
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = tierIcon, fontSize = 40.sp)
                        }

                        Text(
                            text = congratsMessage,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Button(
                            onClick = { showPromotionDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(closeBtnText)
                        }
                    }
                }
            }
        }
    }
}
