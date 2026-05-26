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
