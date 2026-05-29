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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.minlishapp.R
import com.example.minlishapp.data.GoogleLoginRequest
import com.example.minlishapp.data.TokenManager
import com.example.minlishapp.data.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

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
    onLoginSuccess: (userId: String, email: String, displayName: String, targetGoal: String, xp: Int, level: Int, streak: Int) -> Unit,
    onNavigate: (Screen) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("322809065976-21g2210m0rc213m2brjabr0vol0ar8jj.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleAuthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                isLoading = true
                coroutineScope.launch {
                    try {
                        val response = authRepository.loginWithGoogle(GoogleLoginRequest(idToken))
                        isLoading = false
                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body != null && body.success && body.data != null) {
                                val profile = body.data.profile
                                val userId = body.data.user.id
                                val userEmail = body.data.user.email
                                body.data.session?.accessToken?.let { TokenManager.getInstance(context).saveToken(it) }
                                
                                onLoginSuccess(
                                    userId,
                                    userEmail,
                                    profile?.displayName ?: "Học viên",
                                    profile?.targetGoal ?: "IELTS",
                                    profile?.xp ?: 0,
                                    profile?.level ?: 1,
                                    profile?.streak ?: 0
                                )
                                Toast.makeText(context, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show()
                                onNavigate(Screen.Dashboard)
                            } else {
                                errorMessage = body?.message ?: "Đăng nhập Google thất bại!"
                            }
                        } else {
                            errorMessage = "Đăng nhập Google thất bại (Mã lỗi: ${response.code()})"
                        }
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = "Lỗi kết nối: ${e.localizedMessage}"
                    }
                }
            }
        } catch (e: ApiException) {
            isLoading = false
            errorMessage = "Đăng nhập Google bị hủy hoặc thất bại."
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val screenHeight = maxHeight
        val isSmallScreen = screenHeight < 680.dp

        // Dynamic sizes
        val illustrationSize = if (isSmallScreen) minOf(240.dp, screenHeight * 0.3f) else 300.dp
        val topPadding = if (isSmallScreen) 16.dp else 40.dp
        val bottomPadding = if (isSmallScreen) 10.dp else 20.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .systemBarsPadding()
                .then(
                    if (isSmallScreen) Modifier.verticalScroll(rememberScrollState())
                    else Modifier
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isSmallScreen) Arrangement.spacedBy(16.dp) else Arrangement.SpaceBetween
        ) {
            // Phần trên: Tiêu đề & Giới thiệu
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = topPadding)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSmallScreen) 56.dp else 72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Welcome Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isSmallScreen) 28.dp else 36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 20.dp))
                Text(
                    text = "Chào mừng tới MinLish",
                    fontSize = if (isSmallScreen) 22.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 12.dp))
                Text(
                    text = "Phương pháp Spaced Repetition (SM-2) kết hợp trò chơi hóa giúp bạn học từ vựng tiếng Anh không bao giờ quên.",
                    fontSize = if (isSmallScreen) 13.sp else 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Phần giữa: Minh họa chú trâu vàng vẫy chào vui nhộn (Hello)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(illustrationSize + 20.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                // Hào quang tỏa sáng phía sau chú trâu vàng
                Box(
                    modifier = Modifier
                        .size(illustrationSize - 20.dp)
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
                        .size(illustrationSize)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            // Phần dưới: Các nút hành động đăng nhập/khách
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = bottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onNavigate(Screen.Login) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    )
                ) {
                    RedWhiteEmailIcon()
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Đăng nhập",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        if (errorMessage.isNotEmpty()) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            errorMessage = ""
        }
    }
}
