package com.example.minlishapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.minlishapp.data.repository.AuthRepository
import com.example.minlishapp.data.LoginRequest
import com.example.minlishapp.data.LoginResponse
import com.example.minlishapp.data.RegisterRequest
import com.example.minlishapp.data.GoogleLoginRequest
import com.example.minlishapp.data.TokenManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (userId: String, email: String, displayName: String, targetGoal: String, xp: Int, level: Int, streak: Int) -> Unit,
    onNavigate: (Screen) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoginTab by remember { mutableStateOf(true) }
    
    // Form fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // Password visibility toggle
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    
    // Validation Error Message
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onNavigate(Screen.Welcome) }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }

            // Logo & Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
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
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Login Logo",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isLoginTab) "Chào mừng trở lại!" else "Tạo tài khoản mới",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isLoginTab) "Đăng nhập để tiếp tục lộ trình học" else "Đăng ký nhanh chóng chỉ với vài bước",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Tabs Đăng nhập / Đăng ký
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(true to "Đăng nhập", false to "Đăng ký").forEach { (tabState, label) ->
                    val isSelected = isLoginTab == tabState
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surface
                                else Color.Transparent
                            )
                            .clickable {
                                isLoginTab = tabState
                                errorMessage = "" // clear errors
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Inputs Form
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = "" },
                    label = { Text("Địa chỉ Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = "" },
                    label = { Text("Mật khẩu") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        val image = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isLoginTab) ImeAction.Done else ImeAction.Next
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Confirm Password (đăng ký)
                AnimatedVisibility(visible = !isLoginTab) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = "" },
                        label = { Text("Xác nhận mật khẩu") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            val image = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = null)
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Error display
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
            }

            // Submit Button
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Vui lòng nhập đầy đủ thông tin!"
                    } else if (!email.contains("@") || !email.contains(".")) {
                        errorMessage = "Địa chỉ email không hợp lệ!"
                    } else if (password.length < 6) {
                        errorMessage = "Mật khẩu phải chứa ít nhất 6 ký tự!"
                    } else if (!isLoginTab && password != confirmPassword) {
                        errorMessage = "Mật khẩu xác nhận không khớp!"
                    } else {
                        if (isLoginTab) {
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val response = authRepository.login(LoginRequest(email, password))
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
                                            Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                                            onNavigate(Screen.Dashboard)
                                        } else {
                                            errorMessage = body?.message ?: "Đăng nhập thất bại!"
                                        }
                                    } else {
                                        val errorJson = response.errorBody()?.string()
                                        val errorMsg = try {
                                            com.google.gson.Gson().fromJson(errorJson, LoginResponse::class.java)?.message
                                        } catch (jsonEx: Exception) {
                                            null
                                        }
                                        errorMessage = errorMsg ?: "Đăng nhập thất bại (Mã lỗi: ${response.code()})"
                                    }
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = "Lỗi kết nối: ${e.localizedMessage ?: "Không thể kết nối tới máy chủ"}"
                                }
                            }
                        } else {
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                                    val response = authRepository.register(
                                        com.example.minlishapp.data.RegisterRequest(email, password, displayName, "IELTS")
                                    )
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
                                                profile?.displayName ?: displayName,
                                                profile?.targetGoal ?: "IELTS",
                                                profile?.xp ?: 0,
                                                profile?.level ?: 1,
                                                profile?.streak ?: 0
                                            )
                                            Toast.makeText(context, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                                            onNavigate(Screen.Dashboard)
                                        } else {
                                            errorMessage = body?.message ?: "Đăng ký thất bại!"
                                        }
                                    } else {
                                        val errorJson = response.errorBody()?.string()
                                        val errorMsg = try {
                                            com.google.gson.Gson().fromJson(errorJson, LoginResponse::class.java)?.message
                                        } catch (jsonEx: Exception) {
                                            null
                                        }
                                        errorMessage = errorMsg ?: "Đăng ký thất bại (Mã lỗi: ${response.code()})"
                                    }
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = "Lỗi kết nối: ${e.localizedMessage ?: "Không thể kết nối tới máy chủ"}"
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isLoginTab) "Đăng nhập" else "Đăng ký",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Google Login Button
            if (isLoginTab) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    )
                    Text(
                        text = "hoặc",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    )
                }

                Button(
                    onClick = {
                        isLoading = true
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleAuthLauncher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA4335),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFEA4335)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Đăng nhập với Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

        }
    }


}
