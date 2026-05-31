package com.example.minlishapp.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minlishapp.core.network.TokenManager
import com.example.minlishapp.data.*
import com.example.minlishapp.data.repository.AuthRepository
import kotlinx.coroutines.launch
import retrofit2.Response

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.create(application)

    // Form inputs and UI States managed inside ViewModel
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var isLoginTab by mutableStateOf(true)
    var isPasswordVisible by mutableStateOf(false)
    var isConfirmPasswordVisible by mutableStateOf(false)
    var errorMessage by mutableStateOf("")

    fun performLogin(
        onSuccess: (userId: String, email: String, displayName: String, targetGoal: String, xp: Int, level: Int, streak: Int) -> Unit
    ) {
        if (!validateInputs()) return

        isLoading = true
        errorMessage = ""

        viewModelScope.launch {
            try {
                val response = authRepository.login(LoginRequest(email, password))
                isLoading = false
                handleAuthResponse(response, "Đăng nhập thất bại!", onSuccess)
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Lỗi kết nối: ${e.localizedMessage ?: "Không thể kết nối tới máy chủ"}"
            }
        }
    }

    fun performRegister(
        onSuccess: (userId: String, email: String, displayName: String, targetGoal: String, xp: Int, level: Int, streak: Int) -> Unit
    ) {
        if (!validateInputs()) return

        isLoading = true
        errorMessage = ""

        viewModelScope.launch {
            try {
                val displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                val response = authRepository.register(RegisterRequest(email, password, displayName, "IELTS"))
                isLoading = false
                handleAuthResponse(response, "Đăng ký thất bại!") { userId, email, dispName, goal, xp, level, streak ->
                    onSuccess(userId, email, dispName, goal, xp, level, streak)
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Lỗi kết nối: ${e.localizedMessage ?: "Không thể kết nối tới máy chủ"}"
            }
        }
    }

    fun performGoogleLogin(
        idToken: String,
        onSuccess: (userId: String, email: String, displayName: String, targetGoal: String, xp: Int, level: Int, streak: Int) -> Unit
    ) {
        isLoading = true
        errorMessage = ""

        viewModelScope.launch {
            try {
                val response = authRepository.loginWithGoogle(GoogleLoginRequest(idToken))
                isLoading = false
                handleAuthResponse(response, "Đăng nhập Google thất bại!", onSuccess)
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Lỗi kết nối: ${e.localizedMessage ?: "Không thể kết nối tới máy chủ"}"
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Vui lòng nhập đầy đủ thông tin!"
            return false
        }
        if (!email.contains("@") || !email.contains(".")) {
            errorMessage = "Địa chỉ email không hợp lệ!"
            return false
        }
        if (password.length < 6) {
            errorMessage = "Mật khẩu phải chứa ít nhất 6 ký tự!"
            return false
        }
        if (!isLoginTab && password != confirmPassword) {
            errorMessage = "Mật khẩu xác nhận không khớp!"
            return false
        }
        return true
    }

    private fun handleAuthResponse(
        response: Response<LoginResponse>,
        defaultErrorMsg: String,
        onSuccess: (userId: String, email: String, displayName: String, targetGoal: String, xp: Int, level: Int, streak: Int) -> Unit
    ) {
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success && body.data != null) {
                val profile = body.data.profile
                val userId = body.data.user.id
                val userEmail = body.data.user.email
                body.data.session?.accessToken?.let { TokenManager.getInstance(getApplication()).saveToken(it) }

                onSuccess(
                    userId,
                    userEmail,
                    profile?.displayName ?: "Học viên",
                    profile?.targetGoal ?: "IELTS",
                    profile?.xp ?: 0,
                    profile?.level ?: 1,
                    profile?.streak ?: 0
                )
            } else {
                errorMessage = body?.message ?: defaultErrorMsg
            }
        } else {
            val errorJson = response.errorBody()?.string()
            val errorMsg = try {
                com.google.gson.Gson().fromJson(errorJson, LoginResponse::class.java)?.message
            } catch (jsonEx: Exception) {
                null
            }
            errorMessage = errorMsg ?: "$defaultErrorMsg (Mã lỗi: ${response.code()})"
        }
    }
}
