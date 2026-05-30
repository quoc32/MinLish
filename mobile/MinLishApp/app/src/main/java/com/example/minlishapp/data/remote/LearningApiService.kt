package com.example.minlishapp.data.remote

import com.example.minlishapp.data.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface LearningApiService {
    @GET("api/learning/daily")
    suspend fun getDailyPlan(): Response<DailyPlanResponse>

    @POST("api/learning/review")
    suspend fun submitReview(@Body request: ReviewRequest): Response<ReviewResponse>
}
