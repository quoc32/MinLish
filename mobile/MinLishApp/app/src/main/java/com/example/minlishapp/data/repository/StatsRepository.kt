package com.example.minlishapp.data.repository

import com.example.minlishapp.data.StatsDashboardResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.minlishapp.data.remote.StatsApiService
import com.example.minlishapp.BuildConfig

class StatsRepository(private val apiService: StatsApiService) {

    suspend fun getStatsDashboard(userId: String): StatsDashboardResponse {
        return apiService.getStatsDashboard(userId)
    }

    companion object {
        private const val DEFAULT_BASE_URL = "http://10.0.2.2:3000/" // Android emulator loopback to host localhost
        private val BASE_URL = BuildConfig.API_BACKEND_URL?.let {
            if (it.endsWith("/")) it else "$it/"
        } ?: DEFAULT_BASE_URL

        fun create(): StatsRepository {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(StatsApiService::class.java)
            return StatsRepository(apiService)
        }
    }
}
