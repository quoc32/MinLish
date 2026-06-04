package com.example.minlishapp.data.repository

import android.content.Context
import com.example.minlishapp.BuildConfig
import com.example.minlishapp.data.*
import com.example.minlishapp.data.remote.TutorApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class TutorRepository(private val apiService: TutorApiService) {

    suspend fun sendMessage(request: TutorChatRequest): Response<TutorChatResponse> =
        apiService.sendMessage(request)

    suspend fun resetChat(request: TutorResetRequest): Response<TutorResetResponse> =
        apiService.resetChat(request)

    companion object {
        private const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"
        private val BASE_URL = BuildConfig.API_AI_SERVICE_URL?.let {
            if (it.endsWith("/")) it else "$it/"
        } ?: DEFAULT_BASE_URL

        fun create(context: Context): TutorRepository {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return TutorRepository(retrofit.create(TutorApiService::class.java))
        }
    }
}
