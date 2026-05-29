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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LessonCompleteScreen(
    onNavigate: (Screen) -> Unit,
    xpGained: Int,
    streak: Int,
    accuracy: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
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
                    modifier = Modifier.size(160.dp),
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
                            val x = (size.width / 2 + 65.dp.toPx() * cos(angle)).toFloat()
                            val y = (size.height / 2 + 65.dp.toPx() * sin(angle)).toFloat()
                            drawCircle(
                                color = ColorStreakFlame,
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }

                    // Cúp vàng chiến thắng ở giữa
                    Text(text = "🏆", fontSize = 72.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Hoàn Thành Bài Học!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Bạn đã hoàn thành xuất sắc mục tiêu ngày hôm nay. Hãy duy trì phong độ nhé!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                )
            }

            // Phần thống kê phần thưởng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thẻ XP
                RewardCard(
                    weight = 1f,
                    icon = "⚡",
                    value = "+$xpGained XP",
                    label = "Kinh nghiệm",
                    color = ColorGood
                )

                // Thẻ Streak
                RewardCard(
                    weight = 1f,
                    icon = "🔥",
                    value = "$streak ngày",
                    label = "Chuỗi ngày",
                    color = ColorStreakFlame
                )

                // Thẻ Accuracy
                RewardCard(
                    weight = 1f,
                    icon = "🎯",
                    value = "$accuracy%",
                    label = "Chính xác",
                    color = ColorEasy
                )
            }

            // Nút Tiếp tục ở dưới cùng
            Button(
                onClick = { onNavigate(Screen.Dashboard) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Tiếp tục lộ trình",
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
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .weight(weight)
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
