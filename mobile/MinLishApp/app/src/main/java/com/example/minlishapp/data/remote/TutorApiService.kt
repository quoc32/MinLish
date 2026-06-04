package com.example.minlishapp.data.remote

import com.example.minlishapp.data.*
import retrofit2.Response
import retrofit2.http.*

interface TutorApiService {

    @POST("api/tutor/chat")
    suspend fun sendMessage(@Body request: TutorChatRequest): Response<TutorChatResponse>

    @POST("api/tutor/reset")
    suspend fun resetChat(@Body request: TutorResetRequest): Response<TutorResetResponse>

    @GET("health")
    suspend fun healthCheck(): Response<Map<String, String>>
}
