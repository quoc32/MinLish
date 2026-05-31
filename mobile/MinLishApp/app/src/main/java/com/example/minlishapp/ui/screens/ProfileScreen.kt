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
import com.example.minlishapp.core.network.TokenManager
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.minlishapp.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    userProgress: UserProgress,
    onProgressUpdate: (UserProgress) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onNavigate: (Screen) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current

    // Tự động tải profile khi vào màn hình
    LaunchedEffect(Unit) {
        viewModel.fetchProfile(userProgress, onProgressUpdate)
    }
    
    val goalOptions = listOf("IELTS", "TOEIC", "Giao tiếp", "THPT Quốc gia")

    Scaffold(
        bottomBar = { AppBottomBar(currentScreen = Screen.Profile, onNavigate = onNavigate) }
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
                text = "Hồ Sơ",
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

                    if (viewModel.isEditingName) {
                        OutlinedTextField(
                            value = viewModel.nameInput,
                            onValueChange = { viewModel.nameInput = it },
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
                            viewModel.saveProfileName(userProgress, onProgressUpdate)
                        }, enabled = !viewModel.isLoading) {
                            Text("Lưu", fontWeight = FontWeight.Bold)
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
                                onClick = { viewModel.isEditingName = true },
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
                        Text(
                            text = "Học viên Cấp độ ${userProgress.level}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cài đặt chung
            Text(
                text = "Cài đặt học tập",
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
                            Text(text = "Chế độ giao diện Tối", fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                            .clickable { viewModel.showGoalPicker = true }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎯", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Thay đổi mục tiêu học", fontSize = 14.sp, fontWeight = FontWeight.Medium)
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

                    // Dòng cài đặt 3: Reset dữ liệu học tập
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onProgressUpdate(UserProgress())
                                Toast.makeText(context, "Đã khôi phục cài đặt gốc thành công!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔄", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Đặt lại toàn bộ tiến độ",
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
                text = "Thông báo học tập",
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
                                Text(text = "Nhắc nhở học hàng ngày", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "Nhận thông báo để duy trì chuỗi học tập",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = viewModel.dailyReminderEnabled,
                            onCheckedChange = { viewModel.updateDailyReminderEnabled(it) }
                        )
                    }

                    if (viewModel.dailyReminderEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                        // Dòng 1b: Chọn giờ nhắc học
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.showTimePickerDialog = true }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⏰", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "Giờ nhắc học hàng ngày", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = viewModel.dailyReminderTime,
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
                                Text(text = "Ôn tập theo thuật toán SM-2", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "Nhắc ôn các từ đến hạn (Spaced Repetition)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = viewModel.reviewReminderEnabled,
                            onCheckedChange = { viewModel.updateReviewReminderEnabled(it) }
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
                        onProgressUpdate(UserProgress())
                        Toast.makeText(context, "Đăng xuất thành công!", Toast.LENGTH_SHORT).show()
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
                            text = "Đăng xuất tài khoản",
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
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Time Picker Dialog Mock
        if (viewModel.showTimePickerDialog) {
            Dialog(onDismissRequest = { viewModel.showTimePickerDialog = false }) {
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
                            text = "Cài Đặt Giờ Nhắc Học",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Chọn khung giờ nhắc học phù hợp:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Grid of presets
                        val presets = listOf(
                            "01:30" to "Học đêm khuya 🦉",
                            "07:00" to "Sáng sớm 🌅",
                            "09:00" to "Bắt đầu ngày ☀️",
                            "12:15" to "Nghỉ trưa 🍱",
                            "18:00" to "Chiều tối 🌆",
                            "20:30" to "Buổi tối 🌃",
                            "22:30" to "Trước ngủ 🛌"
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
                                        val isSelected = viewModel.dailyReminderTime == timeStr
                                        Card(
                                            onClick = {
                                                viewModel.updateDailyReminderTime(timeStr)
                                                viewModel.showTimePickerDialog = false
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
                                val hour = viewModel.dailyReminderTime.split(":").getOrNull(0)?.toIntOrNull() ?: 9
                                val minute = viewModel.dailyReminderTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0
                                TimePickerDialog(
                                    context,
                                    { _, selectedHour, selectedMinute ->
                                        val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                                        viewModel.updateDailyReminderTime(formattedTime)
                                        viewModel.showTimePickerDialog = false
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
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text("Tự chọn giờ khác...", fontWeight = FontWeight.Bold)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.showTimePickerDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Đóng")
                            }
                        }
                    }
                }
            }
        }
        // Goal Picker Dialog
        if (viewModel.showGoalPicker) {
            Dialog(onDismissRequest = { viewModel.showGoalPicker = false }) {
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
                            text = "Thay đổi mục tiêu học",
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
                                        viewModel.saveProfileGoal(goal, userProgress, onProgressUpdate)
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = goal,
                                        modifier = Modifier.padding(16.dp),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        
                        TextButton(
                            onClick = { viewModel.showGoalPicker = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Đóng", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
