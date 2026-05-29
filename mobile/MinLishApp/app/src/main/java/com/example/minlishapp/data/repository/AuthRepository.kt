package com.example.minlishapp.data.repository

import android.content.Context
import com.example.minlishapp.BuildConfig
import com.example.minlishapp.data.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<LoginResponse>

    @GET("api/auth/profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): Response<ProfileResponse>
}

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val requestBuilder = chain.request().newBuilder()
        val token = TokenManager.getInstance(context).getToken()
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
    }
}

class AuthRepository(private val apiService: AuthApiService) {

    suspend fun login(request: LoginRequest): Response<LoginResponse> = apiService.login(request)
    
    suspend fun register(request: RegisterRequest): Response<LoginResponse> = apiService.register(request)

    suspend fun loginWithGoogle(request: GoogleLoginRequest): Response<LoginResponse> = apiService.loginWithGoogle(request)

    suspend fun getProfile(): Response<ProfileResponse> = apiService.getProfile()

    suspend fun updateProfile(request: ProfileUpdateRequest): Response<ProfileResponse> = apiService.updateProfile(request)

    companion object {
        private const val DEFAULT_BASE_URL = "http://10.0.2.2:3000/" // Android emulator loopback to host localhost
        private val BASE_URL = BuildConfig.API_BACKEND_URL?.let {
            if (it.endsWith("/")) it else "$it/"
        } ?: DEFAULT_BASE_URL

        fun create(context: Context): AuthRepository {
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

            val apiService = retrofit.create(AuthApiService::class.java)
            return AuthRepository(apiService)
        }
    }
}
