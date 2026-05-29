package com.example.minlishapp.data.repository

import com.example.minlishapp.data.LoginRequest
import com.example.minlishapp.data.LoginResponse
import com.example.minlishapp.data.RegisterRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<LoginResponse>
}

class AuthRepository(private val apiService: AuthApiService) {

    suspend fun login(request: LoginRequest): Response<LoginResponse> {
        return apiService.login(request)
    }

    suspend fun register(request: RegisterRequest): Response<LoginResponse> {
        return apiService.register(request)
    }

    companion object {
        private const val BASE_URL = "http://10.0.2.2:3000/" // Android emulator loopback to host localhost

        fun create(): AuthRepository {
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

            val apiService = retrofit.create(AuthApiService::class.java)
            return AuthRepository(apiService)
        }
    }
}
