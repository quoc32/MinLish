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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minlishapp.ui.theme.*

@Composable
fun StatsScreen(onNavigate: (Screen) -> Unit) {
    Scaffold(
        bottomBar = { AppBottomBar(currentScreen = Screen.Stats, onNavigate = onNavigate) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Thống Kê",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Khung tổng quan
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
                        Text(text = "520", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Tổng từ đã học", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🔥", fontSize = 20.sp)
                        Text(text = "15 ngày", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ColorStreakFlame)
                        Text(text = "Streak cao nhất", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "⚡", fontSize = 20.sp)
                        Text(text = "95%", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ColorEasy)
                        Text(text = "Tỷ lệ nhớ lâu", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Biểu đồ 1: Hoạt động học tuần qua (Canvas Bar Chart)
            Text(
                text = "Số từ ôn tập hằng ngày",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val weekData = listOf(
                "T2" to 15,
                "T3" to 22,
                "T4" to 8,
                "T5" to 30,
                "T6" to 25,
                "T7" to 12,
                "CN" to 18
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
                        val maxVal = 35f
                        val widthGap = size.width / (weekData.size)
                        val barWidth = 24.dp.toPx()

                        // Vẽ đường tham chiếu phụ ngang
                        drawLine(
                            color = outlineColor.copy(alpha = 0.5f),
                            start = Offset(0f, size.height * 0.5f),
                            end = Offset(size.width, size.height * 0.5f),
                            strokeWidth = 1.dp.toPx()
                        )

                        weekData.forEachIndexed { index, (_, value) ->
                            val left = index * widthGap + (widthGap - barWidth) / 2
                            val barHeight = (value / maxVal) * size.height
                            val top = size.height - barHeight

                            // Vẽ cột bo góc nhẹ
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Vẽ nhãn ngày ở trục X
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        weekData.forEach { (day, _) ->
                            Text(
                                text = day,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Biểu đồ 2: Đồ thị tròn tỷ lệ nhớ (Donut Chart)
            Text(
                text = "Tỷ lệ trạng thái từ vựng",
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
                    // Canvas Donut Chart
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = Stroke(width = 16.dp.toPx())
                            // Vẽ 3 cung tròn: Đã thuộc (60%), Đang ôn (30%), Học lại (10%)
                            drawArc(
                                color = ColorEasy, // Xanh lá
                                startAngle = -90f,
                                sweepAngle = 216f,
                                useCenter = false,
                                style = stroke
                            )
                            drawArc(
                                color = ColorGood, // Xanh dương
                                startAngle = 126f,
                                sweepAngle = 108f,
                                useCenter = false,
                                style = stroke
                            )
                            drawArc(
                                color = ColorAgain, // Đỏ
                                startAngle = 234f,
                                sweepAngle = 36f,
                                useCenter = false,
                                style = stroke
                            )
                        }
                        Text(
                            text = "520 từ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Chú thích
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LegendRow(color = ColorEasy, label = "Thành thạo (60%)")
                        LegendRow(color = ColorGood, label = "Đang nhớ (30%)")
                        LegendRow(color = ColorAgain, label = "Cần ôn gấp (10%)")
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
