package com.example.minlishapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LanguageSelectionScreen(onNavigate: (Screen) -> Unit) {
    var selectedLang by remember { mutableStateOf("vi") }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isSmallScreen = maxHeight < 640.dp
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding()
                .then(
                    if (isSmallScreen) Modifier.verticalScroll(rememberScrollState())
                    else Modifier
                ),
            verticalArrangement = if (isSmallScreen) Arrangement.spacedBy(16.dp) else Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    IconButton(
                        onClick = { onNavigate(Screen.Welcome) },
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
                        text = "Ngôn ngữ ứng dụng",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 32.dp))

                Text(
                    text = "Vui lòng chọn ngôn ngữ để hiển thị hướng dẫn và giải nghĩa từ vựng trong ứng dụng:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))

                // Thẻ chọn ngôn ngữ: Tiếng Việt
                Card(
                    onClick = { selectedLang = "vi" },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedLang == "vi") MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (selectedLang == "vi") 2.dp else 1.dp,
                        color = if (selectedLang == "vi") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (isSmallScreen) 14.dp else 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🇻🇳", fontSize = if (isSmallScreen) 24.sp else 32.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Tiếng Việt",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Giao diện và giải nghĩa tiếng Việt",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        RadioButton(
                            selected = selectedLang == "vi",
                            onClick = { selectedLang = "vi" }
                        )
                    }
                }

                // Thẻ chọn ngôn ngữ: Tiếng Anh
                Card(
                    onClick = { selectedLang = "en" },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedLang == "en") MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (selectedLang == "en") 2.dp else 1.dp,
                        color = if (selectedLang == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (isSmallScreen) 14.dp else 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🇬🇧", fontSize = if (isSmallScreen) 24.sp else 32.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "English",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Interface and definition in English",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        RadioButton(
                            selected = selectedLang == "en",
                            onClick = { selectedLang = "en" }
                        )
                    }
                }
            }

            Button(
                onClick = { onNavigate(Screen.OnboardingGoals) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmallScreen) 46.dp else 52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Tiếp tục",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
