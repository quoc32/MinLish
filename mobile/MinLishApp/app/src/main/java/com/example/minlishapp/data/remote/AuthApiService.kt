package com.example.minlishapp.data.remote

import com.example.minlishapp.data.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<LoginResponse>

    @GET("api/auth/profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): Response<ProfileResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<SimpleResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<SimpleResponse>
}
