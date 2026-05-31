package com.example.minlishapp.data

data class ProfileStats(
    val xp: Int,
    val level: Int,
    val streak: Int,
    @com.google.gson.annotations.SerializedName("max_streak") val maxStreak: Int,
    @com.google.gson.annotations.SerializedName("target_goal") val targetGoal: String,
    @com.google.gson.annotations.SerializedName("retention_rate") val retentionRate: Double,
    @com.google.gson.annotations.SerializedName("display_name") val displayName: String? = null,
    @com.google.gson.annotations.SerializedName("words_per_day") val wordsPerDay: Int = 20,
    val email: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserData(
    val id: String,
    val email: String
)

data class SessionData(
    @com.google.gson.annotations.SerializedName("access_token") val accessToken: String
)

data class LoginDataResponse(
    val user: UserData,
    val profile: ProfileStats?,
    val session: SessionData? = null
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val data: LoginDataResponse?
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val targetGoal: String,
    val wordsPerDay: Int = 20
)

data class GoogleLoginRequest(
    val idToken: String
)

data class ProfileResponse(
    val success: Boolean,
    val data: ProfileStats?,
    val message: String?
)

data class ProfileUpdateRequest(
    @com.google.gson.annotations.SerializedName("display_name") val displayName: String? = null,
    @com.google.gson.annotations.SerializedName("target_goal") val targetGoal: String? = null,
    @com.google.gson.annotations.SerializedName("words_per_day") val wordsPerDay: Int? = null
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val password: String
)

data class SimpleResponse(
    val success: Boolean,
    val message: String
)
