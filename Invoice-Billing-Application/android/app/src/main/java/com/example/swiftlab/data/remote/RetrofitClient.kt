package com.example.swiftlab.data.remote

import com.example.swiftlab.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClient(private val sessionManager: SessionManager) {

    private var currentBaseUrl: String = sessionManager.baseUrl
    private var cachedService: ApiService? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        sessionManager.token?.let { token ->
            if (token.isNotBlank()) {
                builder.addHeader("Authorization", "Bearer $token")
            }
        }

        builder.addHeader("Accept", "application/json")
        builder.addHeader("X-Device-Id", sessionManager.deviceId)

        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun getService(): ApiService {
        val configuredUrl = sessionManager.baseUrl
        if (cachedService != null && configuredUrl == currentBaseUrl) {
            return cachedService!!
        }

        currentBaseUrl = configuredUrl
        val retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)
        cachedService = service
        return service
    }
}
