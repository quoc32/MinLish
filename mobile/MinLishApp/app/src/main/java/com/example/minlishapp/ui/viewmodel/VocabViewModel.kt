package com.example.minlishapp.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minlishapp.data.*
import com.example.minlishapp.data.repository.DeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VocabViewModel(application: Application) : AndroidViewModel(application) {
    private val deckRepository = DeckRepository.create(application)

    private val _decks = MutableStateFlow<List<Deck>>(emptyList())
    val decks: StateFlow<List<Deck>> = _decks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun fetchDecks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = deckRepository.getAllDecks()
                if (response.isSuccessful && response.body()?.success == true) {
                    val apiDecks = response.body()!!.data
                    if (!apiDecks.isNullOrEmpty()) {
                        val convertedDecks = apiDecks.map { apiDeck ->
                            Deck(
                                id = apiDeck.id,
                                name = apiDeck.name,
                                description = apiDeck.tag ?: "",
                                tags = listOfNotNull(apiDeck.tag),
                                apiProgress = apiDeck.progress.toFloat(),
                                apiWordCount = apiDeck.totalWords
                            )
                        }
                        _decks.value = convertedDecks
                    } else {
                        _decks.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("VocabViewModel", "Failed to fetch decks: ${e.message}")
            }
            _isLoading.value = false
        }
    }

    fun createDeck(name: String, tag: String?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = deckRepository.createDeck(CreateDeckRequest(name = name, tag = tag))
                if (response.isSuccessful && response.body()?.success == true) {
                    fetchDecks()
                    onResult(true, "Deck created successfully")
                } else {
                    onResult(false, response.body()?.message ?: "Failed to create deck")
                }
            } catch (e: Exception) {
                Log.e("VocabViewModel", "Failed to create deck: ${e.message}")
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }

    fun updateDeck(deckId: String, name: String, tag: String?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = deckRepository.updateDeck(deckId, CreateDeckRequest(name = name, tag = tag))
                if (response.isSuccessful && response.body()?.success == true) {
                    fetchDecks()
                    onResult(true, "Deck updated successfully")
                } else {
                    onResult(false, response.body()?.message ?: "Failed to update deck")
                }
            } catch (e: Exception) {
                Log.e("VocabViewModel", "Failed to update deck: ${e.message}")
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }

    fun deleteDeck(deckId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = deckRepository.deleteDeck(deckId)
                if (response.isSuccessful && response.body()?.success == true) {
                    fetchDecks()
                    onResult(true, "Deck deleted successfully")
                } else {
                    onResult(false, response.body()?.message ?: "Failed to delete deck")
                }
            } catch (e: Exception) {
                Log.e("VocabViewModel", "Failed to delete deck: ${e.message}")
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }

    fun fetchDeckCards(deckId: String, onComplete: (List<Word>) -> Unit) {
        viewModelScope.launch {
            try {
                val response = deckRepository.getDeckCards(deckId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val apiCards = response.body()!!.data
                    if (!apiCards.isNullOrEmpty()) {
                        val words = apiCards.map { card ->
                            Word(
                                id = card.id,
                                word = card.word,
                                pronunciation = card.pronunciation,
                                meaning = card.meaning,
                                description = card.descriptionEn ?: "",
                                example = card.example ?: "",
                                wordType = "noun",
                                easeFactor = card.progress?.easeFactor ?: 2.5,
                                repetitions = card.progress?.repetitions ?: 0,
                                intervalDays = card.progress?.interval ?: 0
                            )
                        }
                        
                        val currentDecks = _decks.value.toMutableList()
                        val idx = currentDecks.indexOfFirst { it.id == deckId }
                        if (idx != -1) {
                            currentDecks[idx] = currentDecks[idx].copy(words = words)
                            _decks.value = currentDecks
                        }
                        onComplete(words)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e("VocabViewModel", "Failed to fetch deck cards: ${e.message}")
            }
            onComplete(emptyList())
        }
    }

    fun createCard(deckId: String, word: Word, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = deckRepository.createCard(
                    CreateCardRequest(
                        deckId = deckId,
                        word = word.word,
                        pronunciation = word.pronunciation,
                        meaning = word.meaning,
                        descriptionEn = word.description,
                        example = word.example
                    )
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    // Refresh cards
                    fetchDecks() // Or just update local state
                    onResult(true, "Card created successfully")
                } else {
                    onResult(false, response.body()?.message ?: "Failed to create card")
                }
            } catch (e: Exception) {
                Log.e("VocabViewModel", "Failed to create card: ${e.message}")
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }

    fun updateCard(cardId: String, deckId: String, word: Word, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = deckRepository.updateCard(
                    cardId,
                    CreateCardRequest(
                        deckId = deckId,
                        word = word.word,
                        pronunciation = word.pronunciation,
                        meaning = word.meaning,
                        descriptionEn = word.description,
                        example = word.example
                    )
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    onResult(true, "Card updated successfully")
                } else {
                    onResult(false, response.body()?.message ?: "Failed to update card")
                }
            } catch (e: Exception) {
                Log.e("VocabViewModel", "Failed to update card: ${e.message}")
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }

    fun deleteCard(cardId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = deckRepository.deleteCard(cardId)
                if (response.isSuccessful && response.body()?.success == true) {
                    onResult(true, "Card deleted successfully")
                } else {
                    onResult(false, response.body()?.message ?: "Failed to delete card")
                }
            } catch (e: Exception) {
                Log.e("VocabViewModel", "Failed to delete card: ${e.message}")
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }

    // Export/Import
    fun exportDeck(deckId: String, onResult: (Boolean, DeckExportJson?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = deckRepository.exportDeck(deckId)
                if (response.isSuccessful && response.body() != null) {
                    onResult(true, response.body())
                } else {
                    onResult(false, null)
                }
            } catch (e: Exception) {
                Log.e("VocabViewModel", "Failed to export deck: ${e.message}")
                onResult(false, null)
            }
        }
    }

    fun importDeck(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileContent = inputStream?.bufferedReader().use { it?.readText() }
                
                if (fileContent != null) {
                    // Detect format: CSV or JSON
                    val trimmed = fileContent.trimStart()
                    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                        // JSON format
                        val gson = com.google.gson.Gson()
                        val exportJson = gson.fromJson(fileContent, DeckExportJson::class.java)
                        
                        val response = deckRepository.importDeck(exportJson)
                        if (response.isSuccessful && response.body()?.success == true) {
                            fetchDecks()
                            onResult(true, response.body()?.message ?: "Deck imported successfully")
                        } else {
                            onResult(false, response.body()?.message ?: "Failed to import deck")
                        }
                    } else {
                        // CSV format — ask for deck name via a default
                        val lines = fileContent.lines().filter { it.isNotBlank() }
                        if (lines.size < 2) {
                            onResult(false, "CSV file must have a header and at least one data row")
                            return@launch
                        }
                        val deckName = "Imported CSV ${java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}"
                        val response = deckRepository.importDeckCsv(CsvImportRequest(deckName = deckName, csvData = fileContent))
                        if (response.isSuccessful && response.body()?.success == true) {
                            fetchDecks()
                            onResult(true, response.body()?.message ?: "CSV imported successfully")
                        } else {
                            onResult(false, response.body()?.message ?: "Failed to import CSV")
                        }
                    }
                } else {
                    onResult(false, "Failed to read file")
                }
            } catch (e: Exception) {
                Log.e("VocabViewModel", "Failed to import deck: ${e.message}")
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }

    fun exportDeckCsv(deckId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = deckRepository.exportDeckCsv(deckId)
                if (response.isSuccessful && response.body() != null) {
                    val csvString = response.body()!!.string()
                    onResult(true, csvString)
                } else {
                    onResult(false, null)
                }
            } catch (e: Exception) {
                Log.e("VocabViewModel", "Failed to export deck CSV: ${e.message}")
                onResult(false, null)
            }
        }
    }
}
