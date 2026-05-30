package com.example.minlishapp.data

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
