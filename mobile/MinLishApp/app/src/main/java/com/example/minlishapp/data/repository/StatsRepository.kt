package com.example.minlishapp.data.repository

import com.example.minlishapp.data.StatsDashboardResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header

interface StatsApiService {
    @GET("api/stats/dashboard")
    suspend fun getStatsDashboard(
        @Header("x-user-id") userId: String
    ): StatsDashboardResponse
}

class StatsRepository(private val apiService: StatsApiService) {

    suspend fun getStatsDashboard(userId: String): StatsDashboardResponse {
        return apiService.getStatsDashboard(userId)
    }

    companion object {
        private const val BASE_URL = "http://10.0.2.2:3000/" // Android Emulator loopback to localhost of host machine

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
