package com.example.minlishapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minlishapp.data.DashboardData
import com.example.minlishapp.data.ProfileStats
import com.example.minlishapp.data.DonutChartData
import com.example.minlishapp.data.BarChartData
import com.example.minlishapp.data.repository.StatsRepository
import com.example.minlishapp.ui.screens.Screen
import com.example.minlishapp.ui.screens.AppBottomBar
import com.example.minlishapp.ui.theme.ColorStreakFlame
import com.example.minlishapp.ui.theme.ColorEasy
import com.example.minlishapp.ui.theme.ColorGood
import com.example.minlishapp.ui.theme.ColorAgain
import com.example.minlishapp.ui.theme.MinLishAppTheme
import kotlinx.coroutines.launch
import com.example.minlishapp.core.utils.translated

// Interface definition for UI states
sealed interface StatsUiState {
    object Loading : StatsUiState
    data class Success(val data: DashboardData) : StatsUiState
    data class Error(val message: String) : StatsUiState
}

@Composable
fun StatsScreen(userId: String, appLanguage: String, onNavigate: (Screen) -> Unit) {
    val statsRepository = remember { StatsRepository.create() }
    var uiState by remember { mutableStateOf<StatsUiState>(StatsUiState.Loading) }
    val coroutineScope = rememberCoroutineScope()

    // Use passed userId, fallback to default test user ID if empty
    val activeUserId = if (userId.isBlank()) "b64361ca-719d-4a07-b50f-910d8e05f9da" else userId

    fun fetchStats() {
        coroutineScope.launch {
            uiState = StatsUiState.Loading
            try {
                val response = statsRepository.getStatsDashboard(activeUserId)
                if (response.success && response.data != null) {
                    uiState = StatsUiState.Success(response.data)
                } else {
                    uiState = StatsUiState.Error(response.message ?: "Không thể tải dữ liệu thống kê")
                }
            } catch (e: Exception) {
                uiState = StatsUiState.Error("Lỗi kết nối: ${e.localizedMessage ?: "Không thể kết nối đến máy chủ"}")
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchStats()
    }

    StatsScreenContent(
        uiState = uiState,
        appLanguage = appLanguage,
        onNavigate = onNavigate,
        onRetry = { fetchStats() }
    )
}

@Composable
fun StatsScreenContent(
    uiState: StatsUiState,
    appLanguage: String = "Vietnamese",
    onNavigate: (Screen) -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        bottomBar = { AppBottomBar(currentScreen = Screen.Stats, onNavigate = onNavigate, appLanguage = appLanguage) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is StatsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is StatsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "⚠️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(onClick = onRetry) {
                            Text("Thử lại".translated(appLanguage))
                        }
                    }
                }
                is StatsUiState.Success -> {
                    val dashboardData = state.data
                    val profile = dashboardData.profile
                    val donut = dashboardData.donutChart
                    val barData = dashboardData.barChart

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Thống Kê".translated(appLanguage),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Khung tổng quan động
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "📚", fontSize = 20.sp)
                                    Text(text = "${donut.totalStudied}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(text = "Tổng từ đã học".translated(appLanguage), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "🔥", fontSize = 20.sp)
                                    Text(text = "${profile.streak} " + "ngày".translated(appLanguage), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ColorStreakFlame)
                                    Text(text = "Streak hiện tại".translated(appLanguage), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "⚡", fontSize = 20.sp)
                                    val displayedRetentionRate = if (profile.retentionRate > 1.0) {
                                        profile.retentionRate.toInt()
                                    } else {
                                        (profile.retentionRate * 100).toInt()
                                    }
                                    Text(text = "$displayedRetentionRate%", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ColorEasy)
                                    Text(text = "Tỷ lệ nhớ lâu".translated(appLanguage), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Biểu đồ 1: Hoạt động học tuần qua (Canvas Bar Chart nhận dữ liệu động)
                        Text(
                            text = "Số từ ôn tập hằng ngày".translated(appLanguage),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val barColor = MaterialTheme.colorScheme.primary
                        val outlineColor = MaterialTheme.colorScheme.outline

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Canvas(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    val maxVal = (barData.maxOfOrNull { it.wordsCount } ?: 10).toFloat().coerceAtLeast(10f)
                                    val widthGap = size.width / (barData.size)
                                    val barWidth = 24.dp.toPx()

                                    // Vẽ đường tham chiếu phụ ngang ở giữa (50%)
                                    drawLine(
                                        color = outlineColor.copy(alpha = 0.5f),
                                        start = Offset(0f, size.height * 0.5f),
                                        end = Offset(size.width, size.height * 0.5f),
                                        strokeWidth = 1.dp.toPx()
                                    )

                                    barData.forEachIndexed { index, barItem ->
                                        val left = index * widthGap + (widthGap - barWidth) / 2
                                        val barHeight = (barItem.wordsCount / maxVal) * size.height
                                        val top = size.height - barHeight

                                        // Chỉ vẽ khi cột có chiều cao
                                        if (barHeight > 0f) {
                                            drawRoundRect(
                                                color = barColor,
                                                topLeft = Offset(left, top),
                                                size = Size(barWidth, barHeight),
                                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Vẽ nhãn ngày thực tế ở trục X
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    barData.forEach { barItem ->
                                        Text(
                                            text = barItem.label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Biểu đồ 2: Đồ thị tròn tỷ lệ nhớ (Donut Chart nhận dữ liệu động)
                        Text(
                            text = "Tỷ lệ trạng thái từ vựng".translated(appLanguage),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val total = donut.totalStudied.toFloat().coerceAtLeast(1f)
                                val sweepEasy = (donut.proficient / total) * 360f
                                val sweepGood = (donut.learning / total) * 360f
                                val sweepAgain = (donut.needsReview / total) * 360f

                                val pctEasy = ((donut.proficient / total) * 100).toInt()
                                val pctGood = ((donut.learning / total) * 100).toInt()
                                val pctAgain = ((donut.needsReview / total) * 100).toInt()

                                // Canvas Donut Chart
                                Box(
                                    modifier = Modifier.size(110.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val stroke = Stroke(width = 16.dp.toPx())
                                        var startAngle = -90f

                                        if (sweepEasy > 0f) {
                                            drawArc(
                                                color = ColorEasy, // Xanh lá
                                                startAngle = startAngle,
                                                sweepAngle = sweepEasy,
                                                useCenter = false,
                                                style = stroke
                                            )
                                            startAngle += sweepEasy
                                        }
                                        if (sweepGood > 0f) {
                                            drawArc(
                                                color = ColorGood, // Xanh dương
                                                startAngle = startAngle,
                                                sweepAngle = sweepGood,
                                                useCenter = false,
                                                style = stroke
                                            )
                                            startAngle += sweepGood
                                        }
                                        if (sweepAgain > 0f) {
                                            drawArc(
                                                color = ColorAgain, // Đỏ
                                                startAngle = startAngle,
                                                sweepAngle = sweepAgain,
                                                useCenter = false,
                                                style = stroke
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${donut.totalStudied} " + "từ".translated(appLanguage),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(24.dp))

                                // Chú thích động với tỉ lệ thực tế
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LegendRow(color = ColorEasy, label = "Thành thạo".translated(appLanguage) + " (${pctEasy}%)")
                                    LegendRow(color = ColorGood, label = "Đang nhớ".translated(appLanguage) + " (${pctGood}%)")
                                    LegendRow(color = ColorAgain, label = "Cần ôn gấp".translated(appLanguage) + " (${pctAgain}%)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StatsScreenPreview() {
    val sampleDashboardData = DashboardData(
        profile = ProfileStats(
            xp = 1248,
            level = 12,
            streak = 15,
            maxStreak = 30,
            targetGoal = "IELTS",
            retentionRate = 0.85
        ),
        donutChart = DonutChartData(
            learning = 45,
            proficient = 120,
            needsReview = 15,
            totalStudied = 180
        ),
        barChart = listOf(
            BarChartData("2023-10-01", "Mon", 12),
            BarChartData("2023-10-02", "Tue", 18),
            BarChartData("2023-10-03", "Wed", 8),
            BarChartData("2023-10-04", "Thu", 25),
            BarChartData("2023-10-05", "Fri", 15),
            BarChartData("2023-10-06", "Sat", 30),
            BarChartData("2023-10-07", "Sun", 20)
        )
    )

    MinLishAppTheme {
        StatsScreenContent(
            uiState = StatsUiState.Success(sampleDashboardData),
            onNavigate = {},
            onRetry = {}
        )
    }
}
