package com.example.minlishapp.data.remote

import com.example.minlishapp.data.*
import retrofit2.Response
import retrofit2.http.*

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

    @GET("api/decks/{id}/export")
    suspend fun exportDeck(@Path("id") deckId: String): Response<DeckExportJson>

    @POST("api/decks/import")
    suspend fun importDeck(@Body request: DeckExportJson): Response<ImportDeckResponse>

    @GET("api/import-export/export/{id}/csv")
    suspend fun exportDeckCsv(@Path("id") deckId: String): Response<okhttp3.ResponseBody>

    @POST("api/import-export/import/csv")
    suspend fun importDeckCsv(@Body request: CsvImportRequest): Response<ImportMultipleDecksResponse>

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
