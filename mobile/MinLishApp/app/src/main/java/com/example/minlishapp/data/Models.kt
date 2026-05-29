package com.example.minlishapp.data

data class Word(
    val word: String,
    val pronunciation: String,
    val meaning: String,
    val description: String = "",
    val example: String = "",
    val collocations: List<String> = emptyList(),
    val relatedWords: List<String> = emptyList(),
    val note: String = "",
    val wordType: String = "verb",
    val pronunciationUs: String = "",
    val exampleTranslation: String = "",
    val synonyms: List<String> = emptyList(),
    // SM-2 Spaced Repetition Parameters
    var easeFactor: Double = 2.5,
    var repetitions: Int = 0,
    var intervalDays: Int = 0,
    var nextReviewTime: Long = System.currentTimeMillis()
)

data class Deck(
    val id: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    var words: List<Word> = emptyList()
) {
    val progress: Float
        get() = if (words.isEmpty()) 0f else {
            val learned = words.count { it.repetitions > 0 }
            learned.toFloat() / words.size
        }
}

data class UserProgress(
    var userId: String = "",
    var name: String = "Learner",
    var targetGoal: String = "IELTS",
    var wordsPerDay: Int = 20,
    var streak: Int = 15,
    var xp: Int = 1248,
    var level: Int = 12,
    var accuracy: Int = 95,
    var learnedLanguage: String = "English",
    var appLanguage: String = "English"
)

data class ProfileStats(
    val xp: Int,
    val level: Int,
    val streak: Int,
    @com.google.gson.annotations.SerializedName("max_streak") val maxStreak: Int,
    @com.google.gson.annotations.SerializedName("target_goal") val targetGoal: String,
    @com.google.gson.annotations.SerializedName("retention_rate") val retentionRate: Double,
    @com.google.gson.annotations.SerializedName("display_name") val displayName: String? = null
)

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

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserData(
    val id: String,
    val email: String
)

data class LoginDataResponse(
    val user: UserData,
    val profile: ProfileStats?
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
    val targetGoal: String
)

