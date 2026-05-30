package com.example.minlishapp.data.remote

import com.example.minlishapp.data.StatsDashboardResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface StatsApiService {
    @GET("api/stats/dashboard")
    suspend fun getStatsDashboard(
        @Header("x-user-id") userId: String
    ): StatsDashboardResponse
}
