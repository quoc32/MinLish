package com.example.minlishapp.data

data class DonutChartData(
    val learning: Int,
    val proficient: Int,
    @com.google.gson.annotations.SerializedName("needs_review") val needsReview: Int,
    @com.google.gson.annotations.SerializedName("total_studied") val totalStudied: Int
)

data class BarChartData(
    val date: String,
    val label: String,
    @com.google.gson.annotations.SerializedName("words_count") val wordsCount: Int
)

data class DashboardData(
    val profile: ProfileStats,
    val donutChart: DonutChartData,
    val barChart: List<BarChartData>
)

data class StatsDashboardResponse(
    val success: Boolean,
    val data: DashboardData?,
    val message: String?
)
