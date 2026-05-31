package com.example.minlishapp.data

data class CardApiItem(
    val id: String,
    @com.google.gson.annotations.SerializedName("deck_id") val deckId: String,
    val word: String,
    val pronunciation: String,
    val meaning: String,
    @com.google.gson.annotations.SerializedName("description_en") val descriptionEn: String? = null,
    val example: String? = null,
    @com.google.gson.annotations.SerializedName("word_type") val wordType: String? = null,
    val collocation: String? = null,
    @com.google.gson.annotations.SerializedName("related_words") val relatedWords: String? = null,
    val note: String? = null,
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
    val example: String? = null,
    val wordType: String? = null,
    val collocation: String? = null,
    val relatedWords: String? = null,
    val note: String? = null
)

data class CreateCardResponse(
    val success: Boolean,
    val message: String?,
    val data: CardApiItem?
)
