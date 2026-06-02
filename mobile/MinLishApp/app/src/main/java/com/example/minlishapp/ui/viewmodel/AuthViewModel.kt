package com.example.minlishapp.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minlishapp.core.network.TokenManager
import com.example.minlishapp.data.*
import com.example.minlishapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository.create(application)
    private val tokenManager = TokenManager.getInstance(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _isLoginTab = MutableStateFlow(true)
    val isLoginTab: StateFlow<Boolean> = _isLoginTab.asStateFlow()

    // Forgot Password States
    private val _showForgotPasswordDialog = MutableStateFlow(false)
    val showForgotPasswordDialog: StateFlow<Boolean> = _showForgotPasswordDialog.asStateFlow()

    private val _forgotPasswordEmail = MutableStateFlow("")
    val forgotPasswordEmail: StateFlow<String> = _forgotPasswordEmail.asStateFlow()

    private val _isSendingForgotPassword = MutableStateFlow(false)
    val isSendingForgotPassword: StateFlow<Boolean> = _isSendingForgotPassword.asStateFlow()

    private val _forgotPasswordSuccessMessage = MutableStateFlow("")
    val forgotPasswordSuccessMessage: StateFlow<String> = _forgotPasswordSuccessMessage.asStateFlow()

    private val _forgotPasswordErrorMessage = MutableStateFlow("")
    val forgotPasswordErrorMessage: StateFlow<String> = _forgotPasswordErrorMessage.asStateFlow()

    fun toggleLoginTab() {
        _isLoginTab.value = !_isLoginTab.value
        _errorMessage.value = ""
    }

    fun setIsLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    fun setShowForgotPasswordDialog(show: Boolean) {
        _showForgotPasswordDialog.value = show
        if (!show) {
            _forgotPasswordEmail.value = ""
            _forgotPasswordSuccessMessage.value = ""
            _forgotPasswordErrorMessage.value = ""
        }
    }

    fun setForgotPasswordEmail(email: String) {
        _forgotPasswordEmail.value = email
        _forgotPasswordErrorMessage.value = ""
    }

    fun setForgotPasswordErrorMessage(message: String) {
        _forgotPasswordErrorMessage.value = message
    }

    fun login(request: LoginRequest, onResult: (Boolean, LoginResponse?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""
            try {
                val response = authRepository.login(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()
                    body?.data?.session?.accessToken?.let { tokenManager.saveToken(it) }
                    onResult(true, body)
                } else {
                    val errorJson = response.errorBody()?.string()
                    val errorBody = try {
                        com.google.gson.Gson().fromJson(errorJson, LoginResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }
                    val msg = errorBody?.message ?: response.body()?.message ?: "Đăng nhập thất bại (Mã lỗi: ${response.code()})"
                    _errorMessage.value = msg
                    onResult(false, null)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login error", e)
                _errorMessage.value = e.localizedMessage ?: "Lỗi kết nối"
                onResult(false, null)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(request: RegisterRequest, onResult: (Boolean, LoginResponse?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""
            try {
                val response = authRepository.register(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()
                    body?.data?.session?.accessToken?.let { tokenManager.saveToken(it) }
                    onResult(true, body)
                } else {
                    val errorJson = response.errorBody()?.string()
                    val errorBody = try {
                        com.google.gson.Gson().fromJson(errorJson, LoginResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }
                    val msg = errorBody?.message ?: response.body()?.message ?: "Đăng ký thất bại (Mã lỗi: ${response.code()})"
                    _errorMessage.value = msg
                    onResult(false, null)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Register error", e)
                _errorMessage.value = e.localizedMessage ?: "Lỗi kết nối"
                onResult(false, null)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loginWithGoogle(idToken: String, onResult: (Boolean, LoginResponse?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""
            try {
                val response = authRepository.loginWithGoogle(GoogleLoginRequest(idToken))
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()
                    body?.data?.session?.accessToken?.let { tokenManager.saveToken(it) }
                    onResult(true, body)
                } else {
                    val msg = response.body()?.message ?: "Đăng nhập Google thất bại (Mã lỗi: ${response.code()})"
                    _errorMessage.value = msg
                    onResult(false, null)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google login error", e)
                _errorMessage.value = e.localizedMessage ?: "Lỗi kết nối"
                onResult(false, null)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _isSendingForgotPassword.value = true
            _forgotPasswordErrorMessage.value = ""
            _forgotPasswordSuccessMessage.value = ""
            try {
                val response = authRepository.forgotPassword(ForgotPasswordRequest(email))
                if (response.isSuccessful && response.body()?.success == true) {
                    _forgotPasswordSuccessMessage.value = "Đã gửi liên kết khôi phục tới email của bạn!"
                } else {
                    _forgotPasswordErrorMessage.value = response.body()?.message ?: "Gửi email khôi phục thất bại!"
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Forgot password error", e)
                _forgotPasswordErrorMessage.value = e.localizedMessage ?: "Lỗi kết nối"
            } finally {
                _isSendingForgotPassword.value = false
            }
        }
    }

    fun resetPassword(password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""
            try {
                val response = authRepository.resetPassword(ResetPasswordRequest(password))
                if (response.isSuccessful && response.body()?.success == true) {
                    tokenManager.clearToken()
                    onResult(true, "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.")
                } else {
                    val msg = response.body()?.message ?: "Đổi mật khẩu thất bại!"
                    _errorMessage.value = msg
                    onResult(false, msg)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Reset password error", e)
                _errorMessage.value = e.localizedMessage ?: "Lỗi kết nối"
                onResult(false, e.localizedMessage ?: "Lỗi kết nối")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
