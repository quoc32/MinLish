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
    val id: String = "",
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
    var words: List<Word> = emptyList(),
    var apiProgress: Float? = null,
    var apiWordCount: Int? = null
) {
    val progress: Float
        get() = apiProgress ?: if (words.isEmpty()) 0f else {
            val learned = words.count { it.repetitions > 0 }
            learned.toFloat() / words.size
        }

    val wordCount: Int
        get() = apiWordCount ?: words.size
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
    var appLanguage: String = "English",
    var email: String = ""
)

data class ProfileStats(
    val xp: Int,
    val level: Int,
    val streak: Int,
    @com.google.gson.annotations.SerializedName("max_streak") val maxStreak: Int,
    @com.google.gson.annotations.SerializedName("target_goal") val targetGoal: String,
    @com.google.gson.annotations.SerializedName("retention_rate") val retentionRate: Double,
    @com.google.gson.annotations.SerializedName("display_name") val displayName: String? = null,
    val email: String? = null
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
    val displayName: String? = null,
    val targetGoal: String? = null,
    val wordsPerDay: Int? = null
)

// ============================================================
// Deck API Models
// ============================================================

data class DeckApiItem(
    val id: String,
    val name: String,
    val icon: String?,
    val tag: String?,
    @com.google.gson.annotations.SerializedName("total_words") val totalWords: Int,
    @com.google.gson.annotations.SerializedName("order_index") val orderIndex: Int,
    @com.google.gson.annotations.SerializedName("created_at") val createdAt: String? = null,
    val progress: Double = 0.0,
    @com.google.gson.annotations.SerializedName("is_unlocked") val isUnlocked: Boolean = true
)

data class DeckListResponse(
    val success: Boolean,
    val data: List<DeckApiItem>?,
    val message: String? = null
)

data class CreateDeckRequest(
    val name: String,
    val icon: String? = "📚",
    val tag: String? = null
)

data class CreateDeckResponse(
    val success: Boolean,
    val message: String?,
    val data: DeckApiItem?
)

// ============================================================
// Card API Models
// ============================================================

data class CardApiItem(
    val id: String,
    @com.google.gson.annotations.SerializedName("deck_id") val deckId: String,
    val word: String,
    val pronunciation: String,
    val meaning: String,
    @com.google.gson.annotations.SerializedName("description_en") val descriptionEn: String? = null,
    val example: String? = null,
    val progress: CardProgress? = null
)

data class CardProgress(
    val status: String,
    @com.google.gson.annotations.SerializedName("ease_factor") val easeFactor: Double,
    val interval: Int,
    val repetitions: Int,
    @com.google.gson.annotations.SerializedName("next_review") val nextReview: String
)

data class CardListResponse(
    val success: Boolean,
    val deckName: String? = null,
    val data: List<CardApiItem>?,
    val message: String? = null
)

data class CreateCardRequest(
    val deckId: String,
    val word: String,
    val pronunciation: String = "",
    val meaning: String,
    val descriptionEn: String? = null,
    val example: String? = null
)

data class CreateCardResponse(
    val success: Boolean,
    val message: String?,
    val data: CardApiItem?
)

// ============================================================
// Learning API Models (Daily Plan + Review)
// ============================================================

data class DailyPlanData(
    val wordsPerDay: Int,
    val newCardsCount: Int,
    val reviewCardsCount: Int,
    val inSessionReviewCount: Int,
    val totalNewCardsAvailable: Int = 0,
    val newCards: List<CardApiItem>,
    val reviewCards: List<CardApiItem>,
    val inSessionReviewCards: List<CardApiItem>
)

data class DailyPlanResponse(
    val success: Boolean,
    val data: DailyPlanData?,
    val message: String? = null
)

data class ReviewRequest(
    val cardId: String,
    val quality: String // "again", "hard", "good", "easy"
)

data class Sm2Result(
    @com.google.gson.annotations.SerializedName("ease_factor") val easeFactor: Double,
    val interval: Int,
    @com.google.gson.annotations.SerializedName("interval_minutes") val intervalMinutes: Int = 0,
    val repetitions: Int,
    val status: String,
    @com.google.gson.annotations.SerializedName("next_review") val nextReview: String
)

data class ReviewRewards(
    val xpGained: Int,
    val xpTotal: Int,
    val levelUp: Boolean,
    val level: Int,
    val streak: Int,
    val retentionRate: Int
)

data class DeckProgressResult(
    val deckId: String,
    val progress: Double,
    val completed: Boolean,
    val nextDeckUnlocked: Boolean,
    val nextDeckName: String? = null
)

data class ReviewData(
    val cardId: String,
    val word: String,
    val quality: String,
    val sm2: Sm2Result,
    val rewards: ReviewRewards,
    val deckProgress: DeckProgressResult
)

data class ReviewResponse(
    val success: Boolean,
    val message: String?,
    val data: ReviewData?
)

// ============================================================
// Export / Import API Models
// ============================================================

data class DeckExportJson(
    val export_date: String,
    val app_version: String,
    val format_version: String,
    val data: DeckExportData
)

data class DeckExportData(
    val id: String,
    val name: String,
    val tag: String?,
    val cards: List<CardExportData>
)

data class CardExportData(
    val word: String,
    val pronunciation: String,
    val meaning: String,
    @com.google.gson.annotations.SerializedName("description_en") val descriptionEn: String?,
    val example: String?
)

data class ImportDeckResponse(
    val success: Boolean,
    val message: String?,
    val data: DeckApiItem?
)

data class CsvImportRequest(
    val deckName: String,
    val csvData: String
)
