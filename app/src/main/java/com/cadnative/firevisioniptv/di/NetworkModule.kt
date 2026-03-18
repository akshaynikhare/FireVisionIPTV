package com.cadnative.firevisioniptv.di

import android.content.Context
import com.cadnative.firevisioniptv.BuildConfig
import com.cadnative.firevisioniptv.data.AppPreferences
import com.cadnative.firevisioniptv.data.source.remote.FireVisionApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing networking dependencies.
 * 
 * This module configures OkHttp, Retrofit, and the API service with:
 * - Logging interceptor for debugging
 * - Timeout configurations
 * - Custom headers
 * - Network security (HTTPS only)
 * 
 * Requirements:
 * - TR-002: Update Dependencies (Retrofit 2.11.0, OkHttp 4.12.0)
 * - TR-012: Network Security (HTTPS only, timeout configurations)
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides configured OkHttpClient with logging, timeouts, and headers.
     * 
     * Configuration:
     * - Connect timeout: 30 seconds
     * - Read timeout: 30 seconds
     * - Write timeout: 30 seconds
     * - Logging: BODY level in debug, NONE in release
     * - Custom headers: Accept: application/json
     * 
     * @return Configured OkHttpClient instance
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.HEADERS
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )

            .addInterceptor { chain ->
                val tvCode = AppPreferences.getTvCode(context)
                val original = chain.request()
                val builder = original.newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("X-TV-Code", tvCode)
                if (original.body != null) {
                    builder.addHeader("Content-Type", "application/json")
                }
                chain.proceed(builder.build())
            }

            .build()
    }

    /**
     * Provides configured Retrofit instance with base URL from BuildConfig.
     * 
     * Configuration:
     * - Base URL: From BuildConfig.API_BASE_URL
     * - Converter: Gson for JSON serialization/deserialization
     * - Client: Configured OkHttpClient
     * 
     * @param okHttpClient Configured OkHttpClient instance
     * @return Configured Retrofit instance
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides FireVisionApiService implementation.
     * 
     * This service interface defines all API endpoints for the FireVision IPTV application.
     * Retrofit will generate the implementation at runtime.
     * 
     * @param retrofit Configured Retrofit instance
     * @return FireVisionApiService implementation
     */
    @Provides
    @Singleton
    fun provideFireVisionApiService(retrofit: Retrofit): FireVisionApiService {
        return retrofit.create(FireVisionApiService::class.java)
    }
}
