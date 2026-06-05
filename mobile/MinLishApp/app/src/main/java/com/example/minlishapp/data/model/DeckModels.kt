package com.example.minlishapp.data

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

// Response wrapper from GET /api/import-export/export/:deckId
data class DeckExportResponse(
    val success: Boolean,
    val data: DeckExportPayload?
)

// The actual export payload inside { data: ... }
data class DeckExportPayload(
    val deck: DeckExportDeckInfo,
    val cards: List<CardExportData>
)

data class DeckExportDeckInfo(
    val name: String,
    val icon: String?,
    val tag: String?,
    @com.google.gson.annotations.SerializedName("target_goal") val targetGoal: String?
)

// Used for writing the export file (what we actually save to disk)
data class DeckExportJson(
    val deck: DeckExportDeckInfo,
    val cards: List<CardExportData>
)

data class CardExportData(
    val word: String = "",
    val pronunciation: String = "",
    val meaning: String = "",
    @com.google.gson.annotations.SerializedName("description_en") val descriptionEn: String? = null,
    val example: String? = null,
    @com.google.gson.annotations.SerializedName("word_type") val wordType: String? = null,
    val collocation: String? = null,
    @com.google.gson.annotations.SerializedName("related_words") val relatedWords: String? = null,
    val note: String? = null
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

data class ImportMultipleDecksResponse(
    val success: Boolean,
    val message: String?,
    val data: List<DeckApiItem>?
)
