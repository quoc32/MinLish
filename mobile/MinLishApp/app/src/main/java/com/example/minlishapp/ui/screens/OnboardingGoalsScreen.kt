package com.example.minlishapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minlishapp.data.UserProgress

@Composable
fun OnboardingGoalsScreen(
    userProgress: UserProgress,
    onProgressUpdate: (UserProgress) -> Unit,
    onNavigate: (Screen) -> Unit
) {
    var selectedGoal by remember { mutableStateOf(userProgress.targetGoal) }

    val goals = listOf(
        Triple("IELTS", "🎯", "Academic & IELTS Core"),
        Triple("TOEIC", "📈", "Business & Office English"),
        Triple("Travel", "✈️", "Tourism & Daily English"),
        Triple("General", "🌍", "Communication & Basic")
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isSmallScreen = maxHeight < 640.dp
        val gridHeight = if (isSmallScreen) 210.dp else 280.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isSmallScreen) 16.dp else 24.dp)
                .systemBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    IconButton(
                        onClick = { onNavigate(Screen.LanguageSelection) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Mục tiêu học tập",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Mục tiêu học của bạn là gì?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 12.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 12.dp else 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                ) {
                    items(goals) { (goalName, icon, desc) ->
                        val isSel = selectedGoal == goalName
                        Card(
                            onClick = { selectedGoal = goalName },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSel) 2.dp else 1.dp,
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(if (isSmallScreen) 12.dp else 20.dp),
                                verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 4.dp else 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = icon, fontSize = if (isSmallScreen) 24.sp else 32.sp)
                                    if (isSel) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(if (isSmallScreen) 18.dp else 24.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = goalName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isSmallScreen) 14.sp else 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    fontSize = if (isSmallScreen) 10.sp else 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onProgressUpdate(userProgress.copy(targetGoal = selectedGoal))
                    onNavigate(Screen.OnboardingDailyWords)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmallScreen) 46.dp else 52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Tiếp tục",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Continue",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
