package com.example.minlishapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minlishapp.ui.theme.*
import com.example.minlishapp.core.utils.translated
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LessonCompleteScreen(
    onNavigate: (Screen) -> Unit,
    xpGained: Int,
    streak: Int,
    accuracy: Int,
    appLanguage: String = "Vietnamese"
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isSmallScreen = maxHeight < 640.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isSmallScreen) 16.dp else 24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Spacer trên cùng
            Spacer(modifier = Modifier.height(24.dp))

            // Giữa: Vòng tròn ăn mừng vinh danh
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(if (isSmallScreen) 110.dp else 160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Hiệu ứng pháo hoa xoay tròn nhẹ
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        drawArc(
                            color = ColorEasy,
                            startAngle = -90f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = stroke
                        )
                        // Bông pháo hoa tròn
                        for (i in 0 until 8) {
                            val angle = (i * 45) * (Math.PI / 180)
                            val radiusPx = if (isSmallScreen) 42.dp.toPx() else 65.dp.toPx()
                            val x = (size.width / 2 + radiusPx * cos(angle)).toFloat()
                            val y = (size.height / 2 + radiusPx * sin(angle)).toFloat()
                            drawCircle(
                                color = ColorStreakFlame,
                                radius = 3.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }

                    // Cúp vàng chiến thắng ở giữa
                    Text(text = "🏆", fontSize = if (isSmallScreen) 48.sp else 72.sp)
                }

                Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 24.dp))

                Text(
                    text = "Hoàn Thành Bài Học!".translated(appLanguage),
                    fontSize = if (isSmallScreen) 20.sp else 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Bạn đã hoàn thành xuất sắc mục tiêu ngày hôm nay. Hãy duy trì phong độ nhé!".translated(appLanguage),
                    fontSize = if (isSmallScreen) 12.sp else 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                )
            }

            // Phần thống kê phần thưởng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 8.dp else 12.dp)
            ) {
                // Thẻ XP
                RewardCard(
                    weight = 1f,
                    icon = "⚡",
                    value = "+$xpGained XP",
                    label = "Kinh nghiệm".translated(appLanguage),
                    color = ColorGood,
                    isSmallScreen = isSmallScreen
                )

                // Thẻ Streak
                val streakValue = if (appLanguage == "English") "$streak days" else "$streak ngày"
                RewardCard(
                    weight = 1f,
                    icon = "🔥",
                    value = streakValue,
                    label = "Chuỗi ngày".translated(appLanguage),
                    color = ColorStreakFlame,
                    isSmallScreen = isSmallScreen
                )

                // Thẻ Accuracy
                RewardCard(
                    weight = 1f,
                    icon = "🎯",
                    value = "$accuracy%",
                    label = "Chính xác".translated(appLanguage),
                    color = ColorEasy,
                    isSmallScreen = isSmallScreen
                )
            }

            // Nút Tiếp tục ở dưới cùng
            Button(
                onClick = { onNavigate(Screen.Dashboard) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmallScreen) 46.dp else 52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Tiếp tục lộ trình".translated(appLanguage),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RowScope.RewardCard(
    weight: Float,
    icon: String,
    value: String,
    label: String,
    color: Color,
    isSmallScreen: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .weight(weight)
            .height(if (isSmallScreen) 76.dp else 100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(if (isSmallScreen) 4.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = if (isSmallScreen) 18.sp else 24.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = if (isSmallScreen) 11.sp else 14.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
