package com.example.minlishapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minlishapp.R

@Composable
fun RedWhiteEmailIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White)
            .border(1.5.dp, Color(0xFFDC2626), RoundedCornerShape(3.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(1.dp)) {
            val w = size.width
            val h = size.height
            // Draw envelope folds
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(w / 2f, h * 0.55f)
                lineTo(w, 0f)
            }
            drawPath(
                path = path,
                color = Color(0xFFDC2626),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun WelcomeScreen(
    onLoginSuccess: (userId: String, displayName: String, targetGoal: String, xp: Int, level: Int, streak: Int) -> Unit,
    onNavigate: (Screen) -> Unit
) {
    val context = LocalContext.current
    var showGoogleAccountPicker by remember { mutableStateOf(false) }
    val mockGoogleAccounts = listOf(
        Pair("Nguyen Van A", "nva@gmail.com"),
        Pair("Tran Thi B", "ttb@gmail.com"),
        Pair("MinLish Student", "student@minlish.edu.vn")
    )

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
            // Phần trên: Tiêu đề & Giới thiệu
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Welcome Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Chào mừng tới MinLish",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Phương pháp Spaced Repetition (SM-2) kết hợp trò chơi hóa giúp bạn học từ vựng tiếng Anh không bao giờ quên.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Phần giữa: Minh họa chú trâu vàng vẫy chào vui nhộn (Hello)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(320.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                // Hào quang tỏa sáng phía sau chú trâu vàng
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                Image(
                    painter = painterResource(id = R.drawable.buffalo_hello),
                    contentDescription = "Vietnamese Golden Buffalo Waving Hello",
                    modifier = Modifier
                        .size(300.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            // Phần dưới: Các nút hành động đăng nhập/khách
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onNavigate(Screen.Login) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    )
                ) {
                    RedWhiteEmailIcon()
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Đăng nhập bằng Email",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = { showGoogleAccountPicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA4335),
                        contentColor = Color.White
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEA4335)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Đăng nhập với Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Google Account Picker Mock Dialog
        if (showGoogleAccountPicker) {
            Dialog(onDismissRequest = { showGoogleAccountPicker = false }) {
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
                            text = "Đăng nhập bằng Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Chọn tài khoản để tiếp tục với MinLish",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            mockGoogleAccounts.forEach { (name, mail) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .clickable {
                                            showGoogleAccountPicker = false
                                            onLoginSuccess(
                                                "b64361ca-719d-4a07-b50f-910d8e05f9da",
                                                name,
                                                "IELTS 7.5",
                                                380,
                                                4,
                                                5
                                            )
                                            Toast.makeText(context, "Chào mừng $name!", Toast.LENGTH_SHORT).show()
                                            onNavigate(Screen.LanguageSelection)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name.first().toString().uppercase(),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = mail, color = Color.Gray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        TextButton(
                            onClick = { showGoogleAccountPicker = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Hủy", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
