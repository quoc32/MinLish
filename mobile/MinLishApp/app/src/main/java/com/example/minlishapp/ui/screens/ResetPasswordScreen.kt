package com.example.minlishapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minlishapp.core.network.TokenManager
import com.example.minlishapp.data.ResetPasswordRequest
import com.example.minlishapp.data.repository.AuthRepository
import com.example.minlishapp.core.utils.translated
import kotlinx.coroutines.launch

@Composable
fun ResetPasswordScreen(
    onNavigate: (Screen) -> Unit,
    appLanguage: String = "Vietnamese"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository.create(context) }
    val tokenManager = remember { TokenManager.getInstance(context) }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Đặt lại mật khẩu mới".translated(appLanguage),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Nhập mật khẩu mới của bạn bên dưới để khôi phục quyền truy cập vào tài khoản.".translated(appLanguage),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = "" },
                label = { Text("Mật khẩu mới".translated(appLanguage)) },
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
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = "" },
                label = { Text("Xác nhận mật khẩu mới".translated(appLanguage)) },
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
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Button(
                onClick = {
                    if (password.isBlank() || confirmPassword.isBlank()) {
                        errorMessage = "Vui lòng điền đầy đủ mật khẩu!".translated(appLanguage)
                    } else if (password.length < 6) {
                        errorMessage = "Mật khẩu phải chứa ít nhất 6 ký tự!".translated(appLanguage)
                    } else if (password != confirmPassword) {
                        errorMessage = "Mật khẩu xác nhận không khớp!".translated(appLanguage)
                    } else {
                        isLoading = true
                        errorMessage = ""
                        coroutineScope.launch {
                            try {
                                val response = authRepository.resetPassword(ResetPasswordRequest(password))
                                isLoading = false
                                if (response.isSuccessful && response.body()?.success == true) {
                                    Toast.makeText(context, "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.".translated(appLanguage), Toast.LENGTH_LONG).show()
                                    tokenManager.clearToken()
                                    onNavigate(Screen.Login)
                                } else {
                                    errorMessage = response.body()?.message ?: "Đổi mật khẩu thất bại!".translated(appLanguage)
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "Lỗi kết nối".translated(appLanguage) + ": ${e.localizedMessage}"
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
                        text = "Cập nhật mật khẩu".translated(appLanguage),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TextButton(
                onClick = {
                    tokenManager.clearToken()
                    onNavigate(Screen.Login)
                }
            ) {
                Text("Quay lại đăng nhập".translated(appLanguage))
            }
        }
    }
}
