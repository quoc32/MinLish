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
import com.example.minlishapp.data.remote.LearningApiService

/**
 * Repository wrapping LearningApiService for convenient access
 */
class LearningRepository(private val apiService: LearningApiService) {

    suspend fun getDailyPlan(): Response<DailyPlanResponse> =
        apiService.getDailyPlan()

    suspend fun submitReview(cardId: String, quality: String): Response<ReviewResponse> =
        apiService.submitReview(ReviewRequest(cardId = cardId, quality = quality))

    companion object {
        private const val DEFAULT_BASE_URL = "http://10.0.2.2:3000/"
        private val BASE_URL = BuildConfig.API_BACKEND_URL?.let {
            if (it.endsWith("/")) it else "$it/"
        } ?: DEFAULT_BASE_URL

        fun create(context: Context): LearningRepository {
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

            val apiService = retrofit.create(LearningApiService::class.java)
            return LearningRepository(apiService)
        }
    }
}
