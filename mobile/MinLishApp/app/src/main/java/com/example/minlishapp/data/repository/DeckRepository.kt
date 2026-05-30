package com.example.minlishapp.data.repository

import android.content.Context
import com.example.minlishapp.BuildConfig
import com.example.minlishapp.data.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.minlishapp.core.network.AuthInterceptor
import com.example.minlishapp.data.remote.DeckApiService

/**
 * Repository wrapping DeckApiService for convenient access
 */
class DeckRepository(private val apiService: DeckApiService) {

    // === Deck operations ===
    suspend fun getAllDecks(): Response<DeckListResponse> =
        apiService.getAllDecks()

    suspend fun getDeckCards(deckId: String): Response<CardListResponse> =
        apiService.getDeckCards(deckId)

    suspend fun createDeck(request: CreateDeckRequest): Response<CreateDeckResponse> =
        apiService.createDeck(request)

    suspend fun updateDeck(deckId: String, request: CreateDeckRequest): Response<CreateDeckResponse> =
        apiService.updateDeck(deckId, request)

    suspend fun deleteDeck(deckId: String): Response<CreateDeckResponse> =
        apiService.deleteDeck(deckId)

    suspend fun exportDeck(deckId: String): Response<DeckExportJson> =
        apiService.exportDeck(deckId)

    suspend fun importDeck(request: DeckExportJson): Response<ImportDeckResponse> =
        apiService.importDeck(request)

    suspend fun exportDeckCsv(deckId: String): Response<okhttp3.ResponseBody> =
        apiService.exportDeckCsv(deckId)

    suspend fun importDeckCsv(request: CsvImportRequest): Response<ImportMultipleDecksResponse> =
        apiService.importDeckCsv(request)


    // === Card operations ===
    suspend fun createCard(request: CreateCardRequest): Response<CreateCardResponse> =
        apiService.createCard(request)

    suspend fun updateCard(cardId: String, request: CreateCardRequest): Response<CreateCardResponse> =
        apiService.updateCard(cardId, request)

    suspend fun deleteCard(cardId: String): Response<CreateCardResponse> =
        apiService.deleteCard(cardId)

    companion object {
        private const val DEFAULT_BASE_URL = "http://10.0.2.2:3000/"
        private val BASE_URL = BuildConfig.API_BACKEND_URL?.let {
            if (it.endsWith("/")) it else "$it/"
        } ?: DEFAULT_BASE_URL

        fun create(context: Context): DeckRepository {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = AuthInterceptor(context)

            val client = OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(DeckApiService::class.java)
            return DeckRepository(apiService)
        }
    }
}
