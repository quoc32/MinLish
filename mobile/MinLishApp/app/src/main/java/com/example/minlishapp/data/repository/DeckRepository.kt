package com.example.minlishapp.data.repository

import android.content.Context
import com.example.minlishapp.BuildConfig
import com.example.minlishapp.data.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

/**
 * Retrofit API interface for Deck and Card endpoints
 */
interface DeckApiService {
    // === Deck endpoints ===
    @GET("api/decks")
    suspend fun getAllDecks(): Response<DeckListResponse>

    @GET("api/decks/{id}/cards")
    suspend fun getDeckCards(@Path("id") deckId: String): Response<CardListResponse>

    @POST("api/decks")
    suspend fun createDeck(@Body request: CreateDeckRequest): Response<CreateDeckResponse>

    @PUT("api/decks/{id}")
    suspend fun updateDeck(
        @Path("id") deckId: String,
        @Body request: CreateDeckRequest
    ): Response<CreateDeckResponse>

    @DELETE("api/decks/{id}")
    suspend fun deleteDeck(@Path("id") deckId: String): Response<CreateDeckResponse>

    // === Card endpoints ===
    @POST("api/cards")
    suspend fun createCard(@Body request: CreateCardRequest): Response<CreateCardResponse>

    @PUT("api/cards/{id}")
    suspend fun updateCard(
        @Path("id") cardId: String,
        @Body request: CreateCardRequest
    ): Response<CreateCardResponse>

    @DELETE("api/cards/{id}")
    suspend fun deleteCard(@Path("id") cardId: String): Response<CreateCardResponse>
}

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
